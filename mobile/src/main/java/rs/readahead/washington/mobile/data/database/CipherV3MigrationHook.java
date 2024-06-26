//package rs.readahead.washington.mobile.data.database;
//
//
//
//import net.sqlcipher.database.SQLiteDatabaseHook;
//
//import rs.readahead.washington.mobile.data.sharedpref.SharedPrefs;
//
//
//class CipherV3MigrationHook implements SQLiteDatabaseHook {
//
//    CipherV3MigrationHook() {
//    }
//
////    @Override
//    public void preKey(SQLiteDatabase database) {
////        // nop for now
////    }
////
////    @Override
////    public void postKey(SQLiteDatabase database) {
////        /* V2 - V3 migration */
////        if (!isMigratedV3(database)) {
////            database.rawExecSQL("PRAGMA cipher_migrate;");
////            setMigratedV3(database, true);
////        }
////    }
//
//    private static void setMigratedV3(SQLiteDatabase database, boolean migrated) {
//        SharedPrefs.getInstance().getPref().edit().putBoolean(database.getPath(), migrated).apply();
//    }
//
//    private static boolean isMigratedV3(SQLiteDatabase database) {
//        return SharedPrefs.getInstance().getPref().getBoolean(database.getPath(), false);
//    }
//
//    @Override
//    public void preKey(SQLiteConnection sqLiteConnection) {
//
//    }
//
//    @Override
//    public void postKey(SQLiteConnection sqLiteConnection) {
//
//    }
//}
