package com.midexamp.imagedownloader.feature.image.screen


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil3.compose.AsyncImage
import com.midexamp.imagedownloader.feature.image.viewmodel.ImageViewModel

@Composable
fun ImageScreen(modifier: Modifier = Modifier) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding)
        ) {
            val context = LocalContext.current
            val viewModel: ImageViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ImageViewModel(WorkManager.getInstance(context)) as T
                    }
                }
            )

            val imageUiState by viewModel.imageUiState.collectAsStateWithLifecycle()
            val downloadState = imageUiState.downloadState
            val processImageState = imageUiState.imageProcessState

            Text(text = "📥 Image Downloader")
            Text(text = "Tap to download and process an image")

            Button(onClick = { viewModel.startDownloadAndProcess() }) {
                Text(text = "Start")
            }

            val statusIcon = when {
                downloadState == WorkInfo.State.RUNNING || processImageState == WorkInfo.State.RUNNING -> "⏳"
                downloadState == WorkInfo.State.SUCCEEDED && processImageState == WorkInfo.State.SUCCEEDED -> "✅"
                downloadState == WorkInfo.State.FAILED || processImageState == WorkInfo.State.FAILED -> "❌"
                downloadState == WorkInfo.State.CANCELLED || processImageState == WorkInfo.State.CANCELLED -> "🚫"
                else -> "ℹ️"
            }

            val statusMessage = when {
                downloadState == WorkInfo.State.SUCCEEDED -> "Image Downloaded"
                processImageState == WorkInfo.State.SUCCEEDED -> "Image Processed"
                downloadState == WorkInfo.State.RUNNING -> "Downloading Image"
                processImageState == WorkInfo.State.RUNNING -> "Processing Image"
                downloadState == WorkInfo.State.FAILED -> "Download Failed"
                processImageState == WorkInfo.State.FAILED -> "Process Image Failed"
                downloadState == WorkInfo.State.CANCELLED -> "Download Cancelled"
                processImageState == WorkInfo.State.CANCELLED -> "Process Image Cancelled"
                downloadState == WorkInfo.State.ENQUEUED -> "Download Enqueued"
                processImageState == WorkInfo.State.ENQUEUED -> "Process Image Enqueued"
                downloadState == WorkInfo.State.BLOCKED -> "Download Blocked"
                processImageState == WorkInfo.State.BLOCKED -> "Process Image Blocked"
                else -> "Idle"
            }

            Text(text = "$statusIcon  $statusMessage")

            Text(text = "Download State: $downloadState")
            Text(text = "Process Image State: $processImageState")

            imageUiState.finalImagePath?.let { path ->
                Text(text = "Image Path: $path")
                AsyncImage(
                    model = path,
                    contentDescription = "Image"
                )
            }
        }
    }
}

@Preview
@Composable
fun ImageScreenPreview() {
    ImageScreen()
}