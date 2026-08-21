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

# ========== 2025-08-19 (audit): PDF annotation persistence ==========
# Keep all annotation classes — they are persisted as JSON via org.json so
# field names MUST survive minification.
-keep class com.horizontal.pdfviewer.annotations.** { *; }
-keepclassmembers class com.horizontal.pdfviewer.annotations.** {
    <fields>;
    <init>(...);
}

# Keep public view classes (referenced via reflection in XML layouts)
-keep class com.horizontal.pdfviewer.PdfRendererView { *; }
-keep class com.horizontal.pdfviewer.PdfRendererView$* { *; }
-keep class com.horizontal.pdfviewer.PinchZoomRecyclerView { *; }
-keep class com.horizontal.pdfviewer.PdfViewAdapter { *; }
-keep class com.horizontal.pdfviewer.PdfViewAdapter$* { *; }
-keep class com.horizontal.pdfviewer.annotations.PdfAnnotationOverlayView { *; }

# org.json used for serialization (already in Android framework; harmless repeat)
-keep class org.json.** { *; }
# ========== END PDF annotation rules ==========

# ========== 2025-08-20 (audit-fix rev 7): PDFBox-Android text extraction ==========
# Keep PDFBox + FontBox classes — they are loaded via reflection inside
# PDDocument.load() and PDFTextStripper. Without these rules R8 strips
# the metadata classes and extractText() throws NoClassDefFoundError.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.pdfbox.android.PDFBoxResourceLoader { *; }
-dontwarn com.tom_roush.**
# ========== END PDFBox-Android rules ==========
