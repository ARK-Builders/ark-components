import java.net.URI

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = URI("https://jitpack.io")
        }
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/ARK-Builders/arklib-android")
            credentials {
                username = "token"
                password = "\u0037\u0066\u0066\u0036\u0030\u0039\u0033\u0066\u0032\u0037\u0033\u0036\u0033\u0037\u0064\u0036\u0037\u0066\u0038\u0030\u0034\u0039\u0062\u0030\u0039\u0038\u0039\u0038\u0066\u0034\u0066\u0034\u0031\u0064\u0062\u0033\u0064\u0033\u0038\u0065"
            }
        }
    }

    versionCatalogs {
        create("libraries") {
            from(files("./gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Ark Components"
include(":scorewidget")
include(":tagselector")
include(":folderstree")
include(":utils")
include(":filepicker")
include(":sample")
include(":about")
