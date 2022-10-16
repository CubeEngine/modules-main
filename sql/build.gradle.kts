plugins {
    id("org.cubeengine.parent.module")
}

val log4jVersion: String by project
val hikariCPVersion: String by project
val jooqVersion: String by project

dependencies {
    api("org.jooq:jooq:$jooqVersion")

    compileOnly("org.apache.logging.log4j:log4j-core:$log4jVersion")
    compileOnly("com.zaxxer:HikariCP:$hikariCPVersion")
}
