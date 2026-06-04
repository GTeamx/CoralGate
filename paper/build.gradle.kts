plugins {

    `java-library`
    id("com.gradleup.shadow") version "9.4.2"

}

java {

    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

}

repositories {

    mavenCentral()
    mavenLocal()

    // PacketEvents repository.
    maven("https://repo.codemc.io/repository/maven-releases/")

    // Paper repository.
    maven("https://repo.papermc.io/repository/maven-public/")

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
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")

    // Core implementation.
    implementation(project(":core"))

}

tasks.processResources {

    // Get versions.
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace plugin.yml
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }

    // Replace properties.
    filesMatching("platform.properties") {

        expand(
            "name" to project.name,
            "version" to project.version,
            "coreVersion" to coreVersion,
            "packetEventsVersion" to packetEventsVersion
        )

    }

}

tasks.shadowJar {

    // Wait for the core shadowJar to finish.
    dependsOn(project(":core").tasks.named("shadowJar"))

    archiveBaseName.set("CoralGate-Paper")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate bStats.
    relocate("org.bstats", "coralgate.libs.bstats")

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
