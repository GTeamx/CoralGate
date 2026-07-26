plugins {

    `java-library`
    alias(libs.plugins.shadow)

}

group = "cloud.gteam.coralgate"
version = libs.versions.coreVersion.get()

java {

    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

}

repositories {

    mavenCentral()

    // PacketEvents repository.
    maven("https://repo.codemc.io/repository/maven-releases/")

}

dependencies {

    // Dependencies.
    implementation(libs.lamp.common)
    implementation(libs.jankson)
    implementation(libs.async.http.client)
    implementation(libs.jetbrains.annotations)
    implementation(libs.boosted.yaml)

    compileOnly(libs.packetevents.api)
    compileOnly(libs.gson)
    compileOnly(libs.adventure.api)

}

tasks.processResources {

    inputs.property("version", version)
    filteringCharset = "UTF-8"

    filesNotMatching("**/*.png") {
        expand(inputs.properties)
    }

}

tasks.jar {
    enabled = false // only shadowJar is used
}

tasks.shadowJar {

    archiveClassifier = "" // produce core.jar instead of core-all.jar

    relocate("dev.dejvokep.boostedyaml", "cloud.gteam.coralgate.libs.boostedyaml")
    relocate("io.netty", "cloud.gteam.coralgate.libs.netty")

    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

}

tasks.build {
    dependsOn(tasks.shadowJar)
}
