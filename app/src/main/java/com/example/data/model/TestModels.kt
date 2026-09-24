package com.example.data.model

enum class TestPhase {
    IDLE,
    PREPARING,
    WRITING,
    VERIFYING,
    PAUSED,
    COMPLETED,
    CANCELLED,
    ERROR
}

enum class BlockState {
    PENDING,
    WRITING,
    VERIFYING,
    PASSED,
    CORRUPTED
}

data class BlockInfo(
    val index: Int,
    val sizeMb: Int,
    var state: BlockState = BlockState.PENDING,
    var writeSpeedMbps: Double = 0.0,
    var readSpeedMbps: Double = 0.0,
    var corruptBytes: Long = 0L,
    var fileName: String = ""
)

enum class TestPreset(val displayName: String, val sizeMb: Long) {
    QUICK("Quick (256 MB)", 256L),
    STANDARD("Standard (1 GB)", 1024L),
    HEAVY("Heavy (4 GB)", 4096L),
    EXTREME("Extreme (16 GB)", 16384L),
    CUSTOM("Custom", -1L),
    FULL_DRIVE("Full Storage", -2L)
}

data class TestConfig(
    val preset: TestPreset = TestPreset.QUICK,
    val customSizeMb: Long = 512L,
    val blockSizeMb: Int = 16, // 4, 16, or 64 MB
    val autoDeleteTestFiles: Boolean = true,
    val verifyMethod: VerifyMethod = VerifyMethod.FULL,
    val simulateFakeDrive: Boolean = false // Demo mode for testing without real fake USB
)

enum class VerifyMethod(val title: String, val description: String) {
    FULL("Full Verification", "Check every single byte with seed pattern"),
    SAMPLING("Fast Sampling", "Check headers, footers, and random byte samples"),
    REVERSE("Reverse Sequential", "Read back blocks in reverse order to detect cyclic buffer overwrites")
}

data class LiveSpeedPoint(
    val timestampMs: Long,
    val writeSpeedMbps: Double,
    val readSpeedMbps: Double
)

enum class LogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR,
    SYSTEM
}

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class StorageDriveInfo(
    val uriString: String? = null,
    val name: String = "USBストレージ未選択",
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val isWritable: Boolean = false,
    val isUsbOtg: Boolean = false,
    val isFallbackInternal: Boolean = false
) {
    val isSelected: Boolean
        get() = isUsbOtg || isFallbackInternal

    val totalGbFormatted: String
        get() = if (totalBytes > 0) String.format("%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0)) else "-- GB"

    val freeGbFormatted: String
        get() = if (totalBytes > 0) String.format("%.2f GB", freeBytes / (1024.0 * 1024.0 * 1024.0)) else "-- GB"

    val usedGbFormatted: String
        get() {
            if (totalBytes <= 0) return "-- GB"
            val used = (totalBytes - freeBytes).coerceAtLeast(0L)
            return String.format("%.2f GB", used / (1024.0 * 1024.0 * 1024.0))
        }

    val usedRatio: Float
        get() = if (totalBytes > 0) ((totalBytes - freeBytes).toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

data class TestProgressState(
    val phase: TestPhase = TestPhase.IDLE,
    val currentBlockIndex: Int = 0,
    val totalBlocks: Int = 0,
    val bytesWritten: Long = 0L,
    val bytesVerified: Long = 0L,
    val targetBytes: Long = 0L,
    val currentWriteSpeedMbps: Double = 0.0,
    val currentReadSpeedMbps: Double = 0.0,
    val maxWriteSpeedMbps: Double = 0.0,
    val maxReadSpeedMbps: Double = 0.0,
    val avgWriteSpeedMbps: Double = 0.0,
    val avgReadSpeedMbps: Double = 0.0,
    val corruptBlockCount: Int = 0,
    val corruptBytesCount: Long = 0L,
    val elapsedTimeMs: Long = 0L,
    val estimatedTimeRemainingMs: Long = 0L,
    val blocks: List<BlockInfo> = emptyList(),
    val errorMessage: String? = null,
    val statusText: String = "準備完了"
) {
    val overallProgressRatio: Float
        get() {
            if (targetBytes <= 0L) return 0f
            return when (phase) {
                TestPhase.WRITING, TestPhase.PREPARING -> {
                    (bytesWritten.toFloat() / targetBytes.toFloat() * 0.5f).coerceIn(0f, 0.5f)
                }
                TestPhase.VERIFYING -> {
                    (0.5f + (bytesVerified.toFloat() / targetBytes.toFloat() * 0.5f)).coerceIn(0.5f, 1f)
                }
                TestPhase.COMPLETED -> 1.0f
                else -> 0f
            }
        }

    val phaseProgressRatio: Float
        get() {
            if (targetBytes <= 0L) return 0f
            return when (phase) {
                TestPhase.WRITING -> (bytesWritten.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f)
                TestPhase.VERIFYING -> (bytesVerified.toFloat() / targetBytes.toFloat()).coerceIn(0f, 1f)
                TestPhase.COMPLETED -> 1.0f
                else -> 0f
            }
        }
}
