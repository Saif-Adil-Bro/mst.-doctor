# Proguard rules for Health Knowledge App
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okio.**
-dontwarn javax.annotation.**

# Keep Gson TypeToken generic signatures under R8 full mode
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep domain, entity, remote DTO, and Gson-serialized classes
-keep class com.health.knowledge.data.** { *; }
-keep class com.health.knowledge.domain.** { *; }
-keep class com.health.knowledge.pregnancy.** { *; }
-keep class com.health.knowledge.habit.** { *; }
-keep class com.health.knowledge.util.DatabaseSeeder$* { *; }
-keep class com.health.knowledge.util.UserPreferencesManager$* { *; }

