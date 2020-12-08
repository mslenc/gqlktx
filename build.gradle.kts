import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.70")
    }
}

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("maven-publish")
}

group = "com.github.mslenc"
version = "0.13.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.mslenc:utilsktx:1.1.0")

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    // https://mvnrepository.com/artifact/io.github.lukehutch/fast-classpath-scanner
    implementation("io.github.lukehutch:fast-classpath-scanner:2.21")

    testImplementation("junit:junit:4.12")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
