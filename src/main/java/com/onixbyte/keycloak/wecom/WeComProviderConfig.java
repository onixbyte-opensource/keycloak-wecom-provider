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

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

/**
 * Reads the WeCom-specific keys (corpId / agentId / secret) that
 * {@link WeComIdentityProviderFactory} exposes in the admin console.
 *
 * <p>Do not delegate to {@code getClientId()} / {@code getClientSecret()}:
 * those read the standard OAuth2 keys ("clientId" / "clientSecret"), which
 * are never populated for this provider. A null corpId used to surface as
 * "Value is null" when building the authorisation URL.
 */
public class WeComProviderConfig extends OAuth2IdentityProviderConfig {

    public static final String AGENT_ID = "agentId";
    public static final String CORP_ID = "corpId";
    public static final String SECRET = "secret";

    public WeComProviderConfig() {
        super();
    }

    public WeComProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getCorpId() {
        return getConfig().get(CORP_ID);
    }

    public String getAgentId() {
        return getConfig().get(AGENT_ID);
    }

    public String getSecret() {
        return getConfig().get(SECRET);
    }
}
