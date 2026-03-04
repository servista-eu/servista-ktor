rootProject.name = "servista-ktor"

pluginManagement { includeBuild("../gradle-platform") }

includeBuild("../servista-commons")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") { from(files("../gradle-platform/catalog/libs.versions.toml")) }
    }
}
