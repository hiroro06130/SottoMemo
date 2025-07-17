package com.example.sottomemo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface EventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Event event);

    @Update // この行を追加
    void update(Event event);

    @Delete // この行を追加
    void delete(Event event);

    @Query("DELETE FROM event_table WHERE memoId = :memoId")
    void deleteEventsByMemoId(long memoId);

    @Query("SELECT * FROM event_table WHERE eventDate >= :startOfDay AND eventDate < :endOfDay ORDER BY time ASC")
    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay);
}