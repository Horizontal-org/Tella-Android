# Database Module Migration Status

## ✅ Completed

1. ✅ Created `tella-database` module structure
2. ✅ Created `build.gradle` for tella-database
3. ✅ Updated `settings.gradle` to include tella-database
4. ✅ Updated `mobile/build.gradle` to depend on tella-database
5. ✅ Copied all database files to tella-database module

## ⚠️ Pending Issues

### 1. Preferences Dependency
**Issue:** `CipherOpenHelper` and `HorizontalSQLiteOpenHelper` depend on `org.horizontal.tella.mobile.data.sharedpref.Preferences`

**Solution Options:**
- **Option A (Recommended):** Create `DatabasePreferences` interface in tella-database, mobile provides implementation
- **Option B:** Move Preferences to a separate module (tella-preferences)
- **Option C:** Keep Preferences dependency and handle via compile-time resolution

**Current Status:** Created `DatabasePreferences.kt` interface, need to update CipherOpenHelper to use it.

### 2. Domain Entity Dependencies
**Issue:** `DataSource` and `*DataSource` classes depend on domain entities in mobile module

**Solution:** 
- Keep DataSource classes in mobile module (they implement domain repository interfaces)
- They can use `HorizontalSQLiteOpenHelper` from tella-database
- This is actually the correct architecture - repositories belong in the app layer

### 3. Files to Keep in Mobile
These files should stay in mobile because they depend on domain entities:
- `DataSource.java`
- `GoogleDriveDataSource.java`
- `DropBoxDataSource.java`
- `NextCloudDataSource.java`
- `UwaziDataSource.java`
- `ResourceDataSource.java`
- `KeyDataSource.java` (depends on DataSource classes)
- `DataBaseUtils.kt` (may depend on domain entities)

### 4. Files Successfully Moved to tella-database
- `CipherOpenHelper.java` (needs Preferences abstraction)
- `HorizontalSQLiteOpenHelper.java` (needs Preferences abstraction)
- `D.kt`
- `DatabaseModuleProvider.kt`
- `Hex.java`
- All `modules/` directory (DatabaseModule interface and all module implementations)

## Next Steps

1. **Update CipherOpenHelper** to accept `DatabasePreferences` instead of static imports
2. **Update HorizontalSQLiteOpenHelper** to accept `DatabasePreferences`
3. **Create PreferencesAdapter** in mobile that implements `DatabasePreferences`
4. **Move DataSource classes back to mobile** (they belong there)
5. **Update imports** in mobile module
6. **Test compilation** of both modules

## Architecture Decision

**Final Structure:**
```
tella-database/
  ├── Core infrastructure (CipherOpenHelper, HorizontalSQLiteOpenHelper)
  ├── Database modules (Settings, Forms, Media, etc.)
  ├── D.kt (constants)
  └── DatabasePreferences interface

mobile/
  ├── DataSource classes (implement domain repositories)
  ├── PreferencesAdapter (implements DatabasePreferences)
  └── Uses tella-database for database operations
```

This architecture:
- ✅ Separates database schema from business logic
- ✅ Keeps repositories in app layer (correct architecture)
- ✅ Allows F-Droid builds (can exclude cloud modules)
- ✅ Maintains clean dependencies
