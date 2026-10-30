package com.rabbitear.app.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rabbitear.app.data.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val GruvboxDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFE8019),          // orange-lit: #fe8019
    onPrimary = Color(0xFF14100C),        // bg-hard: #14100c
    primaryContainer = Color(0xFFD65D0E),  // orange: #d65d0e
    onPrimaryContainer = Color(0xFFEBDBB2),// fg: #ebdbb2
    secondary = Color(0xFF98971A),        // green-lit: #98971a
    onSecondary = Color(0xFF14100C),
    secondaryContainer = Color(0xFF79740E),// green: #79740e
    onSecondaryContainer = Color(0xFFEBDBB2),
    tertiary = Color(0xFF689D6A),         // aqua: #689d6a
    onTertiary = Color(0xFF14100C),
    background = Color(0xFF1D1811),       // bg: #1d1811
    onBackground = Color(0xFFEBDBB2),     // fg: #ebdbb2
    surface = Color(0xFF262019),          // bg-soft: #262019
    onSurface = Color(0xFFEBDBB2),        // fg: #ebdbb2
    surfaceVariant = Color(0xFF322A20),   // bg1: #322a20
    onSurfaceVariant = Color(0xFFBDAE93), // fg-dim: #bdae93
    outline = Color(0xFF4A3F31)           // rule: #4a3f31
)

private val GruvboxLightColorScheme = lightColorScheme(
    primary = Color(0xFFD65D0E),          // burnt orange
    onPrimary = Color(0xFFFBF1C7),
    primaryContainer = Color(0xFFFE8019),
    onPrimaryContainer = Color(0xFF14100C),
    secondary = Color(0xFF79740E),        // avocado green
    onSecondary = Color(0xFFFBF1C7),
    secondaryContainer = Color(0xFF98971A),
    onSecondaryContainer = Color(0xFF14100C),
    tertiary = Color(0xFF427B58),
    onTertiary = Color(0xFFFBF1C7),
    background = Color(0xFFFBF1C7),       // heavy cream background
    onBackground = Color(0xFF1D1811),
    surface = Color(0xFFEBDBB2),
    onSurface = Color(0xFF14100C),
    surfaceVariant = Color(0xFFD5C4A1),
    onSurfaceVariant = Color(0xFF665C54),
    outline = Color(0xFF8A7D66)
)

private val MonospaceTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = FontFamily.Monospace),
        displayMedium = displayMedium.copy(fontFamily = FontFamily.Monospace),
        displaySmall = displaySmall.copy(fontFamily = FontFamily.Monospace),
        headlineLarge = headlineLarge.copy(fontFamily = FontFamily.Monospace),
        headlineMedium = headlineMedium.copy(fontFamily = FontFamily.Monospace),
        headlineSmall = headlineSmall.copy(fontFamily = FontFamily.Monospace),
        titleLarge = titleLarge.copy(fontFamily = FontFamily.Monospace),
        titleMedium = titleMedium.copy(fontFamily = FontFamily.Monospace),
        titleSmall = titleSmall.copy(fontFamily = FontFamily.Monospace),
        bodyLarge = bodyLarge.copy(fontFamily = FontFamily.Monospace),
        bodyMedium = bodyMedium.copy(fontFamily = FontFamily.Monospace),
        bodySmall = bodySmall.copy(fontFamily = FontFamily.Monospace),
        labelLarge = labelLarge.copy(fontFamily = FontFamily.Monospace),
        labelMedium = labelMedium.copy(fontFamily = FontFamily.Monospace),
        labelSmall = labelSmall.copy(fontFamily = FontFamily.Monospace)
    )
}

@Composable
fun RabbitEarTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) GruvboxDarkColorScheme else GruvboxLightColorScheme
    MaterialTheme(colorScheme = scheme, typography = MonospaceTypography, content = content)
}

sealed interface Screen {
    data object Home : Screen
    data object Subscriptions : Screen
    data object Slate : Screen
    data object Reruns : Screen
    data object LatestEpisodes : Screen
    data object Downloads : Screen
    data object Search : Screen
    data class Detail(val p: Podcast) : Screen
    data object Settings : Screen
}

@Composable
fun RenderScreen(
    currentScreen: Screen,
    vm: VM,
    navStack: SnapshotStateList<Screen>,
    onToggleSettings: () -> Unit
) {
    when (val s = currentScreen) {
        Screen.Home -> Home(
            vm,
            onSubscriptions = { navStack.add(Screen.Subscriptions) },
            onSlate = { navStack.add(Screen.Slate) },
            onReruns = { navStack.add(Screen.Reruns) },
            onLatestEpisodes = { navStack.add(Screen.LatestEpisodes) },
            onDownloads = { navStack.add(Screen.Downloads) },
            onSettings = onToggleSettings
        )
        Screen.Subscriptions -> SubscriptionsScreen(
            vm,
            onSearch = { navStack.add(Screen.Search) },
            onOpen = { navStack.add(Screen.Detail(it)) },
            onSettings = onToggleSettings,
            back = { navStack.removeAt(navStack.lastIndex) }
        )
        Screen.Slate -> SlateScreen(
            vm,
            back = { navStack.removeAt(navStack.lastIndex) },
            onOpenPodcast = { navStack.add(Screen.Detail(it)) },
            onSettings = onToggleSettings
        )
        Screen.Reruns -> RerunsScreen(
            vm,
            back = { navStack.removeAt(navStack.lastIndex) },
            onOpenPodcast = { navStack.add(Screen.Detail(it)) },
            onSettings = onToggleSettings
        )
        Screen.LatestEpisodes -> LatestEpisodesScreen(
            vm,
            back = { navStack.removeAt(navStack.lastIndex) },
            onOpenPodcast = { navStack.add(Screen.Detail(it)) },
            onSettings = onToggleSettings
        )
        Screen.Downloads -> DownloadsScreen(
            vm,
            back = { navStack.removeAt(navStack.lastIndex) },
            onOpenPodcast = { navStack.add(Screen.Detail(it)) },
            onSettings = onToggleSettings
        )
        Screen.Search -> SearchScreen(
            vm,
            back = { navStack.removeAt(navStack.lastIndex) },
            open = { navStack.add(Screen.Detail(it)) },
            onSettings = onToggleSettings
        )
        is Screen.Detail -> DetailScreen(
            vm,
            s.p,
            back = { navStack.removeAt(navStack.lastIndex) },
            onSettings = onToggleSettings
        )
        Screen.Settings -> {}
    }
}

