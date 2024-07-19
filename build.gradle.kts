plugins {
    id("java")
    alias(libs.plugins.paperweight) apply true
}

group = "co.pvphub"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paperApi.get())

    compileOnly(libs.protocol.lib)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    assemble {
        dependsOn("reobfJar")
    }
    test {
        useJUnitPlatform()
    }
}