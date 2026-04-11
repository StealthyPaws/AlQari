plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.al_qari"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.al_qari"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // Java project (no Kotlin)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // If you want ViewBinding instead of findViewById:
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    //noinspection UseTomlInstead
    implementation("com.android.volley:volley:1.2.1")
    // UI basics (from version catalog)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // --- Networking ---
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // Choose ONE converter:
    // A) GSON (simplest for Java)
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    // B) (Optional) If you prefer Moshi in Java:
    // implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    // implementation("com.squareup.moshi:moshi:1.15.1")  // note: no moshi-kotlin needed for Java

    // OkHttp logging (dev)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("com.google.android.material:material:1.9.0")

    implementation("com.android.volley:volley:1.2.1")
    implementation("com.alphacephei:vosk-android:0.3.47")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
