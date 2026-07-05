package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Data Classes (Moshi compatible) ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String,
    val schema: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// --- Structuring our AI Analysis Results ---

@JsonClass(generateAdapter = true)
data class AIProjectAnalysis(
    val issues: List<AIIssue> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AIIssue(
    val filePath: String,
    val issueTitle: String,
    val severity: String, // HIGH, MEDIUM, LOW, CRITICAL
    val explanation: String,
    val originalCode: String = "",
    val proposedCode: String = ""
)

@JsonClass(generateAdapter = true)
data class AIDebuggerResponse(
    val explanation: String,
    val proposedChanges: List<AIPatch>? = null
)

@JsonClass(generateAdapter = true)
data class AIPatch(
    val filePath: String,
    val explanation: String,
    val originalCode: String,
    val proposedCode: String
)

// --- Retrofit Setup ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private val TAG = "GeminiClient"

    // Initialize Moshi with Kotlin Reflection Adapter
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Scans project files to find potential build-breaking compilation errors.
     */
    suspend fun analyzeProject(files: List<Pair<String, String>>): AIProjectAnalysis = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AIProjectAnalysis(
                issues = listOf(
                    AIIssue(
                        filePath = "System",
                        issueTitle = "API Key Missing",
                        severity = "CRITICAL",
                        explanation = "Please add your Gemini API Key in the AI Studio Secrets panel to activate full static analysis, dependency correction, and error fixing features.",
                        originalCode = "",
                        proposedCode = ""
                    )
                )
            )
        }

        val filesContentStr = files.joinToString("\n\n") { (path, content) ->
            "--- FILE: $path ---\n$content"
        }

        val prompt = """
            You are an elite Android Build Expert and Static Analyzer.
            Inspect the following files of an Android project and identify potential build errors, compiler breakages, syntax mismatches, namespace errors, SDK incompatibilities, or missing dependencies.
            Return a structured list of issues found.
            
            Android Project Files:
            $filesContentStr
        """.trimIndent()

        // JSON Schema for structural response
        val schema = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "issues" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "filePath" to mapOf(
                                "type" to "STRING",
                                "description" to "Relative path of the file containing the issue, e.g., 'app/build.gradle.kts'."
                            ),
                            "issueTitle" to mapOf(
                                "type" to "STRING",
                                "description" to "A brief, clear title of the issue, e.g., 'Missing Dependency'."
                            ),
                            "severity" to mapOf(
                                "type" to "STRING",
                                "description" to "Severity level: CRITICAL, HIGH, MEDIUM, LOW."
                            ),
                            "explanation" to mapOf(
                                "type" to "STRING",
                                "description" to "Detailed description explaining what is wrong and how it breaks compilation."
                            ),
                            "originalCode" to mapOf(
                                "type" to "STRING",
                                "description" to "The exact block of code causing the issue. Must match a unique substring in the file."
                            ),
                            "proposedCode" to mapOf(
                                "type" to "STRING",
                                "description" to "The recommended replacement code to resolve the issue."
                            )
                        ),
                        "required" to listOf("filePath", "issueTitle", "severity", "explanation")
                    )
                )
            ),
            "required" to listOf("issues")
        )

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = schema
                    )
                ),
                temperature = 0.2f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a professional Android Static Compiler Analyzer. Analyze code files precisely, matching actual issues, and output only valid JSON matching the specified schema."))
            )
        )

        try {
            val response = service.generateContent(apiKey, request)
            val jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonStr.isNullOrEmpty()) {
                return@withContext AIProjectAnalysis(issues = emptyList())
            }
            Log.d(TAG, "Analysis Raw Response: $jsonStr")
            val adapter = moshi.adapter(AIProjectAnalysis::class.java)
            adapter.fromJson(jsonStr) ?: AIProjectAnalysis(issues = emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Error performing project analysis", e)
            AIProjectAnalysis(
                issues = listOf(
                    AIIssue(
                        filePath = "System",
                        issueTitle = "Analysis Failed",
                        severity = "HIGH",
                        explanation = "An error occurred during AI static analysis: ${e.message}. Please check your connection and try again.",
                        originalCode = "",
                        proposedCode = ""
                    )
                )
            )
        }
    }

    /**
     * Analyzes a build console error log and proposes exact file modifications to resolve it.
     */
    suspend fun debugBuildError(
        logMessage: String,
        files: List<Pair<String, String>>
    ): AIDebuggerResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AIDebuggerResponse(
                explanation = "Please configure your Gemini API Key in the Secrets panel to activate automatic error resolution and patch recommendations."
            )
        }

        val filesContentStr = files.joinToString("\n\n") { (path, content) ->
            "--- FILE: $path ---\n$content"
        }

        val prompt = """
            Your task is to analyze the following Android compilation error log and propose precise file modifications to resolve the build issue.
            
            Build Console Output Log:
            $logMessage
            
            Android Project Files:
            $filesContentStr
        """.trimIndent()

        val schema = mapOf(
            "type" to "OBJECT",
            "properties" to mapOf(
                "explanation" to mapOf(
                    "type" to "STRING",
                    "description" to "A concise, professional explanation explaining why the build failed and how the proposed patches solve it."
                ),
                "proposedChanges" to mapOf(
                    "type" to "ARRAY",
                    "items" to mapOf(
                        "type" to "OBJECT",
                        "properties" to mapOf(
                            "filePath" to mapOf(
                                "type" to "STRING",
                                "description" to "Path of the file to modify."
                            ),
                            "explanation" to mapOf(
                                "type" to "STRING",
                                "description" to "What is being corrected in this file."
                            ),
                            "originalCode" to mapOf(
                                "type" to "STRING",
                                "description" to "The exact block of code to replace. Must exist in the original file."
                            ),
                            "proposedCode" to mapOf(
                                "type" to "STRING",
                                "description" to "The correct replacement content."
                            )
                        ),
                        "required" to listOf("filePath", "explanation", "originalCode", "proposedCode")
                    )
                )
            ),
            "required" to listOf("explanation")
        )

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                responseFormat = ResponseFormat(
                    text = ResponseFormatText(
                        mimeType = "application/json",
                        schema = schema
                    )
                ),
                temperature = 0.1f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are a professional Android Compiler and Debugger Assistant. Analyze build error logs, identify the exact broken lines, and output precise repair proposals in JSON."))
            )
        )

        try {
            val response = service.generateContent(apiKey, request)
            val jsonStr = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonStr.isNullOrEmpty()) {
                return@withContext AIDebuggerResponse(explanation = "Failed to analyze log.")
            }
            Log.d(TAG, "Debug Raw Response: $jsonStr")
            val adapter = moshi.adapter(AIDebuggerResponse::class.java)
            adapter.fromJson(jsonStr) ?: AIDebuggerResponse(explanation = "Failed to parse debugger suggestions.")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing debugging", e)
            AIDebuggerResponse(
                explanation = "An error occurred while debugging the build failure: ${e.message}"
            )
        }
    }
}
