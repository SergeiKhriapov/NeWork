// build.gradle.kts (project-level)
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.android.library") version "9.0.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

// Здесь НЕ должно быть блока repositories (они объявлены в settings.gradle.kts)