plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zeekoorg.mobsofglory"
    compileSdk = 34 // أحدث بيئة عمل

    defaultConfig {
        applicationId = "com.zeekoorg.mobsofglory"
        minSdk = 24 // يدعم معظم هواتف الأندرويد الحالية
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true // لتصغير حجم اللعبة
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // تفعيل ViewBinding لتسهيل ربط واجهات XML بالكود
    buildFeatures {
        viewBinding = true 
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0") // لتصميم الواجهات والبطاقات
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // مكتبة Media3 لتشغيل فيديو البداية والخلفيات الملحمية
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
}

