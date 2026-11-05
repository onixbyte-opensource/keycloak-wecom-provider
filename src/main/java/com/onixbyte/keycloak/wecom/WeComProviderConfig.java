package com.onixbyte.keycloak.wecom;

import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;

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
