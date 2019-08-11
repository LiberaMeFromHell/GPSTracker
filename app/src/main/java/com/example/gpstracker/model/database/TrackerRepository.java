package com.example.gpstracker.model.database;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.gpstracker.model.pojo.LocationPoint;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class TrackerRepository {

    private static TrackerDatabase db;

    private static TrackerRepository repository;

    public TrackerRepository(Context context) {
        db = TrackerDatabase.getInstance(context);
    }

    public static TrackerRepository getInstance(Context context) {
        if (repository == null) {
            repository = new TrackerRepository(context);
        }
        return repository;
    }

    public static void insert(LocationPoint location) {
        try {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    db.trackerDao().insert(location);
                    return null;
                }
            }.execute().get();

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void deleteAll() {
        try {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... voids) {
                    db.trackerDao().deleteAll();
                    return null;
                }
            }.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static List<LocationPoint> getAll() {
        return db.trackerDao().getAll();
    }
}



