package ru.netology.nework.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.data.db.entity.EventEntity  // Этот импорт должен быть!

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY datetime DESC")
    fun getAllLiveData(): LiveData<List<EventEntity>>  // Возвращает List<EventEntity>

    @Query("SELECT * FROM events ORDER BY datetime DESC")
    suspend fun getAll(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(events: List<EventEntity>)

    @Query("DELETE FROM events")
    suspend fun clearAll()

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)
}