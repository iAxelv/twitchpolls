import org.gradle.api.file.DuplicatesStrategy

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.foxy"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("com.github.twitch4j:twitch4j:1.19.0")
    implementation("com.github.jwdeveloper.TikTok-Live-Java:Client:1.11.17-Release")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("1.21.8")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to project.version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveBaseName.set("StreamManager")
        archiveClassifier.set("")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/versions/**/module-info.class")
        exclude("module-info.class")

        relocate("com.fasterxml.jackson", "com.foxy.streammanager.libs.jackson")
        relocate("com.github.twitch4j", "com.foxy.streammanager.libs.twitch4j")
        relocate("feign", "com.foxy.streammanager.libs.feign")
        relocate("io.github.xanthic", "com.foxy.streammanager.libs.xanthic")
        relocate("io.github.jwdeveloper.tiktok", "com.foxy.streammanager.libs.tiktok")
    }

    build {
        dependsOn(shadowJar)
    }
}