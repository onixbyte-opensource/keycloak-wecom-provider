plugins {
    java
}

group = "com.onixbyte.keycloak"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.keycloak.core)
    implementation(libs.keycloak.private)
    implementation(libs.keycloak.services)
    implementation(libs.jackson.databind)
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release = 17
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.jar {
    archiveBaseName = "keycloak-wecom-provider"
}
