plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    mavenLocal()

    // PacketEvents repo
    maven("https://repo.codemc.io/repository/maven-releases/")

    // Spigot snapshots
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    // Get Versions
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra
    val bstatsVersion: String by rootProject.extra

    // Dependencies
    implementation("org.bstats:bstats-bukkit:$bstatsVersion")
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.bukkit:$lampVersion")

    compileOnly("com.github.retrooper:packetevents-spigot:$packetEventsVersion")
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")

    // Core implementation
    implementation(project(":core"))
}

tasks.processResources {
    // Get Versions
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace plugin.yml
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version
        )
    }

    // Replace properties
    filesMatching("platform.properties") {
        expand("name" to project.name,
            "version" to project.version,

            "core" to mapOf(
                "version" to coreVersion
            ),
            "packetevents" to mapOf(
                "version" to packetEventsVersion
            )
        )
    }
}

tasks.shadowJar {
    // Wait for the core shadowJar to finish
    dependsOn(project(":core").tasks.named("shadowJar"))

    // Rename the output Jar
    val coreVersion: String by rootProject.extra

    archiveBaseName.set("CoralGate-$coreVersion-Spigot")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate bstats
    relocate("org.bstats", "coralgate.libs.org.bstats")

    // Relocate netty
    relocate("io.netty", "coralgate.libs.netty")

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
