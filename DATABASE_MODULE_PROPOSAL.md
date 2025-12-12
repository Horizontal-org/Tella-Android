# Database Module Proposal

## Recommended Structure

```
tella-database/
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/horizontal/tella/mobile/data/database/
│   │   │       ├── core/                    # Core database infrastructure
│   │   │       │   ├── CipherOpenHelper.java
│   │   │       │   ├── HorizontalSQLiteOpenHelper.java
│   │   │       │   ├── DatabaseModuleProvider.kt
│   │   │       │   └── D.kt
│   │   │       ├── modules/                 # Database modules
│   │   │       │   ├── DatabaseModule.java
│   │   │       │   ├── settings/
│   │   │       │   ├── forms/
│   │   │       │   ├── media/
│   │   │       │   ├── reports/
│   │   │       │   ├── uwazi/
│   │   │       │   ├── feedback/
│   │   │       │   ├── resources/
│   │   │       │   ├── googledrive/         # Optional for F-Droid
│   │   │       │   ├── dropbox/             # Optional for F-Droid
│   │   │       │   └── nextcloud/
│   │   │       ├── repositories/            # Repository implementations
│   │   │       │   ├── DataSource.java
│   │   │       │   ├── UwaziDataSource.java
│   │   │       │   ├── ResourceDataSource.java
│   │   │       │   ├── GoogleDriveDataSource.java  # Optional
│   │   │       │   ├── DropBoxDataSource.java      # Optional
│   │   │       │   └── NextCloudDataSource.java
│   │   │       ├── utils/
│   │   │       │   ├── DataBaseUtils.kt
│   │   │       │   └── Hex.java
│   │   │       └── KeyDataSource.java
│   │   └── AndroidManifest.xml
│   ├── fdroid/                              # F-Droid variant (excludes cloud)
│   │   └── java/
│   │       └── org/horizontal/tella/mobile/data/database/
│   │           └── modules/
│   │               └── (empty or stubs)
│   └── playstore/                           # Play Store variant (includes all)
│       └── java/
│           └── org/horizontal/tella/mobile/data/database/
│               └── modules/
│                   ├── googledrive/
│                   └── dropbox/
└── proguard-rules.pro
```

## Module Dependencies

### tella-database/build.gradle
```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'org.horizontal.tella.mobile.data.database'
    compileSdk versions.compileSdk
    
    defaultConfig {
        minSdk versions.minSdk
        consumerProguardFiles "proguard-rules.pro"
    }
    
    // Build variants for F-Droid vs Play Store
    flavorDimensions "distribution"
    productFlavors {
        fdroid {
            dimension "distribution"
            // Excludes Google Drive and Dropbox
        }
        playstore {
            dimension "distribution"
            // Includes all modules
        }
    }
}

dependencies {
    // Core dependencies
    implementation "net.zetetic:sqlcipher-android:$versions.sqlcipher"
    implementation "androidx.sqlite:sqlite-ktx:$versions.sqlite"
    
    // Optional dependencies (only for playstore variant)
    playstoreImplementation "com.google.api-client:google-api-client-android:$versions.googleApiClientAndroidVersion"
    playstoreImplementation "com.dropbox.core:dropbox-core-sdk:$versions.dropboxVersion"
    
    // Project dependencies
    api project(':tella-keys')  // For encryption
    
    // Testing
    testImplementation "junit:junit:$versions.junit"
    androidTestImplementation "androidx.test.ext:junit:$versions.testJunit"
}
```

## Benefits

### 1. **Clean Separation**
- Database code isolated from app code
- Clear module boundaries
- Easier to understand dependencies

### 2. **F-Droid Support**
- Can exclude cloud modules at build time
- No code changes needed
- Smaller APK size

### 3. **Testing**
- Test database module independently
- Mock database in app tests
- Unit test repositories separately

### 4. **Reusability**
- Can be used by other modules if needed
- Clear API surface
- Versioned independently

### 5. **Build Performance**
- Parallel compilation
- Incremental builds
- Better caching

## Migration Steps

### Step 1: Create Module Structure
1. Create `tella-database/` directory
2. Create `build.gradle` with library configuration
3. Add to `settings.gradle`

### Step 2: Move Database Code
1. Move `data/database/` from `mobile/` to `tella-database/`
2. Update package names if needed
3. Update imports in `mobile/` module

### Step 3: Configure Dependencies
1. Add `tella-database` dependency to `mobile/build.gradle`
2. Update any cross-module dependencies
3. Configure build variants

### Step 4: Split Cloud Modules
1. Create `googledrive/`, `dropbox/`, `nextcloud/` submodules
2. Move code from `CloudDatabaseModule`
3. Update `DatabaseModuleProvider`

### Step 5: Test Both Variants
1. Build F-Droid variant (no cloud modules)
2. Build Play Store variant (all modules)
3. Verify functionality

## Alternative: Keep in Mobile Module

### When to Keep in Mobile:
- ✅ Small project
- ✅ No need for F-Droid variant
- ✅ Database tightly coupled to app
- ✅ Simpler build setup

### When to Extract:
- ✅ Multi-module architecture (you have this)
- ✅ Need F-Droid builds (you need this)
- ✅ Want to test independently
- ✅ Plan to reuse database code

## Recommendation

**Extract to `tella-database` module** because:
1. You already use multi-module architecture
2. You need F-Droid support
3. You're refactoring to modular database
4. Better long-term maintainability

## Implementation Example

### settings.gradle
```gradle
include ':tella-database'
include ':tella-vault'
include ':tella-keys'
include ':tella-locking-ui'
include ':shared-ui'
include ':mobile'
include ':pdfviewer'
```

### mobile/build.gradle
```gradle
dependencies {
    implementation project(':tella-database')
    implementation project(':tella-vault')
    // ... other dependencies
}
```

### tella-database/build.gradle
```gradle
dependencies {
    api project(':tella-keys')
    // ... database dependencies
}
```
