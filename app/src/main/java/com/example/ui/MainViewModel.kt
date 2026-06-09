package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ResearchState {
    object Idle : ResearchState
    object Loading : ResearchState
    data class Success(val report: Report) : ResearchState
    data class Error(val message: String) : ResearchState
}

sealed interface SelectedTab {
    object Overview : SelectedTab
    object Insights : SelectedTab
    object Notes : SelectedTab
    object Sources : SelectedTab
    object MindMap : SelectedTab
    object InteractiveChat : SelectedTab
}

sealed interface AppScreen {
    object Dashboard : AppScreen
    object CreateResearch : AppScreen
    object TrendMonitor : AppScreen
    data class ProjectWorkspace(val projectId: Long) : AppScreen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.projectDao())

    // UI state
    var currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
        private set

    var isDarkMode = MutableStateFlow(true) // Futuristic Dark Mode by default
        private set

    var selectedTab = MutableStateFlow<SelectedTab>(SelectedTab.Overview)
        private set

    // Selected model tier: "gemini-3.5-flash" (Standard) vs "gemini-3.1-pro-preview" (Expert AI Analyst)
    var selectedModelName = MutableStateFlow("gemini-3.5-flash")
        private set

    // All Projects
    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subscriptions & Trends
    val subscriptions: StateFlow<List<Subscription>> = repository.allSubscriptions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trendUpdates: StateFlow<List<TrendUpdate>> = repository.allTrendUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanningTrend = MutableStateFlow(false)

    // Active project selection
    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    
    val selectedProject: StateFlow<Project?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getProject(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeReport: StateFlow<Report?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getReport(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeNotes: StateFlow<List<ProjectNote>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getNotes(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSources: StateFlow<List<ProjectSource>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSources(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeChats: StateFlow<List<ChatMessage>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getChatMessages(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Execution States
    var researchState = MutableStateFlow<ResearchState>(ResearchState.Idle)
        private set

    var chatIsReplying = MutableStateFlow(false)
        private set

    // Voice simulation toggle to show anims
    var isRecordingVoice = MutableStateFlow(false)
        private set

    fun setDarkMode(dark: Boolean) {
        isDarkMode.value = dark
    }

    fun setModel(pro: Boolean) {
        selectedModelName.value = if (pro) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
    }

    fun changeTab(tab: SelectedTab) {
        selectedTab.value = tab
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
        selectedTab.value = SelectedTab.Overview
        currentScreen.value = AppScreen.ProjectWorkspace(projectId)
    }

    fun navigateToDashboard() {
        _selectedProjectId.value = null
        currentScreen.value = AppScreen.Dashboard
    }

    fun navigateToCreatePage() {
        _selectedProjectId.value = null
        researchState.value = ResearchState.Idle
        currentScreen.value = AppScreen.CreateResearch
    }

    fun navigateToTrendMonitor() {
        _selectedProjectId.value = null
        currentScreen.value = AppScreen.TrendMonitor
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(projectId)
            if (_selectedProjectId.value == projectId) {
                navigateToDashboard()
            }
        }
    }

    fun toggleFavorite(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProject(project.copy(isFavorite = !project.isFavorite))
        }
    }

    // Report Deserialize helpers for UI
    fun getInsightsForReport(report: Report?): List<String> {
        if (report == null) return emptyList()
        return repository.deserializeList(report.insightsJson)
    }

    fun getStatsForReport(report: Report?): List<MetricAndStat> {
        if (report == null) return emptyList()
        return repository.deserializeStats(report.statsJson)
    }

    fun getTimelineForReport(report: Report?): List<TimelineMilestone> {
        if (report == null) return emptyList()
        return repository.deserializeTimeline(report.timelineJson)
    }

    fun getCitationsForReport(report: Report?): List<Citation> {
        if (report == null) return emptyList()
        return repository.deserializeCitations(report.citationsJson)
    }

    fun getFollowUpQueries(report: Report?): List<String> {
        if (report == null) return emptyList()
        return repository.deserializeList(report.followUpQuestionsJson)
    }

    // Research Initiator
    fun initiateResearch(title: String, topic: String, language: String, depth: String) {
        if (title.isBlank()) return
        
        viewModelScope.launch {
            researchState.value = ResearchState.Loading
            
            // Create the record initially
            val projectId = repository.createProject(
                title = title.trim(),
                topicDescription = topic.trim(),
                language = language,
                depth = depth
            )
            
            _selectedProjectId.value = projectId

            // Perform research call
            val result = repository.performResearch(projectId, selectedModelName.value)
            
            if (result.isSuccess) {
                val report = result.getOrThrow()
                researchState.value = ResearchState.Success(report)
                selectProject(projectId)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown Research error."
                researchState.value = ResearchState.Error(errorMsg)
            }
        }
    }

    // Action creators for dynamic resources
    fun addLocalNote(title: String, content: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveNote(projectId, title, content)
        }
    }

    fun deleteLocalNote(note: ProjectNote) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNote(note)
        }
    }

    fun addLocalSource(title: String, url: String, rating: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSource(projectId, title, url, rating)
        }
    }

    fun deleteLocalSource(source: ProjectSource) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSource(source)
        }
    }

    fun sendChatMessage(message: String) {
        val projectId = _selectedProjectId.value ?: return
        if (message.isBlank()) return

        viewModelScope.launch {
            chatIsReplying.value = true
            // Save user message immediately to the Room list
            repository.addChatMessage(projectId, "user", message)
            
            // Invoke model answer
            val result = repository.chatWithResearchBuddy(projectId, selectedModelName.value, message)
            chatIsReplying.value = false
        }
    }

    fun startVoiceInputSimulation() {
        isRecordingVoice.value = true
    }

    fun stopVoiceInputSimulation() {
        isRecordingVoice.value = false
    }

    // Advanced visualizer serializers linkers
    fun getGraphNodesForReport(report: Report?): List<GraphNode> {
        return repository.deserializeGraphNodes(report?.networkGraphNodesJson)
    }

    fun getGraphEdgesForReport(report: Report?): List<GraphEdge> {
        return repository.deserializeGraphEdges(report?.networkGraphEdgesJson)
    }

    fun getGeoMapPointsForReport(report: Report?): List<GeoMapPoint> {
        return repository.deserializeGeoMapPoints(report?.geoMapPointsJson)
    }

    // Trend Monitor controller action triggers
    fun addSubscription(keyword: String, frequency: String) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            isScanningTrend.value = true
            repository.addSubscription(keyword.trim(), frequency)
            isScanningTrend.value = false
        }
    }

    fun deleteSubscription(id: Long) {
        viewModelScope.launch {
            repository.deleteSubscription(id)
        }
    }

    fun scanSubscriptionNow(subscriptionId: Long, keyword: String) {
        viewModelScope.launch {
            isScanningTrend.value = true
            repository.scanTrendForSubscription(subscriptionId, keyword, selectedModelName.value)
            isScanningTrend.value = false
        }
    }

    fun markTrendRead(id: Long) {
        viewModelScope.launch {
            repository.markTrendUpdateAsRead(id)
        }
    }

    fun markAllTrendsRead() {
        viewModelScope.launch {
            repository.markAllTrendUpdatesAsRead()
        }
    }
}
