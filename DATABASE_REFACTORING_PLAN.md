# Database Architecture Refactoring Plan

## Executive Summary

**Recommendation: Single Database with Pluggable Modules**

Keep one database (`HorizontalSQLiteOpenHelper`) but make modules conditionally loadable. This allows:
- ✅ F-Droid builds without Google Drive/Dropbox
- ✅ Better code organization
- ✅ Easier testing
- ✅ Maintainability

---

## Current Architecture Issues

### 1. **CloudDatabaseModule is Monolithic**
- Bundles Google Drive, Dropbox, and NextCloud
- Cannot exclude individual services
- Hard to test in isolation

### 2. **KeyDataSource is Hardcoded**
- Directly instantiates all cloud services
- No way to conditionally exclude modules
- Tight coupling

### 3. **DataSource is Too Large**
- Implements 8+ repository interfaces
- Mixes concerns (Settings, Forms, Media, Reports, etc.)
- Hard to maintain and test

---

## Proposed Architecture

### Phase 1: Split CloudDatabaseModule

**Current:**
```
CloudDatabaseModule (contains all 3 services)
```

**Proposed:**
```
modules/
  ├── googledrive/
  │   └── GoogleDriveDatabaseModule.java
  ├── dropbox/
  │   └── DropboxDatabaseModule.java
  └── nextcloud/
      └── NextCloudDatabaseModule.java
```

**Benefits:**
- Each service is independent
- Can exclude individually
- Easier to test

### Phase 2: Refactor HorizontalSQLiteOpenHelper

**Current:**
```java
databaseModules.add(new CloudDatabaseModule()); // Hardcoded
```

**Proposed:**
```java
// Use DatabaseModuleProvider
DatabaseModuleProvider provider = getModuleProvider(); // DI or BuildConfig
databaseModules.addAll(provider.getModules());
```

### Phase 3: Refactor KeyDataSource

**Current:**
```java
// Hardcoded initialization
asyncGoogleDriveSubject.onNext(GoogleDriveDataSource.getInstance(...));
asyncDropBoxSubject.onNext(DropBoxDataSource.getInstance(...));
```

**Proposed:**
```kotlin
// Use dependency injection or factory pattern
class KeyDataSource(
    private val moduleProvider: DatabaseModuleProvider,
    private val dataSourceFactory: DataSourceFactory
) {
    fun initKeyDataSource() {
        // Only initialize enabled modules
        if (moduleProvider.isGoogleDriveEnabled()) {
            asyncGoogleDriveSubject.onNext(dataSourceFactory.createGoogleDrive())
        }
        // ... etc
    }
}
```

### Phase 4: Refactor DataSource (Optional)

**Current:**
- One large `DataSource` class implementing 8+ interfaces

**Proposed:**
- Split into module-specific repositories:
  - `SettingsRepository` (from DataSource)
  - `FormsRepository` (from DataSource)
  - `MediaRepository` (from DataSource)
  - `ReportsRepository` (from DataSource)
  - etc.

**OR** keep DataSource but use composition:
```kotlin
class DataSource(
    private val settingsRepo: SettingsRepository,
    private val formsRepo: FormsRepository,
    // ...
) : IServersRepository, ITellaUploadServersRepository, ...
```

---

## Implementation Strategy

### Option A: Build Variants (Recommended for F-Droid)

**1. Create build variants:**
```gradle
android {
    buildTypes {
        fdroid {
            // Exclude Google Drive and Dropbox
        }
        playstore {
            // Include all modules
        }
    }
}
```

**2. Use different source sets:**
```
src/
  ├── main/          # Core modules
  ├── fdroid/        # F-Droid specific (empty or stubs)
  └── playstore/     # Play Store specific (cloud modules)
```

**3. Module registration:**
```kotlin
// In HorizontalSQLiteOpenHelper
val provider = when {
    BuildConfig.FLAVOR == "fdroid" -> FdroidDatabaseModuleProvider()
    else -> DefaultDatabaseModuleProvider()
}
databaseModules.addAll(provider.getModules())
```

### Option B: Dependency Injection (Hilt)

