package com.jianan.parkwhere.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.jianan.parkwhere.data.preferences.SettingsManager;

/**
 * Singleton Room database for storing {@link CarPark} entities.
 *
 * This database ensures only one instance exists throughout the application's
 * lifecycle. On fresh installation, the database is pre-populated from the bundled
 * {@code carpark.db} asset and persists between application restarts.
 *
 * The database provides an access point to the {@link CarParkDao}.
 */
@Database(entities = {CarPark.class}, version = 1)
public abstract class CarParkDatabase extends RoomDatabase {
    public abstract CarParkDao carParkDao();
    private static final String DB_NAME = "carpark.db";
    private static volatile CarParkDatabase instance;

    /**
     * Returns the singleton instance of {@link CarParkDatabase}.
     *
     * @param context the application context
     * @return the singleton {@link CarParkDatabase} instance
     */
    public static CarParkDatabase getDatabase(Context context, boolean shouldCreateFromAsset){
        if  (instance == null){
            // Synchronised prevents race conditions in multi-threaded environments by ensuring only one thread can create the INSTANCE at any point in time
            synchronized (CarParkDatabase.class) {
                if (instance == null) {
                    // Room database persist between application restarts after it is created from the fresh installation
                    // This check ensures that createFromAsset() is called only if the DB does not exist avoiding unnecessary access to the asset directory which may leak resources

                    Context appContext = context.getApplicationContext();
                    // SettingsManager settingsManager = new SettingsManager(appContext);

                    // Old code, delete after the new is integrated properly
                    // SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("park_where_preferences", Context.MODE_PRIVATE);
                    // boolean isFirstRun = prefs.getBoolean("fresh_install", true);

                    RoomDatabase.Builder<CarParkDatabase> builder = Room.databaseBuilder(
                            appContext,
                            CarParkDatabase.class,
                            DB_NAME
                    );

                    // Only use createFromAsset() if this is the first time the application is run
                    if (shouldCreateFromAsset) {
                        builder = builder.createFromAsset(DB_NAME);
                    }

                    instance = builder
                            .addCallback(new Callback() {
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    Log.d("CarParkDatabase", "Database has been opened.");
                                }
                            })
                            .build();
                }
            }
        }
        return instance;
    }
}
