/*
 * Copyright 2026 Onixbyte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.onixbyte.keycloak.wecom;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class WeComIdentityProvider extends AbstractOAuth2IdentityProvider<WeComProviderConfig>
        implements SocialIdentityProvider<WeComProviderConfig> {

    private static final Logger LOG = Logger.getLogger(WeComIdentityProvider.class);

    public static final String AUTH_URL = "https://login.work.weixin.qq.com/wwlogin/sso/login";
    static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";
    static final String GET_USER_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/get";

    private static final String LOGIN_TYPE_CORP_APP = "CorpApp";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long TOKEN_EXPIRY_MARGIN_SECONDS = 300;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private static final ObjectMapper JSON = new ObjectMapper();
    // Tokens are cached per corpId+secret pair. The provider instance is
    // recreated for every login, so the cache must be static to persist
    // across requests. The expiry margin keeps us from handing out a token
    // that WeCom may already have expired.
    private static final Map<String, CachedToken> TOKEN_CACHE = new ConcurrentHashMap<>();

    public WeComIdentityProvider(KeycloakSession session, WeComProviderConfig config) {
        super(session, config);
    }

    @Override
    protected String getDefaultScopes() {
        // WeCom's SSO page does not use OAuth2 scopes; returning empty
        // prevents the OAuth2 base class from appending a scope parameter
        // that WeCom would reject.
        return "";
    }

    @Override
    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        // login_type=CorpApp selects QR-code login for a self-built app;
        // appid/agentid identify it, and the redirect URI must match the
        // domain authorised in the WeCom admin console.
        return UriBuilder.fromUri(AUTH_URL)
                .queryParam("login_type", LOGIN_TYPE_CORP_APP)
                .queryParam("appid", getConfig().getCorpId())
                .queryParam("agentid", getConfig().getAgentId())
                .queryParam("redirect_uri", request.getRedirectUri())
                .queryParam("state", request.getState().getEncoded());
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        // IdentityBrokerService's sub-resource locator hands the callback
        // request to whatever object is returned here, so it must be a
        // class carrying JAX-RS annotations. The JAR must also ship an
        // (empty) META-INF/beans.xml: RESTEasy Reactive ignores unregistered
        // sub-resource classes and replies 405 Method Not Allowed.
        return new WeComEndpoint(this, callback, event, session);
    }

    @Override
    public BrokeredIdentityContext getFederatedIdentity(String code) {
        String accessToken = getAppAccessToken();
        String userId = exchangeCodeForUserId(accessToken, code);
        JsonNode profile = fetchUserProfile(accessToken, userId);
        return buildIdentity(userId, profile);
    }

    private String getAppAccessToken() {
        String corpId = requireConfig(getConfig().getCorpId(), "Client ID (Corp ID)");
        String secret = requireConfig(getConfig().getSecret(), "Client Secret (app secret)");
        String cacheKey = corpId + '\u0000' + secret;

        long now = Instant.now().getEpochSecond();
        CachedToken cached = TOKEN_CACHE.get(cacheKey);
        if (cached != null && cached.expiresAt() > now) {
            return cached.token();
        }

        JsonNode response = getJson(GET_TOKEN_URL
                + "?corpid=" + encode(corpId)
                + "&corpsecret=" + encode(secret));
        String token = text(response, "access_token");
        if (token == null || token.isBlank()) {
            throw new IdentityBrokerException("wecom: gettoken returned no access_token");
        }

        long expiresIn = response.path("expires_in").asLong(7200L);
        long ttl = Math.max(expiresIn - TOKEN_EXPIRY_MARGIN_SECONDS, 60L);
        TOKEN_CACHE.put(cacheKey, new CachedToken(token, now + ttl));
        LOG.debugf("Fetched WeCom app access token for corp %s, expires in %d seconds", corpId, expiresIn);
        return token;
    }

    private String exchangeCodeForUserId(String accessToken, String code) {
        JsonNode response = getJson(GET_USER_INFO_URL
                + "?access_token=" + encode(accessToken)
                + "&code=" + encode(code));
        String userId = text(response, "userid");
        if (userId == null || userId.isBlank()) {
            throw new IdentityBrokerException(
                    "wecom: code exchange returned no userid; only corporate members are supported with login_type=CorpApp");
        }
        return userId;
    }

    private JsonNode fetchUserProfile(String accessToken, String userId) {
        return getJson(GET_USER_URL
                + "?access_token=" + encode(accessToken)
                + "&userid=" + encode(userId));
    }

    private BrokeredIdentityContext buildIdentity(String userId, JsonNode profile) {
        String name = text(profile, "name");
        String email = text(profile, "email");

        BrokeredIdentityContext identity = new BrokeredIdentityContext(userId, getConfig());
        identity.setId(userId);
        identity.setBrokerUserId(userId);
        identity.setUsername(userId);
        identity.setModelUsername(userId);
        identity.setFirstName(name);
        identity.setEmail(email == null || email.isBlank() ? null : email);
        AbstractJsonUserAttributeMapper.storeUserProfileForMapper(identity, profile, getConfig().getAlias());

        LOG.debugf("Authenticated WeCom user %s", userId);
        return identity;
    }

    private JsonNode getJson(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IdentityBrokerException("wecom: request failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdentityBrokerException("wecom: request interrupted", e);
        }

        if (response.statusCode() != 200) {
            throw new IdentityBrokerException("wecom: HTTP " + response.statusCode());
        }

        JsonNode json;
        try {
            json = JSON.readTree(response.body());
        } catch (IOException e) {
            throw new IdentityBrokerException("wecom: invalid JSON response", e);
        }

        // WeCom reports API failures with HTTP 200 + a non-zero errcode, so
        // the status code alone is not enough to detect an error.
        int errcode = json.path("errcode").asInt(0);
        if (errcode != 0) {
            throw new IdentityBrokerException("wecom: " + errcode + " " + json.path("errmsg").asText(""));
        }
        return json;
    }

    private static String requireConfig(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IdentityBrokerException("wecom: missing required configuration '" + name + "'");
        }
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record CachedToken(String token, long expiresAt) {
    }
}
