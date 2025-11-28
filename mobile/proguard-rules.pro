# Add project specific ProGuard rules here.

# ========== MISSING CLASSES - AUTO GENERATED ==========
-dontwarn aQute.bnd.annotation.Version
-dontwarn org.kxml2.io.KXmlParser,org.kxml2.io.KXmlSerializer
# ========== END MISSING CLASSES ==========

# Keep the interfaces + concrete impls
-keep class org.xmlpull.v1.XmlPullParser { *; }
-keep class org.xmlpull.v1.XmlSerializer { *; }
-keep class org.xmlpull.v1.XmlPullParserFactory { *; }
-keep class org.kxml2.io.KXmlParser { *; }
-keep class org.kxml2.io.KXmlSerializer { *; }

-dontwarn org.xmlpull.v1.**
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

# ========== FIXED XML PARSER RULES ==========
# Fix for XmlResourceParser conflict - THIS IS THE KEY FIX
-dontwarn android.content.res.XmlResourceParser
-dontwarn org.xmlpull.v1.**

# Keep XML parser implementations but allow obfuscation
-keep,allowobfuscation class * implements org.xmlpull.v1.XmlPullParser { *; }
-keep,allowobfuscation class * implements org.xmlpull.v1.XmlSerializer { *; }

# Keep kxml2 specifically
-keep,allowobfuscation class org.kxml2.** { *; }
-keep,allowobfuscation class net.sf.kxml2.** { *; }

# Keep factory classes
-keepnames class org.xmlpull.v1.XmlPullParserFactory

# ========== END XML FIXES ==========

# Keep JavaRosa classes
-keep class org.javarosa.** { *; }
-keep class org.opendatakit.** { *; }
-dontwarn org.javarosa.**
-dontwarn org.joda.time.**

# --- XML pull + kxml2 ---
-keep class org.xmlpull.v1.XmlPullParser { *; }
-keep class org.xmlpull.v1.XmlSerializer { *; }
-keep class org.xmlpull.v1.XmlPullParserFactory { *; }

-keep class org.kxml2.io.KXmlParser { *; }
-keep class org.kxml2.io.KXmlSerializer { *; }

# These were fine to silence warnings
-dontwarn org.xmlpull.v1.**
-dontwarn android.content.res.XmlResourceParser

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

# Hilt
-keep class * extends java.lang.annotation.Annotation { *; }
-keep @javax.inject.Inject class * { *; }

# Keep Gson adapters and creators
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.InstanceCreator { *; }

# Keep fields annotated with @SerializedName so Gson can bind correctly
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep the polymorphic base and ALL its subtypes used at runtime
-keep class org.horizontal.tella.mobile.domain.entity.googledrive.Config { *; }
-keep class * extends org.horizontal.tella.mobile.domain.entity.googledrive.Config { *; }
-keep class com.google.api.services.drive.model.** { *; }

# Make sure we keep useful metadata
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

-dontwarn org.kxml2.io.KXml**


# ========== SIMPLE XML FRAMEWORK RULES ==========
-keep class org.simpleframework.xml.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Keep all SimpleXML annotated classes and their members
-keep @org.simpleframework.xml.Root class * {
    *;
}

# Keep all classes that have SimpleXML annotations
-keepclassmembers class * {
    @org.simpleframework.xml.Attribute *;
    @org.simpleframework.xml.Element *;
    @org.simpleframework.xml.ElementList *;
    @org.simpleframework.xml.ElementArray *;
    @org.simpleframework.xml.ElementMap *;
    @org.simpleframework.xml.Text *;
    @org.simpleframework.xml.Transient *;
}

# Keep SimpleXML core classes and constructors
-keep class org.simpleframework.xml.core.** {
    *;
}

-keepclasseswithmembers class org.simpleframework.xml.core.* {
    public <init>(...);
}

# Specifically keep the ElementListLabel constructor
-keep class org.simpleframework.xml.core.ElementListLabel {
    public <init>(...);
}

# Keep serializer/deserializer classes
-keep class org.simpleframework.xml.transform.** { *; }
-keep class org.simpleframework.xml.convert.** { *; }
-keep class org.simpleframework.xml.filter.** { *; }

# Keep package-info classes (they contain important metadata)
-keep class **.package-info

# Keep all factory classes
-keep class org.simpleframework.xml.stream.** { *; }


# Keep the polymorphic base and its subtypes
-keep class org.horizontal.tella.mobile.domain.entity.reports.Author { *; }
-keep class * extends org.horizontal.tella.mobile.domain.entity.reports.Author { *; }

# If you use RuntimeTypeAdapterFactory or a custom deserializer:
-keep class com.google.gson.typeadapters.RuntimeTypeAdapterFactory { *; }
-keep class your.package.AuthorDeserializer { *; }

# (You already have)
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Google HTTP Client core types used reflectively
-keep class com.google.api.client.googleapis.json.GoogleJsonError { *; }
-keep class com.google.api.client.json.GenericJson { *; }
-keep class com.google.api.client.util.GenericData { *; }

# Fields annotated with @Key must be kept with original names
-keepclassmembers class ** {
    @com.google.api.client.util.Key <fields>;
}

