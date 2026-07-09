plugins {

    `java-library`
    alias(libs.plugins.shadow)

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

    // Dependencies.
    implementation(libs.bstats.bukkit)
    implementation(libs.lamp.common)
    implementation(libs.lamp.bukkit)

    compileOnly(libs.packetevents.spigot)
    compileOnly(libs.spigot.api)

    // Core implementation.
    implementation(project(":core"))

}

tasks.processResources {

    inputs.property("name", project.name)
    inputs.property("version", project.version)
    inputs.property("coreVersion", libs.versions.coreVersion.get())
    inputs.property("packeteventsVersion", libs.versions.packetevents.get())

    // Replaces placeholders in BOTH yml files.
    filesMatching(listOf("plugin.yml", "paper-plugin.yml", "platform.properties")) {
        expand(inputs.properties)
    }

}

tasks.shadowJar {

    // Wait for the core shadowJar to finish.
    dependsOn(":core:shadowJar")

    archiveBaseName = "CoralGate-Spigot"
    archiveVersion = project.version.toString()
    archiveClassifier = ""

    // Relocate bStats.
    relocate("org.bstats", "cloud.gteam.coralgate.libs.bstats")

    // Relocate netty.
    relocate("io.netty", "cloud.gteam.coralgate.libs.netty")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

}

tasks.build {
    dependsOn(tasks.shadowJar)
}
