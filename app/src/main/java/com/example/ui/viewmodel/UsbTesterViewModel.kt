package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.TestReportEntity
import com.example.data.model.*
import com.example.engine.UsbTesterEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UsbTesterViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val contentResolver = application.contentResolver

    private val database = AppDatabase.getDatabase(context)
    private val reportDao = database.testReportDao()

    private val engine = UsbTesterEngine(context, contentResolver)

    val progressState = engine.progressState
    val speedPoints = engine.speedPoints
    val logs = engine.logs

    private val _driveInfo = MutableStateFlow(StorageDriveInfo())
    val driveInfo: StateFlow<StorageDriveInfo> = _driveInfo.asStateFlow()

    private val _testConfig = MutableStateFlow(TestConfig())
    val testConfig: StateFlow<TestConfig> = _testConfig.asStateFlow()

    private val _selectedUri = MutableStateFlow<Uri?>(null)
    val selectedUri: StateFlow<Uri?> = _selectedUri.asStateFlow()

    val testHistory: StateFlow<List<TestReportEntity>> = reportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeReport = MutableStateFlow<TestReportEntity?>(null)
    val activeReport: StateFlow<TestReportEntity?> = _activeReport.asStateFlow()

    init {
        // Default to unselected USB drive state
        deselectDrive()
    }

    fun setSelectedDriveUri(uri: Uri?) {
        if (uri == null) return
        _selectedUri.value = uri

        // Try to take persistable URI permission for SAF OTG access
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            // Permission may already be held or transient
        }

        try {
            val treeFile = DocumentFile.fromTreeUri(context, uri)
            if (treeFile != null) {
                val driveName = treeFile.name ?: "USB Storage Device"
                var totalBytes = 0L
                var freeBytes = 0L

                try {
                    val pfd = contentResolver.openFileDescriptor(treeFile.uri, "r")
                    if (pfd != null) {
                        val stat = android.os.StatFs(pfd.fileDescriptor.toString())
                        totalBytes = stat.blockCountLong * stat.blockSizeLong
                        freeBytes = stat.availableBlocksLong * stat.blockSizeLong
                        pfd.close()
                    }
                } catch (e: Exception) {
                    // Ignored
                }

                if (totalBytes <= 0L) {
                    val internalFile = context.getExternalFilesDir(null) ?: context.filesDir
                    freeBytes = internalFile.usableSpace
                    totalBytes = internalFile.totalSpace
                }

                _driveInfo.value = StorageDriveInfo(
                    uriString = uri.toString(),
                    name = driveName,
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    isWritable = treeFile.canWrite(),
                    isUsbOtg = true,
                    isFallbackInternal = false
                )
            }
        } catch (e: Exception) {
            deselectDrive()
        }
    }

    fun selectSandboxStorage() {
        _selectedUri.value = null
        val internalFile = context.getExternalFilesDir(null) ?: context.filesDir
        val freeBytes = internalFile.usableSpace
        val totalBytes = internalFile.totalSpace

        _driveInfo.value = StorageDriveInfo(
            uriString = null,
            name = "アプリ内ストレージ (Internal Sandbox)",
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            isWritable = internalFile.canWrite(),
            isUsbOtg = false,
            isFallbackInternal = true
        )
    }

    fun deselectDrive() {
        _selectedUri.value = null
        _driveInfo.value = StorageDriveInfo(
            uriString = null,
            name = "USBストレージ未選択",
            totalBytes = 0L,
            freeBytes = 0L,
            isWritable = false,
            isUsbOtg = false,
            isFallbackInternal = false
        )
    }

    fun updateConfig(config: TestConfig) {
        _testConfig.value = config
    }

    fun startTest() {
        val currentPhase = progressState.value.phase
        if (currentPhase == TestPhase.WRITING || currentPhase == TestPhase.VERIFYING) return

        viewModelScope.launch {
            val finalState = engine.executeTest(_selectedUri.value, _testConfig.value)

            if (finalState.phase == TestPhase.COMPLETED) {
                // Generate report certificate entity
                val totalMb = finalState.targetBytes / (1024 * 1024)
                val corruptMb = finalState.corruptBytesCount / (1024 * 1024)
                val verifiedMb = (totalMb - corruptMb).coerceAtLeast(0L)

                val isFake = finalState.corruptBlockCount > 0 || _testConfig.value.simulateFakeDrive
                val healthScore = if (isFake) {
                    ((verifiedMb.toDouble() / totalMb.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 75)
                } else 100

                val report = TestReportEntity(
                    timestampMs = System.currentTimeMillis(),
                    driveName = _driveInfo.value.name,
                    driveUri = _selectedUri.value?.toString(),
                    testedSizeMb = totalMb,
                    verifiedSizeMb = verifiedMb,
                    totalBlocks = finalState.totalBlocks,
                    passedBlocks = finalState.totalBlocks - finalState.corruptBlockCount,
                    corruptBlocks = finalState.corruptBlockCount,
                    avgWriteSpeedMbps = finalState.avgWriteSpeedMbps,
                    avgReadSpeedMbps = finalState.avgReadSpeedMbps,
                    maxWriteSpeedMbps = finalState.maxWriteSpeedMbps,
                    maxReadSpeedMbps = finalState.maxReadSpeedMbps,
                    durationSeconds = (finalState.elapsedTimeMs / 1000).coerceAtLeast(1),
                    isFakeDrive = isFake,
                    isGenuine = !isFake,
                    healthScore = healthScore,
                    summaryText = finalState.statusText,
                    logSnippetJson = ""
                )

                val insertedId = reportDao.insertReport(report)
                _activeReport.value = report.copy(id = insertedId)
            }
        }
    }

    fun pauseTest() {
        engine.pauseTest()
    }

    fun resumeTest() {
        engine.resumeTest()
    }

    fun cancelTest() {
        engine.cancelTest()
    }

    fun cleanupTestFiles() {
        engine.cleanupTestFiles(_selectedUri.value)
    }

    fun dismissReportDialog() {
        _activeReport.value = null
    }

    fun showReport(report: TestReportEntity) {
        _activeReport.value = report
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch {
            reportDao.deleteReportById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            reportDao.clearAllReports()
        }
    }
}
