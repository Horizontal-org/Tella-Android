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
-keep class rs.readahead.washington.mobile.data.entity.** { *; }

# evernote android-job
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper
-keep public class com.evernote.android.job.v21.PlatformJobService
-keep public class com.evernote.android.job.v14.PlatformAlarmService
-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
-keep public class com.evernote.android.job.JobBootReceiver
-keep public class com.evernote.android.job.JobRescheduleService

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
#-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.** { *; }

# Preserve the special static methods that are required in all enumeration classes.
-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# Retain generated class which implement Unbinder.
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }


# Prevent obfuscation of types which use ButterKnife annotations since the simple name
# is used to reflectively look up the generated ViewBinding.
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }


# simplexml
# Keep public classes and methods.
-dontwarn com.bea.xml.stream.**
-dontwarn org.simpleframework.xml.stream.**
-keep class org.simpleframework.xml.**{ *; }
-keepclassmembers,allowobfuscation class * {
    @org.simpleframework.xml.* <fields>;
    @org.simpleframework.xml.* <init>(...);
}


# collect
#-dontwarn com.google.**
#-dontwarn au.com.bytecode.**
-dontwarn org.joda.time.**
#-dontwarn org.osmdroid.**
-dontwarn org.xmlpull.v1.**
-keep public class org.xmlpull.**
-keep class org.javarosa.**
#-keep class android.support.v7.widget.** { *; }


# todo: check which one
-dontwarn org.xmlpull.v1.**
-dontnote org.xmlpull.v1.**
-keep class org.xmlpull.** { *; }


# android job
-dontwarn com.evernote.android.job.v24.**
-dontwarn com.evernote.android.job.gcm.**
-dontwarn com.evernote.android.job.util.GcmAvailableHelper
-keep public class com.evernote.android.job.v21.PlatformJobService
-keep public class com.evernote.android.job.v14.PlatformAlarmService
-keep public class com.evernote.android.job.v14.PlatformAlarmReceiver
-keep public class com.evernote.android.job.JobBootReceiver
-keep public class com.evernote.android.job.JobRescheduleService


# crashalytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**


#android additional
-keep public class android.support.v7.widget.** { *; }
-keep public class android.support.v7.internal.widget.** { *; }
-keep public class android.support.v7.internal.view.menu.** { *; }

-keep public class android.support.v4.widget.** { *; }
-keep public class android.support.v4.internal.widget.** { *; }
-keep public class android.support.v4.internal.view.menu.** { *; }

-keep public class * extends android.support.v4.view.ActionProvider {
    public <init>(android.content.Context);
}


# sqlcypher
#-libraryjars libs/commons-codec.jar
#-libraryjars libs/guava-r09.jar
#-libraryjars libs/sqlcipher.jar

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-dontobfuscate
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

-dontwarn javax.annotation.**

-dontwarn android.app.**
-dontwarn android.support.**
-dontwarn android.view.**
-dontwarn android.widget.**

-dontwarn com.google.common.primitives.**

-dontwarn **CompatHoneycomb
-dontwarn **CompatHoneycombMR2
-dontwarn **CompatCreatorHoneycombMR2

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep class net.sqlcipher.** {
    *;
}

-keep class net.sqlcipher.database.** {
    *;
}

# odk collect
-dontwarn com.google.**
-dontwarn au.com.bytecode.**
-dontwarn org.joda.time.**
-dontwarn org.osmdroid.**
-dontwarn org.xmlpull.v1.**

-keep class org.javarosa.**
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