# If you use these factories, keep them too (safe + small)
-keep class com.google.api.client.json.gson.GsonFactory { *; }
-keep class com.google.api.client.json.jackson2.JacksonFactory { *; }

# Keep annotations/signatures so reflection works
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

-keep class org.horizontal.tella.mobile.domain.entity.uwazi.** { *; }
-keepclassmembers class org.horizontal.tella.mobile.domain.entity.uwazi.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Make sure the annotations are kept so Gson can see them
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses

# ---- TELLA NEXTCLOUD INTEGRATION ----
# Keep all Nextcloud data / repository classes (Hilt, Rx, reflection, etc.)
-keep class org.horizontal.tella.mobile.data.nextcloud.** { *; }

-keep class org.horizontal.tella.mobile.domain.entity.nextcloud.** { *; }


# Apache HttpClient 3 & Jackrabbit WebDAV (only if used)
-dontwarn org.apache.commons.httpclient.**
-dontwarn org.apache.jackrabbit.**

# If javax.naming shows up from DefaultHostnameVerifier or similar:
-dontwarn javax.naming.**
-dontwarn javax.naming.ldap.**
-dontwarn javax.naming.directory.**

# FindBugs annotations occasionally referenced
-dontwarn edu.umd.cs.findbugs.annotations.**


# Keep ALL model classes used with Gson
-keep class org.horizontal.tella.mobile.domain.entity.** { *; }

# Keep annotations AND signature information
-keepattributes Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep classes that use @SerializedName
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep type adapters
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

########## NEXTCLOUD / OWNCloud – FULL KEEP ##########

# Keep the whole OwnCloud / Nextcloud Android library
-keep class com.owncloud.android.lib.** { *; }
-dontwarn com.owncloud.android.lib.**

# Keep the newer Nextcloud common client (NextcloudClient etc.)
-keep class com.nextcloud.common.** { *; }
-dontwarn com.nextcloud.common.**

# If you use Nextcloud SSO (optional, but safe):
-keep class com.nextcloud.android.sso.** { *; }
-dontwarn com.nextcloud.android.sso.**
-keep class com.nextcloud.** { *; }
-keepclassmembers class com.nextcloud.** {
    @com.google.gson.annotations.SerializedName <fields>;
    @org.simpleframework.xml.* <fields>;
}
-keep class org.horizontal.tella.mobile.data.nextcloud.** { *; }
-keep class org.horizontal.tella.mobile.domain.entity.nextcloud.** { *; }
-keep class com.nextcloud.android.lib.common.OwnCloudClientFactory { *; }
-keep class com.nextcloud.common.NextcloudClient { *; }
-keep class dagger.** { *; }
-dontwarn dagger.**
-keepattributes KotlinMetadata

-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations


# Keep default constructors for Nextcloud classes
-keepclassmembers class com.nextcloud.** {
    public <init>();
}

# Keep all inner/nested classes
-keep class com.nextcloud.**$* { *; }

# Keep Android R classes (resource references)
-keep class **.R$* { *; }

# Keep DI generated classes (Hilt/Dagger)
-keep class *$$InjectAdapter { *; }
-keep class *$$ModuleAdapter { *; }
-keep class *$$MembersInjector { *; }
-keep class *$$Factory { *; }
-keep class *$$Builder { *; }


# Keep constructors for upload operations
-keepclassmembers class com.owncloud.android.lib.resources.files.** {
    public <init>(...);
}

# Keep ChunkedFileUploadRemoteOperation and UploadFileRemoteOperation fully
-keep class com.owncloud.android.lib.resources.files.UploadFileRemoteOperation { *; }
-keep class com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation { *; }

# Keep progress listener interface
-keep class com.owncloud.android.lib.common.network.OnDatatransferProgressListener { *; }

# Keep OkHttp/Okio streaming classes
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep nested classes (important for reflection)
-keep class com.nextcloud.**$* { *; }

-dontwarn org.json.**
-keep class org.json.** { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Persistence Models ---
# Keep all entity classes used with Gson/SharedPrefs
-keep class org.horizontal.tella.mobile.data.entity.** { *; }
-keep class org.horizontal.tella.mobile.domain.entity.** { *; }

# Keep fields annotated with @SerializedName
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- JNI Bindings ---
# Keep native methods so JNI doesn’t break
-keepclasseswithmembernames class * {
    native <methods>;
}

# --- Cryptographic Keys ---
# Keep key store wrappers and lifecycle keys
-keep class org.hzontal.tella.keys.** { *; }
-keep class org.hzontal.tella.keys.wrapper.** { *; }
-keep class org.hzontal.tella.keys.key.** { *; }

# --- Preferences ---
# Keep SharedPrefs and CommonPrefs classes
-keep class org.hzontal.shared_ui.data.CommonPrefs { *; }
-keep class org.horizontal.tella.mobile.data.sharedpref.SharedPrefs { *; }
-keep class org.horizontal.tella.mobile.data.sharedpref.Preferences { *; }

# --- Vault ---
# Keep Vault and KeyRxVault classes
-keep class com.hzontal.tella_vault.Vault { *; }
-keep class org.horizontal.tella.mobile.data.KeyRxVault { *; }
# ========== END SIMPLE XML RULES ==========