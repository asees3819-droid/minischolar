package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY dateCreated DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<Project?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)

    // Reports
    @Query("SELECT * FROM reports WHERE projectId = :projectId LIMIT 1")
    fun getReportForProject(projectId: Long): Flow<Report?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: Report)

    // Notes
    @Query("SELECT * FROM notes WHERE projectId = :projectId ORDER BY lastUpdated DESC")
    fun getNotesForProject(projectId: Long): Flow<List<ProjectNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ProjectNote)

    @Delete
    suspend fun deleteNote(note: ProjectNote)

    // Sources
    @Query("SELECT * FROM sources WHERE projectId = :projectId ORDER BY id DESC")
    fun getSourcesForProject(projectId: Long): Flow<List<ProjectSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: ProjectSource)

    @Delete
    suspend fun deleteSource(source: ProjectSource)

    // Chats
    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getChatMessagesForProject(projectId: Long): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY dateSubscribed DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: Long): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)

    @Query("DELETE FROM trend_updates WHERE subscriptionId = :subscriptionId")
    suspend fun deleteTrendUpdatesBySubscriptionId(subscriptionId: Long)

    // Trend Updates
    @Query("SELECT * FROM trend_updates ORDER BY dateUpdated DESC")
    fun getAllTrendUpdates(): Flow<List<TrendUpdate>>

    @Query("SELECT * FROM trend_updates WHERE subscriptionId = :subscriptionId ORDER BY dateUpdated DESC")
    fun getTrendUpdatesForSubscription(subscriptionId: Long): Flow<List<TrendUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrendUpdate(trendUpdate: TrendUpdate)

    @Query("UPDATE trend_updates SET isRead = 1 WHERE id = :id")
    suspend fun markTrendUpdateAsRead(id: Long)

    @Query("UPDATE trend_updates SET isRead = 1")
    suspend fun markAllTrendUpdatesAsRead()
}
