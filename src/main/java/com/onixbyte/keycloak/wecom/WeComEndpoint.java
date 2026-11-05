package com.onixbyte.keycloak.wecom;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.UserAuthenticationIdentityProvider.AuthenticationCallback;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

public class WeComEndpoint {

    private static final Logger LOG = Logger.getLogger(WeComEndpoint.class);

    private final WeComIdentityProvider provider;
    private final AuthenticationCallback callback;
    private final EventBuilder event;
    private final KeycloakSession session;

    public WeComEndpoint(
            WeComIdentityProvider provider,
            AuthenticationCallback callback,
            EventBuilder event,
            KeycloakSession session
    ) {
        this.provider = provider;
        this.callback = callback;
        this.event = event;
        this.session = session;
    }

    @GET
    public Response authResponse(
            @QueryParam("state") String state,
            @QueryParam("code") String code,
            @QueryParam("error") String error,
            @QueryParam("error_description") String errorDescription
    ) {
        if (state == null) {
            return errorPage(Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_MISSING_STATE_ERROR);
        }

        try {
            AuthenticationSessionModel authSession = callback.getAndVerifyAuthenticationSession(state);
            session.getContext().setAuthenticationSession(authSession);

            if (error != null) {
                LOG.warnf("WeCom login returned an error: %s (%s)", error, errorDescription);
                if (AbstractOAuth2IdentityProvider.ACCESS_DENIED.equals(error)) {
                    return callback.cancelled(provider.getConfig());
                }
                return callback.error(provider.getConfig(), Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }

            if (code == null) {
                return errorPage(Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_MISSING_CODE_OR_ERROR_ERROR);
            }

            BrokeredIdentityContext identity = provider.getFederatedIdentity(code);
            identity.setIdp(provider);
            identity.setAuthenticationSession(authSession);
            return callback.authenticated(identity);
        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (IdentityBrokerException e) {
            LOG.error("WeCom login callback failed", e);
            return errorPage(Response.Status.BAD_GATEWAY, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        } catch (Exception e) {
            LOG.error("WeCom login callback failed", e);
            return errorPage(Response.Status.INTERNAL_SERVER_ERROR, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
        }
    }

    private Response errorPage(Response.Status status, String message) {
        event.event(EventType.IDENTITY_PROVIDER_LOGIN);
        event.error(Errors.IDENTITY_PROVIDER_LOGIN_FAILURE);
        return ErrorPage.error(session, null, status, message);
    }
}
