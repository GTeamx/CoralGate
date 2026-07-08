plugins {

    `java-library`
    id("com.gradleup.shadow") version "9.5.1"

}

java {

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

}

repositories {

    mavenCentral()
    mavenLocal()

    // PacketEvents repository.
    maven("https://repo.codemc.io/repository/maven-releases/")

    // BungeeCord repository.
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")

}

dependencies {

    // Get versions.
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra
    val bstatsVersion: String by rootProject.extra

    // Dependencies.
    implementation("org.bstats:bstats-bungeecord:$bstatsVersion")
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.bungee:$lampVersion")

    compileOnly("com.github.retrooper:packetevents-bungeecord:$packetEventsVersion")
    compileOnly("net.md-5:bungeecord-api:1.16-R0.4")

    // Core implementation.
    implementation(project(path = ":core", configuration = "shadow"))

}

tasks.processResources {

    // Get versions.
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace bungee.yml
    filesMatching("bungee.yml") {
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
    dependsOn(project(":core").tasks.named("shadowJar"))

    archiveBaseName.set("CoralGate-Bungeecord")
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
