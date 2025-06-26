package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Event event);

    @Query("SELECT * FROM event_table WHERE eventDate >= :startOfDay AND eventDate < :endOfDay ORDER BY time ASC")
    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay);
}