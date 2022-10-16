plugins {
    id("org.cubeengine.parent.module")
}

val log4jVersion: String by project
val hikariCPVersion: String by project
val jooqVersion: String by project

dependencies {
    implementation("org.jooq:jooq:$jooqVersion") // TODO shade

    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion") // TODO provided dependency
    implementation("com.zaxxer:HikariCP:$hikariCPVersion") // TODO provided dependency
}
