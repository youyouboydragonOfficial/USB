package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_reports")
data class TestReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestampMs: Long,
    val driveName: String,
    val driveUri: String?,
    val testedSizeMb: Long,
    val verifiedSizeMb: Long,
    val totalBlocks: Int,
    val passedBlocks: Int,
    val corruptBlocks: Int,
    val avgWriteSpeedMbps: Double,
    val avgReadSpeedMbps: Double,
    val maxWriteSpeedMbps: Double,
    val maxReadSpeedMbps: Double,
    val durationSeconds: Long,
    val isFakeDrive: Boolean,
    val isGenuine: Boolean,
    val healthScore: Int, // 0 to 100
    val summaryText: String,
    val logSnippetJson: String
)
