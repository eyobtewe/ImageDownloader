package com.midexamp.imagedownloader.feature.image.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.io.File
import java.net.URL

class DownloadWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val urlString = inputData.getString("url")
            ?: return Result.failure(workDataOf("error" to "Missing url"))

        return try {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                URL(urlString).openStream().use { input ->
                    val outFile = File(applicationContext.cacheDir, "image_${System.currentTimeMillis()}.jpg")
                    outFile.outputStream().use { it.write(input.readBytes()) }
                    outFile
                }
            }

            Log.d("DownloadWorker", "Image downloaded to ${file.absolutePath}")
            Result.success(workDataOf("image_path" to file.absolutePath))
        } catch (e: java.io.IOException) {
            Log.e("DownloadWorker", "I/O error while downloading image", e)
            Result.retry()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Unexpected error", e)
            Result.failure(workDataOf("error" to (e.message ?: "unknown")))
        }
    }
}
