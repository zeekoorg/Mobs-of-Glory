buildscript {
    repositories {
        google()
        mavenCentral()
        // مستودع هواوي الأساسي لجلب البلوقن
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        
        // بلوقن خدمات جوجل
        classpath("com.google.gms:google-services:4.4.2")
        
        // بلوقن خدمات هواوي (أحدث إصدار مستقر)
        classpath("com.huawei.agconnect:agcp:1.9.1.302")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
