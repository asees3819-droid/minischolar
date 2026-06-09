package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.ResearchState
import com.example.ui.components.VoiceInputButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewResearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val researchState by viewModel.researchState.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()

    var titleInput by remember { mutableStateOf("") }
    var topicDescriptionInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var selectedDepth by remember { mutableStateOf("Analytical") }

    val languages = listOf("English", "Español", "Français", "Deutsch", "日本語", "中文")
    val depths = listOf("Brief", "Analytical", "Deep Academic")

    var langDropdownExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = researchState,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "ScreenStateSwitch"
        ) { state ->
            if (state is ResearchState.Loading) {
                // Interactive synthesis progress screen
                ResearchSynthesisLoadingScreen()
            } else {
                // Input Form Screen
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "NEW STUDY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                    Text(
                                        text = "INITIALIZE STUDIO",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { viewModel.navigateToDashboard() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back Button")
                                }
                            }
                        )
                    }
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Title / Question
                        Text(
                            text = "Research Topic Focus Area",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("research_title_input"),
                            placeholder = { Text("e.g. CRISPR Agriculture Ethics or Fusion Energy Roadmap") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Detail context description
                        Text(
                            text = "Additional Specifications & Guidelines (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = topicDescriptionInput,
                            onValueChange = { topicDescriptionInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("research_topic_input"),
                            placeholder = { Text("Describe specific points, queries, fact-checking requests, or context you'd like the AI to investigate in detail...") },
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Voice input control
                        VoiceInputButton(
                            isRecording = isRecordingVoice,
                            onRecordingToggle = { isListening ->
                                if (isListening) viewModel.startVoiceInputSimulation()
                                else viewModel.stopVoiceInputSimulation()
                            },
                            onPhraseSimulated = { text ->
                                if (titleInput.isEmpty()) {
                                    titleInput = if (text.length > 30) text.take(30) + " Study" else text
                                }
                                topicDescriptionInput = text
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Attributes Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Language Dropdown
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Target Language",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                            .clickable { langDropdownExpanded = true },
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(selectedLanguage)
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = langDropdownExpanded,
                                        onDismissRequest = { langDropdownExpanded = false }
                                    ) {
                                        languages.forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = {
                                                    selectedLanguage = lang
                                                    langDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Depth Selector
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "Synthesis Level",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    depths.forEachIndexed { idx, d ->
                                        val isSelected = selectedDepth == d
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else Color.Transparent
                                                )
                                                .clickable { selectedDepth = d },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = d.split(" ")[0],
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Launch Synthesis button
                        Button(
                            onClick = {
                                if (titleInput.isNotBlank()) {
                                    viewModel.initiateResearch(
                                        title = titleInput,
                                        topic = topicDescriptionInput,
                                        language = selectedLanguage,
                                        depth = selectedDepth
                                    )
                                }
                            },
                            enabled = titleInput.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("start_synthesis_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Launch Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch Analytical Engine", fontWeight = FontWeight.Black)
                        }

                        // Display potential compile-time or runtime synthesis error
                        if (state is ResearchState.Error) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResearchSynthesisLoadingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "StarRotate")
    val angleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AngleRotate"
    )

    // Animated processes statements
    val loadingPhrases = listOf(
        "Initializing secure satellite intelligence nodes...",
        "Querying global digital libraries & archives...",
        "Evaluating reliability ratings of metadata streams...",
        "Compiling historical chronologies & milestone metrics...",
        "Running algorithmic statistical fact-check queries...",
        "Structuring responsive Mind Map logic graphs...",
        "Constructing complete Research Report..."
    )

    var currentPhraseIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            currentPhraseIdx = (currentPhraseIdx + 1) % loadingPhrases.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                // Outer rotating ring
                CircularProgressIndicator(
                    modifier = Modifier.size(110.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 3.dp
                )

                // Outer primary track
                CircularProgressIndicator(
                    modifier = Modifier.size(130.dp),
                    strokeWidth = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )

                // Center logo
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Cosmo Spark",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(angleRotation)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Synthesizing Dossier Study Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated textual transition for processes
            AnimatedContent(
                targetState = loadingPhrases[currentPhraseIdx],
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> -height } + fadeOut()
                },
                label = "PhraseSlide"
            ) { phrase ->
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This usually takes about 15-30 seconds depending on topic scope.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
