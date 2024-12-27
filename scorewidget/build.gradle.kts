import java.net.URI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("pl.allegro.tech.build.axion-release")
    `maven-publish`
}

android {
    namespace = "dev.arkbuilders.components"
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libraries.androidx.compose.activity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.extended)
    implementation(libraries.arklib)
    implementation(libraries.orbit.mvi.viewmodel)
    implementation(libs.orbit.mvi.compose)
    implementation(libs.counterslider)
    testImplementation(libraries.junit)
    androidTestImplementation(libraries.androidx.test.junit)
    androidTestImplementation(libraries.androidx.test.espresso)
}

val libVersion: String = scmVersion.version

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "dev.arkbuilders.components"
            artifactId = "scorewidget"
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