**1. Create module providers:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModuleProviderModule {
    @Provides
    @Singleton
    fun provideDatabaseModules(): List<DatabaseModule> {
        val modules = mutableListOf<DatabaseModule>()
        modules.addAll(getCoreModules())
        
        // Conditionally add cloud modules
        if (BuildConfig.ENABLE_GOOGLE_DRIVE) {
            modules.add(GoogleDriveDatabaseModule())
        }
        // ... etc
        return modules
    }
}
```

**2. Inject into HorizontalSQLiteOpenHelper:**
```kotlin
class HorizontalSQLiteOpenHelper @Inject constructor(
    private val modules: List<DatabaseModule>
) {
    // Use injected modules
}
```

---

## Migration Steps

### Step 1: Split CloudDatabaseModule
1. Create `GoogleDriveDatabaseModule.java`
2. Create `DropboxDatabaseModule.java`
3. Create `NextCloudDatabaseModule.java`
4. Move table creation logic to respective modules
5. Update `HorizontalSQLiteOpenHelper` to use new modules

### Step 2: Create DatabaseModuleProvider
1. Implement `DatabaseModuleProvider` interface
2. Create `DefaultDatabaseModuleProvider`
3. Create `FdroidDatabaseModuleProvider` (excludes cloud modules)

### Step 3: Update HorizontalSQLiteOpenHelper
1. Accept `DatabaseModuleProvider` in constructor
2. Use provider to get modules dynamically
3. Remove hardcoded module registration

### Step 4: Refactor KeyDataSource
1. Make it accept a factory/provider
2. Conditionally initialize only enabled modules
3. Use dependency injection if using Hilt

### Step 5: Create Build Variants
1. Add `fdroid` and `playstore` flavors
2. Configure source sets
3. Test both builds

---

## Benefits

### For F-Droid Builds
- ✅ No Google Drive code
- ✅ No Dropbox code
- ✅ Smaller APK size
- ✅ No proprietary dependencies

### For Code Quality
- ✅ Better separation of concerns
- ✅ Easier testing (mock modules)
- ✅ Clearer dependencies
- ✅ More maintainable

### For Development
- ✅ Faster builds (exclude unused modules)
- ✅ Easier to add new cloud services
- ✅ Better IDE navigation
- ✅ Clearer module boundaries

---

## KeyDataSource Refactoring

### Current Issues
- Hardcoded initialization of all services
- No way to exclude modules
- Tight coupling

### Proposed Solution

**Option 1: Factory Pattern**
```kotlin
interface DataSourceFactory {
    fun createGoogleDrive(): GoogleDriveDataSource?
    fun createDropbox(): DropBoxDataSource?
    fun createNextCloud(): NextCloudDataSource?
}

class DefaultDataSourceFactory(
    private val context: Context,
    private val key: ByteArray
) : DataSourceFactory {
    override fun createGoogleDrive() = GoogleDriveDataSource.getInstance(context, key)
    override fun createDropbox() = DropBoxDataSource.getInstance(context, key)
    override fun createNextCloud() = NextCloudDataSource.getInstance(context, key)
}

class FdroidDataSourceFactory(
    private val context: Context,
    private val key: ByteArray
) : DataSourceFactory {
    override fun createGoogleDrive() = null // Not available
    override fun createDropbox() = null // Not available
    override fun createNextCloud() = NextCloudDataSource.getInstance(context, key)
}
```

**Option 2: Dependency Injection**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {
    @Provides
    @Singleton
    fun provideKeyDataSource(
        context: Context,
        @ApplicationContext appContext: Context
    ): KeyDataSource {
        val key = MyApplication.getMainKeyHolder().get().getKey().getEncoded()
        return KeyDataSource(context, key, getDataSourceFactory())
    }
}
```

---

## DataSource Refactoring (Future)

### Current State
- One large class with 2000+ lines
- Implements 8+ repository interfaces
- Mixes multiple concerns

### Proposed Split

**Option 1: Keep DataSource, Use Composition**
```kotlin
class DataSource(
    private val settingsRepo: SettingsRepository,
    private val formsRepo: FormsRepository,
    private val mediaRepo: MediaRepository,
    // ...
) : IServersRepository, ITellaUploadServersRepository, ... {
    // Delegate to repositories
}
```

**Option 2: Split into Separate Repositories**
```kotlin
// Each in its own file
class SettingsRepository : ISettingsRepository { }
class FormsRepository : IFormsRepository { }
class MediaRepository : IMediaRepository { }
// etc.
```

**Recommendation:** Start with Option 1 (composition), then gradually split if needed.

---

## Summary

1. **Single Database** ✅ - Keep one database, use modules
2. **Split CloudDatabaseModule** ✅ - Separate into 3 modules
3. **Dynamic Module Loading** ✅ - Use DatabaseModuleProvider
4. **Refactor KeyDataSource** ✅ - Use factory/DI pattern
5. **Build Variants** ✅ - F-Droid vs Play Store
6. **DataSource Refactoring** ⏳ - Future improvement

This architecture allows you to:
- Hide Google Drive/Dropbox for F-Droid
- Maintain clean code organization
- Test modules independently
- Add new modules easily
