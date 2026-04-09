# ==========================================
# 🛡️ قواعد حماية وتجاهل مكتبات هواوي (HMS)
# ==========================================
-dontwarn com.huawei.**
-dontwarn android.telephony.HwTelephonyManager
-dontwarn com.huawei.android.os.**
-dontwarn com.huawei.libcore.io.**
-dontwarn com.huawei.hianalytics.**
-keep class com.huawei.hms.** { *; }
-keep class com.huawei.hianalytics.** { *; }

# ==========================================
# 🛡️ قواعد حماية إعلانات ياندكس (Yandex Ads)
# ==========================================
-keep class com.yandex.mobile.ads.** { *; }
-dontwarn com.yandex.mobile.ads.**

# ==========================================
# 🛡️ إعدادات عامة للحماية
# ==========================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes Exceptions
