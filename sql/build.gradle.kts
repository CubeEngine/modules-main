plugins {
    id("org.cubeengine.parent.module")
}

group = "org.cubeengine.module"
version = "1.1.0-SNAPSHOT"

description = "Provide SQL with jOOQ"

val log4jVersion: String by project
val hikariCPVersion: String by project
val jooqVersion: String by project

dependencies {
    implementation("org.jooq:jooq:$jooqVersion") // TODO shade

    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion") // TODO provided dependency
    implementation("com.zaxxer:HikariCP:$hikariCPVersion") // TODO provided dependency
}

repositories {
    maven("https://repo.cubeengine.org")
}
