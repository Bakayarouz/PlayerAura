plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1"
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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks {
    build {
        dependsOn("reobfJar")
    }
}
