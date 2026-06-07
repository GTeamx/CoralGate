plugins {

    `java-library`
    id("com.gradleup.shadow") version "9.4.2"

}

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

}

repositories {

    mavenCentral()
    mavenLocal()

    // PacketEvents repository.
    maven("https://repo.codemc.io/repository/maven-releases/")

    // Velocity repository.
    maven("https://repo.papermc.io/repository/maven-public/")

}

dependencies {

    // Get versions.
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra
    val bstatsVersion: String by rootProject.extra

    // Dependencies.
    implementation("org.bstats:bstats-velocity:$bstatsVersion")
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.velocity:$lampVersion")
    implementation("io.github.revxrsal:lamp.brigadier:$lampVersion")

    compileOnly("com.github.retrooper:packetevents-velocity:$packetEventsVersion")
    compileOnly("com.velocitypowered:velocity-api:3.4.0")

    // Core implementation.
    implementation(project(":core"))

}

tasks.processResources {

    // Get versions.
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace plugin.yml
    filesMatching("velocity-plugin.json") {
        expand("version" to project.version)
    }

    // Replace properties.
    filesMatching("platform.properties") {

        expand(
            "name" to project.name,
            "version" to project.version,
            "coreVersion" to coreVersion,
            "packeteventsVersion" to packetEventsVersion
        )

    }

}

tasks.shadowJar {

    // Wait for the core shadowJar to finish.
    mustRunAfter(project(":core").tasks.named("shadowJar"))

    archiveBaseName.set("CoralGate-Velocity")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate bStats.
    relocate("org.bstats", "cloud.gteam.coralgate.libs.bstats")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

}

tasks.build {
    dependsOn(tasks.shadowJar)
}
