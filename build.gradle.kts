plugins {
    java
    application
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

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

application {
    mainClass.set("org.lostclient.muling.server.Main")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}