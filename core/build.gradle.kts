plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.1"
}

group = "cloud.gteam.coralgate"
version = rootProject.extra["coreVersion"]!!

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()

    // PacketEvents repo
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra

    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("org.spongepowered:configurate-hocon:4.2.0")

    compileOnly("com.github.retrooper:packetevents-api:$packetEventsVersion")

    compileOnly("com.google.code.gson:gson:2.14.0")

    implementation("blue.endless:jankson:1.2.3")
    implementation("org.asynchttpclient:async-http-client:3.0.10")
    implementation("org.jetbrains:annotations:26.1.0")
}

// -------------------------
// ⭐ Shadow (Shade) Config
// -------------------------
tasks.shadowJar {
    archiveClassifier.set("") // produce core.jar instead of core-all.jar
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
