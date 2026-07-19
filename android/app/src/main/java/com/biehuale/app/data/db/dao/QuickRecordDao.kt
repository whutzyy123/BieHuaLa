package com.biehuale.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.biehuale.app.data.db.entity.QuickRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickRecordDao {

    @Query("SELECT * FROM quick_records ORDER BY sort_order ASC, id ASC")
    fun observeAll(): Flow<List<QuickRecordEntity>>

    @Query("SELECT * FROM quick_records ORDER BY sort_order ASC, id ASC")
    suspend fun getAll(): List<QuickRecordEntity>

    @Query("SELECT * FROM quick_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QuickRecordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: QuickRecordEntity): Long

    @Update
    suspend fun update(entity: QuickRecordEntity)

    @Query("DELETE FROM quick_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quick_records WHERE category_id = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)

    @Query("DELETE FROM quick_records WHERE account_id = :accountId")
    suspend fun deleteByAccountId(accountId: Long)

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM quick_records")
    suspend fun maxSortOrder(): Int
}
