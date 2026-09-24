package com.example.engine

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.coroutineContext

class UsbTesterEngine(
    private val context: Context,
    private val contentResolver: ContentResolver
) {
    private val _progressState = MutableStateFlow(TestProgressState())
    val progressState: StateFlow<TestProgressState> = _progressState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _speedPoints = MutableStateFlow<List<LiveSpeedPoint>>(emptyList())
    val speedPoints: StateFlow<List<LiveSpeedPoint>> = _speedPoints.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var isPaused = false

    @Volatile
    private var isCancelled = false

    fun pauseTest() {
        isPaused = true
        addLog(LogLevel.WARNING, "TEST", "テストを一時停止しました")
        _progressState.update { it.copy(phase = TestPhase.PAUSED) }
    }

    fun resumeTest() {
        isPaused = false
        addLog(LogLevel.INFO, "TEST", "テストを再開しました")
        _progressState.update { 
            val prevPhase = if (it.bytesVerified > 0) TestPhase.VERIFYING else TestPhase.WRITING
            it.copy(phase = prevPhase) 
        }
    }

    fun cancelTest() {
        isCancelled = true
        addLog(LogLevel.WARNING, "TEST", "テスト中止リクエストを受信しました")
        _progressState.update { it.copy(phase = TestPhase.CANCELLED, statusText = "テストが中止されました") }
    }

    private fun addLog(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = level,
            tag = tag,
            message = message
        )
        _logs.update { current ->
            (current + entry).takeLast(200)
        }
    }

    suspend fun executeTest(
        targetFolderUri: Uri?,
        config: TestConfig
    ): TestProgressState = withContext(Dispatchers.IO) {
        isPaused = false
        isCancelled = false
        _speedPoints.value = emptyList()
        _logs.value = emptyList()

        addLog(LogLevel.SYSTEM, "INIT", "=== USB Drive Guard 検証エンジン起動 ===")
        addLog(LogLevel.INFO, "CFG", "プリセット: ${config.preset.displayName}, ブロック: ${config.blockSizeMb}MB, デモ偽装モード: ${config.simulateFakeDrive}")

        // 1. Resolve target location
        val (targetDir, isInternal) = resolveTargetDirectory(targetFolderUri)
        if (targetDir == null) {
            val errorMsg = "書き込み先ストレージフォルダの取得に失敗しました。"
            addLog(LogLevel.ERROR, "INIT", errorMsg)
            _progressState.update { it.copy(phase = TestPhase.ERROR, errorMessage = errorMsg) }
            return@withContext _progressState.value
        }

        addLog(LogLevel.SUCCESS, "INIT", "テスト対象フォルダ確認完了: ${targetDir.name ?: "Unknown"}")

        // 2. Prepare test dataset layout
        val totalBytesToTest = calculateTargetBytes(config, targetDir)
        val blockSizeBytes = config.blockSizeMb * 1024 * 1024L
        val blockCount = ((totalBytesToTest + blockSizeBytes - 1) / blockSizeBytes).toInt().coerceAtLeast(1)

        val initialBlocks = List(blockCount) { index ->
            val actualSizeMb = if (index == blockCount - 1) {
                val rem = totalBytesToTest % blockSizeBytes
                if (rem == 0L) config.blockSizeMb else (rem / (1024 * 1024)).toInt().coerceAtLeast(1)
            } else {
                config.blockSizeMb
            }
            BlockInfo(
                index = index,
                sizeMb = actualSizeMb,
                state = BlockState.PENDING,
                fileName = "usb_guard_block_%04d.dat".format(index)
            )
        }

        _progressState.value = TestProgressState(
            phase = TestPhase.PREPARING,
            totalBlocks = blockCount,
            targetBytes = totalBytesToTest,
            blocks = initialBlocks,
            statusText = "テスト用ファイル領域準備中..."
        )

        addLog(LogLevel.INFO, "PLAN", "総テスト容量: ${totalBytesToTest / (1024 * 1024)} MB (${blockCount} ブロック)")

        // Create container test subfolder "_USB_GUARD_TEST_"
        val testSubDir = getOrCreateTestFolder(targetDir)
        if (testSubDir == null) {
            val errorMsg = "テスト用サブフォルダの作成に失敗しました。書き込み権限を確認してください。"
            addLog(LogLevel.ERROR, "PREP", errorMsg)
            _progressState.update { it.copy(phase = TestPhase.ERROR, errorMessage = errorMsg) }
            return@withContext _progressState.value
        }

        val startTimeMs = System.currentTimeMillis()
        var totalWriteTimeMs = 0L
        var totalReadTimeMs = 0L

        // Write Speed tracking
        var maxWriteMbps = 0.0
        var maxReadMbps = 0.0

        try {
            // ==========================================
            // PHASE 1: WRITE DUMMY TEST BLOCKS
            // ==========================================
            _progressState.update { it.copy(phase = TestPhase.WRITING, statusText = "ダミーデータ書き込み中...") }
            addLog(LogLevel.INFO, "WRITE", "ダミーデータパターンの連続書き込みを開始します...")

            val writeChunkBuffer = ByteArray(256 * 1024) // 256KB I/O buffer

            for (i in 0 until blockCount) {
                checkCancellation()
                waitIfPaused()

                val block = _progressState.value.blocks[i]
                updateBlockState(i, BlockState.WRITING)

                val blockFile = testSubDir.createFile("application/octet-stream", block.fileName)
                if (blockFile == null) {
                    addLog(LogLevel.ERROR, "WRITE", "ブロック #${i} のファイル作成に失敗しました")
                    updateBlockState(i, BlockState.CORRUPTED)
                    continue
                }

                val targetBlockBytes = block.sizeMb * 1024 * 1024L
                var blockBytesWritten = 0L
                val blockStartTime = System.currentTimeMillis()

                val outputStream: OutputStream? = openOutputStream(blockFile)
                if (outputStream == null) {
                    addLog(LogLevel.ERROR, "WRITE", "ファイル出力ストリームのオープンに失敗: ${block.fileName}")
                    updateBlockState(i, BlockState.CORRUPTED)
                    continue
                }

                try {
                    outputStream.use { stream ->
                        while (blockBytesWritten < targetBlockBytes) {
                            checkCancellation()
                            waitIfPaused()

                            val bytesToGenerate = minOf(writeChunkBuffer.size.toLong(), targetBlockBytes - blockBytesWritten).toInt()
                            
                            // Generate deterministic seed block pattern
                            generateSeedPattern(writeChunkBuffer, i, blockBytesWritten, bytesToGenerate)

                            // If fake drive simulation mode is active and block > 2, simulate silent data wrap/truncation
                            if (config.simulateFakeDrive && i >= 2 && blockBytesWritten > (targetBlockBytes / 2)) {
                                // Simulate controller overwriting header or zero filling
                                for (k in 0 until bytesToGenerate) {
                                    writeChunkBuffer[k] = 0x00.toByte()
                                }
                            }

                            val writeChunkStart = System.currentTimeMillis()
                            stream.write(writeChunkBuffer, 0, bytesToGenerate)
                            val writeChunkDuration = (System.currentTimeMillis() - writeChunkStart).coerceAtLeast(1)

                            blockBytesWritten += bytesToGenerate
                            val currentTotalWritten = _progressState.value.bytesWritten + bytesToGenerate

                            // Speed calculation
                            val chunkMbps = (bytesToGenerate.toDouble() / (1024 * 1024)) / (writeChunkDuration / 1000.0)
                            if (chunkMbps > maxWriteMbps && chunkMbps < 5000.0) {
                                maxWriteMbps = chunkMbps
                            }

                            val currentElapsedMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1)
                            val avgWriteMbps = (currentTotalWritten.toDouble() / (1024 * 1024)) / (currentElapsedMs / 1000.0)
                            val bytesRemaining = (totalBytesToTest - currentTotalWritten).coerceAtLeast(0L)
                            val etaMs = if (avgWriteMbps > 0) ((bytesRemaining / (1024 * 1024)) / avgWriteMbps * 1000).toLong() else 0L

                            _progressState.update { state ->
                                state.copy(
                                    currentBlockIndex = i,
                                    bytesWritten = currentTotalWritten,
                                    currentWriteSpeedMbps = chunkMbps,
                                    avgWriteSpeedMbps = avgWriteMbps,
                                    maxWriteSpeedMbps = maxWriteMbps,
                                    elapsedTimeMs = currentElapsedMs,
                                    estimatedTimeRemainingMs = etaMs,
                                    statusText = "書き込み中 [Block ${i + 1}/$blockCount] (${String.format("%.1f", chunkMbps)} MB/s)"
                                )
                            }

                            // Record live speed point
                            if (currentTotalWritten % (2 * 1024 * 1024) == 0L) {
                                recordSpeedPoint(chunkMbps, 0.0)
                            }
                        }
                        stream.flush()
                    }

                    val blockDuration = (System.currentTimeMillis() - blockStartTime).coerceAtLeast(1)
                    totalWriteTimeMs += blockDuration
                    val blockMbps = (block.sizeMb.toDouble()) / (blockDuration / 1000.0)

                    addLog(LogLevel.INFO, "WRITE", "ブロック #${i + 1} 作成完了 (${block.sizeMb}MB, ${String.format("%.1f", blockMbps)} MB/s)")
                    updateBlockState(i, BlockState.PENDING, writeSpeed = blockMbps)

                } catch (e: Exception) {
                    addLog(LogLevel.ERROR, "WRITE", "ブロック #${i + 1} 書き込みエラー: ${e.localizedMessage}")
                    updateBlockState(i, BlockState.CORRUPTED)
                }
            }

            val totalWrittenMb = _progressState.value.bytesWritten / (1024 * 1024)
            addLog(LogLevel.SUCCESS, "WRITE", "全ダミーデータ書き込み完了: ${totalWrittenMb}MB")

            // ==========================================
            // PHASE 2: VERIFICATION & CHECKSUM READ
            // ==========================================
            _progressState.update { it.copy(phase = TestPhase.VERIFYING, statusText = "データ整合性・検証中...") }
            addLog(LogLevel.INFO, "VERIFY", "書き込みデータの読み出し検証（偽装・破損テスト）を開始します...")

            val readChunkBuffer = ByteArray(256 * 1024)
            val expectedChunkBuffer = ByteArray(256 * 1024)
            val verifyStartTime = System.currentTimeMillis()

            var corruptBlockCounter = 0
            var totalCorruptBytes = 0L

            val verificationOrder = when (config.verifyMethod) {
                VerifyMethod.REVERSE -> (0 until blockCount).reversed()
                else -> (0 until blockCount).toList()
            }

            for (i in verificationOrder) {
                checkCancellation()
                waitIfPaused()

                val block = _progressState.value.blocks[i]
                updateBlockState(i, BlockState.VERIFYING)

                val blockFile = testSubDir.findFile(block.fileName)
                if (blockFile == null || !blockFile.exists()) {
                    addLog(LogLevel.ERROR, "VERIFY", "ブロック #${i + 1} のファイルが見つかりません (消去/オーバーライト疑惑)")
                    corruptBlockCounter++
                    totalCorruptBytes += block.sizeMb * 1024 * 1024L
                    updateBlockState(i, BlockState.CORRUPTED)
                    continue
                }

                val inputStream: InputStream? = openInputStream(blockFile)
                if (inputStream == null) {
                    addLog(LogLevel.ERROR, "VERIFY", "ブロック #${i + 1} 読み出しストリームオープン失敗")
                    corruptBlockCounter++
                    totalCorruptBytes += block.sizeMb * 1024 * 1024L
                    updateBlockState(i, BlockState.CORRUPTED)
                    continue
                }

                var blockBytesRead = 0L
                var blockCorruptBytes = 0L
                val blockReadStart = System.currentTimeMillis()

                try {
                    inputStream.use { stream ->
                        val targetBlockBytes = block.sizeMb * 1024 * 1024L

                        while (blockBytesRead < targetBlockBytes) {
                            checkCancellation()
                            waitIfPaused()

                            val bytesToRead = minOf(readChunkBuffer.size.toLong(), targetBlockBytes - blockBytesRead).toInt()
                            val actualRead = stream.read(readChunkBuffer, 0, bytesToRead)

                            if (actualRead <= 0) {
                                // Unexpected EOF -> file truncated
                                val missing = bytesToRead
                                blockCorruptBytes += missing
                                break
                            }

                            // Generate expected pattern for verification comparison
                            generateSeedPattern(expectedChunkBuffer, i, blockBytesRead, actualRead)

                            val readChunkStart = System.currentTimeMillis()
                            val durationMs = (System.currentTimeMillis() - readChunkStart).coerceAtLeast(1)

                            // Byte-by-byte checksum comparison
                            for (b in 0 until actualRead) {
                                if (readChunkBuffer[b] != expectedChunkBuffer[b]) {
                                    blockCorruptBytes++
                                }
                            }

                            blockBytesRead += actualRead
                            val currentTotalVerified = _progressState.value.bytesVerified + actualRead

                            val chunkReadMbps = (actualRead.toDouble() / (1024 * 1024)) / (durationMs / 1000.0)
                            if (chunkReadMbps > maxReadMbps && chunkReadMbps < 5000.0) {
                                maxReadMbps = chunkReadMbps
                            }

                            val verifyElapsedMs = (System.currentTimeMillis() - verifyStartTime).coerceAtLeast(1)
                            val avgReadMbps = (currentTotalVerified.toDouble() / (1024 * 1024)) / (verifyElapsedMs / 1000.0)
                            val remainingVerifyBytes = (totalBytesToTest - currentTotalVerified).coerceAtLeast(0L)
                            val verifyEtaMs = if (avgReadMbps > 0) ((remainingVerifyBytes / (1024 * 1024)) / avgReadMbps * 1000).toLong() else 0L

                            _progressState.update { state ->
                                state.copy(
                                    currentBlockIndex = i,
                                    bytesVerified = currentTotalVerified,
                                    currentReadSpeedMbps = chunkReadMbps,
                                    avgReadSpeedMbps = avgReadMbps,
                                    maxReadSpeedMbps = maxReadMbps,
                                    corruptBytesCount = totalCorruptBytes + blockCorruptBytes,
                                    corruptBlockCount = corruptBlockCounter,
                                    elapsedTimeMs = (System.currentTimeMillis() - startTimeMs),
                                    estimatedTimeRemainingMs = verifyEtaMs,
                                    statusText = "検証中 [Block ${i + 1}/$blockCount] (${String.format("%.1f", chunkReadMbps)} MB/s)"
                                )
                            }

                            if (currentTotalVerified % (2 * 1024 * 1024) == 0L) {
                                recordSpeedPoint(0.0, chunkReadMbps)
                            }
                        }
                    }

                    val blockReadDuration = (System.currentTimeMillis() - blockReadStart).coerceAtLeast(1)
                    totalReadTimeMs += blockReadDuration
                    val blockReadMbps = (block.sizeMb.toDouble()) / (blockReadDuration / 1000.0)

                    if (blockCorruptBytes > 0) {
                        corruptBlockCounter++
                        totalCorruptBytes += blockCorruptBytes
                        addLog(LogLevel.ERROR, "VERIFY", "❌ ブロック #${i + 1} 破損検出! 異物/消失バイト: $blockCorruptBytes バイト")
                        updateBlockState(i, BlockState.CORRUPTED, readSpeed = blockReadMbps, corruptBytes = blockCorruptBytes)
                    } else {
                        addLog(LogLevel.SUCCESS, "VERIFY", "✅ ブロック #${i + 1} 整合性チェックPASS (${block.sizeMb}MB, ${String.format("%.1f", blockReadMbps)} MB/s)")
                        updateBlockState(i, BlockState.PASSED, readSpeed = blockReadMbps)
                    }

                } catch (e: Exception) {
                    corruptBlockCounter++
                    totalCorruptBytes += block.sizeMb * 1024 * 1024L
                    addLog(LogLevel.ERROR, "VERIFY", "ブロック #${i + 1} 読み込み例外: ${e.localizedMessage}")
                    updateBlockState(i, BlockState.CORRUPTED)
                }
            }

            // Clean up test files if auto-cleanup enabled
            if (config.autoDeleteTestFiles) {
                addLog(LogLevel.INFO, "CLEAN", "自動クリーンアップ: テストファイルを削除します...")
                cleanupTestSubFolder(testSubDir)
                addLog(LogLevel.SUCCESS, "CLEAN", "テスト用ファイルの自動削除完了")
            }

            // ==========================================
            // PHASE 3: SUMMARY & DIAGNOSIS
            // ==========================================
            _progressState.update { state ->
                val finalStatus = if (corruptBlockCounter > 0) {
                    "⚠️ 容量偽装 / データ破損を検出しました! (破損ブロック: $corruptBlockCounter)"
                } else {
                    "✨ 検証合格! データ破損・容量偽装は検出されませんでした。"
                }
                state.copy(
                    phase = TestPhase.COMPLETED,
                    corruptBlockCount = corruptBlockCounter,
                    corruptBytesCount = totalCorruptBytes,
                    statusText = finalStatus
                )
            }

            if (corruptBlockCounter > 0) {
                addLog(LogLevel.ERROR, "RESULT", "🚨 【警告】 偽装容量/セクタ不良検出! 合計破損量: ${totalCorruptBytes / (1024 * 1024)} MB")
            } else {
                addLog(LogLevel.SUCCESS, "RESULT", "🎉 【正常】 USBストレージの信頼性が確認されました。")
            }

        } catch (e: Exception) {
            if (isCancelled) {
                addLog(LogLevel.WARNING, "CANCEL", "ユーザーによりテストがキャンセルされました")
                if (config.autoDeleteTestFiles) {
                    cleanupTestSubFolder(testSubDir)
                }
            } else {
                val err = "テスト実行中の予期せぬエラー: ${e.localizedMessage}"
                addLog(LogLevel.ERROR, "FATAL", err)
                _progressState.update { it.copy(phase = TestPhase.ERROR, errorMessage = err) }
            }
        }

        return@withContext _progressState.value
    }

    fun cleanupTestFiles(targetFolderUri: Uri?) {
        val (targetDir, _) = resolveTargetDirectory(targetFolderUri)
        if (targetDir == null) return
        val testSubDir = targetDir.findFile("_USB_GUARD_TEST_DATA_") ?: return
        cleanupTestSubFolder(testSubDir)
        addLog(LogLevel.SUCCESS, "CLEAN", "手動クリーンアップ完了")
    }

    private fun cleanupTestSubFolder(testSubDir: DocumentFile) {
        try {
            for (file in testSubDir.listFiles()) {
                file.delete()
            }
            testSubDir.delete()
        } catch (e: Exception) {
            addLog(LogLevel.WARNING, "CLEAN", "クリーンアップ中の警告: ${e.localizedMessage}")
        }
    }

    private fun recordSpeedPoint(writeMbps: Double, readMbps: Double) {
        val now = System.currentTimeMillis()
        val point = LiveSpeedPoint(now, writeMbps, readMbps)
        _speedPoints.update { current ->
            (current + point).takeLast(60) // keep last 60 points for dynamic graph
        }
    }

    private fun updateBlockState(
        index: Int,
        newState: BlockState,
        writeSpeed: Double = -1.0,
        readSpeed: Double = -1.0,
        corruptBytes: Long = 0L
    ) {
        _progressState.update { current ->
            val updatedList = current.blocks.mapIndexed { idx, block ->
                if (idx == index) {
                    block.copy(
                        state = newState,
                        writeSpeedMbps = if (writeSpeed >= 0) writeSpeed else block.writeSpeedMbps,
                        readSpeedMbps = if (readSpeed >= 0) readSpeed else block.readSpeedMbps,
                        corruptBytes = if (corruptBytes > 0) corruptBytes else block.corruptBytes
                    )
                } else block
            }
            current.copy(blocks = updatedList)
        }
    }

    private fun checkCancellation() {
        if (isCancelled) throw InterruptedException("Cancelled by user")
    }

    private suspend fun waitIfPaused() {
        while (isPaused && !isCancelled) {
            coroutineContext.ensureActive()
            kotlinx.coroutines.delay(200)
        }
    }

    private fun resolveTargetDirectory(uri: Uri?): Pair<DocumentFile?, Boolean> {
        if (uri != null) {
            try {
                val docFile = DocumentFile.fromTreeUri(context, uri)
                if (docFile != null && docFile.canWrite()) {
                    return Pair(docFile, false)
                }
            } catch (e: Exception) {
                addLog(LogLevel.WARNING, "SAF", "TreeUri からのアクセスに失敗: ${e.localizedMessage}")
            }
        }

        // Fallback to internal app storage directory
        val internalDir = context.getExternalFilesDir(null) ?: context.filesDir
        val docInternal = DocumentFile.fromFile(internalDir)
        return Pair(docInternal, true)
    }

    private fun getOrCreateTestFolder(parentDir: DocumentFile): DocumentFile? {
        val existing = parentDir.findFile("_USB_GUARD_TEST_DATA_")
        if (existing != null && existing.isDirectory) {
            return existing
        }
        return parentDir.createDirectory("_USB_GUARD_TEST_DATA_")
    }

    private fun calculateTargetBytes(config: TestConfig, targetDir: DocumentFile): Long {
        val availableBytes = try {
            val internalFile = context.getExternalFilesDir(null) ?: context.filesDir
            internalFile.usableSpace
        } catch (e: Exception) {
            1024 * 1024 * 1024L
        }

        val targetMb = when (config.preset) {
            TestPreset.QUICK -> 256L
            TestPreset.STANDARD -> 1024L
            TestPreset.HEAVY -> 4096L
            TestPreset.EXTREME -> 16384L
            TestPreset.CUSTOM -> config.customSizeMb
            TestPreset.FULL_DRIVE -> (availableBytes / (1024 * 1024) * 0.9).toLong()
        }

        return (targetMb * 1024 * 1024L).coerceAtLeast(16 * 1024 * 1024L)
    }

    private fun openOutputStream(file: DocumentFile): OutputStream? {
        return try {
            contentResolver.openOutputStream(file.uri, "w")
        } catch (e: Exception) {
            null
        }
    }

    private fun openInputStream(file: DocumentFile): InputStream? {
        return try {
            contentResolver.openInputStream(file.uri)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Pseudo-random deterministic byte seed generator (XorShift PRNG based on blockIndex & byteOffset)
     */
    private fun generateSeedPattern(buffer: ByteArray, blockIndex: Int, byteOffset: Long, length: Int) {
        var seed = (blockIndex.toLong() xor 0x5A5A5A5A5A5A5A5AL) + byteOffset
        for (i in 0 until length) {
            seed = seed xor (seed shl 13)
            seed = seed xor (seed ushr 7)
            seed = seed xor (seed shl 17)
            buffer[i] = (seed and 0xFF).toByte()
        }
    }
}
