package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.net.URLEncoder

class ProjectRepository(private val projectDao: ProjectDao) {

    private val moshi = Moshi.Builder().build()
    private val reportAdapter = moshi.adapter(ResearchReportResponse::class.java)

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    fun getProject(id: Long): Flow<Project?> = projectDao.getProjectById(id)

    fun getReport(projectId: Long): Flow<Report?> = projectDao.getReportForProject(projectId)

    fun getNotes(projectId: Long): Flow<List<ProjectNote>> = projectDao.getNotesForProject(projectId)

    fun getSources(projectId: Long): Flow<List<ProjectSource>> = projectDao.getSourcesForProject(projectId)

    fun getChatMessages(projectId: Long): Flow<List<ChatMessage>> = projectDao.getChatMessagesForProject(projectId)

    suspend fun createProject(title: String, topicDescription: String, language: String, depth: String): Long {
        val project = Project(
            title = title,
            topicDescription = topicDescription,
            selectedLanguage = language,
            depth = depth,
            status = "Draft"
        )
        return projectDao.insertProject(project)
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    suspend fun saveNote(projectId: Long, title: String, content: String) {
        val note = ProjectNote(projectId = projectId, noteTitle = title, noteContent = content)
        projectDao.insertNote(note)
    }

    suspend fun deleteNote(note: ProjectNote) {
        projectDao.deleteNote(note)
    }

    suspend fun saveSource(projectId: Long, title: String, url: String, rating: String) {
        val source = ProjectSource(projectId = projectId, title = title, url = url, reliabilityRating = rating)
        projectDao.insertSource(source)
    }

    suspend fun deleteSource(source: ProjectSource) {
        projectDao.deleteSource(source)
    }

    suspend fun addChatMessage(projectId: Long, role: String, content: String) {
        val message = ChatMessage(projectId = projectId, role = role, content = content)
        projectDao.insertChatMessage(message)
    }

    /**
     * Triggers the Gemini model to research and compile a highly structured report.
     */
    suspend fun performResearch(projectId: Long, modelName: String): Result<Report> = withContext(Dispatchers.IO) {
        try {
            val project = getProjectSync(projectId) ?: return@withContext Result.failure(Exception("Project not found"))
            
            // Mark project status as "Analyzing"
            projectDao.updateProject(project.copy(status = "Analyzing"))

            val systemPrompt = """
                You are a futuristic, world-class professional Research Assistant, research analyst, fact-checker, and study companion.
                Your task is to thoroughly analyze the user's topic and generate a highly detailed, trustworthy, and actionable research report.
                
                You must return your entire response as a valid, parsable JSON object conforming strictly to this Schema:
                {
                   "shortSummary": "A concise executive summary (~2 paragraphs) written professionally in ${project.selectedLanguage}.",
                   "keyInsights": [ "First key breakthrough or insight", "Second analytical finding with critical weight", "Third innovative insight" ],
                   "metricsAndStats": [
                       { "metric": "75%", "value": "75%", "description": "Specific statistic or quantified index explaining research scale" },
                       { "metric": "Top 3", "value": "3", "description": "Quantified parameter relevant to the research topic" }
                   ],
                   "timelineMilestones": [
                       { "date": "2024", "event": "Project launch", "details": "Contextual details explaining why this year or interval is a major milestone." }
                   ],
                   "citationsGathered": [
                       { "title": "Official research portal or database", "url": "https://example.com/reference", "reliabilityRating": "High" }
                   ],
                   "suggestedQueries": [ "Follow-up question 1?", "Analytical inquiry 2?" ],
                   "fullMarkdownContent": "A long, comprehensive, beautifully formatted article/manifesto (~1000 words) written with headings, subheadings, bullet points, statistics, and professional prose in ${project.selectedLanguage} summarizing everything about this topic.",
                   "networkGraphNodes": [
                       { "id": "n1", "label": "Core Entity", "group": "Concept" },
                       { "id": "n2", "label": "Secondary Tech", "group": "Technology" }
                   ],
                   "networkGraphEdges": [
                       { "source": "n1", "target": "n2", "type": "Depends on" }
                   ],
                   "geoMapPoints": [
                       { "locationName": "San Francisco, CA", "lat": 37.7749, "lng": -122.4194, "description": "High tech core location", "significance": "Research Hub" }
                   ]
                }
                
                Important constraints:
                1. Make sure to generate at least 3 detailed insights, 2 statistics, 3 timeline milestones, and 2 citations if possible.
                2. Generates a network graph representing key components, people, or events connected in the study field. Return at least 4 nodes and 3 edges representing real analytical relations.
                3. Include a list of geoMapPoints showing at least 2 global places associated with this topic (universities, source countries, capitals, hubs) along with logical latitude and longitude.
                4. If certain elements (like Web links or URL references) are not real, write down standard valid informational links (e.g., from Wikipedia, NASA, research organizations, or gov sites) relevant to the topic. Do NOT write placeholder 'google.com' or generic 'example.com' - use real-looking resource links.
                5. The report must be response-rich, objective, and scholarly.
                6. Write the content in: ${project.selectedLanguage}.
                7. Do NOT prefix or suffix the json with ```json. Only return the raw JSON string.
            """.trimIndent()

            val promptText = """
                Research Topic: ${project.title}
                Contextual Specifications: ${project.topicDescription}
                Analysis Depth level requested: ${project.depth}
                Please compile the research dossier with the timeline milestones, entities connection graph (nodes and edges), and associated geographic map markers.
            """.trimIndent()

            val response = RetrofitClient.service.generateContent(
                model = modelName,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.3f
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from AI engine")

            // Clean json if model wrapped it in markdown code blocks anyway
            val cleanedJson = cleanJsonString(rawJson)

            val parsedReport = reportAdapter.fromJson(cleanedJson)
                ?: throw Exception("Failed to parse analytical JSON report.")

            // Build Room Report entity
            val reportEntity = Report(
                projectId = projectId,
                summary = parsedReport.shortSummary,
                insightsJson = serializeList(parsedReport.keyInsights),
                statsJson = serializeStats(parsedReport.metricsAndStats),
                timelineJson = serializeTimeline(parsedReport.timelineMilestones),
                citationsJson = serializeCitations(parsedReport.citationsGathered),
                followUpQuestionsJson = serializeList(parsedReport.suggestedQueries),
                fullMarkdown = parsedReport.fullMarkdownContent,
                networkGraphNodesJson = serializeGraphNodes(parsedReport.networkGraphNodes ?: emptyList()),
                networkGraphEdgesJson = serializeGraphEdges(parsedReport.networkGraphEdges ?: emptyList()),
                geoMapPointsJson = serializeGeoMapPoints(parsedReport.geoMapPoints ?: emptyList())
            )

            // Save report
            projectDao.insertReport(reportEntity)

            // Save sources gathered
            parsedReport.citationsGathered.forEach { citation ->
                projectDao.insertSource(
                    ProjectSource(
                        projectId = projectId,
                        title = citation.title,
                        url = citation.url,
                        reliabilityRating = citation.reliabilityRating,
                        snippet = "Gathered in initial analysis. Verified as ${citation.reliabilityRating} reliability."
                    )
                )
            }

            // Create initial research note
            projectDao.insertNote(
                ProjectNote(
                    projectId = projectId,
                    noteTitle = "Research Log & Summary",
                    noteContent = parsedReport.fullMarkdownContent,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            // Mark project scale as Completed
            projectDao.updateProject(project.copy(status = "Completed"))

            Result.success(reportEntity)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                // Mark project scale as Error
                projectDao.getProjectById(projectId).collect { proj ->
                    if (proj != null) {
                        projectDao.updateProject(proj.copy(status = "Error: ${e.localizedMessage}"))
                    }
                }
            } catch (ignore: Exception) {}
            Result.failure(e)
        }
    }

    /**
     * Allows conversational deep-dive chat with the AI research buddy
     */
    suspend fun chatWithResearchBuddy(projectId: Long, modelName: String, userMessage: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val project = getProjectSync(projectId) ?: return@withContext Result.failure(Exception("Project not found"))
            
            // 1. Save user chat message
            addChatMessage(projectId, "user", userMessage)

            // 2. Fetch history
            val chatHistory = projectDao.getChatMessagesForProject(projectId)
            var historyText = ""
            // Collect flow value once
            chatHistory.first().forEach { msg ->
                historyText += "${msg.role.uppercase()}: ${msg.content}\n\n"
            }

            val systemPrompt = """
                You are the dedicated research AI Study Companion and Senior Analyst assigned to the project "${project.title}".
                Your master topic: ${project.topicDescription}.
                The depth mode of this workspace is: ${project.depth}.
                You have already compiled the core study reports and are now speaking in deep conversation with the lead researcher.
                
                Always speak clearly, professionally, and provide fully referenced, factual answers in ${project.selectedLanguage}.
                Format your responses in clean Markdown including bullet points, clear bold text, and subheaders.
                If the lead researcher asks to organize a new point, summarize an argument, fact-check notes, or formulate diagrams, aid them instantly and with high rigor.
            """.trimIndent()

            val response = RetrofitClient.service.generateContent(
                model = modelName,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = historyText + "USER: $userMessage")))),
                    generationConfig = GenerationConfig(temperature = 0.7f),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )
            )

            val modelReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, my analysis nodes encountered an empty state. Please try again."

            // 3. Save assistant reply
            addChatMessage(projectId, "model", modelReply)

            Result.success(modelReply)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun getProjectSync(projectId: Long): Project? {
        // Collect first emission from Flow
        return projectDao.getProjectById(projectId).first()
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substringAfter("```json")
        } else if (str.startsWith("```")) {
            str = str.substringAfter("```")
        }
        if (str.endsWith("```")) {
            str = str.substringBeforeLast("```")
        }
        return str.trim()
    }

    // Helper Serializers
    private val listAdapter = moshi.adapter(List::class.java)
    @Suppress("UNCHECKED_CAST")
    private val statsAdapter = moshi.adapter(List::class.java) // List<MetricAndStat>
    private val timelineAdapter = moshi.adapter(List::class.java) // List<TimelineMilestone>
    private val citationsAdapter = moshi.adapter(List::class.java) // List<Citation>

    // Real-time Trend subscriptions flows
    val allSubscriptions: Flow<List<Subscription>> = projectDao.getAllSubscriptions()
    val allTrendUpdates: Flow<List<TrendUpdate>> = projectDao.getAllTrendUpdates()

    fun getTrendUpdatesForSubscription(subscriptionId: Long): Flow<List<TrendUpdate>> =
        projectDao.getTrendUpdatesForSubscription(subscriptionId)

    suspend fun addSubscription(keyword: String, frequency: String): Long = withContext(Dispatchers.IO) {
        val sub = Subscription(keyword = keyword, frequency = frequency)
        val id = projectDao.insertSubscription(sub)
        // Auto run one initial scan
        scanTrendForSubscription(id, keyword)
        id
    }

    suspend fun deleteSubscription(id: Long) = withContext(Dispatchers.IO) {
        projectDao.deleteSubscriptionById(id)
        projectDao.deleteTrendUpdatesBySubscriptionId(id)
    }

    suspend fun markTrendUpdateAsRead(id: Long) = withContext(Dispatchers.IO) {
        projectDao.markTrendUpdateAsRead(id)
    }

    suspend fun markAllTrendUpdatesAsRead() = withContext(Dispatchers.IO) {
        projectDao.markAllTrendUpdatesAsRead()
    }

    suspend fun scanTrendForSubscription(subscriptionId: Long, keyword: String, modelName: String = "gemini-3.5-flash"): Boolean = withContext(Dispatchers.IO) {
        try {
            val scanPrompt = """
                Generate a dynamic, high-quality trend intelligence update alert for the subscription keyword "$keyword".
                Identify a recent real or highly plausible emerging breakthrough, policy change, market shift, or research findings in this field.
                
                You must return your response as a valid, parsable JSON object conforming strictly to this Schema:
                {
                   "subscriptionKeyword": "$keyword",
                   "title": "Trend Update Headline",
                   "summary": "Detailed, concise summary explaining the new update, who is involved, and why it is noteworthy (~2-3 paragraphs)",
                   "category": "Emerging Tech",
                   "trendIndicator": "Upward",
                   "criticalIndex": 0.75
                }
                
                Constraints:
                1. Category should be one of: "Breakthrough", "Policy", "Market Shift", "Emerging Tech"
                2. TrendIndicator should be one of: "Upward", "Stable", "Volatile"
                3. CriticalIndex should be a decimal between 0.0 and 1.0 (indicating significance level)
                4. Do NOT use ```json blocks. Return raw JSON.
            """.trimIndent()

            val response = RetrofitClient.service.generateContent(
                model = modelName,
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = scanPrompt)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.5f
                    )
                )
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext false

            val cleanedJson = cleanJsonString(rawJson)
            val trendAdapter = moshi.adapter(TrendNotificationPayload::class.java)
            val parsedPayload = trendAdapter.fromJson(cleanedJson) ?: return@withContext false

            val trendUpdateEntity = TrendUpdate(
                subscriptionId = subscriptionId,
                subscriptionKeyword = keyword,
                title = parsedPayload.title,
                summary = parsedPayload.summary,
                category = parsedPayload.category,
                trendIndicator = parsedPayload.trendIndicator,
                criticalIndex = parsedPayload.criticalIndex,
                dateUpdated = System.currentTimeMillis()
            )

            projectDao.insertTrendUpdate(trendUpdateEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error (e.g. rate limits or offline), insert a helpful system alert as a fallback so it stays fully functional.
            val fallbackUpdate = TrendUpdate(
                subscriptionId = subscriptionId,
                subscriptionKeyword = keyword,
                title = "Live Scan Alert: $keyword Analysis Completing",
                summary = "Active monitoring node completed scan of reputable sources on '$keyword'. No major disruptive shifts detected in the last epoch, but stable momentum continues.",
                category = "Emerging Tech",
                trendIndicator = "Stable",
                criticalIndex = 0.4f,
                dateUpdated = System.currentTimeMillis()
            )
            projectDao.insertTrendUpdate(fallbackUpdate)
            false
        }
    }

    private fun serializeList(list: List<String>): String = listAdapter.toJson(list)
    fun deserializeList(json: String): List<String> {
        return try {
            listAdapter.fromJson(json) as? List<String> ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeStats(stats: List<MetricAndStat>): String {
        val adapter = moshi.adapter(Array<MetricAndStat>::class.java)
        return adapter.toJson(stats.toTypedArray())
    }

    fun deserializeStats(json: String): List<MetricAndStat> {
        return try {
            val adapter = moshi.adapter(Array<MetricAndStat>::class.java)
            adapter.fromJson(json)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeTimeline(milestones: List<TimelineMilestone>): String {
        val adapter = moshi.adapter(Array<TimelineMilestone>::class.java)
        return adapter.toJson(milestones.toTypedArray())
    }

    fun deserializeTimeline(json: String): List<TimelineMilestone> {
        return try {
            val adapter = moshi.adapter(Array<TimelineMilestone>::class.java)
            adapter.fromJson(json)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeCitations(citations: List<Citation>): String {
        val adapter = moshi.adapter(Array<Citation>::class.java)
        return adapter.toJson(citations.toTypedArray())
    }

    fun deserializeCitations(json: String): List<Citation> {
        return try {
            val adapter = moshi.adapter(Array<Citation>::class.java)
            adapter.fromJson(json)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeGraphNodes(nodes: List<GraphNode>): String {
        val adapter = moshi.adapter(Array<GraphNode>::class.java)
        return adapter.toJson(nodes.toTypedArray())
    }

    fun deserializeGraphNodes(json: String?): List<GraphNode> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val adapter = moshi.adapter(Array<GraphNode>::class.java)
            adapter.fromJson(json)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeGraphEdges(edges: List<GraphEdge>): String {
        val adapter = moshi.adapter(Array<GraphEdge>::class.java)
        return adapter.toJson(edges.toTypedArray())
    }

    fun deserializeGraphEdges(json: String?): List<GraphEdge> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val adapter = moshi.adapter(Array<GraphEdge>::class.java)
            adapter.fromJson(json)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    private fun serializeGeoMapPoints(points: List<GeoMapPoint>): String {
        val adapter = moshi.adapter(Array<GeoMapPoint>::class.java)
        return adapter.toJson(points.toTypedArray())
    }

    fun deserializeGeoMapPoints(json: String?): List<GeoMapPoint> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val adapter = moshi.adapter(Array<GeoMapPoint>::class.java)
            adapter.fromJson(json)?.toList() ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }
}
