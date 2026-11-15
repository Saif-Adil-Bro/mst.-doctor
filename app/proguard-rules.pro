# Proguard rules for Health Knowledge App
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn okio.**
-dontwarn javax.annotation.**

# Keep domain, entity, remote DTO, and Gson-serialized classes
-keep class com.health.knowledge.data.** { *; }
-keep class com.health.knowledge.domain.** { *; }
-keep class com.health.knowledge.pregnancy.** { *; }
-keep class com.health.knowledge.habit.** { *; }
-keepclassmembers class com.health.knowledge.util.DatabaseSeeder$* { *; }
-keepclassmembers class com.health.knowledge.util.UserPreferencesManager$* { *; }

