plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "ru.appforsale.alicecommands"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "3.1.2"
val exposedVersion = "0.57.0"
val flywayVersion = "11.7.2"
val logbackVersion = "1.5.18"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-compression:$ktorVersion")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.5")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("com.networknt:json-schema-validator:1.5.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("org.testcontainers:postgresql:1.20.6")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("ru.appforsale.alicecommands.api.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyAdminWeb") {
    from("${rootProject.projectDir}/admin-web")
    into(layout.buildDirectory.dir("resources/main/admin"))
    include("**/*")
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("copyAdminWeb")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.layout.projectDirectory.asFile
}

tasks.register<JavaExec>("validateContent") {
    group = "verification"
    description = "Validate content bundle JSON against schema"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("ru.appforsale.alicecommands.api.tools.ValidateContentMainKt")
    workingDir = rootProject.layout.projectDirectory.asFile
    val contentFile = project.findProperty("contentFile")?.toString() ?: "seed/catalog-audit-fixed.json"
    args(contentFile)
    systemProperty("contentFile", contentFile)
}

tasks.register<JavaExec>("validateSmartHomeDevices") {
    group = "verification"
    description = "Validate smart home devices JSON against schema"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("ru.appforsale.alicecommands.api.tools.ValidateSmartHomeDevicesMainKt")
    workingDir = rootProject.layout.projectDirectory.asFile
    val contentFile = project.findProperty("contentFile")?.toString() ?: "seed/smarthome-devices-example.json"
    args(contentFile)
    systemProperty("contentFile", contentFile)
}
