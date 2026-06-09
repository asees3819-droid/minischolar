package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.MainViewModel
import com.example.ui.SelectedTab
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val project by viewModel.selectedProject.collectAsState()
    val report by viewModel.activeReport.collectAsState()
    val notes by viewModel.activeNotes.collectAsState()
    val sources by viewModel.activeSources.collectAsState()
    val chats by viewModel.activeChats.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val chatIsReplying by viewModel.chatIsReplying.collectAsState()

    val scope = rememberCoroutineScope()

    // Dialog state
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<ProjectNote?>(null) }

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${project!!.depth.uppercase()} DOSSIER WORKSTATION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Text(
                            text = project!!.title,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToDashboard() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back Button")
                    }
                },
                actions = {
                    // Bookmark Favorite Toggle
                    IconButton(onClick = { viewModel.toggleFavorite(project!!) }) {
                        Icon(
                            imageVector = if (project!!.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Toggle",
                            tint = if (project!!.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // PDF Export Trigger
                    IconButton(onClick = { showPdfExportDialog = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export to PDF Document")
                    }

                    // Delete active workspace
                    IconButton(onClick = { viewModel.deleteProject(project!!.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Dossier", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = when (selectedTab) {
                    SelectedTab.Overview -> 0
                    SelectedTab.Insights -> 1
                    SelectedTab.Notes -> 2
                    SelectedTab.Sources -> 3
                    SelectedTab.MindMap -> 4
                    SelectedTab.InteractiveChat -> 5
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dossier_tabs"),
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[when (selectedTab) {
                            SelectedTab.Overview -> 0
                            SelectedTab.Insights -> 1
                            SelectedTab.Notes -> 2
                            SelectedTab.Sources -> 3
                            SelectedTab.MindMap -> 4
                            SelectedTab.InteractiveChat -> 5
                        }]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == SelectedTab.Overview,
                    onClick = { viewModel.changeTab(SelectedTab.Overview) },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == SelectedTab.Insights,
                    onClick = { viewModel.changeTab(SelectedTab.Insights) },
                    text = { Text("Insights & Stats") },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == SelectedTab.Notes,
                    onClick = { viewModel.changeTab(SelectedTab.Notes) },
                    text = { Text("Study Logs (${notes.size})") },
                    icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == SelectedTab.Sources,
                    onClick = { viewModel.changeTab(SelectedTab.Sources) },
                    text = { Text("Sources (${sources.size})") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == SelectedTab.MindMap,
                    onClick = { viewModel.changeTab(SelectedTab.MindMap) },
                    text = { Text("Logic Map") },
                    icon = { Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == SelectedTab.InteractiveChat,
                    onClick = { viewModel.changeTab(SelectedTab.InteractiveChat) },
                    text = { Text("Analyst Chat") },
                    icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        SelectedTab.Overview -> OverviewTab(
                            project = project!!,
                            report = report,
                            viewModel = viewModel
                        )
                        SelectedTab.Insights -> InsightsTab(
                            report = report,
                            viewModel = viewModel
                        )
                        SelectedTab.Notes -> NotesTab(
                            notes = notes,
                            onAddNoteClick = { showAddNoteDialog = true },
                            onEditNoteClick = { note ->
                                noteToEdit = note
                                showAddNoteDialog = true
                            },
                            onDeleteNoteClick = { viewModel.deleteLocalNote(it) }
                        )
                        SelectedTab.Sources -> SourcesTab(
                            sources = sources,
                            onAddSourceClick = { showAddSourceDialog = true },
                            onDeleteSourceClick = { viewModel.deleteLocalSource(it) }
                        )
                        SelectedTab.MindMap -> MindMapTab(
                            project = project!!,
                            report = report,
                            viewModel = viewModel
                        )
                        SelectedTab.InteractiveChat -> ChatTab(
                            chats = chats,
                            replying = chatIsReplying,
                            followUps = viewModel.getFollowUpQueries(report),
                            onSendChat = { viewModel.sendChatMessage(it) }
                        )
                    }
                }
            }
        }

        // --- Dialogs Section ---

        // Add Note Dialog
        if (showAddNoteDialog) {
            AddNoteDialog(
                noteValue = noteToEdit,
                onDismiss = {
                    showAddNoteDialog = false
                    noteToEdit = null
                },
                onSave = { title, body ->
                    if (noteToEdit != null) {
                        viewModel.deleteLocalNote(noteToEdit!!)
                    }
                    viewModel.addLocalNote(title, body)
                    showAddNoteDialog = false
                    noteToEdit = null
                }
            )
        }

        // Add Source Dialog
        if (showAddSourceDialog) {
            AddSourceDialog(
                onDismiss = { showAddSourceDialog = false },
                onSave = { title, url, rRating ->
                    viewModel.addLocalSource(title, url, rRating)
                    showAddSourceDialog = false
                }
            )
        }

        // PDF Export Dialog
        if (showPdfExportDialog) {
            PdfExportDialog(
                project = project!!,
                report = report,
                onDismiss = { showPdfExportDialog = false }
            )
        }
    }
}

// --- TAB SUB-COMPOSABLES ---

@Composable
fun OverviewTab(
    project: Project,
    report: Report?,
    viewModel: MainViewModel
) {
    if (report == null) {
        LoadingPlaceholder(tabName = "Overview report dossier")
        return
    }

    val timeline = remember(report) { viewModel.getTimelineForReport(report) }
    val queries = remember(report) { viewModel.getFollowUpQueries(report) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Executive Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Executive Summary Dossier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Milestones Timeline List
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timeline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Chronology & Timeline Milestones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        if (timeline.isEmpty()) {
            Text("No milestones captured for this topic.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            timeline.forEach { milestone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.width(72.dp)
                        ) {
                            Text(
                                text = milestone.date,
                                modifier = Modifier.padding(6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = milestone.event,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = milestone.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Suggested follow ups
        if (queries.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recommended Research Inquiries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            queries.forEach { query ->
                Surface(
                    onClick = { viewModel.changeTab(SelectedTab.InteractiveChat) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsTab(
    report: Report?,
    viewModel: MainViewModel
) {
    if (report == null) {
        LoadingPlaceholder(tabName = "Insights dossier")
        return
    }

    val insights = remember(report) { viewModel.getInsightsForReport(report) }
    val stats = remember(report) { viewModel.getStatsForReport(report) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Bulleted Key Insights Cards
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.OfflineBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Key Scientific Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        insights.forEachIndexed { i, insight ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${i + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual stats chart simulation row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WaterfallChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Core Quantified Metrics & Indices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (stats.isEmpty()) {
            Text("No statistics collected for this topic model.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            stats.forEach { stat ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = stat.metric,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stat.value,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Progress indicator simulating scale
                        LinearProgressIndicator(
                            progress = { 0.75f }, // Mock-up fill value
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotesTab(
    notes: List<ProjectNote>,
    onAddNoteClick: () -> Unit,
    onEditNoteClick: (ProjectNote) -> Unit,
    onDeleteNoteClick: (ProjectNote) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Researcher Work Log & Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onAddNoteClick,
                modifier = Modifier.testTag("add_note_button"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Record", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (notes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No supplemental notes compiled yet.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditNoteClick(note) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = note.noteTitle,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { onDeleteNoteClick(note) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Delete Icon", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = note.noteContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourcesTab(
    sources: List<ProjectSource>,
    onAddSourceClick: () -> Unit,
    onDeleteSourceClick: (ProjectSource) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bibliography Source Manager",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onAddSourceClick,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Link", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (sources.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No bibliography references collected.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sources) { source ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = source.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Quality badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (source.reliabilityRating == "High") MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = source.reliabilityRating,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (source.reliabilityRating == "High") MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.tertiary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(onClick = { onDeleteSourceClick(source) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove Source", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = source.snippet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapTab(
    project: Project,
    report: Report?,
    viewModel: MainViewModel
) {
    if (report == null) {
        LoadingPlaceholder(tabName = "Mind map logic structures")
        return
    }

    var visualSelection by remember { mutableStateOf(0) } // 0 = Concept Network, 1 = Spatial Maps, 2 = Chrono Timeline

    val nodes = remember(report) { viewModel.getGraphNodesForReport(report) }
    val edges = remember(report) { viewModel.getGraphEdgesForReport(report) }
    val points = remember(report) { viewModel.getGeoMapPointsForReport(report) }
    val milestones = remember(report) { viewModel.getTimelineForReport(report) }

    Column(modifier = Modifier.fillMaxSize()) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("visuals_selector_row")
        ) {
            SegmentedButton(
                selected = visualSelection == 0,
                onClick = { visualSelection = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = { SegmentedButtonDefaults.Icon(active = visualSelection == 0) }
            ) {
                Text("Concept Web", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = visualSelection == 1,
                onClick = { visualSelection = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = { SegmentedButtonDefaults.Icon(active = visualSelection == 1) }
            ) {
                Text("Spatial Maps", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = visualSelection == 2,
                onClick = { visualSelection = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = { SegmentedButtonDefaults.Icon(active = visualSelection == 2) }
            ) {
                Text("Chrono Timeline", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            when (visualSelection) {
                0 -> {
                    NetworkGraphWidget(nodes = nodes, edges = edges)
                }
                1 -> {
                    GeographicalMapWidget(points = points)
                }
                2 -> {
                    InteractiveTimelineWidget(milestones = milestones)
                }
            }
        }
    }
}

@Composable
fun ChatTab(
    chats: List<ChatMessage>,
    replying: Boolean,
    followUps: List<String>,
    onSendChat: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Auto-scroll on new chats
    LaunchedEffect(chats.size, replying) {
        if (chats.isNotEmpty()) {
            listState.animateScrollToItem(chats.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat History List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (chats.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Deep-Dive exploration companion active.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Ask your expert study assistant to summarize arguments, clarify statistics, suggest avenues, or translate files.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(chats) { chat ->
                    val isModel = chat.role == "model"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isModel) 2.dp else 16.dp,
                                bottomEnd = if (isModel) 16.dp else 2.dp
                            ),
                            color = if (isModel) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isModel) "AI Scientist companion" else "Lead Lead Researcher",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isModel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chat.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isModel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (replying) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Text("Replying...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // Suggested Chips
        if (chats.isEmpty() && followUps.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(followUps) { query ->
                    Surface(
                        onClick = { onSendChat(query) },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = query,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Chat input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                placeholder = { Text("Probe deeper details...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = {
                    if (textInput.isNotBlank()) {
                        onSendChat(textInput.trim())
                        textInput = ""
                    }
                },
                modifier = Modifier.testTag("send_chat_button"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = Color.White)
            }
        }
    }
}

@Composable
fun LoadingPlaceholder(tabName: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("Gathering $tabName nodes offline...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// --- DIALOG CONTAINERS ---

@Composable
fun AddNoteDialog(
    noteValue: ProjectNote?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(noteValue?.noteTitle ?: "") }
    var content by remember { mutableStateOf(noteValue?.noteContent ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (noteValue != null) "Edit Research Log" else "Add New Field Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content Notes") },
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(title, content) },
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Note")
                    }
                }
            }
        }
    }
}

@Composable
fun AddSourceDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("High") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add Custom Citation Reference",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document / Portal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Referenced URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Reliability Weight Rating", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("High", "Medium", "Unverified").forEach { option ->
                        val isSelected = rating == option
                        Surface(
                            onClick = { rating = option },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(title, url, rating) },
                        enabled = title.isNotBlank() && url.isNotBlank(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Reference")
                    }
                }
            }
        }
    }
}

@Composable
fun PdfExportDialog(
    project: Project,
    report: Report?,
    onDismiss: () -> Unit
) {
    var isExporting by remember { mutableStateOf(true) }
    val progressFlow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progressFlow.animateTo(
            targetValue = 100f,
            animationSpec = tween(3200, easing = LinearEasing)
        )
        isExporting = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isExporting) {
                    Icon(
                        imageVector = Icons.Default.AllInclusive,
                        contentDescription = "Exporting",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Structuring Scholarly PDF Document",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val textVal = when {
                        progressFlow.value < 30f -> "Allocating LaTeX publication canvas..."
                        progressFlow.value < 65f -> "Translating bibliographic citations..."
                        else -> "Drafting executive summary charts vector block buffers..."
                    }
                    Text(
                        text = textVal,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressFlow.value / 100f },
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${progressFlow.value.toInt()}%", style = MaterialTheme.typography.labelSmall)
                } else {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Academic Publication Compiled Successfully!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Document 'AI_Research_${project.title.replace(" ", "_")}.pdf' compiled to local device storage buffers.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Simple simulated preview document
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = "OFFICIAL DOSSIER MANUSCRIPT: " + project.title.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "AUTHORS: ResearchAI Analyst Core · SOURCE: Deep Synthesizer v1beta\n" +
                                        "ABSTRACT: " + (report?.summary?.take(100) ?: "") + "...",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 10.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mock Print")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open PDF")
                        }
                    }
                }
            }
        }
    }
}
