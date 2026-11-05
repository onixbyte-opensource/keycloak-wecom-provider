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
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(WeComProviderConfig.AGENT_ID)
                    .label("Agent ID")
                    .helpText("企业微信自建应用的 AgentId。")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .required(true)
                    .add()
                .build();
    }
}
