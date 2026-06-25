package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import com.example.data.diagnostics.AuraDiagnostics
import com.example.data.diagnostics.ServiceStatus

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class DiagnosticInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val model = url.pathSegments.getOrNull(2) ?: "unknown-model"
        val method = request.method
        
        AuraDiagnostics.log(
            module = "GEMINI_API",
            level = "INFO",
            message = "Outgoing API call using model: $model",
            details = "URL: ${url.redactApiKey()}\nMethod: $method"
        )
        
        val startTime = System.currentTimeMillis()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errMsg = "Network connectivity exception: ${e.localizedMessage}"
            AuraDiagnostics.setLastErrorMessage(errMsg)
            AuraDiagnostics.log(
                module = "GEMINI_API",
                level = "ERROR",
                message = "Network connectivity exception after ${duration}ms",
                details = e.stackTraceToString(),
                latencyMs = duration
            )
            AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
            throw e
        }
        
        val duration = System.currentTimeMillis() - startTime
        val code = response.code
        
        if (response.isSuccessful) {
            AuraDiagnostics.log(
                module = "GEMINI_API",
                level = "INFO",
                message = "API request succeeded in ${duration}ms (HTTP $code)",
                details = "Model: $model",
                latencyMs = duration
            )
            AuraDiagnostics.setGeminiStatus(ServiceStatus.CONNECTED)
        } else {
            val errorBody = try {
                response.peekBody(1024 * 10).string()
            } catch (ex: Exception) {
                "Unable to read error body"
            }

            // Extract specific error message from the JSON response
            val specificErrorMsg = try {
                val moshiObj = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val mapAdapter = moshiObj.adapter(Map::class.java)
                val parsed = mapAdapter.fromJson(errorBody) as? Map<*, *>
                val errorMap = parsed?.get("error") as? Map<*, *>
                val msg = errorMap?.get("message") as? String
                msg ?: "HTTP $code error."
            } catch (ex: Exception) {
                "HTTP $code error."
            }

            val fullErrMsg = "API request failed with HTTP status code $code. Detail: $specificErrorMsg"
            AuraDiagnostics.setLastErrorMessage(fullErrMsg)

            AuraDiagnostics.log(
                module = "GEMINI_API",
                level = "ERROR",
                message = "API request failed with HTTP status code $code (${duration}ms)",
                details = "Response Body: $errorBody",
                latencyMs = duration
            )
            if (code == 400 || code == 403 || code == 401 || code == 429 || code >= 500) {
                AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
            }
        }
        
        return response
    }
    
    private fun HttpUrl.redactApiKey(): String {
        val key = queryParameter("key") ?: return toString()
        if (key.length <= 4) return toString().replace(key, "REDACTED")
        return toString().replace(key, "REDACTED_${key.takeLast(4)}")
    }
}

class RetryWithExponentialBackoffInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: Exception? = null
        
        val strategy = com.example.data.diagnostics.GeminiKeyManager.backoffStrategy
        val maxLimit = if (strategy == "Aggressive") 3 else 5
        val baseDelayMs = if (strategy == "Aggressive") 600L else 2500L
        val multiplier = if (strategy == "Aggressive") 1.8 else 2.5
        
        var attempt = 0
        var delayMs = baseDelayMs

        while (attempt < maxLimit) {
            attempt++
            try {
                if (attempt > 1) {
                    AuraDiagnostics.log(
                        module = "GEMINI_API",
                        level = "WARN",
                        message = "Handshake error ($strategy retry). Retrying in ${delayMs}ms...",
                        details = "Attempt $attempt of $maxLimit. Strategy: $strategy. Factor: ${multiplier}x."
                    )
                    Thread.sleep(delayMs)
                    delayMs = (delayMs * multiplier).toLong()
                }
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    if (attempt > 1) {
                        AuraDiagnostics.log(
                            module = "GEMINI_API",
                            level = "INFO",
                            message = "Retry succeeded! Connection established on attempt $attempt.",
                            details = "Successfully self-healed connection. Protocol: ${response.protocol}"
                        )
                    }
                    return response
                } else if (response.code == 429 || (response.code >= 500 && response.code <= 599)) {
                    val statusText = if (response.code == 429) "429 Rate Limit" else "${response.code} Server Error"
                    AuraDiagnostics.log(
                        module = "GEMINI_API",
                        level = "WARN",
                        message = "Received transient status $statusText. Scheduling backoff retry...",
                        details = "Attempt $attempt of $maxLimit ($strategy strategy)."
                    )
                    if (attempt == maxLimit) {
                        AuraDiagnostics.log(
                            module = "GEMINI_API",
                            level = "ERROR",
                            message = "Transient retry mechanism exhausted after $maxLimit attempts ($statusText).",
                            details = "Backoff strategy failed to self-heal connections."
                        )
                        return response
                    }
                    response.close()
                } else {
                    return response
                }
            } catch (e: Exception) {
                exception = e
                AuraDiagnostics.log(
                    module = "GEMINI_API",
                    level = "WARN",
                    message = "Network connectivity exception: ${e.localizedMessage}",
                    details = "Attempt $attempt of $maxLimit.\nTrace: ${e.stackTraceToString()}"
                )
                if (attempt == maxLimit) {
                    AuraDiagnostics.log(
                        module = "GEMINI_API",
                        level = "ERROR",
                        message = "Network connectivity exception. Retry mechanism exhausted after $maxLimit attempts.",
                        details = "Exception: ${e.localizedMessage}"
                    )
                    throw e
                }
            }
        }
        if (response != null) return response
        if (exception != null) throw exception
        throw java.io.IOException("Transient retry mechanism exhausted.")
    }
}

object RetrofitClient {
    private var cachedService: GeminiApiService? = null
    private var lastEndpoint: String = ""
    private var lastTimeoutSeconds: Int = -1

    val service: GeminiApiService
        get() {
            val currentEndpoint = com.example.data.diagnostics.GeminiKeyManager.customEndpoint
            val finalEndpoint = if (currentEndpoint.isNotBlank()) {
                if (currentEndpoint.endsWith("/")) currentEndpoint else "$currentEndpoint/"
            } else {
                "https://generativelanguage.googleapis.com/"
            }
            val currentTimeout = com.example.data.diagnostics.GeminiKeyManager.customTimeoutSeconds
            
            if (cachedService == null || lastEndpoint != finalEndpoint || lastTimeoutSeconds != currentTimeout) {
                lastEndpoint = finalEndpoint
                lastTimeoutSeconds = currentTimeout
                
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(currentTimeout.toLong(), TimeUnit.SECONDS)
                    .readTimeout(currentTimeout.toLong(), TimeUnit.SECONDS)
                    .writeTimeout(currentTimeout.toLong(), TimeUnit.SECONDS)
                    .addInterceptor(RetryWithExponentialBackoffInterceptor())
                    .addInterceptor(DiagnosticInterceptor())
                    .build()

                val moshi = Moshi.Builder()
                    .addLast(KotlinJsonAdapterFactory())
                    .build()
                val retrofit = Retrofit.Builder()
                    .baseUrl(finalEndpoint)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                cachedService = retrofit.create(GeminiApiService::class.java)
            }
            return cachedService!!
        }
}
