plugins {
    id("java")
}

group = "com.github.officialastramc"
version = "2.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.1")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("pluginVersion" to project.version)
        }
        filteringCharset = "UTF-8"
        expand(project.properties)
    }
    compileJava {
        options.encoding = "UTF-8"
    }
}