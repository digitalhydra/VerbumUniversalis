package com.verbum.universalis.data.github

import android.util.Log
import com.squareup.okhttp3.MediaType.Companion.toMediaType
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.Request
import com.squareup.okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GitHubApiService {
    companion object {
        private const val TAG = "GitHubApiService"
        private const val API_BASE = "https://api.github.com"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun listRepos(token: String): List<Repo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$API_BASE/user/repos")
                .addHeader("Authorization", "token $token")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                Log.e(TAG, "Failed to list repos: ${response.message}")
                return@withContext emptyList()
            }

            val jsonArray = JSONArray(body)
            val repos = mutableListOf<Repo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                repos.add(
                    Repo(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        fullName = obj.getString("full_name"),
                        isPrivate = obj.getBoolean("private"),
                        htmlUrl = obj.getString("html_url")
                    )
                )
            }
            repos
        } catch (e: Exception) {
            Log.e(TAG, "Error listing repos", e)
            emptyList()
        }
    }

    suspend fun createRepo(token: String, name: String, isPrivate: Boolean = true): Repo? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", name)
                put("private", isPrivate)
                put("auto_init", true) // Initialize with README
            }

            val mediaType = "application/json".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$API_BASE/user/repos")
                .addHeader("Authorization", "token $token")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Failed to create repo: ${response.message}")
                return@withContext null
            }

            val obj = JSONObject(responseBody)
            Repo(
                id = obj.getInt("id"),
                name = obj.getString("name"),
                fullName = obj.getString("full_name"),
                isPrivate = obj.getBoolean("private"),
                htmlUrl = obj.getString("html_url")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating repo", e)
            null
        }
    }

    suspend fun addDeployKey(token: String, repoFullName: String, title: String, key: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("key", key)
                put("read_only", false)
            }

            val mediaType = "application/json".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$API_BASE/repos/$repoFullName/keys")
                .addHeader("Authorization", "token $token")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            response.body?.close()

            if (response.isSuccessful) {
                Log.i(TAG, "Deploy key added successfully")
                true
            } else {
                Log.e(TAG, "Failed to add deploy key: ${response.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding deploy key", e)
            false
        }
    }
}

data class Repo(
    val id: Int,
    val name: String,
    val fullName: String,
    val isPrivate: Boolean,
    val htmlUrl: String
)
