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
    long insert(Event event);

    @Update // この行を追加
    void update(Event event);

    @Delete // この行を追加
    void delete(Event event);

    @Query("DELETE FROM event_table WHERE memoId = :memoId")
    void deleteEventsByMemoId(long memoId);

    @Query("SELECT * FROM event_table WHERE eventDate >= :startOfDay AND eventDate < :endOfDay ORDER BY time ASC")
    LiveData<List<Event>> getEventsForDay(long startOfDay, long endOfDay);

    @Query("SELECT * FROM event_table WHERE memoId = :memoId")
    List<Event> getEventsByMemoIdSync(long memoId);

    @Query("SELECT * FROM event_table WHERE eventDate >= :startOfDay AND eventDate < :endOfDay ORDER BY time ASC")
    List<Event> getEventsForDaySync(long startOfDay, long endOfDay);

    @Query("SELECT * FROM event_table WHERE id = :id LIMIT 1")
    Event getEventByIdSync(long id);

    @Query("SELECT * FROM event_table WHERE eventDate >= :currentTime ORDER BY eventDate ASC LIMIT 1")
    LiveData<Event> getNextEvent(long currentTime);
}