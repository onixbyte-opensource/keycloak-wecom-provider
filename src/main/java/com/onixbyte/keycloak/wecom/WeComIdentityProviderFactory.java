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

import java.util.List;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class WeComIdentityProviderFactory extends AbstractIdentityProviderFactory<WeComIdentityProvider>
        implements SocialIdentityProviderFactory<WeComIdentityProvider> {

    public static final String PROVIDER_ID = "wecom";

    @Override
    public String getName() {
        return "企业微信";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public WeComIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new WeComIdentityProvider(session, new WeComProviderConfig(model));
    }

    @Override
    public WeComProviderConfig createConfig() {
        return new WeComProviderConfig();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // Property names must match the keys read by WeComProviderConfig's
        // getters: Keycloak stores the console values under these exact
        // keys, and a mismatch silently resolves to null.
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(WeComProviderConfig.CORP_ID)
                    .label("Corp ID")
                    .helpText("企业微信的企业 ID（corpid）。")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .required(true)
                    .add()
                .property()
                    .name(WeComProviderConfig.AGENT_ID)
                    .label("Agent ID")
                    .helpText("企业微信自建应用的 AgentId。")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .required(true)
                    .add()
                .property()
                    .name(WeComProviderConfig.SECRET)
                    .label("App Secret")
                    .helpText("企业微信自建应用的 Secret。")
                    .type(ProviderConfigProperty.PASSWORD)
                    .required(true)
                    .add()
                .build();
    }
}