@Composable
fun LandscapePlayerCard(
    vm: VM,
    modifier: Modifier = Modifier
) {
    val currentGuid = vm.currentMediaId()
    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
    val isFav = currentGuid != null && favorites.any { it.guid == currentGuid }
    var showSpeedDialog by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF14100C),
        modifier = modifier
            .fillMaxHeight()
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (vm.nowTitle.isNotEmpty()) {
                // Top Row: Speed & Favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSpeedDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "Speed & Chapters",
                            tint = if (vm.isProfileModified) Color(0xFFCC241D) else Color(0xFFFE8019),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    FavoriteButton(
                        isFavorite = isFav,
                        onToggle = { vm.toggleFavoriteCurrent() },
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Artwork
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF262019),
                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                    modifier = Modifier
                        .size(100.dp)
                        .padding(2.dp)
                ) {
                    if (!vm.nowArtwork.isNullOrEmpty()) {
                        AsyncImage(
                            model = vm.nowArtwork,
                            contentDescription = "Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(44.dp))
                        }
                    }
                }

                // Title & Show
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = vm.nowTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEBDBB2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = vm.nowShow,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFBDAE93),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Stereo Level Meter
                StereoLevelMeter(leftLevel = vm.rmsLeft, rightLevel = vm.rmsRight, isPlaying = vm.isPlaying)

                // Slider & Progress
                val pos = vm.positionMs
                val dur = vm.durationMs
                var sliderPos by remember { mutableFloatStateOf(-1f) }
                val currentSliderVal = if (sliderPos >= 0) sliderPos else if (dur > 0) pos.toFloat() / dur.toFloat() else 0f

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = currentSliderVal.coerceIn(0f, 1f),
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = {
                            if (dur > 0) {
                                vm.seekTo((sliderPos * dur).toLong())
                            }
                            sliderPos = -1f
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFE8019),
                            activeTrackColor = Color(0xFFFE8019),
                            inactiveTrackColor = Color(0xFF322A20)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(if (sliderPos >= 0 && dur > 0) (sliderPos * dur).toLong() else pos), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                        Text(formatMs(dur), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                    }
                }

                // Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.skipBack() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Replay30, contentDescription = "Back 30s", tint = Color(0xFFFE8019), modifier = Modifier.size(24.dp))
                    }
                    Surface(
                        onClick = { vm.togglePlay() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFE8019),
                        border = BorderStroke(1.dp, Color(0xFFD65D0E)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color(0xFF14100C),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    IconButton(onClick = { vm.skipForward() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Forward30, contentDescription = "Forward 30s", tint = Color(0xFFFE8019), modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = { vm.skip() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFFFE8019), modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                // Empty state when no episode is loaded
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF8A7D66), modifier = Modifier.size(48.dp))
                        Text("RABBIT EAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                        Text("No episode playing", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDAE93))
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        SpeedAndChaptersDialog(vm = vm, onDismiss = { showSpeedDialog = false })
    }
}

@Composable
fun Root(vm: VM, openScreen: String? = null) {
    val navStack = remember(openScreen) {
        val initial = if (openScreen == "LatestEpisodes") {
            listOf(Screen.Home, Screen.LatestEpisodes)
        } else {
            listOf(Screen.Home)
        }
        mutableStateListOf<Screen>().apply { addAll(initial) }
    }
    val currentScreen = navStack.lastOrNull() ?: Screen.Home
    var showFullscreenPlayer by remember { mutableStateOf(false) }
    var showSettingsOverlay by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var lastTitle by remember { mutableStateOf("") }
    LaunchedEffect(vm.nowTitle) {
        lastTitle = vm.nowTitle
    }

    BackHandler(showSettingsOverlay || navStack.size > 1) {
        if (showSettingsOverlay) {
            showSettingsOverlay = false
        } else {
            navStack.removeAt(navStack.lastIndex)
        }
    }

    // Gesture detection
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val gestureModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = {
                totalDragX = 0f
                totalDragY = 0f
            },
            onDragEnd = {
                val absX = kotlin.math.abs(totalDragX)
                val absY = kotlin.math.abs(totalDragY)

                if (absY > absX && absY > 80f) {
                    if (totalDragY < -80f) {
                        if (vm.nowTitle.isNotEmpty()) {
                            showFullscreenPlayer = true
                        }
                    } else if (totalDragY > 80f) {
                        showFullscreenPlayer = false
                    }
                } else if (absX > absY && absX > 80f) {
                    if (totalDragX > 80f) {
                        if (navStack.size > 1) {
                            navStack.removeAt(navStack.lastIndex)
                        }
                    }
                }
            },
            onDrag = { change, dragAmount ->
                change.consume()
                totalDragX += dragAmount.x
                totalDragY += dragAmount.y
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().then(gestureModifier)) {
        if (isLandscape) {
            // LANDSCAPE SPLIT-SCREEN LAYOUT
            Row(modifier = Modifier.fillMaxSize()) {
                // Left 50%: Constricted Fullscreen Player (Persists until vertical rotation)
                LandscapePlayerCard(
                    vm = vm,
                    modifier = Modifier.weight(1f)
                )

                // Divider line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF4A3F31))
                )

                // Right 50%: Constricted Version of Full UI (Scrollable with Screen Wipe)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            )
                        },
                        label = "screen_wipe_landscape"
                    ) { screen ->
                        RenderScreen(
                            currentScreen = screen,
                            vm = vm,
                            navStack = navStack,
                            onToggleSettings = { showSettingsOverlay = !showSettingsOverlay }
                        )
                    }
                }
            }

            if (showSettingsOverlay) {
                SettingsOverlay(
                    vm = vm,
                    onDismiss = { showSettingsOverlay = false }
                )
            }
        } else {
            // PORTRAIT LAYOUT
            Scaffold(bottomBar = { MiniPlayer(vm, onClick = { showFullscreenPlayer = true }) }) { pad ->
                Box(Modifier.padding(pad)) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            slideInHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            )
                        },
                        label = "screen_wipe_portrait"
                    ) { screen ->
                        RenderScreen(
                            currentScreen = screen,
                            vm = vm,
                            navStack = navStack,
                            onToggleSettings = { showSettingsOverlay = !showSettingsOverlay }
                        )
                    }

                    if (showSettingsOverlay) {
                        SettingsOverlay(
                            vm = vm,
                            onDismiss = { showSettingsOverlay = false }
                        )
                    }
                }
            }

            // Fullscreen Player Wipe Up/Down Animation
            AnimatedVisibility(
                visible = showFullscreenPlayer && vm.nowTitle.isNotEmpty(),
                enter = slideInVertically(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    targetOffsetY = { fullHeight -> fullHeight }
                ) + fadeOut()
            ) {
                FullscreenPlayer(
                    vm = vm,
                    onDismiss = { showFullscreenPlayer = false }
                )
            }
        }

        if (vm.showSkipConfirmationDialog) {
            SkipConfirmationDialog(
                onConfirmMarkPlayed = { vm.confirmSkip(markPlayed = true) },
                onConfirmSkipOnly = { vm.confirmSkip(markPlayed = false) },
                onDismiss = { vm.showSkipConfirmationDialog = false }
            )
        }
    }
}

@Composable
fun SkipConfirmationDialog(
    onConfirmMarkPlayed: () -> Unit,
    onConfirmSkipOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Skip Episode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
        },
        text = {
            Text("Would you also like to mark this episode as played?", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEBDBB2))
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirmMarkPlayed()
                onDismiss()
            }) {
                Text("Yes", fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onConfirmSkipOnly()
                onDismiss()
            }) {
                Text("No", color = Color(0xFFBDAE93))
            }
        }
    )
}

