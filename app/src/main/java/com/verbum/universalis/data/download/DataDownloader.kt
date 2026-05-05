package com.verbum.universalis.data.download

import android.util.Log
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataDownloader {
    companion object {
        private const val TAG = "DataDownloader"
        // Verbum Universalis data URLs (GitHub Releases)
        private const val BASE_URL = "https://github.com/YOUR_USER/VERBUM_DATA/releases/download/v1.0.0"
        const val CATENA_DB_URL = "${BASE_URL}/verbum_catena.db"
        const val CROSS_REFS_DB_URL = "${BASE_URL}/verbum_cross_refs.db"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadCatenaDatabase(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext downloadBinaryFile(CATENA_DB_URL, outputFile)
    }

    suspend fun downloadCrossRefsDatabase(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext downloadBinaryFile(CROSS_REFS_DB_URL, outputFile)
    }

    suspend fun downloadBinaryFile(url: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            body.byteStream().use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.i(TAG, "Downloaded binary $url to ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
    }
}
