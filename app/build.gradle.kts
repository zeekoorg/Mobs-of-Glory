plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // 💡 تم تعطيل هذه الإضافات مؤقتاً لتجنب خطأ غياب ملفات JSON حتى تقوم بإضافتها مستقبلاً
    // id("com.google.gms.google-services")
    // id("com.huawei.agconnect")
}

android {
    namespace = "com.zeekoorg.mobsofglory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zeekoorg.mobsofglory"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-ULTRA"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true 
        sourceCompatibility = JavaVersion.VERSION_21
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
    // 💡 تحديث مكتبة الـ Desugaring إلى أحدث إصدار مطلوب (2.1.4) لحل خطأ خدمات جوجل
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 💡 تم التحديث إلى 1.4.1 بناءً على ما يطلبه المترجم في تقريرك
    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // Yandex Mobile Ads (أحدث إصدار مستقر)
    implementation("com.yandex.android:mobileads:7.18.5")

    // خدمات جوجل (أحدث إصدارات متوافقة)
    implementation("com.google.android.gms:play-services-base:18.7.0") 
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
    
    // خدمات هواوي (أحدث إصدارات متوافقة)
    implementation("com.huawei.hms:base:6.13.0.303")
    implementation("com.huawei.hms:ads-identifier:3.4.62.300")
}
