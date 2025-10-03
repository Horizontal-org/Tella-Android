# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\tomislav.randjic\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# tella
-keep class org.horizontal.tella.mobile.data.entity.** { *; }

# evernote android-job
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper
-keep public class com.evernote.android.job.v21.PlatformJobService
-keep public class com.evernote.android.job.v14.PlatformAlarmService
-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
-keep public class com.evernote.android.job.JobBootReceiver
-keep public class com.evernote.android.job.JobRescheduleService

# kXML2 implementations (required by META-INF/services)
-keep class org.kxml2.io.KXmlParser { *; }
-keep class org.kxml2.io.KXmlSerializer { *; }
-keepnames class org.xmlpull.v1.XmlPullParserFactory
-keep class * implements org.xmlpull.v1.XmlPullParser { *; }
-keep class * implements org.xmlpull.v1.XmlSerializer { *; }

# If you **removed** commons-httpclient and commons-logging, you don't need the dontwarns below.
# But if anything still references them transitively, add:
-dontwarn javax.servlet.**
-dontwarn javax.naming.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.log.**
-dontwarn org.apache.avalon.**
-dontwarn edu.umd.cs.findbugs.annotations.**

# okhttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# old okhttp?
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# retrofit2 (http://square.github.io/retrofit/)
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions
-dontwarn javax.annotation.**

# Gson
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Preserve the special static methods that are required in all enumeration classes.
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep the XmlPull parser/serializer implementations
-keep class org.kxml2.io.KXmlParser { *; }
-keep class org.kxml2.io.KXmlSerializer { *; }

# Keep the Service Provider entry point and related SPI contracts
-keepnames class org.xmlpull.v1.XmlPullParserFactory
-keep class * implements org.xmlpull.v1.XmlPullParser { *; }
-keep class * implements org.xmlpull.v1.XmlSerializer { *; }

# collect
-dontwarn org.joda.time.**
-keep class org.javarosa.**

# android job
-dontwarn com.evernote.android.job.v24.**
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper

# crashalytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.crashlytics.**
-dontwarn com.google.firebase.crashlytics.**

# sqlcypher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# odk collect
-dontwarn com.google.**
-dontwarn au.com.bytecode.**
-dontwarn org.joda.time.**
-dontwarn org.osmdroid.**

-keep class android.support.v7.widget.** { *; }

-dontobfuscate

# proofmode
-keep class org.witness.proofmode.** { *; }
-keep class org.spongycastle.** { *; }
-dontwarn org.spongycastle.**

# bitcoinj
-keep,includedescriptorclasses class org.bitcoinj.wallet.Protos$** { *; }
-keepclassmembers class org.bitcoinj.wallet.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-keep,includedescriptorclasses class org.bitcoin.protocols.payments.Protos$** { *; }
-keepclassmembers class org.bitcoin.protocols.payments.Protos { com.google.protobuf.Descriptors$FileDescriptor descriptor; }
-dontwarn org.bitcoinj.store.WindowsMMapHack
-dontwarn org.bitcoinj.store.LevelDBBlockStore
-dontnote org.bitcoinj.crypto.DRMWorkaround
-dontnote org.bitcoinj.crypto.TrustStoreLoader$DefaultTrustStoreLoader
-dontnote com.subgraph.orchid.crypto.PRNGFixes
-dontwarn okio.DeflaterSink
-dontwarn okio.Okio
-dontnote com.squareup.okhttp.internal.Platform
-dontwarn org.bitcoinj.store.LevelDBFullPrunedBlockStore**

# slf4j
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# Android-Image-Cropper
-keep class androidx.appcompat.widget.** { *; }
-keep class org.apache.commons.** { *; }

# androidx credentials
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
  *;
}

# ADD THESE NEW RULES FOR XML PARSING:

# Allow XML parser implementations to be optimized
-keepattributes Exceptions,Signature,InnerClasses,EnclosingMethod

# Keep XML parser factory implementations
-keep class * implements org.xmlpull.v1.XmlPullParser {
    public <methods>;
}

# But don't keep the actual XML parser classes that conflict with Android
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**

# Keep necessary XML-related classes for SimpleXML
-keep class org.simpleframework.xml.** { *; }
-keep class org.simpleframework.xml.stream.** { *; }

# Exclude kxml2 from optimization to prevent conflicts
-keep class org.kxml2.** { *; }
-dontwarn org.kxml2.**

# Hilt
-keep class * extends java.lang.annotation.Annotation { *; }
-keep @javax.inject.Inject class * { *; }