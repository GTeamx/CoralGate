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

    // Spigot repository.
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")

    // bungeecord-chat repository.
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")

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
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")

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

    archiveBaseName.set("CoralGate-Spigot")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate bStats.
    relocate("org.bstats", "cloud.gteam.coralgate.libs.bstats")

    // Relocate netty.
    relocate("io.netty", "cloud.gteam.coralgate.libs.netty")

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
