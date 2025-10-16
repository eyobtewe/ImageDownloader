package com.midexamp.imagedownloader.feature.image.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

class GrayScaleWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object { private const val TAG = "GrayScaleWorker" }

    override suspend fun doWork(): Result {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val path = inputData.getString("image_path") ?: return@withContext Result.failure()
                val srcFile = File(path)
                if (!srcFile.exists()) {
                    Log.e(TAG, "Source file does not exist: $path")
                    return@withContext Result.failure()
                }

                val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                val bitmap = BitmapFactory.decodeFile(path, options) ?: run {
                    Log.e(TAG, "Failed to decode bitmap: $path")
                    return@withContext Result.failure()
                }

                val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(grayBitmap)
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                    isFilterBitmap = true
                }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                bitmap.recycle()

                val grayFile = File.createTempFile("gray_image_", ".png", applicationContext.cacheDir)
                FileOutputStream(grayFile).use { out ->
                    if (!grayBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                        Log.e(TAG, "Failed to compress gray bitmap")
                        return@withContext Result.failure()
                    }
                }
                grayBitmap.recycle()

                Result.success(workDataOf("final_path" to grayFile.absolutePath))
            } catch (oom: OutOfMemoryError) {
                Log.e(TAG, "Out of memory while converting image", oom)
                Result.failure()
            } catch (e: Exception) {
                Log.e(TAG, "Error converting image to grayscale: ${e.message}", e)
                Result.failure()
            }
        }
    }
}