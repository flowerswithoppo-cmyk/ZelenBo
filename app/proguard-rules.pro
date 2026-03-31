# ZelenBo release rules.
# Keep Kotlin metadata and Hilt generated classes.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn javax.inject.**
-dontwarn kotlin.**

