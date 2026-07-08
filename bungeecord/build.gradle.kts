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

    // BungeeCord repository.
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")

}

dependencies {

    // Dependencies.
    implementation(libs.bstats.bungeecord)
    implementation(libs.lamp.common)
    implementation(libs.lamp.bungee)

    compileOnly(libs.packetevents.bungeecord)
    compileOnly(libs.bungeecord.api)

    // Core implementation.
    implementation(project(path = ":core", configuration = "shadow"))

}

tasks.processResources {

    inputs.property("name", project.name)
    inputs.property("version", project.version)
    inputs.property("coreVersion", libs.versions.coreVersion.get())
    inputs.property("packeteventsVersion", libs.versions.packetevents.get())

    // Replace properties.
    filesMatching(listOf("bungee.yml", "platform.properties")) {
        expand(inputs.properties)
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
