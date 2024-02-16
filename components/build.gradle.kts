import java.net.URI

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("pl.allegro.tech.build.axion-release")
    `maven-publish`
}

android {
    namespace = "dev.arkbuilders.components"
    compileSdk = 33

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("org.orbit-mvi:orbit-viewmodel:6.1.0")
    implementation("dev.arkbuilders:arklib:0.3.5")
    implementation("com.mikepenz:fastadapter:5.7.0")
    implementation("com.mikepenz:fastadapter-extensions-binding:5.7.0")
    implementation("com.mikepenz:fastadapter-extensions-diff:5.7.0")

    implementation("com.github.kirich1409:viewbindingpropertydelegate-noreflection:1.5.9")

    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("com.github.skydoves:balloon:1.6.4")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    val coilVersion = "2.4.0"
    implementation("io.coil-kt:coil:$coilVersion")
    implementation("io.coil-kt:coil-gif:$coilVersion")
    implementation("io.coil-kt:coil-svg:$coilVersion")
    implementation("io.coil-kt:coil-video:$coilVersion")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

val libVersion: String = scmVersion.version

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "dev.arkbuilders"
            artifactId = "components"
            version = libVersion
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GithubPackages"
            url = URI("https://maven.pkg.github.com/ARK-Builders/ark-components-android")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
