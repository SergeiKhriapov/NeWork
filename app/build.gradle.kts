plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "ru.netology.nework"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.netology.nework"
        minSdk = 23
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = false
    }
}

dependencies {
    // Desugaring (java.time)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // AndroidX / UI
    implementation("androidx.core:core-ktx:1.12.0")               // обновлено
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0") // обновлено
    implementation("androidx.activity:activity-ktx:1.12.4")       // 👈 КЛЮЧЕВОЕ ИЗМЕНЕНИЕ
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle / ViewModel / Navigation
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")   // обновлено
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7") // обновлено
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")// обновлено
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")      // обновлено

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit / OkHttp / Moshi
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")          // обновлено
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")             // обновлено
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1")

    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")            // обновлено
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Image loading
    implementation("io.coil-kt:coil:2.6.0")                       // обновлено

    // ExoPlayer
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1") // обновлено
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")    // обновлено
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1") // обновлено
}