rootProject.name = "Spleef-OG"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.battleplugins.org/releases/")
        maven("https://repo.battleplugins.org/snapshots/")
    }
}

pluginManagement { repositories { gradlePluginPortal() } }
