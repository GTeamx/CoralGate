plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.3.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    mavenLocal()

    // PacketEvents repo
    maven("https://repo.codemc.io/repository/maven-releases/")

    // Paper repo
    maven("https://repo.papermc.io/repository/maven-public/")
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
    compileOnly("io.papermc.paper:paper-api:1.20-R0.1-SNAPSHOT")

    // Core implementation
    implementation(project(":core"))
}

tasks.processResources {
    // Get Versions
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    // Replace plugin.yml
    filesMatching("paper-plugin.yml") {
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

    archiveBaseName.set("CoralGate-$coreVersion-Paper")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // Relocate bstats
    relocate("org.bstats", "coralgate.libs.org.bstats")

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