@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isFavorite) "Remove from Reruns" else "Add to Reruns",
            tint = if (isFavorite) Color(0xFFCC241D) else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun SlateButton(
    inSlate: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (inSlate) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
            contentDescription = if (inSlate) "Remove from Slate" else "Add to Slate",
            tint = if (inSlate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ToolboxButton(
    onClick: () -> Unit,
    isOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF322A20),
        border = BorderStroke(2.dp, Color(0xFF4A3F31)),
        modifier = modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.HomeRepairService,
                contentDescription = if (isOpen) "Close Settings" else "Settings & Toolbox",
                tint = if (isOpen) Color(0xFFFE8019) else Color(0xFFBDAE93),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeedAndChaptersDialog(
    vm: VM,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    val currentSpeed = vm.currentSpeed
    val chapters = vm.currentChapters
    val currentEp = vm.currentEpisode

    var transcriptLines by remember(currentEp?.guid) { mutableStateOf<List<TranscriptLine>>(emptyList()) }
    var loadingTranscript by remember(currentEp?.guid) { mutableStateOf(false) }

    LaunchedEffect(currentEp?.guid) {
        val tUrl = currentEp?.transcriptUrl
        if (!tUrl.isNullOrEmpty()) {
            loadingTranscript = true
            transcriptLines = TranscriptParser.fetchAndParse(tUrl, currentEp.transcriptType)
            loadingTranscript = false
        }
    }

    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (activeTab) {
                        0 -> "Playback Speed"
                        1 -> "Chapters (${chapters.size})"
                        else -> "Episode Transcript"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFEBDBB2)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Speed", "Chapters (${chapters.size})", "Transcript").forEachIndexed { index, label ->
                        val isSel = activeTab == index
                        Surface(
                            onClick = { activeTab = index },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFFFE8019) else Color(0xFF322A20),
                            border = BorderStroke(1.dp, Color(0xFF4A3F31))
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color(0xFF14100C) else Color(0xFFEBDBB2),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        Text("Select Playback Speed:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEBDBB2))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            speeds.forEach { sp ->
                                val isSelected = (currentSpeed == sp)
                                Surface(
                                    onClick = { vm.setSpeed(sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFFE8019) else Color(0xFF322A20),
                                    border = BorderStroke(1.dp, Color(0xFF4A3F31))
                                ) {
                                    Text(
                                        text = "${sp}x",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF14100C) else Color(0xFFEBDBB2),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (chapters.isEmpty()) {
                            Text("No chapters found in episode description or metadata.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBDAE93))
                        } else {
                            chapters.forEach { ch ->
                                Surface(
                                    onClick = {
                                        vm.seekTo(ch.startMs)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF262019),
                                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ch.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFFEBDBB2), modifier = Modifier.weight(1f))
                                        Text(formatMs(ch.startMs), style = MaterialTheme.typography.labelSmall, color = Color(0xFFFE8019))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (loadingTranscript) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFFE8019))
                            }
                        } else if (transcriptLines.isEmpty()) {
                            Text("No native transcript available for this episode.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFBDAE93))
                        } else {
                            val pos = vm.positionMs
                            transcriptLines.forEach { line ->
                                val isCurrent = pos >= line.startMs && (line.endMs == 0L || pos <= line.endMs)
                                Surface(
                                    onClick = {
                                        vm.seekTo(line.startMs)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) Color(0xFF322A20) else Color(0xFF262019),
                                    border = BorderStroke(1.dp, if (isCurrent) Color(0xFFFE8019) else Color(0xFF4A3F31)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            if (!line.speaker.isNullOrEmpty()) {
                                                Text(line.speaker, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                            }
                                            Text(formatMs(line.startMs), style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                                        }
                                        Text(
                                            text = line.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isCurrent) Color(0xFFFE8019) else Color(0xFFEBDBB2),
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFFE8019))
            }
        }
    )
}

private const val METER_SEGMENT_COUNT = 12
private const val METER_DECAY_SPEED = 0.12f

@Composable
fun StereoLevelMeter(
    leftLevel: Float = 0f,
    rightLevel: Float = 0f,
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    var ghostLeft by remember { mutableFloatStateOf(0f) }
    var ghostRight by remember { mutableFloatStateOf(0f) }

    val activeLeft = if (isPlaying) leftLevel.coerceIn(0f, 1f) else 0f
    val activeRight = if (isPlaying) rightLevel.coerceIn(0f, 1f) else 0f

    LaunchedEffect(activeLeft, activeRight, isPlaying) {
        while (isPlaying) {
            delay(30)
            ghostLeft = if (activeLeft > ghostLeft) activeLeft else {
                (ghostLeft - METER_DECAY_SPEED).coerceAtLeast(0f)
            }
            ghostRight = if (activeRight > ghostRight) activeRight else {
                (ghostRight - METER_DECAY_SPEED).coerceAtLeast(0f)
            }
        }
        if (!isPlaying) {
            ghostLeft = 0f
            ghostRight = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .background(Color(0xFF14100C), RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, Color(0xFF4A3F31)), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val channelHeight = (height - 3.dp.toPx()) / 2f

            drawHorizontalChannel(
                label = "L",
                level = activeLeft,
                ghostLevel = ghostLeft,
                yStart = 0f,
                width = width,
                channelHeight = channelHeight,
                segmentCount = METER_SEGMENT_COUNT
            )

            drawHorizontalChannel(
                label = "R",
                level = activeRight,
                ghostLevel = ghostRight,
                yStart = channelHeight + 3.dp.toPx(),
                width = width,
                channelHeight = channelHeight,
                segmentCount = METER_SEGMENT_COUNT
            )
        }
    }
}

private fun DrawScope.drawHorizontalChannel(
    label: String,
    level: Float,
    ghostLevel: Float,
    yStart: Float,
    width: Float,
    channelHeight: Float,
    segmentCount: Int
) {
    val segmentWidth = width / segmentCount
    val maxSegments = (level * segmentCount).toInt().coerceIn(0, segmentCount)
    val ghostSegments = (ghostLevel * segmentCount).toInt().coerceIn(0, segmentCount)

    for (i in 0 until segmentCount) {
        val x = i * segmentWidth
        val isActive = i < maxSegments
        val isGhost = i < ghostSegments && !isActive

        val color = when {
            isActive && i >= segmentCount * 0.8 -> Color(0xFFCC241D)     // Clipping Red
            isActive && i >= segmentCount * 0.6 -> Color(0xFFFE8019)     // Loud Orange
            isActive -> Color(0xFF98971A)                                 // Normal Lush Green
            isGhost -> Color(0xFFEBDBB2).copy(alpha = 0.35f)             // Ghost tail
            else -> Color(0xFF322A20)                                     // Dimmed slot
        }

        // 1. Mirrored Glass Reflection Behind Tube
        val reflectionOffset = channelHeight * 0.7f
        val reflectionAlpha = when {
            isActive -> 0.25f
            isGhost -> 0.12f
            else -> 0.04f
        }
        drawRoundRect(
            color = color.copy(alpha = reflectionAlpha),
            topLeft = Offset(x + 1.dp.toPx(), yStart + reflectionOffset),
            size = Size((segmentWidth - 2.dp.toPx()).coerceAtLeast(1f), (channelHeight * 0.6f).coerceAtLeast(1f)),
            cornerRadius = CornerRadius((channelHeight * 0.3f).coerceAtLeast(0.5f), (channelHeight * 0.3f).coerceAtLeast(0.5f))
        )

        // 2. Main Vacuum Tube Glass Body
        val tubeRadius = CornerRadius((channelHeight * 0.45f).coerceAtLeast(0.5f), (channelHeight * 0.45f).coerceAtLeast(0.5f))
        drawRoundRect(
            color = color,
            topLeft = Offset(x, yStart),
            size = Size((segmentWidth - 2.dp.toPx()).coerceAtLeast(1f), channelHeight),
            cornerRadius = tubeRadius
        )

        // 3. Tube Bezel / Outline
        drawRoundRect(
            color = if (isActive) Color(0xFF4A3F31) else Color(0xFF262019),
            topLeft = Offset(x, yStart),
            size = Size((segmentWidth - 2.dp.toPx()).coerceAtLeast(1f), channelHeight),
            cornerRadius = tubeRadius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )

        // 4. Glowing Vacuum Tube Filament Core & Specular Glass Reflection
        if (isActive) {
            val coreColor = if (i >= segmentCount * 0.8) Color(0xFFFFD700) else Color(0xFFFFFFE0)
            val filamentW = (segmentWidth - 2.dp.toPx()) * 0.55f
            val filamentH = channelHeight * 0.35f
            val filamentX = x + ((segmentWidth - 2.dp.toPx()) - filamentW) / 2f
            val filamentY = yStart + (channelHeight - filamentH) / 2f

            // Inner Filament Core
            drawRoundRect(
                color = coreColor.copy(alpha = 0.85f),
                topLeft = Offset(filamentX, filamentY),
                size = Size(filamentW.coerceAtLeast(1f), filamentH.coerceAtLeast(1f)),
                cornerRadius = CornerRadius((filamentH * 0.5f).coerceAtLeast(0.5f), (filamentH * 0.5f).coerceAtLeast(0.5f))
            )

            // Specular Glass Shine
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(x + 2.dp.toPx(), yStart + 0.5.dp.toPx()),
                size = Size((segmentWidth - 6.dp.toPx()).coerceAtLeast(1f), (channelHeight * 0.25f).coerceAtLeast(1f)),
                cornerRadius = CornerRadius(0.5.dp.toPx(), 0.5.dp.toPx())
            )
        }
    }
}

private fun formatRemainingAndTotal(posMs: Long, durMs: Long): String {
    val remMs = (durMs - posMs).coerceAtLeast(0)
    return "-${formatMs(remMs)} / ${formatMs(durMs)}"
}

@Composable
fun MiniPlayer(vm: VM, onClick: () -> Unit) {
    if (vm.nowTitle.isEmpty()) return
    val currentGuid = vm.currentMediaId()
    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
    val isFav = currentGuid != null && favorites.any { it.guid == currentGuid }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val pos = vm.positionMs
    val dur = vm.durationMs
    val progressRatio = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = Color(0xFF14100C),
        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Top Row: Podcast Artwork (far left) + Title/Show + Time
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF262019),
                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                    modifier = Modifier.size(52.dp)
                ) {
                    if (!vm.nowArtwork.isNullOrEmpty()) {
                        AsyncImage(
                            model = vm.nowArtwork,
                            contentDescription = "Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vm.nowTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEBDBB2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vm.nowShow,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBDAE93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatRemainingAndTotal(pos, dur),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFE8019)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Stereo Level Meter (below title/info, above scrub bar & controls)
            StereoLevelMeter(leftLevel = vm.rmsLeft, rightLevel = vm.rmsRight, isPlaying = vm.isPlaying)

            Spacer(Modifier.height(4.dp))

            // Thinner Scrub Bar on Mini Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color(0xFF322A20), RoundedCornerShape(1.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressRatio)
                        .fillMaxHeight()
                        .background(Color(0xFFFE8019), RoundedCornerShape(1.dp))
                )
            }

            Spacer(Modifier.height(6.dp))

            // Bottom Row: 100% Larger Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showSpeedDialog = true }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "Speed & Chapters",
                        tint = if (vm.isProfileModified) Color(0xFFCC241D) else Color(0xFFFE8019),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = { vm.toggleFavoriteCurrent() }, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) Color(0xFFCC241D) else Color(0xFFFE8019),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = { vm.skipBack() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Replay30, contentDescription = "Back 30s", tint = Color(0xFFFE8019), modifier = Modifier.size(28.dp))
                }

                Surface(
                    onClick = { vm.togglePlay() },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFE8019),
                    border = BorderStroke(1.dp, Color(0xFFD65D0E)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color(0xFF14100C),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                IconButton(onClick = { vm.skipForward() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Forward30, contentDescription = "Forward 30s", tint = Color(0xFFFE8019), modifier = Modifier.size(28.dp))
                }

                IconButton(onClick = { vm.skip() }, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color(0xFFFE8019), modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (showSpeedDialog) {
        SpeedAndChaptersDialog(vm = vm, onDismiss = { showSpeedDialog = false })
    }
}

@Composable
fun FullscreenPlayer(vm: VM, onDismiss: () -> Unit) {
    val currentGuid = vm.currentMediaId()
    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
    val isFav = currentGuid != null && favorites.any { it.guid == currentGuid }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val pos = vm.positionMs
    val dur = vm.durationMs

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = { showSpeedDialog = true }) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "Speed & Chapters",
                        tint = if (vm.isProfileModified) Color(0xFFCC241D) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .aspectRatio(1f)
                ) {
                    if (!vm.nowArtwork.isNullOrEmpty()) {
                        AsyncImage(
                            model = vm.nowArtwork,
                            contentDescription = "Artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vm.nowTitle,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = vm.nowShow,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                FavoriteButton(
                    isFavorite = isFav,
                    onToggle = { vm.toggleFavoriteCurrent() },
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Stereo Level Meter
            StereoLevelMeter(leftLevel = vm.rmsLeft, rightLevel = vm.rmsRight, isPlaying = vm.isPlaying)

            Spacer(Modifier.height(12.dp))

            // Scrub Bar & Remaining/Total Time
            var sliderPos by remember { mutableFloatStateOf(-1f) }
            val currentSliderVal = if (sliderPos >= 0) sliderPos else if (dur > 0) pos.toFloat() / dur.toFloat() else 0f

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentSliderVal.coerceIn(0f, 1f),
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = {
                        if (dur > 0) {
                            vm.seekTo((sliderPos * dur).toLong())
                        }
                        sliderPos = -1f
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(if (sliderPos >= 0 && dur > 0) (sliderPos * dur).toLong() else pos), style = MaterialTheme.typography.bodySmall)
                    Text(formatRemainingAndTotal(pos, dur), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.skipBack() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Replay30, contentDescription = "Skip back 30 seconds", modifier = Modifier.size(32.dp))
                }
                FilledIconButton(
                    onClick = { vm.togglePlay() },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = { vm.skipForward() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Forward30, contentDescription = "Skip forward 30 seconds", modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { vm.skip() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next episode", modifier = Modifier.size(32.dp))
                }
            }
        }
    }

    if (showSpeedDialog) {
        SpeedAndChaptersDialog(vm = vm, onDismiss = { showSpeedDialog = false })
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    val hr = min / 60
    val remMin = min % 60
    return if (hr > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hr, remMin, sec)
    } else {
        String.format(Locale.US, "%02d:%02d", remMin, sec)
    }
}

@Composable
private fun gradientEdgeBorder(
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    bgColor: Color = MaterialTheme.colorScheme.background,
    width: Dp = 4.dp
): BorderStroke {
    return BorderStroke(
        width = width,
        brush = Brush.linearGradient(
            colors = listOf(
                bgColor.copy(alpha = 0.2f),
                primaryColor,
                primaryColor.copy(alpha = 0.8f),
                bgColor.copy(alpha = 0.2f)
            )
        )
    )
}

@Composable
fun MockupMenuItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF322A20),
    badgeTextColor: Color = Color(0xFFBDAE93),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF262019),
        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accentColor,
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                    )
                }
                .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF14100C),
                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEBDBB2)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A7D66),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!badgeText.isNullOrEmpty()) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor,
                    border = BorderStroke(1.dp, Color(0xFF4A3F31))
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF8A7D66),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PlayOrderToggle(
    currentMode: Mode?,
    onSelectMode: (Mode) -> Unit,
    enabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CEREAL MODE",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A7D66),
            letterSpacing = androidx.compose.ui.unit.TextUnit(0.3f, androidx.compose.ui.unit.TextUnitType.Em),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF14100C),
            border = BorderStroke(2.dp, Color(0xFF4A3F31)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isOldest = currentMode == Mode.OLDEST
                val isLatest = currentMode == Mode.NEWEST

                Surface(
                    onClick = { if (enabled) onSelectMode(Mode.OLDEST) },
                    enabled = enabled,
                    color = if (isOldest) Color(0xFFFE8019) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Oldest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isOldest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOldest) Color(0xFF14100C) else Color(0xFFBDAE93)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF14100C)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(0xFF14100C),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31))
                    ) {
                        Text(
                            text = "VS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A7D66),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    onClick = { if (enabled) onSelectMode(Mode.NEWEST) },
                    enabled = enabled,
                    color = if (isLatest) Color(0xFFFE8019) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Latest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isLatest) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLatest) Color(0xFF14100C) else Color(0xFFBDAE93)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    vm: VM,
    back: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadedEpisodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var storageUsedBytes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val states = vm.repo.dao.allEpisodeStatesNow()
        val downloaded = states.filter { it.localPath != null && File(it.localPath).exists() }
        downloadedEpisodes = downloaded
        storageUsedBytes = downloaded.sumOf { File(it.localPath!!).length() }
    }

    val usedMb = storageUsedBytes / (1024 * 1024)
    val capMb = vm.repo.storageCapMb()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            onClick = {
                                scope.launch {
                                    vm.repo.deleteAllPlayedDownloads()
                                    val states = vm.repo.dao.allEpisodeStatesNow()
                                    val downloaded = states.filter { it.localPath != null && File(it.localPath).exists() }
                                    downloadedEpisodes = downloaded
                                    storageUsedBytes = downloaded.sumOf { File(it.localPath!!).length() }
                                    android.widget.Toast.makeText(context, "Deleted played downloads", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF322A20),
                            border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete Played", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                            }
                        }
                        ToolboxButton(onClick = onSettings)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = Color(0xFF262019),
                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Storage Usage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                        Text("$usedMb MB / $capMb MB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                    }
                    val ratio = if (capMb > 0) (usedMb.toFloat() / capMb.toFloat()).coerceIn(0f, 1f) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF322A20), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .background(Color(0xFFFE8019), RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            if (downloadedEpisodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No downloaded episodes stored.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFBDAE93)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(downloadedEpisodes, key = { it.guid }) { e ->
                        var podcast by remember { mutableStateOf<Podcast?>(null) }
                        LaunchedEffect(e.podcastUrl) {
                            podcast = vm.repo.dao.podcast(e.podcastUrl)
                        }

                        EpisodeBlock(
                            episode = e,
                            podcastTitle = podcast?.title,
                            podcastImage = podcast?.image,
                            onPlay = { vm.play(e) },
                            onToggleSlate = { vm.toggleSlate(e) },
                            onToggleFavorite = { vm.toggleFavorite(e) },
                            onToggleDownload = {
                                vm.toggleDownload(e)
                                scope.launch {
                                    val states = vm.repo.dao.allEpisodeStatesNow()
                                    val downloaded = states.filter { it.localPath != null && File(it.localPath).exists() }
                                    downloadedEpisodes = downloaded
                                    storageUsedBytes = downloaded.sumOf { File(it.localPath!!).length() }
                                }
                            },
                            onTogglePlayed = { vm.setPlayed(e, !e.played) },
                            onOpenPodcast = { podcast?.let { onOpenPodcast(it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Home(
    vm: VM,
    onSubscriptions: () -> Unit,
    onSlate: () -> Unit,
    onReruns: () -> Unit,
    onLatestEpisodes: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit
) {
    val subs by remember { vm.repo.dao.subscriptions() }.collectAsState(emptyList())
    val favs by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
    val slateList by remember { vm.repo.dao.slateEpisodes() }.collectAsState(emptyList())

    LaunchedEffect(Unit) { vm.repo.refreshAll() }

    val currentMode = vm.repo.mode()
    val favCount = favs.size
    val slateCount = slateList.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // --- BRAND HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Rab",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFE8019)
                    )
                    Text(
                        "bitEar",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEBDBB2)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "CEREAL MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF8A7D66),
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.28f, androidx.compose.ui.unit.TextUnitType.Em)
                )
            }

            ToolboxButton(onClick = onSettings)
        }

        // --- PLAY ORDER TOGGLE ---
        PlayOrderToggle(
            currentMode = currentMode,
            onSelectMode = { vm.startSchedule(it) },
            enabled = subs.isNotEmpty()
        )

        // --- WHAT'S COMING UP PREVIEW ---
        if (currentMode != null && subs.isNotEmpty()) {
            var comingUp by remember(currentMode) { mutableStateOf<List<Episode>>(emptyList()) }
            LaunchedEffect(currentMode) {
                comingUp = vm.repo.peekNextScheduledRotation(currentMode, 3)
            }

            if (comingUp.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "COMING UP IN CEREAL MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A7D66),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(0.3f, androidx.compose.ui.unit.TextUnitType.Em),
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    comingUp.forEach { ep ->
                        Surface(
                            onClick = { vm.play(ep) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF262019),
                            border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = ep.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFEBDBB2),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- LIBRARY SECTION ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "LIBRARY",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A7D66),
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.3f, androidx.compose.ui.unit.TextUnitType.Em),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            // 1. Latest Episodes (Top)
            MockupMenuItem(
                title = "Latest Episodes",
                subtitle = "Newest drop from every feed",
                icon = Icons.Default.NewReleases,
                accentColor = Color(0xFF98971A),
                badgeText = "NEW",
                badgeColor = Color(0xFF98971A),
                badgeTextColor = Color(0xFF14100C),
                onClick = onLatestEpisodes
            )

            // 2. Subscriptions
            MockupMenuItem(
                title = "Subscriptions",
                subtitle = if (subs.isEmpty()) "No feeds subscribed" else "${subs.size} feeds · updated today",
                icon = Icons.Default.RssFeed,
                accentColor = Color(0xFF79740E),
                badgeText = if (subs.isNotEmpty()) "${subs.size}" else null,
                badgeColor = Color(0xFFFE8019),
                badgeTextColor = Color(0xFF14100C),
                onClick = onSubscriptions
            )

            // 3. Reruns
            MockupMenuItem(
                title = "Reruns",
                subtitle = if (favCount == 0) "No reruns saved" else "In progress · $favCount saved",
                icon = Icons.Default.Favorite,
                accentColor = Color(0xFFCC241D),
                badgeText = if (favCount > 0) "$favCount" else null,
                badgeColor = Color(0xFF322A20),
                badgeTextColor = Color(0xFFBDAE93),
                onClick = onReruns
            )

            // 4. Slate
            MockupMenuItem(
                title = "Slate",
                subtitle = if (slateCount == 0) "Queue empty" else "Tonight's queue · $slateCount program${if (slateCount == 1) "" else "s"}",
                icon = Icons.AutoMirrored.Filled.PlaylistAddCheck,
                accentColor = Color(0xFF689D6A),
                badgeText = if (slateCount > 0) "$slateCount" else null,
                badgeColor = Color(0xFF322A20),
                badgeTextColor = Color(0xFFBDAE93),
                onClick = onSlate
            )

            // 5. Random Episode Button
            val scope = rememberCoroutineScope()
            val bgColor = MaterialTheme.colorScheme.background

            Button(
                onClick = {
                    scope.launch {
                        val subList = vm.repo.dao.subscriptionsNow()
                        if (subList.isNotEmpty()) {
                            val p = subList.random()
                            val eps = vm.repo.dao.episodes(p.feedUrl).firstOrNull() ?: emptyList()
                            if (eps.isNotEmpty()) {
                                val randomEp = eps.random()
                                vm.play(randomEp)
                            }
                        }
                    }
                },
                enabled = subs.isNotEmpty(),
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD79921),
                    contentColor = Color(0xFF14100C)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Random", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getFirst15Words(desc: String?): String {
    if (desc.isNullOrBlank()) return ""
    val plain = desc.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
    val words = plain.split(" ")
    return if (words.size <= 15) {
        plain
    } else {
        words.take(15).joinToString(" ") + "…"
    }
}

@Composable
fun EpisodeBlock(
    episode: Episode,
    podcastTitle: String?,
    podcastImage: String? = null,
    onPlay: () -> Unit,
    onToggleSlate: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    onTogglePlayed: () -> Unit,
    onOpenPodcast: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val greenColor = Color(0xFF98971A) // thin green bar for partial/played progress
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    // Played vs Partial progress calculation
    val progressRatio = if (episode.durationMs > 0 && episode.positionMs > 0) {
        (episode.positionMs.toFloat() / episode.durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isPartial = !episode.played && progressRatio > 0f

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF262019),
        border = BorderStroke(1.dp, Color(0xFF4A3F31)), // Faint minimal outline
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top: Reserved for Podcast Title & Episode Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (podcastImage != null || episode.image != null) {
                        AsyncImage(
                            model = episode.image ?: podcastImage,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .then(if (onOpenPodcast != null) Modifier.clickable { onOpenPodcast() } else Modifier),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (!podcastTitle.isNullOrBlank()) {
                            Text(
                                text = podcastTitle,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (onOpenPodcast != null) Modifier.clickable { onOpenPodcast() } else Modifier
                            )
                        }
                        Text(
                            text = episode.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (episode.played) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Middle: Reserved 2 lines for Episode Description
                val cleanDesc = remember(episode.description) { getFirst15Words(episode.description) }
                Text(
                    text = if (cleanDesc.isNotEmpty()) cleanDesc else "Published ${df.format(Date(episode.pubDate))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Bottom: Sequential Buttons [Add to Slate] [Heart] [Delete/Download] [Mark Listened] [Play]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Add to Slate
                        IconButton(onClick = onToggleSlate, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (episode.inSlate) Icons.AutoMirrored.Filled.PlaylistAddCheck else Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = if (episode.inSlate) "In Slate" else "Add to Slate",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2. Heart (Reruns / Favorites)
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (episode.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (episode.favorite) "Favorite" else "Add to Favorites",
                                tint = if (episode.favorite) Color(0xFFCC241D) else primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3. Delete / Download
                        IconButton(onClick = onToggleDownload, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (episode.localPath != null) Icons.Default.Delete else Icons.Default.Download,
                                contentDescription = if (episode.localPath != null) "Delete Download" else "Download",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 4. Mark Listened / Played
                        IconButton(onClick = onTogglePlayed, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (episode.played) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                contentDescription = if (episode.played) "Mark Unplayed" else "Mark Listened",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 5. Play
                    IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Episode",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Thin green bar representing percentage of total play time stretching the length of the episode block
            if (isPartial) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0xFF322A20))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .fillMaxHeight()
                            .background(greenColor)
                    )
                }
            } else if (episode.played) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(greenColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestEpisodesScreen(
    vm: VM,
    back: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSettings: () -> Unit
) {
    var items by remember { mutableStateOf<List<Pair<Podcast, Episode>>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        items = vm.repo.getLatestEpisodesForSubscriptions()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Latest Episodes") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        ToolboxButton(onClick = onSettings)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No episodes found for your subscriptions.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(items, key = { it.second.guid }) { (podcast, episode) ->
                    EpisodeBlock(
                        episode = episode,
                        podcastTitle = podcast.title,
                        podcastImage = podcast.image,
                        onPlay = { vm.play(episode) },
                        onToggleSlate = { vm.toggleSlate(episode) },
                        onToggleFavorite = { vm.toggleFavorite(episode) },
                        onToggleDownload = { vm.toggleDownload(episode) },
                        onTogglePlayed = { vm.setPlayed(episode, !episode.played) },
                        onOpenPodcast = { onOpenPodcast(podcast) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlateScreen(
    vm: VM,
    back: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSettings: () -> Unit
) {
    val slateEpisodes by remember { vm.repo.dao.slateEpisodes() }.collectAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slate Queue") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (slateEpisodes.any { it.played }) {
                            Surface(
                                onClick = { vm.clearPlayedSlate() },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF322A20),
                                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear Played", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                }
                            }
                        }
                        ToolboxButton(onClick = onSettings)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (slateEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Your Slate queue is empty. Add episodes from any show to build your queue.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                itemsIndexed(slateEpisodes, key = { _, e -> e.guid }) { index, e ->
                    var podcast by remember { mutableStateOf<Podcast?>(null) }
                    LaunchedEffect(e.podcastUrl) {
                        podcast = vm.repo.dao.podcast(e.podcastUrl)
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        EpisodeBlock(
                            episode = e,
                            podcastTitle = podcast?.title,
                            podcastImage = podcast?.image,
                            onPlay = { vm.play(e) },
                            onToggleSlate = { vm.toggleSlate(e) },
                            onToggleFavorite = { vm.toggleFavorite(e) },
                            onToggleDownload = { vm.toggleDownload(e) },
                            onTogglePlayed = { vm.setPlayed(e, !e.played) },
                            onOpenPodcast = { podcast?.let { onOpenPodcast(it) } }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val currentGuids = slateEpisodes.map { it.guid }.toMutableList()
                                            val temp = currentGuids[index]
                                            currentGuids[index] = currentGuids[index - 1]
                                            currentGuids[index - 1] = temp
                                            vm.reorderSlate(currentGuids)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }

                                if (index < slateEpisodes.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val currentGuids = slateEpisodes.map { it.guid }.toMutableList()
                                            val temp = currentGuids[index]
                                            currentGuids[index] = currentGuids[index + 1]
                                            currentGuids[index + 1] = temp
                                            vm.reorderSlate(currentGuids)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { vm.playSlateNext(e.guid) },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Play Next", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFE8019))
                                }
                                TextButton(
                                    onClick = { vm.addSlateToEnd(e.guid) },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Add to End", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBDAE93))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RerunsScreen(
    vm: VM,
    back: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSettings: () -> Unit
) {
    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reruns") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        ToolboxButton(onClick = onSettings)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No reruns saved yet. Tap the heart button on any episode to save it here.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(favorites, key = { it.guid }) { e ->
                    var podcast by remember { mutableStateOf<Podcast?>(null) }
                    LaunchedEffect(e.podcastUrl) {
                        podcast = vm.repo.dao.podcast(e.podcastUrl)
                    }

                    EpisodeBlock(
                        episode = e,
                        podcastTitle = podcast?.title,
                        podcastImage = podcast?.image,
                        onPlay = { vm.play(e) },
                        onToggleSlate = { vm.toggleSlate(e) },
                        onToggleFavorite = { vm.toggleFavorite(e) },
                        onToggleDownload = { vm.toggleDownload(e) },
                        onTogglePlayed = { vm.setPlayed(e, !e.played) },
                        onOpenPodcast = { podcast?.let { onOpenPodcast(it) } }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    vm: VM,
    onSearch: () -> Unit,
    onOpen: (Podcast) -> Unit,
    onSettings: () -> Unit,
    back: () -> Unit
) {
    val subs by remember { vm.repo.dao.subscriptions() }.collectAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        ToolboxButton(onClick = onSettings)
                    }
                }
            )
        }
    ) { innerPadding ->
        if (subs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No subscriptions yet. Tap search above to find podcasts.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(120.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(subs, key = { it.feedUrl }) { p ->
                    Column(Modifier.clickable { onOpen(p) }) {
                        AsyncImage(
                            model = p.image,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = p.title,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(vm: VM, back: () -> Unit, open: (Podcast) -> Unit, onSettings: () -> Unit) {
    LaunchedEffect(Unit) {
        vm.loadTrending()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            OutlinedTextField(
                value = vm.searchQuery,
                onValueChange = { vm.performDeepSearch(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search shows, topics, episodes, RSS...") },
                trailingIcon = {
                    Row {
                        if (vm.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.performDeepSearch("") }) { Icon(Icons.Default.Clear, "Clear") }
                        }
                        IconButton(onClick = { vm.performDeepSearch(vm.searchQuery) }) { Icon(Icons.Default.Search, "Search") }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.performDeepSearch(vm.searchQuery) })
            )
            Spacer(Modifier.width(8.dp))
            ToolboxButton(onClick = onSettings)
        }

        if (vm.isSearchingState) LinearProgressIndicator(Modifier.fillMaxWidth())

        val results = vm.searchResultsState
        if (results != null) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. PODCAST RESULTS
                if (results.podcasts.isNotEmpty()) {
                    item {
                        Text(
                            "PODCASTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A7D66),
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.28f, androidx.compose.ui.unit.TextUnitType.Em),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(results.podcasts, key = { it.feedUrl }) { p ->
                        Surface(
                            onClick = { open(p) },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF262019),
                            border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = p.image,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = p.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEBDBB2),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = p.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFBDAE93),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF8A7D66),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. RELATED EPISODES
                if (results.episodes.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "RELATED EPISODES",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8A7D66),
                            letterSpacing = androidx.compose.ui.unit.TextUnit(0.28f, androidx.compose.ui.unit.TextUnitType.Em),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(results.episodes, key = { it.second.guid }) { (podcast, episode) ->
                        EpisodeBlock(
                            episode = episode,
                            podcastTitle = podcast.title,
                            podcastImage = podcast.image,
                            onPlay = { vm.play(episode) },
                            onToggleSlate = { vm.toggleSlate(episode) },
                            onToggleFavorite = { vm.toggleFavorite(episode) },
                            onToggleDownload = { vm.toggleDownload(episode) },
                            onTogglePlayed = { vm.setPlayed(episode, !episode.played) },
                            onOpenPodcast = { open(podcast) }
                        )
                    }
                }

                if (results.podcasts.isEmpty() && results.episodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No podcast shows or episodes matched '${vm.searchQuery}'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFBDAE93)
                            )
                        }
                    }
                }
            }
        } else if (vm.trendingPodcastsState.isNotEmpty()) {
            // TRENDING SHOWS
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        "TRENDING SHOWS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A7D66),
                        letterSpacing = androidx.compose.ui.unit.TextUnit(0.28f, androidx.compose.ui.unit.TextUnitType.Em),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(vm.trendingPodcastsState, key = { it.feedUrl }) { p ->
                    Surface(
                        onClick = { open(p) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = p.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEBDBB2),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = p.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFBDAE93),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF8A7D66),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(vm: VM, p: Podcast, back: () -> Unit, onSettings: () -> Unit) {
    val dao = vm.repo.dao
    val scope = rememberCoroutineScope()
    val eps by remember(p) { dao.episodes(p.feedUrl) }.collectAsState(emptyList())
    val pod by remember(p) { dao.podcastFlow(p.feedUrl) }.collectAsState(null)
    var podProfile by remember(p.feedUrl) { mutableStateOf<PodcastProfile?>(null) }

    LaunchedEffect(p) {
        vm.repo.open(p)
        podProfile = dao.profile(p.feedUrl)
    }

    val subscribed = pod?.subscribed == true

    LazyColumn {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                ToolboxButton(onClick = onSettings)
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AsyncImage(p.image, null, Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)))
                Column(Modifier.padding(start = 16.dp)) {
                    Text(p.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
                    Text(p.author, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    if (subscribed) OutlinedButton({ vm.sub(p, false) }) { Text("Subscribed") }
                    else Button({ vm.sub(p, true) }) { Text("Subscribe") }
                }
            }
        }

        // Custom Playback Profile for this Subscription
        if (subscribed) {
            item {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = Color(0xFF262019),
                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawRect(
                                    color = Color(0xFFFE8019), // orange
                                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                )
                            }
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val enabled = podProfile?.enabled == true
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Custom Playback Profile", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                Text("Show-specific speed & chapter skips", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = { on ->
                                    val cur = podProfile ?: PodcastProfile(p.feedUrl, speed = 1.0f, skipIntroSec = 0, skipOutroSec = 0, enabled = true)
                                    val updated = cur.copy(enabled = on)
                                    podProfile = updated
                                    scope.launch { vm.repo.saveProfile(updated) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF14100C),
                                    checkedTrackColor = Color(0xFFFE8019),
                                    uncheckedThumbColor = Color(0xFF8A7D66),
                                    uncheckedTrackColor = Color(0xFF322A20)
                                )
                            )
                        }

                        if (enabled) {
                            val cur = podProfile ?: PodcastProfile(p.feedUrl, speed = 1.0f, skipIntroSec = 0, skipOutroSec = 0, enabled = true)

                            // Speed
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Speed", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDAE93))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val newSpeed = ((cur.speed * 100 - 25).toInt() / 100f).coerceAtLeast(0.5f)
                                            val updated = cur.copy(speed = newSpeed)
                                            podProfile = updated
                                            scope.launch { vm.repo.saveProfile(updated) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                    Text("${cur.speed}x", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    IconButton(
                                        onClick = {
                                            val newSpeed = ((cur.speed * 100 + 25).toInt() / 100f).coerceAtMost(3.0f)
                                            val updated = cur.copy(speed = newSpeed)
                                            podProfile = updated
                                            scope.launch { vm.repo.saveProfile(updated) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Intro
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Skip Intro", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDAE93))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val newIntro = (cur.skipIntroSec - 15).coerceAtLeast(0)
                                            val updated = cur.copy(skipIntroSec = newIntro)
                                            podProfile = updated
                                            scope.launch { vm.repo.saveProfile(updated) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                    Text("${cur.skipIntroSec}s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    IconButton(
                                        onClick = {
                                            val newIntro = (cur.skipIntroSec + 15).coerceAtMost(120)
                                            val updated = cur.copy(skipIntroSec = newIntro)
                                            podProfile = updated
                                            scope.launch { vm.repo.saveProfile(updated) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Outro
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Skip Outro", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDAE93))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val newOutro = (cur.skipOutroSec - 15).coerceAtLeast(0)
                                            val updated = cur.copy(skipOutroSec = newOutro)
                                            podProfile = updated
                                            scope.launch { vm.repo.saveProfile(updated) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                    Text("${cur.skipOutroSec}s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    IconButton(
                                        onClick = {
                                            val newOutro = (cur.skipOutroSec + 15).coerceAtMost(120)
                                            val updated = cur.copy(skipOutroSec = newOutro)
                                            podProfile = updated
                                            scope.launch { vm.repo.saveProfile(updated) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        items(eps.distinctBy { it.guid }, key = { it.guid }) { e ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                EpisodeBlock(
                    episode = e,
                    podcastTitle = p.title,
                    podcastImage = p.image,
                    onPlay = { vm.play(e) },
                    onToggleSlate = { vm.toggleSlate(e) },
                    onToggleFavorite = { vm.toggleFavorite(e) },
                    onToggleDownload = { vm.toggleDownload(e) },
                    onTogglePlayed = { vm.setPlayed(e, !e.played) }
                )
            }
        }
    }
}

private fun formatRefreshInterval(hours: Int): String {
    return if (hours < 24) {
        if (hours == 1) "1 hour" else "$hours hours"
    } else {
        val days = hours / 24
        if (days == 1) "1 day" else "$days days"
    }
}

private fun formatSleepTimerMinutes(minutes: Int): String {
    return when {
        minutes <= 0 -> "Off"
        minutes == 60 -> "1 hour"
        else -> "$minutes min"
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = Intent().apply {
            action = android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
fun PermissionDetailCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = Color(0xFF262019),
        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = accentColor,
                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                    )
                }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF14100C),
                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                }
            }

            Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDAE93))

            Surface(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF322A20),
                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                modifier = Modifier
                    .align(Alignment.End)
                    .height(30.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Open Android Settings", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFFE8019))
                Text("RabbitEar User Guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HelpItem("Syndication Schedule", "Continuous rotation playback of subscribed podcasts. Choose 'Oldest' or 'Latest' play order.")
                HelpItem("Library Sections", "• Subscriptions: Manage RSS feeds\n• Slate: Custom persistent queue\n• Reruns: Saved favorite episodes\n• Latest Episodes: Newest drops from all feeds")
                HelpItem("Random Button", "Picks a random episode from a random subscription using pure RNG every press.")
                HelpItem("Episode Blocks & Progress", "• Thin Green Bar: Represents % position played\n• Full Green Bar: Completed/listened\n• Buttons: [Slate] [Heart/Reruns] [Download/Delete] [Mark Listened] [Play]")
                HelpItem("Toolbox & Settings Pop-up", "Persistent toolbox button on every screen opens the settings overlay without losing your place.")
                HelpItem("Playback Profiles & Speed", "Configure global or per-show playback speeds (0.5x–3.0x) and auto-skip intro/outro durations.")
                HelpItem("Sleep Timer", "Set a 1–60 min timer that automatically pauses playback when expired.")
                HelpItem("Vehicle Bluetooth Auto-Play", "Automatically starts playback when connected to a car Bluetooth system and pauses on disconnect or incoming calls.")
                HelpItem("Off Grid Mode", "Displays 'OFF GRID' when both WiFi and Mobile Data are disabled.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = Color(0xFFFE8019), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun HelpItem(title: String, description: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF262019),
        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDAE93))
        }
    }
}

@Composable
fun SettingsOverlay(
    vm: VM,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshHours by remember { mutableIntStateOf(vm.repo.refreshIntervalHours()) }
    val isTimerActive = vm.sleepTimerRemainingSec > 0
    var timerMinutes by remember {
        mutableIntStateOf(
            if (isTimerActive) {
                (vm.sleepTimerRemainingSec + 59) / 60
            } else {
                vm.repo.sleepTimerMinutes()
            }
        )
    }
    var showPermissionsDetail by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    var wifiOn by remember { mutableStateOf(vm.repo.wifiEnabled()) }
    var mobileDataOn by remember { mutableStateOf(vm.repo.mobileDataEnabled()) }
    val isOffGrid = !wifiOn && !mobileDataOn

    var dataPath by remember { mutableStateOf(vm.repo.dataDirectoryUri()) }

    // SAF Launchers for Directory Picker, Export, and Import
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try { context.contentResolver.takePersistableUriPermission(uri, flags) } catch (_: Exception) {}
            vm.repo.setDataDirectoryUri(uri.toString())
            dataPath = uri.toString()
            android.widget.Toast.makeText(context, "Data folder set to selected directory", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        vm.repo.exportSettings(out)
                    }
                    vm.repo.autoBackupAllFormats(context)
                    android.widget.Toast.makeText(context, "Settings exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val mime = context.contentResolver.getType(uri) ?: ""
                    val filename = uri.lastPathSegment?.lowercase() ?: ""
                    val (subsCount, stateCount) = if (filename.endsWith(".opml") || mime.contains("xml") || mime.contains("opml")) {
                        val count = context.contentResolver.openInputStream(uri)?.use { input ->
                            vm.repo.importOpml(input)
                        } ?: 0
                        count to 0
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            vm.repo.importSettings(input)
                        } ?: (0 to 0)
                    }
                    android.widget.Toast.makeText(
                        context,
                        "Imported $subsCount subscriptions and $stateCount episode states",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF14100C),
            border = BorderStroke(2.dp, Color(0xFF4A3F31)),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.85f)  // Outer 15% visible on sides
                .fillMaxHeight(0.85f) // Outer 15% visible on bottom
                .padding(top = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume click so tapping inside doesn't dismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (showPermissionsDetail) {
                    // Header for Permissions Detail View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { showPermissionsDetail = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFEBDBB2), modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    "Permissions",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEBDBB2)
                                )
                                Text(
                                    "APP DETAILS & SHORTCUTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8A7D66),
                                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.2f, androidx.compose.ui.unit.TextUnitType.Em)
                                )
                            }
                        }

                        ToolboxButton(onClick = onDismiss, isOpen = true)
                    }

                    HorizontalDivider(color = Color(0xFF4A3F31), thickness = 1.dp)

                    // 1. Notifications
                    PermissionDetailCard(
                        title = "Notifications",
                        subtitle = "POST_NOTIFICATIONS",
                        description = "Allows RabbitEar to post active playback controls in the notification bar and alert when new podcast drops arrive.",
                        icon = Icons.Default.Notifications,
                        accentColor = Color(0xFFFE8019),
                        onOpenSettings = { openAppNotificationSettings(context) }
                    )

                    // 2. Foreground Audio Service
                    PermissionDetailCard(
                        title = "Foreground Audio Service",
                        subtitle = "FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                        description = "Keeps podcast audio playing smoothly when RabbitEar is running in the background or when your device screen is off.",
                        icon = Icons.Default.PlayCircle,
                        accentColor = Color(0xFF79740E),
                        onOpenSettings = { openAppSettings(context) }
                    )

                    // 3. Network Access
                    PermissionDetailCard(
                        title = "Network Access",
                        subtitle = "INTERNET & ACCESS_NETWORK_STATE",
                        description = "Required to stream podcast episodes, download audio files for offline listening, and fetch RSS feed updates.",
                        icon = Icons.Default.Wifi,
                        accentColor = Color(0xFF689D6A),
                        onOpenSettings = { openAppSettings(context) }
                    )

                    // 4. Background Refresh & Schedule
                    PermissionDetailCard(
                        title = "Background Refresh Schedule",
                        subtitle = "RECEIVE_BOOT_COMPLETED & WORKMANAGER",
                        description = "Allows RabbitEar to automatically check for new podcast episodes in the background according to your configured refresh interval.",
                        icon = Icons.Default.Sync,
                        accentColor = Color(0xFFD79921),
                        onOpenSettings = { openAppSettings(context) }
                    )

                    // 5. Vehicle Bluetooth Auto-Play
                    PermissionDetailCard(
                        title = "Vehicle Bluetooth Auto-Play",
                        subtitle = "BLUETOOTH_CONNECT & ACTION_ACL_CONNECTED",
                        description = "Detects when RabbitEar connects to your vehicle's Bluetooth audio system to automatically resume podcast playback.",
                        icon = Icons.Default.DirectionsCar,
                        accentColor = Color(0xFFFE8019),
                        onOpenSettings = { openAppSettings(context) }
                    )
                } else {
                    // Header for Main Settings View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBDBB2)
                            )
                            Text(
                                "CONFIGURATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A7D66),
                                letterSpacing = androidx.compose.ui.unit.TextUnit(0.25f, androidx.compose.ui.unit.TextUnitType.Em)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { showHelpDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF322A20),
                                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = Color(0xFFFE8019), modifier = Modifier.size(16.dp))
                                    Text("Help", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                }
                            }

                            // Single crossed hammer & wrench icon
                            Icon(
                                Icons.Default.Construction,
                                contentDescription = "Crossed hammer and wrench",
                                tint = Color(0xFF8A7D66),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            ToolboxButton(onClick = onDismiss, isOpen = true)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF4A3F31), thickness = 1.dp)

                    // 1. Check for new episodes
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFF79740E),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Check for new episodes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBDBB2)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = {
                                        val next = if (refreshHours > 24) refreshHours - 24 else refreshHours - 1
                                        val newHours = next.coerceAtLeast(1)
                                        refreshHours = newHours
                                        vm.repo.setRefreshIntervalHours(newHours)
                                        vm.repo.updateRefreshSchedule(context, newHours)
                                    },
                                    enabled = refreshHours > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease interval", tint = Color(0xFFFE8019), modifier = Modifier.size(20.dp))
                                }

                                Text(
                                    text = formatRefreshInterval(refreshHours),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFE8019)
                                )

                                IconButton(
                                    onClick = {
                                        val next = if (refreshHours >= 24) refreshHours + 24 else refreshHours + 1
                                        val newHours = next.coerceAtMost(720)
                                        refreshHours = newHours
                                        vm.repo.setRefreshIntervalHours(newHours)
                                        vm.repo.updateRefreshSchedule(context, newHours)
                                    },
                                    enabled = refreshHours < 720,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase interval", tint = Color(0xFFFE8019), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // 2. Network Connection Toggles (WiFi & Mobile Data)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = if (isOffGrid) Color(0xFFCC241D) else Color(0xFF79740E),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // WiFi Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFFEBDBB2), modifier = Modifier.size(16.dp))
                                        Text("WiFi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    }
                                    Switch(
                                        checked = wifiOn,
                                        onCheckedChange = { on ->
                                            wifiOn = on
                                            vm.repo.setWifiEnabled(on)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF14100C),
                                            checkedTrackColor = Color(0xFFFE8019),
                                            uncheckedThumbColor = Color(0xFF8A7D66),
                                            uncheckedTrackColor = Color(0xFF322A20)
                                        )
                                    )
                                }

                                HorizontalDivider(color = Color(0xFF4A3F31), thickness = 1.dp)

                                // Mobile Data Toggle Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color(0xFFEBDBB2), modifier = Modifier.size(16.dp))
                                        Text("Mobile Data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    }
                                    Switch(
                                        checked = mobileDataOn,
                                        onCheckedChange = { on ->
                                            mobileDataOn = on
                                            vm.repo.setMobileDataEnabled(on)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF14100C),
                                            checkedTrackColor = Color(0xFFFE8019),
                                            uncheckedThumbColor = Color(0xFF8A7D66),
                                            uncheckedTrackColor = Color(0xFF322A20)
                                        )
                                    )
                                }
                            }

                            if (isOffGrid) {
                                Spacer(Modifier.width(12.dp))
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp)
                                ) {
                                    Text(
                                        text = "OFF\nGRID",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFCC241D),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Data Storage Folder (RabbitEarData)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFF689D6A),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Data Storage Folder (RabbitEarData)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                            Text(dataPath, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDAE93), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    onClick = { folderPickerLauncher.launch(null) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF322A20),
                                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("Select Folder", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    }
                                }
                                Surface(
                                    onClick = {
                                        val defaultPath = vm.repo.dataDirectoryPath()
                                        vm.repo.setDataDirectoryPath(defaultPath)
                                        dataPath = defaultPath
                                        android.widget.Toast.makeText(context, "Data folder set to $defaultPath", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF322A20),
                                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("Use Default", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    }
                                }
                            }
                        }
                    }

                    // 4. Audio Processing (Voice Boost & Level Normalization)
                    var voiceBoostOn by remember { mutableStateOf(vm.repo.voiceBoostEnabled()) }
                    var normalizeOn by remember { mutableStateOf(vm.repo.audioNormalizationEnabled()) }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFF79740E),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Voice Boost", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    Text("Enhance vocal frequency levels", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                                }
                                Switch(
                                    checked = voiceBoostOn,
                                    onCheckedChange = { on ->
                                        voiceBoostOn = on
                                        vm.repo.setVoiceBoostEnabled(on)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF14100C),
                                        checkedTrackColor = Color(0xFFFE8019),
                                        uncheckedThumbColor = Color(0xFF8A7D66),
                                        uncheckedTrackColor = Color(0xFF322A20)
                                    )
                                )
                            }

                            HorizontalDivider(color = Color(0xFF4A3F31), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Audio Level Normalization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    Text("Smooth out volume spikes across episodes", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8A7D66))
                                }
                                Switch(
                                    checked = normalizeOn,
                                    onCheckedChange = { on ->
                                        normalizeOn = on
                                        vm.repo.setAudioNormalizationEnabled(on)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF14100C),
                                        checkedTrackColor = Color(0xFFFE8019),
                                        uncheckedThumbColor = Color(0xFF8A7D66),
                                        uncheckedTrackColor = Color(0xFF322A20)
                                    )
                                )
                            }
                        }
                    }

                    // 5. Sleep Timer
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFF689D6A),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Sleep Timer",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEBDBB2)
                                )
                                Switch(
                                    checked = isTimerActive,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            val activeMin = if (timerMinutes <= 0) 20 else timerMinutes
                                            timerMinutes = activeMin
                                            vm.repo.setSleepTimerMinutes(activeMin)
                                            vm.startSleepTimer(activeMin, context)
                                        } else {
                                            vm.cancelSleepTimer(context)
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF14100C),
                                        checkedTrackColor = Color(0xFFFE8019),
                                        uncheckedThumbColor = Color(0xFF8A7D66),
                                        uncheckedTrackColor = Color(0xFF322A20)
                                    )
                                )
                            }

                            if (isTimerActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = {
                                            val newMin = (timerMinutes - 5).coerceAtLeast(1)
                                            timerMinutes = newMin
                                            vm.repo.setSleepTimerMinutes(newMin)
                                            vm.startSleepTimer(newMin, context)
                                        },
                                        enabled = timerMinutes > 1,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease sleep timer", tint = Color(0xFFFE8019), modifier = Modifier.size(20.dp))
                                    }

                                    Text(
                                        text = formatSleepTimerMinutes(timerMinutes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFE8019)
                                    )

                                    IconButton(
                                        onClick = {
                                            val newMin = (timerMinutes + 5).coerceAtMost(60)
                                            timerMinutes = newMin
                                            vm.repo.setSleepTimerMinutes(newMin)
                                            vm.startSleepTimer(newMin, context)
                                        },
                                        enabled = timerMinutes < 60,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase sleep timer", tint = Color(0xFFFE8019), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 5. Global Playback Profile (Positioned between Sleep Timer and Backup & Restore)
                    var globalSpeed by remember { mutableFloatStateOf(1.0f) }
                    var globalIntro by remember { mutableIntStateOf(0) }
                    var globalOutro by remember { mutableIntStateOf(0) }

                    LaunchedEffect(Unit) {
                        val p = vm.repo.getProfileForPodcast("GLOBAL")
                        globalSpeed = p.speed
                        globalIntro = p.skipIntroSec
                        globalOutro = p.skipOutroSec
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFFD79921), // yellow accent
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Global Playback Profile",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBDBB2)
                            )

                            // Speed Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Playback Speed", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDAE93))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val newSpeed = ((globalSpeed * 100 - 25).toInt() / 100f).coerceAtLeast(0.5f)
                                            globalSpeed = newSpeed
                                            scope.launch { vm.repo.saveProfile(PodcastProfile("GLOBAL", newSpeed, globalIntro, globalOutro, true)) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                    Text("${globalSpeed}x", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    IconButton(
                                        onClick = {
                                            val newSpeed = ((globalSpeed * 100 + 25).toInt() / 100f).coerceAtMost(3.0f)
                                            globalSpeed = newSpeed
                                            scope.launch { vm.repo.saveProfile(PodcastProfile("GLOBAL", newSpeed, globalIntro, globalOutro, true)) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF4A3F31), thickness = 1.dp)

                            // Skip Intro Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Skip Intro", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDAE93))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val newIntro = (globalIntro - 15).coerceAtLeast(0)
                                            globalIntro = newIntro
                                            scope.launch { vm.repo.saveProfile(PodcastProfile("GLOBAL", globalSpeed, newIntro, globalOutro, true)) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                    Text("${globalIntro}s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    IconButton(
                                        onClick = {
                                            val newIntro = (globalIntro + 15).coerceAtMost(120)
                                            globalIntro = newIntro
                                            scope.launch { vm.repo.saveProfile(PodcastProfile("GLOBAL", globalSpeed, newIntro, globalOutro, true)) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF4A3F31), thickness = 1.dp)

                            // Skip Outro Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Skip Outro", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBDAE93))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val newOutro = (globalOutro - 15).coerceAtLeast(0)
                                            globalOutro = newOutro
                                            scope.launch { vm.repo.saveProfile(PodcastProfile("GLOBAL", globalSpeed, globalIntro, newOutro, true)) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                    Text("${globalOutro}s", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFE8019))
                                    IconButton(
                                        onClick = {
                                            val newOutro = (globalOutro + 15).coerceAtMost(120)
                                            globalOutro = newOutro
                                            scope.launch { vm.repo.saveProfile(PodcastProfile("GLOBAL", globalSpeed, globalIntro, newOutro, true)) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color(0xFFFE8019), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 6. Backup & Restore
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFFD79921),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Backup & Restore (JSON / OPML / HTML / DB)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBDBB2)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = { exportLauncher.launch("rabbitear_backup.json") },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF322A20),
                                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = Color(0xFFEBDBB2), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Export All", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    }
                                }

                                Surface(
                                    onClick = { importLauncher.launch(arrayOf("application/json", "text/xml", "text/x-opml", "*/*")) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF322A20),
                                    border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color(0xFFEBDBB2), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Import", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                    }
                                }
                            }
                        }
                    }

                    // 4. Permissions (Positioned directly above Support Development)
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFF98971A), // green-lit
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Permissions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBDBB2)
                            )
                            Surface(
                                onClick = { showPermissionsDetail = true },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF322A20),
                                border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFEBDBB2), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Manage Permissions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEBDBB2))
                                }
                            }
                        }
                    }

                    // 5. Support Development
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color(0xFF262019),
                        border = BorderStroke(1.dp, Color(0xFF4A3F31)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRect(
                                        color = Color(0xFFFE8019),
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                    )
                                }
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Support Development",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEBDBB2)
                            )
                            Surface(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            "https://ko-fi.com/pueralbe".toUri()
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFE8019),
                                border = BorderStroke(1.dp, Color(0xFFD65D0E)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF14100C), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Donate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF14100C))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}
