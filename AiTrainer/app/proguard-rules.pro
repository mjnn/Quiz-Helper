# Gson model classes used with reflection
-keep class com.aitrainer.practice.data.** { *; }

# Keep generic type information for TypeToken
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
