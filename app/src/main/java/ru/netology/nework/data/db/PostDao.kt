package ru.netology.nework.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nework.data.db.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY published DESC")
    suspend fun getAll(): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clearAll()
}