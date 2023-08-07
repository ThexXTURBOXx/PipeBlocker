plugins {
    id("java")
}

repositories {
    mavenCentral()
}

tasks.withType<Test> {
}

// include the filter from the root src folder
tasks.processResources.configure {
    from("${rootDir}/src/main/resources")
}

dependencies {
    compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
    compileOnly("org.apache.logging.log4j:log4j-core:2.0-beta9")
    testImplementation("org.apache.logging.log4j:log4j-api:2.0-beta9")
    testImplementation("org.apache.logging.log4j:log4j-core:2.0-beta9")

    testImplementation("junit:junit:4.13.2")
}