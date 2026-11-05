# Contributing to keycloak-wecom-provider

Thank you for considering a contribution! This document explains how to set up
a development environment, build and verify the provider, and submit changes.

## Reporting issues

- Open an issue on the
  [issue tracker](https://github.com/onixbyte-opensource/keycloak-wecom-provider/issues).
- For bugs, please include:
  - Keycloak version and how it is deployed (bare metal, Docker, Kubernetes…);
  - the relevant Keycloak server log excerpt (`KC-SERVICES0047` registration
    lines and any stack trace);
  - the provider configuration (with secrets redacted).

## Development environment

| Requirement | Notes |
| ----------- | ----- |
| JDK 17 | The Gradle toolchain is pinned to Java 17 (`gradle.properties`); any JDK 17+ installation that the toolchain can use is fine |
| Git | Any recent version |

No additional setup is needed – the Gradle wrapper downloads Gradle itself.

## Building

```bash
./gradlew clean build
```

On success you will find:

- `build/libs/keycloak-wecom-provider-<version>.jar` – the deployable provider;
- `build/libs/keycloak-wecom-provider-<version>.jar.sha256` – a checksum file
  in `sha256sum -c` format.

### Verifying the artefact

Before deploying (or publishing) a build, please check:

1. The JAR contains the SPI registration file
   `META-INF/services/org.keycloak.broker.social.SocialIdentityProviderFactory`
   whose content is exactly `com.onixbyte.keycloak.wecom.WeComIdentityProviderFactory`;
2. The JAR contains `META-INF/beans.xml` – Keycloak 26.8+ (RESTEasy Reactive)
   will not recognise the callback sub-resource without it;
3. The bytecode target is Java 17 (class file major version 61).

## Code guidelines

- **Never hard-code secrets.** All credentials must come from the identity
  provider configuration.
- **Keep config keys aligned.** `WeComProviderConfig` getters must read the
  same keys that `WeComIdentityProviderFactory#getConfigProperties` exposes
  (`corpId`, `agentId`, `secret`). A mismatch silently yields `null` values.
- Keep the provider compatible with the Keycloak 26.8 SPI surface
  (`UserAuthenticationIdentityProvider`, Jakarta namespace).
- Keep Java source at language level 17.

## Commit messages

We use conventional-style commit messages:

```text
<type>(<scope>): <subject>

<body>
```

`type` is one of `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`,
`chore`, `build`, `ci`. Keep the subject in the imperative mood and under 50
characters, for example:

```text
fix(wecom): align config keys with provider config map
```

## Submitting changes

1. Fork the repository and create a topic branch:

   ```bash
   git switch -c fix/short-description
   ```

2. Make your changes and ensure `./gradlew clean build` passes.
3. Commit using the message format above.
4. Push to your fork and open a pull request against `main`, describing what
   changed and why. Link any related issues.

## Licensing

By contributing, you agree that your contributions will be licensed under the
[Apache License 2.0](LICENSE) that covers this project.
