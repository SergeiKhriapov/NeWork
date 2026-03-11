package ru.netology.nework.data.db.dao

import androidx.lifecycle.LiveData
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

    @Query("SELECT * FROM posts ORDER BY published DESC")
    fun getAllLiveData(): LiveData<List<PostEntity>>

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getById(id: Long): PostEntity?
}