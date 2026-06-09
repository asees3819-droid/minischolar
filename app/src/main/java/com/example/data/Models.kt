package com.example.data

import androidx.room.*

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val topicDescription: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val status: String = "Draft", // Analyzing, Completed, Error
    val isFavorite: Boolean = false,
    val selectedLanguage: String = "English",
    val depth: String = "Analytical" // Brief, Analytical, Deep
)

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val summary: String,
    val insightsJson: String, // List of insights: ["insight1", "insight2"]
    val statsJson: String, // List of {"metric": "X", "value": "Y", "description": "Z"}
    val timelineJson: String, // List of {"event": "A", "date": "B", "details": "C"}
    val citationsJson: String, // List of {"title": "X", "url": "Y", "reliability": "Z"}
    val followUpQuestionsJson: String, // List of strings
    val fullMarkdown: String = "",
    val networkGraphNodesJson: String = "[]",
    val networkGraphEdgesJson: String = "[]",
    val geoMapPointsJson: String = "[]"
)

@Entity(tableName = "notes")
data class ProjectNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val noteTitle: String,
    val noteContent: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "sources")
data class ProjectSource(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val url: String,
    val reliabilityRating: String, // "High", "Medium", "Unverified"
    val snippet: String = ""
)

@Entity(tableName = "chats")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val role: String, // "user", "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val keyword: String,
    val dateSubscribed: Long = System.currentTimeMillis(),
    val frequency: String = "Real-time" // Real-time, Daily, Weekly
)

@Entity(tableName = "trend_updates")
data class TrendUpdate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subscriptionId: Long,
    val subscriptionKeyword: String,
    val title: String,
    val summary: String,
    val dateUpdated: Long = System.currentTimeMillis(),
    val category: String, // e.g. "Breakthrough", "Policy", "Market Shift", "Emerging Tech"
    val trendIndicator: String = "Stable", // "Upward", "Stable", "Volatile"
    val criticalIndex: Float = 0.5f, // scale of 0 to 1
    val isRead: Boolean = false
)
