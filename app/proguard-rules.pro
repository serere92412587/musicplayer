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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# jaudiotaggerを難読化・最適化の対象から除外する
-keep class org.jaudiotagger.** { *; }
-keepclassmembers class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# jaudiotaggerの中に含まれる「PC専用の機能（Androidには存在しない機能）」への参照を無視する
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn java.nio.file.**

# jaudiotagger全体を保持
-keep class org.jaudiotagger.** { *; }
-keep interface org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# 未使用リソースの最適化で壊れるのを防ぐ
-keepattributes Signature, *Annotation*, EnclosingMethod

# Androidに存在しないJavaデスクトップ用ライブラリの警告をすべて抑制
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn javax.imageio.**
-dontwarn java.beans.**
-dontwarn java.nio.file.**