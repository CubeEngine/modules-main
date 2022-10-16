rootProject.name = "main-aggregator"

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public")
        mavenLocal()
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
