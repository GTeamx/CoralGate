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
}

dependencies {
    // Get Versions
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra
    val bstatsVersion: String by rootProject.extra

    // Dependencies
    implementation("org.bstats:bstats-bungeecord:$bstatsVersion")
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.bungee:$lampVersion")

    compileOnly("com.github.retrooper:packetevents-spigot:$packetEventsVersion")
    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")

    // Core implementation
    implementation(project(path = ":core", configuration = "shadow"))
}

tasks.processResources {
    // Get Versions
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace bungee.yml
    filesMatching("bungee.yml") {
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

    archiveBaseName.set("CoralGate-$coreVersion-Bungeecord")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate bstats
    relocate("org.bstats", "coralgate.libs.org.bstats")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}


tasks.build {
    dependsOn(tasks.shadowJar)
}
