package com.verbun.universalis.data.oauth

import android.util.Log
import com.squareup.okhttp3.MediaType.Companion.toMediaType
import com.squareup.okhttp3.OkHttpClient
import com.squareup.okhttp3.Request
import com.squareup.okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OAuthManager {
    companion object {
        private const val TAG = "OAuthManager"
        private const val CLIENT_ID = "YOUR_GITHUB_CLIENT_ID" // Replace with real client ID
        private const val GITHUB_DEVICE_URL = "https://github.com/login/device/code"
        private const val GITHUB_TOKEN_URL = "https://github.com/login/oauth/access_token"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun startDeviceFlow(): DeviceFlowResult = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/json".toMediaType()
            val body = JSONObject().put("client_id", CLIENT_ID).toString()
                .toRequestBody(mediaType)

            val request = Request.Builder()
                .url(GITHUB_DEVICE_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext DeviceFlowResult.Error("Failed to start device flow: ${response.message}")
            }

            val json = JSONObject(responseBody)
            val deviceCode = json.getString("device_code")
            val userCode = json.getString("user_code")
            val verificationUri = json.getString("verification_uri")
            val interval = json.optInt("interval", 5)

            DeviceFlowResult.Success(deviceCode, userCode, verificationUri, interval)

        } catch (e: Exception) {
            Log.e(TAG, "Device flow error", e)
            DeviceFlowResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun pollForToken(deviceCode: String): TokenResult = withContext(Dispatchers.IO) {
        try {
            val mediaType = "application/json".toMediaType()
            val body = JSONObject()
                .put("client_id", CLIENT_ID)
                .put("device_code", deviceCode)
                .put("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .toString()
                .toRequestBody(mediaType)

            val request = Request.Builder()
                .url(GITHUB_TOKEN_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext TokenResult.Error("Failed to poll token: ${response.message}")
            }

            val json = JSONObject(responseBody)
            if (json.has("error")) {
                val error = json.getString("error")
                if (error == "authorization_pending") {
                    return@withContext TokenResult.Pending
                }
                return@withContext TokenResult.Error(error)
            }

            val accessToken = json.getString("access_token")
            TokenResult.Success(accessToken)

        } catch (e: Exception) {
            Log.e(TAG, "Token poll error", e)
            TokenResult.Error(e.message ?: "Unknown error")
        }
    }
}

sealed class DeviceFlowResult {
    data class Success(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val interval: Int
    ) : DeviceFlowResult()

    data class Error(val message: String) : DeviceFlowResult()
}

sealed class TokenResult {
    data class Success(val accessToken: String) : TokenResult()
    object Pending : TokenResult()
    data class Error(val message: String) : TokenResult()
}
