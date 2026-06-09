package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String,
    val properties: Map<String, SchemaProperty>? = null,
    val description: String? = null,
    val required: List<String>? = null,
    val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Structuring Research Report Output via Moshi ---

@JsonClass(generateAdapter = true)
data class MetricAndStat(
    val metric: String,
    val value: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class TimelineMilestone(
    val date: String,
    val event: String,
    val details: String
)

@JsonClass(generateAdapter = true)
data class Citation(
    val title: String,
    val url: String,
    val reliabilityRating: String // "High", "Medium", "Unverified"
)

@JsonClass(generateAdapter = true)
data class GraphNode(
    val id: String,
    val label: String,
    val group: String // "Concept", "Entity", "Technology", "Agent", etc.
)

@JsonClass(generateAdapter = true)
data class GraphEdge(
    val source: String,
    val target: String,
    val type: String // connection name/label
)

@JsonClass(generateAdapter = true)
data class GeoMapPoint(
    val locationName: String,
    val lat: Double,
    val lng: Double,
    val description: String,
    val significance: String
)

@JsonClass(generateAdapter = true)
data class TrendNotificationPayload(
    val subscriptionKeyword: String,
    val title: String,
    val summary: String,
    val category: String, // Breakthrough, Policy, Competitor, Regulation, Tech
    val trendIndicator: String, // Upward, Stable, Volatile
    val criticalIndex: Float // 0.0 - 1.0
)

@JsonClass(generateAdapter = true)
data class ResearchReportResponse(
    val shortSummary: String,
    val keyInsights: List<String>,
    val metricsAndStats: List<MetricAndStat>,
    val timelineMilestones: List<TimelineMilestone>,
    val citationsGathered: List<Citation>,
    val suggestedQueries: List<String>,
    val fullMarkdownContent: String,
    val networkGraphNodes: List<GraphNode>? = emptyList(),
    val networkGraphEdges: List<GraphEdge>? = emptyList(),
    val geoMapPoints: List<GeoMapPoint>? = emptyList()
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
         level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}
