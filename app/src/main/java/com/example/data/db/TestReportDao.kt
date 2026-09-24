package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TestReportDao {
    @Query("SELECT * FROM test_reports ORDER BY timestampMs DESC")
    fun getAllReports(): Flow<List<TestReportEntity>>

    @Query("SELECT * FROM test_reports WHERE id = :id")
    suspend fun getReportById(id: Long): TestReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: TestReportEntity): Long

    @Query("DELETE FROM test_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM test_reports")
    suspend fun clearAllReports()
}
