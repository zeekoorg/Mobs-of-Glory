plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // تفعيل بلوقن خدمات جوجل
    id("com.google.gms.google-services")
    // تفعيل بلوقن خدمات هواوي
    id("com.huawei.agconnect")
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1") // أحدث نسخة مستقرة
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // أحدث إصدارات Media3 لعام 2026
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // ==========================================
    // 💰 مكتبات الإعلانات وخدمات جوجل / هواوي لعام 2026
    // ==========================================
    
    // Yandex Mobile Ads (أحدث إصدار مستقر 7.18.5)
    implementation("com.yandex.android:mobileads:7.18.5")

    // خدمات جوجل (ضرورية لياندكس لجمع معرف الإعلان وضمان الأرباح)
    implementation("com.google.android.gms:play-services-base:18.7.0") 
    implementation("com.google.android.gms:play-services-ads-identifier:18.1.0")
    
    // خدمات هواوي (ضرورية لياندكس لتعمل على أجهزة هواوي بدون جوجل)
    implementation("com.huawei.hms:base:6.13.0.303")
    implementation("com.huawei.hms:ads-identifier:3.4.62.300")
}
