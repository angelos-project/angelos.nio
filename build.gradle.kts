plugins {
    base
}

repositories {
    mavenCentral()
    mavenLocal()
}

group = "angelos"
version = "0.0.1"

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

buildscript {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.android}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}