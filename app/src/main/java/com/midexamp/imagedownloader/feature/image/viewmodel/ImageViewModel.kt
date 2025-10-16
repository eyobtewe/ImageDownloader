package com.midexamp.imagedownloader.feature.image.viewmodel

import com.midexamp.imagedownloader.feature.image.uistate.ImageUiState
import com.midexamp.imagedownloader.feature.image.worker.DownloadWorker
import com.midexamp.imagedownloader.feature.image.worker.GrayScaleWorker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "image_download_and_process_work"
private const val INPUT_URL_KEY = "url"
private const val OUTPUT_FINAL_PATH_KEY = "final_path"

class ImageViewModel(private val workManager: WorkManager): ViewModel() {
    private val _imageUiState = MutableStateFlow(ImageUiState())
    val imageUiState = _imageUiState.asStateFlow()
    private var downloadRequestId: UUID? = null
    private var processImageRequestId: UUID? = null

    init {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(UNIQUE_WORK_NAME)
                .collect { workInfos ->
                    val downloadInfo = workInfos.firstOrNull { it.id == downloadRequestId }
                    val processInfo = workInfos.firstOrNull { it.id == processImageRequestId }

                    val downloadState = downloadInfo?.state
                    val processState = processInfo?.state
                    val imagePath = if (processState == WorkInfo.State.SUCCEEDED) {
                        processInfo?.outputData?.getString(OUTPUT_FINAL_PATH_KEY)
                    } else null

                    _imageUiState.update { current ->
                        current.copy(
                            downloadState = downloadState ?: current.downloadState,
                            imageProcessState = processState ?: current.imageProcessState,
                            finalImagePath = imagePath ?: current.finalImagePath
                        )
                    }
                }
        }
    }

    fun startDownloadAndProcess(url: String = "https://cdn.pixabay.com/photo/2022/08/22/11/04/skate-7403432_1280.jpg") {
        val inputData = workDataOf(INPUT_URL_KEY to url)
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(inputData)
            .build()
        downloadRequestId = downloadRequest.id

        val processImageRequest = OneTimeWorkRequestBuilder<GrayScaleWorker>()
            .build()
        processImageRequestId = processImageRequest.id

        workManager.beginUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )
            .then(processImageRequest)
            .enqueue()
    }

    fun cancelWork() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}