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

    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    val lampVersion: String by rootProject.extra
    val packetEventsVersion: String by rootProject.extra
    val bstatsVersion: String by rootProject.extra

    implementation("org.bstats:bstats-velocity:$bstatsVersion")
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.velocity:$lampVersion")
    implementation("io.github.revxrsal:lamp.brigadier:$lampVersion")

    compileOnly("com.github.retrooper:packetevents-spigot:$packetEventsVersion")
    compileOnly("com.velocitypowered:velocity-api:3.4.0")

    // ✅ core as a normal project dependency
    implementation(project(":core"))
}

tasks.processResources {
    val packetEventsVersion: String by rootProject.extra
    val coreVersion: String by rootProject.extra

    filesMatching("velocity-plugin.json") {
        expand("version" to project.version)
    }

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
    mustRunAfter(project(":core").tasks.named("shadowJar"))
    val coreVersion: String by rootProject.extra

    archiveBaseName.set("CoralGate-$coreVersion-Velocity")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")

    // ✅ this will automatically include core (because of implementation(project(":core")))
    // and all other implementation deps

    relocate("org.bstats", "coralgate.libs.org.bstats")

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
