import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.yehao.leyuan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yehao.leyuan"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
        ndk {
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a"))
        }
        val base = (project.findProperty("UPDATE_BASE_URL") as? String)?.trim().orEmpty()
        val updateBaseUrl = base.ifEmpty { "https://azhansy.github.io/yehao-leyuan/" }
        buildConfigField(
            "String",
            "UPDATE_BASE_URL",
            "\"${updateBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                val storeRel = keystoreProperties.getProperty("storeFile")
                    ?: error("key.properties: missing storeFile (path relative to project root, e.g. app/key.keystore)")
                storeFile = rootProject.file(storeRel).also { f ->
                    require(f.exists()) { "Keystore not found: ${f.absolutePath}" }
                }
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: error("key.properties: missing keyAlias")
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: error("key.properties: missing storePassword")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: error("key.properties: missing keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
