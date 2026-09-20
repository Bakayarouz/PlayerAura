plugins {
    java
}

group = "com.auraplugin"
version = "1.0.0"
description = "High-performance ItemDisplay Aura Plugin"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    // Paper API dependency (works across all 1.21.x sub-versions)
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
