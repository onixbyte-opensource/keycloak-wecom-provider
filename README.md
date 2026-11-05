# keycloak-wecom-provider

A [WeCom](https://work.weixin.qq.com/) (WeChat Work) social identity provider for
[Keycloak](https://www.keycloak.org/) 26.8 and later.

It adds a 'WeCom' identity provider to Keycloak so that members of your WeCom
organisation can sign in to any Keycloak realm with a QR-code scan – no extra
middleware, no custom themes.

## Features

- QR-code login through the WeCom single sign-on page (`login_type=CorpApp`).
- Corporate members only: the callback exchanges the authorisation code for a
  userid via `auth/getuserinfo`, so only verified members of your organisation
  can federate into the realm.
- Built-in admin console fields for Corp ID, Agent ID and App Secret – no
  hand-editing of the provider config JSON required.
- In-memory caching of the WeCom app access token with a five-minute expiry
  margin, so sign-ins do not hammer the `gettoken` endpoint.

## Requirements

| Dependency | Version |
| ---------- | ------- |
| Keycloak | 26.8+ (Quarkus distribution) |
| Java | 17 (bytecode target; run on Keycloak's bundled Java 21 runtime) |
| WeCom | A self-built application (自建应用) with its Corp ID, Agent ID and Secret |

## Installation

### From a release

1. Download `keycloak-wecom-provider-<version>.jar` (and the matching
   `.sha256` file) from the
   [releases page](https://github.com/onixbyte-opensource/keycloak-wecom-provider/releases).
2. Verify the download:

   ```bash
   sha256sum -c keycloak-wecom-provider-<version>.jar.sha256
   ```

3. Copy the JAR into Keycloak's providers directory:

   ```bash
   cp keycloak-wecom-provider-<version>.jar $KEYCLOAK_HOME/providers/
   ```

4. Rebuild and restart Keycloak:

   ```bash
   $KEYCLOAK_HOME/bin/kc.sh build
   $KEYCLOAK_HOME/bin/kc.sh start
   ```

### From source

```bash
./gradlew clean build
cp build/libs/keycloak-wecom-provider-<version>.jar $KEYCLOAK_HOME/providers/
```

The build emits both `build/libs/keycloak-wecom-provider-<version>.jar` and a
`sha256sum`-compatible checksum file next to it.

## Configuration

In the Keycloak Admin Console, go to **Identity Providers → Add provider →
WeCom (企业微信)** and fill in:

| Field | Description | Example |
| ----- | ----------- | ------- |
| Corp ID | The `corpId` of your WeCom organisation | `ww000000000000000000` |
| Agent ID | The agent id of your self-built application | `100000` |
| App Secret | The secret of your self-built application | *(kept masked in the console)* |

Then configure the callback in the WeCom admin console:

1. Set the **authorised callback domain** (可信域名 / 授权回调域) to your
   Keycloak hostname.
2. Use the following redirect URI on the Keycloak side (Keycloak sets this up
   automatically when the provider is created, but it must be reachable and
   match the domain authorised in WeCom):

   ```
   https://<keycloak-host>/realms/<realm>/broker/wecom/endpoint
   ```

## Compatibility notes

- Keycloak 26.8 runs on RESTEasy Reactive; Classic is no longer available.
  This provider ships an empty `META-INF/beans.xml` as required by the
  [Keycloak upgrade guide](https://www.keycloak.org/docs/latest/upgrading/index.html),
  so the callback sub-resource is recognised at runtime. If you fork this
  project, keep that file in the JAR – without it the callback fails with
  `405 Method Not Allowed`.
- Scopes are intentionally empty: the WeCom SSO page does not use OAuth2
  scopes.
- Only `login_type=CorpApp` is supported; external (non-member) users will be
  rejected at the code-exchange step.

## Licence

Licensed under the [Apache License 2.0](LICENCE).
