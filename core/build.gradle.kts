import org.gradle.kotlin.dsl.shadowJar

plugins {

    `java-library`
    id("com.gradleup.shadow") version "9.4.2"

}

group = "cloud.gteam.coralgate"
version = rootProject.extra["coreVersion"]!!

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

}

repositories {

    mavenCentral()

    // PacketEvents repository.
    maven("https://repo.codemc.io/repository/maven-releases/")

}

dependencies {

    // Get versions.
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra

    // Dependencies.
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("blue.endless:jankson:1.2.3")
    compileOnly("com.google.code.gson:gson:2.14.0")
    implementation("org.asynchttpclient:async-http-client:2.16.0")
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("dev.dejvokep:boosted-yaml:1.3.7")

    compileOnly("com.github.retrooper:packetevents-api:$packetEventsVersion")

}

tasks.processResources {

    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"

    filesNotMatching("**/*.png") {
        expand(props)
    }

}

tasks.shadowJar {

    archiveClassifier.set("") // produce core.jar instead of core-all.jar

    relocate("dev.dejvokep.boostedyaml", "cloud.gteam.coralgate.libs.boostedyaml")

}

tasks.build {
    dependsOn(tasks.shadowJar)
}
