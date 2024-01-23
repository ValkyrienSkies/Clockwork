plugins {
    kotlin("jvm")
    kotlin("kapt")
    `java-library`
    `maven-publish`
}

dependencies {
    // JOML for Math
    api("org.joml:joml:1.10.4")
    api("org.joml:joml-primitives:1.10.0")

    compileOnlyApi("org.jetbrains:annotations:23.0.0")
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}

