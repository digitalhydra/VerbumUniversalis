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
        // Verbum data repos - download from GitHub raw content
        const val CATENA_DB_URL = "https://raw.githubusercontent.com/digitalhydra/verbum-data/main/verbum_catena.db"
        const val CROSS_REFS_DB_URL = "https://raw.githubusercontent.com/digitalhydra/verbum-data/main/verbum_cross_refs.db"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadFile(url: String, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Download failed: ${response.code}")
                return@withContext false
            }

            val body = response.body ?: return@withContext false
            val content = body.string()
            outputFile.writeText(content)
            
            Log.i(TAG, "Downloaded $url to ${outputFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            false
        }
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

    suspend fun downloadCatenaDatabase(outputFile: File): Boolean = downloadBinaryFile(CATENA_DB_URL, outputFile)
    
    suspend fun downloadCrossRefsDatabase(outputFile: File): Boolean = downloadBinaryFile(CROSS_REFS_DB_URL, outputFile)
}
