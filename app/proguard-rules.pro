# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Generated from AGP missing_rules.txt for release minification.
-dontwarn coil.base.R$id
-dontwarn com.islami.Aha.R$drawable

# ── Room entities ──
# Room uses reflection for entities; field names must not be obfuscated.
-keep class com.islami.Aha.data.model.Habit { *; }
-keep class com.islami.Aha.data.model.UserHabitEntity { *; }
-keep class com.islami.Aha.data.model.HabitCompletionRecord { *; }
-keep class com.islami.Aha.data.model.DailyCompletionCount { *; }
-keep class com.islami.Aha.data.model.CategoryCompletionCount { *; }

# ── Room DAOs ──
-keep class com.islami.Aha.data.local.HabitDao { *; }
-keep class com.islami.Aha.data.local.UserHabitDao { *; }
-keep class com.islami.Aha.data.local.HabitCompletionDao { *; }
-keep class com.islami.Aha.data.local.AppDatabase { *; }

# ── Enums used by Room TypeConverters ──
-keep enum com.islami.Aha.ui.addhabit.SunnahCategoryType { *; }
-keep enum com.islami.Aha.ui.addhabit.FrequencyType { *; }

# ── Domain models synced with Firestore ──
# Firestore maps fields by name; obfuscation breaks deserialization.
-keep class com.islami.Aha.domain.model.SunnahHabit { *; }

# ── Firebase / Firestore ──
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Kotlin serialization & metadata ──
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# ── BroadcastReceivers (referenced in manifest) ──
-keep class com.islami.Aha.util.NotificationReceiver { *; }
-keep class com.islami.Aha.util.BootCompletedReceiver { *; }

# ── Hilt ──
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
