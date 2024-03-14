import java.net.URI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("pl.allegro.tech.build.axion-release")
    `maven-publish`
}

android {
    namespace = "dev.arkbuilders.components.filepicker"
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

    buildFeatures {
        viewBinding = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libraries.ark.component.utils)
    implementation(libraries.ark.component.folderstree)
    implementation(libraries.ark.component.tagselector)
    implementation(libraries.ark.component.scorewidget)

    implementation(libraries.androidx.core.ktx)
    implementation(libraries.androidx.appcompat)
    implementation(libraries.android.material)
    testImplementation(libraries.junit)
    androidTestImplementation(libraries.androidx.test.junit)
    androidTestImplementation(libraries.androidx.test.espresso)

    implementation(libraries.coil)
    implementation(libraries.coil.gif)
    implementation(libraries.coil.svg)
    implementation(libraries.coil.video)

    implementation(libraries.androidx.fragment.ktx)

    implementation(libraries.fastadapter)
    implementation(libraries.fastadapter.extensions.binding)
    implementation(libraries.fastadapter.extensions.diff)
    implementation(libraries.arklib)
    implementation(libraries.orbit.mvi.viewmodel)
    implementation(libraries.viewbinding.property.delegate)
}

val libVersion: String = /*scmVersion.version*/"0.2.0-SNAPSHOT"

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "dev.arkbuilders.components"
            artifactId = "filepicker"
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