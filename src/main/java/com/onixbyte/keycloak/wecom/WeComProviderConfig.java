package com.onixbyte.keycloak.wecom;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

public class WeComProviderConfig extends OAuth2IdentityProviderConfig {

    public static final String AGENT_ID = "agentId";

    public WeComProviderConfig() {
        super();
    }

    public WeComProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getCorpId() {
        return getClientId();
    }

    public String getAgentId() {
        return getConfig().get(AGENT_ID);
    }

    public String getSecret() {
        return getClientSecret();
    }
}
