package com.jianan.parkwhere.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {CarPark.class}, version = 1)
public abstract class CarParkDatabase extends RoomDatabase {
    public abstract CarParkDao carParkDao();
    private static final String DB_NAME = "carpark.db";
    private static volatile CarParkDatabase INSTANCE;

    public static CarParkDatabase getDatabase(Context context){
        if  (INSTANCE == null){
            // Synchronised prevents race conditions in multi-threaded environments by ensuring only one thread can create the INSTANCE at any point in time
            synchronized (CarParkDatabase.class) {
                if (INSTANCE == null) {

                    // Room database persist between application restarts after it is created from the fresh installation
                    // This check ensures that createFromAsset() is called only if the DB does not exist avoiding unnecessary access to the asset directory which may leak resources
                    SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("park_where_preferences", Context.MODE_PRIVATE);
                    boolean isFirstRun = prefs.getBoolean("fresh_install", true);

                    RoomDatabase.Builder<CarParkDatabase> builder = Room.databaseBuilder(
                            context.getApplicationContext(),
                            CarParkDatabase.class,
                            DB_NAME
                    );

                    // Only use createFromAsset() if this is the first time the application is run
                    if (isFirstRun) {
                        builder = builder.createFromAsset(DB_NAME);

                        // Mark database as initialised after fresh installation
                        prefs.edit().putBoolean("fresh_install", false).apply();
                    }

                    INSTANCE = builder
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Log.d("CarParkDatabase", "Database has been created.");
                                }

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
        return INSTANCE;
    }
}
