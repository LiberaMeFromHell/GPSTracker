package com.example.gpstracker.model.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.gpstracker.model.pojo.LocationPoint;

import java.util.List;

@Dao
public interface TrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LocationPoint> locationPoints);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocationPoint locationPoint);

    @Query("DELETE FROM LocationPoint")
    void deleteAll();

    @Query("SELECT * FROM LocationPoint")
    List<LocationPoint> getAll();
}
