package com.biehuale.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.biehuale.app.data.backup.BackupExporter
import com.biehuale.app.data.backup.BackupImporter
import com.biehuale.app.data.backup.ImportPreview
import com.biehuale.app.data.backup.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 备份 ViewModel（导出/导入）
 *
 * 详见 docs/PRD.md §10
 *
 * 流程：
 *  - export(uri) 由 Composable 拿到 SAF Uri 后调用
 *  - previewImport(uri) 同样
 *  - confirmImport(preview) 用户在 Dialog 确认后调用
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BackupEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BackupEvent> = _events.asSharedFlow()

    /**
     * 导出到 Uri（SAF 提供）
     */
    fun export(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isExporting = true, lastError = null)
            val result = backupExporter.export(uri)
            _state.value = _state.value.copy(isExporting = false)
            result.onSuccess {
                _events.emit(BackupEvent.ExportSuccess)
            }.onFailure { e ->
                _events.emit(BackupEvent.Error("导出失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    /**
     * 预览导入（弹 Dialog 让用户确认）
     */
    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true, lastError = null, pendingPreview = null)
            val result = backupImporter.preview(uri)
            result.onSuccess { preview ->
                _state.value = _state.value.copy(
                    isImporting = false,
                    pendingPreview = preview
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(isImporting = false)
                _events.emit(BackupEvent.Error("导入预览失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    /**
     * 确认导入
     */
    fun confirmImport(preview: ImportPreview) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isImporting = true)
            val result = backupImporter.applyImport(preview)
            _state.value = _state.value.copy(isImporting = false, pendingPreview = null)
            result.onSuccess { r ->
                _events.emit(BackupEvent.ImportSuccess(r))
            }.onFailure { e ->
                _events.emit(BackupEvent.Error("导入失败：${e.message ?: "未知错误"}"))
            }
        }
    }

    /**
     * 取消导入（关闭 Dialog）
     */
    fun cancelImport() {
        _state.value = _state.value.copy(pendingPreview = null)
    }

    /**
     * 生成默认文件名（用于 SAF CreateDocument 提示）
     */
    fun generateFileName(): String = backupExporter.generateDefaultFileName()
}

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val pendingPreview: ImportPreview? = null,
    val lastError: String? = null
)

sealed interface BackupEvent {
    data object ExportSuccess : BackupEvent
    data class ImportSuccess(val result: ImportResult) : BackupEvent
    data class Error(val message: String) : BackupEvent
}
