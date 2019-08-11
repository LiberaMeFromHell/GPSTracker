package com.example.gpstracker.model.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.gpstracker.model.pojo.LocationPoint;

@Database(entities = {LocationPoint.class}, version = 1, exportSchema = false)
public abstract class TrackerDatabase extends RoomDatabase {

    abstract TrackerDao trackerDao();

    private static TrackerDatabase db;

    static TrackerDatabase getInstance(Context context) {
        if (db == null) {
            db = Room.databaseBuilder(context, TrackerDatabase.class,"GPSTracker")
                    .build();
        }
        return db;
    }

}
