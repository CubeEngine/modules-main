rootProject.name = "main-aggregator"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.cubeengine.org")
        maven("https://repo.spongepowered.org/repository/maven-public")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenLocal()
    }

    val conventionPluginVersion: String by settings
    plugins {
        id("org.cubeengine.parent.module") version (conventionPluginVersion)
    }
}

include("conomy",
        "docs",
        "kickban",
        "locker",
        "multiverse",
        "netherportals",
        "portals",
        "protector",
        "roles",
        "sql",
        "teleport",
        "travel",
        "vanillaplus",
        "worldcontrol",
        "world",
        "zoned")
