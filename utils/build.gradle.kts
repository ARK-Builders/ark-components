import java.net.URI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("pl.allegro.tech.build.axion-release")
    `maven-publish`
}

android {
    namespace = "dev.arkbuilders.components.utils"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libraries.androidx.core.ktx)
    implementation(libraries.androidx.appcompat)
    implementation(libraries.android.material)
    testImplementation(libraries.junit)
    androidTestImplementation(libraries.androidx.test.junit)
    androidTestImplementation(libraries.androidx.test.espresso)
}

val libVersion: String = scmVersion.version

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "dev.arkbuilders.components"
            artifactId = "utils"
            version = libVersion
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GithubPackages"
            url = URI("https://maven.pkg.github.com/ARK-Builders/ark-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}