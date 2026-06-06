plugins {

    `java-library`
    id("com.gradleup.shadow") version "9.4.2"

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

    // Legacy Paper repository.
    maven("https://repo.papermc.io/repository/maven-snapshots/")

}

dependencies {

    // Get versions.
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra
    val bstatsVersion: String by rootProject.extra

    // Dependencies.
    implementation("org.bstats:bstats-bukkit:$bstatsVersion")
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.bukkit:$lampVersion")

    compileOnly("com.github.retrooper:packetevents-spigot:$packetEventsVersion")
    compileOnly("org.github.paperspigot:paperspigot-api:1.8.8-R0.1-SNAPSHOT")

    // Core implementation.
    implementation(project(":core"))

}

tasks.processResources {

    // Get versions.
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace plugin.yml
    filesMatching("plugin.yml") {
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

    archiveBaseName.set("CoralGate-Paper")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate Netty for AsyncHTTPClient.
    relocate("io.netty", "cloud.gteam.coralgate.libs.netty")

    // Relocate bStats.
    relocate("org.bstats", "cloud.gteam.coralgate.libs.bstats")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

}

tasks.compileJava {
    dependsOn(project(":core").tasks.named("jar"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
