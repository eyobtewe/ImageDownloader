package com.midexamp.imagedownloader.feature.image.uistate

import androidx.work.WorkInfo

data class ImageUiState(
    val downloadState: WorkInfo.State? = null,
    val imageProcessState: WorkInfo.State? = null,
    val finalImagePath: String? = null,
    val status: String = "Idle",
    val isRunning: Boolean = false
)