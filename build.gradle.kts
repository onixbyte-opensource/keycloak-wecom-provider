import java.security.MessageDigest

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
    doLast {
        val jarFile = archiveFile.get().asFile
        val digest = MessageDigest.getInstance("SHA-256")
        jarFile.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        File(jarFile.parentFile, "${jarFile.name}.sha256").writeText("$hash  ${jarFile.name}\n")
    }
}
