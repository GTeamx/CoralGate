plugins {

    `java-library`
    alias(libs.plugins.shadow)

}

java {

    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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

    // Dependencies.
    implementation(libs.bstats.velocity)
    implementation(libs.lamp.common)
    implementation(libs.lamp.velocity)
    implementation(libs.lamp.brigadier)

    compileOnly(libs.packetevents.velocity)
    compileOnly(libs.velocity.api)

    // Core implementation.
    implementation(project(":core"))

}

tasks.processResources {

    // Get versions.
    inputs.property("name", project.name)
    inputs.property("version", project.version)
    inputs.property("coreVersion", libs.versions.coreVersion.get())
    inputs.property("packeteventsVersion", libs.versions.packetevents.get())

    // Replace properties.
    filesMatching(listOf("velocity-plugin.json", "platform.properties")) {
        expand(inputs.properties)
    }

}

tasks.shadowJar {

    // Wait for the core shadowJar to finish.
    dependsOn(":core:shadowJar")

    archiveBaseName = "CoralGate-Velocity"
    archiveVersion = project.version.toString()
    archiveClassifier = ""

    // Relocate bStats.
    relocate("org.bstats", "cloud.gteam.coralgate.libs.bstats")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

}

tasks.build {
    dependsOn(tasks.shadowJar)
}
