plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zeekoorg.mobsofglory"
    compileSdk = 35 // الترقية لـ SDK 35

    defaultConfig {
        applicationId = "com.zeekoorg.mobsofglory"
        minSdk = 26
        targetSdk = 35 // الترقية لـ SDK 35
        versionCode = 1
        versionName = "1.0-ULTRA"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true // تفعيل الحماية والضغط
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21 // استخدام جافا 21 الحديثة
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.7.0") // نسخة حديثة متوافقة مع SDK 35
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // أحدث إصدارات Media3 لعام 2026
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
}
