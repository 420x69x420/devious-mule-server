plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "org.lostclient.muling"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(group = "org.projectlombok", name = "lombok", version = "1.18.20")

    compileOnly(group = "org.projectlombok", name = "lombok", version = "1.18.20")

    implementation(group = "com.google.code.gson", name = "gson", version = "2.8.5")
    //implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.7.25")
    implementation(group = "org.java-websocket", name = "Java-WebSocket", version = "1.5.2")
}

tasks {
    application {
        mainClass.set("org.lostclient.muling.server.Main")
        mainClassName = "org.lostclient.muling.server.Main"
    }

    shadowJar {
        archiveClassifier.set("shaded")
        exclude("META-INF/*")
        includeEmptyDirs = false
        isPreserveFileTimestamps = false
    }
}