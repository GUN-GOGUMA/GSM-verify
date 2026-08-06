plugins {
    java
}

group = "dev.gungoguma"
version = "1.0.0"

val pluginVersion = version.toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to pluginVersion)
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
