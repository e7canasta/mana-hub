pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenLocal() // mana-hive contracts JAR
        mavenCentral()
    }
}

rootProject.name = "mana-hub"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("shared-kernel")
include("identity")
include("audit")
include("residence")
include("population")
include("coverage")
include("care")
include("history")
include("policy")
include("surveillance")
include("evidence")
include("streams")
include("observation")
include("integration")
include("event-bridge")
include("insights")
include("bootstrap")
include("clients")
include("blueprints")
include("panel-api")
