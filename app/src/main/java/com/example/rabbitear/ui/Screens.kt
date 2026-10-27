package com.example.rabbitear.ui

import androidx.core.net.toUri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.example.rabbitear.data.Mode
import com.example.rabbitear.data.Podcast
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
    data object Search : Screen
    data class Detail(val p: Podcast) : Screen
    data object Settings : Screen
}

@Composable
fun Root(vm: VM) {
    val navStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = navStack.lastOrNull() ?: Screen.Home
    var showFullscreenPlayer by remember { mutableStateOf(false) }

    var lastTitle by remember { mutableStateOf("") }
    LaunchedEffect(vm.nowTitle) {
        if (vm.nowTitle.isNotEmpty() && lastTitle != vm.nowTitle) {
            showFullscreenPlayer = true
        }
        lastTitle = vm.nowTitle
    }

    BackHandler(navStack.size > 1) {
        navStack.removeAt(navStack.lastIndex)
    }

    Scaffold(bottomBar = { MiniPlayer(vm, onClick = { showFullscreenPlayer = true }) }) { pad ->
        Box(Modifier.padding(pad)) {
            when (val s = currentScreen) {
                Screen.Home -> Home(
                    vm,
                    onSubscriptions = { navStack.add(Screen.Subscriptions) },
                    onSlate = { navStack.add(Screen.Slate) },
                    onReruns = { navStack.add(Screen.Reruns) },
                    onLatestEpisodes = { navStack.add(Screen.LatestEpisodes) },
                    onSettings = { navStack.add(Screen.Settings) }
                )
                Screen.Subscriptions -> SubscriptionsScreen(
                    vm,
                    onSearch = { navStack.add(Screen.Search) },
                    onOpen = { navStack.add(Screen.Detail(it)) },
                    onSettings = { navStack.add(Screen.Settings) },
                    back = { navStack.removeAt(navStack.lastIndex) }
                )
                Screen.Slate -> SlateScreen(
                    vm,
                    back = { navStack.removeAt(navStack.lastIndex) },
                    onOpenPodcast = { navStack.add(Screen.Detail(it)) },
                    onSettings = { navStack.add(Screen.Settings) }
                )
                Screen.Reruns -> RerunsScreen(
                    vm,
                    back = { navStack.removeAt(navStack.lastIndex) },
                    onOpenPodcast = { navStack.add(Screen.Detail(it)) },
                    onSettings = { navStack.add(Screen.Settings) }
                )
                Screen.LatestEpisodes -> LatestEpisodesScreen(
                    vm,
                    back = { navStack.removeAt(navStack.lastIndex) },
                    onOpenPodcast = { navStack.add(Screen.Detail(it)) },
                    onSettings = { navStack.add(Screen.Settings) }
                )
                Screen.Search -> SearchScreen(
                    vm,
                    back = { navStack.removeAt(navStack.lastIndex) },
                    open = { navStack.add(Screen.Detail(it)) },
                    onSettings = { navStack.add(Screen.Settings) }
                )
                is Screen.Detail -> DetailScreen(
                    vm,
                    s.p,
                    back = { navStack.removeAt(navStack.lastIndex) },
                    onSettings = { navStack.add(Screen.Settings) }
                )
                Screen.Settings -> SettingsScreen(
                    vm,
                    back = { navStack.removeAt(navStack.lastIndex) }
                )
            }
        }
    }

    if (showFullscreenPlayer && vm.nowTitle.isNotEmpty()) {
        FullscreenPlayer(
            vm,
            onDismiss = { showFullscreenPlayer = false }
        )
    }
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

@Composable
fun MiniPlayer(vm: VM, onClick: () -> Unit) {
    if (vm.nowTitle.isEmpty()) return
    val currentGuid = vm.currentMediaId()
    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
    val isFav = currentGuid != null && favorites.any { it.guid == currentGuid }

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(vm.nowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(vm.nowShow, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            FavoriteButton(isFavorite = isFav, onToggle = { vm.toggleFavoriteCurrent() })
            IconButton({ vm.skipBack() }) { Icon(Icons.Default.Replay30, "Skip back 30 seconds") }
            IconButton({ vm.togglePlay() }) { Icon(if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/pause") }
            IconButton({ vm.skipForward() }) { Icon(Icons.Default.Forward30, "Skip forward 30 seconds") }
            IconButton({ vm.skip() }) { Icon(Icons.Default.SkipNext, "Next episode") }
        }
    }
}

@Composable
fun FullscreenPlayer(vm: VM, onDismiss: () -> Unit) {
    val currentGuid = vm.currentMediaId()
    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
    val isFav = currentGuid != null && favorites.any { it.guid == currentGuid }

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
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        modifier = Modifier.size(32.dp)
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

            Spacer(Modifier.height(24.dp))

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

            Spacer(Modifier.height(24.dp))

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
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(if (sliderPos >= 0 && dur > 0) (sliderPos * dur).toLong() else pos), style = MaterialTheme.typography.bodySmall)
                    Text(formatMs(dur), style = MaterialTheme.typography.bodySmall)
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
            text = "PLAY ORDER",
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

@Composable
fun Home(
    vm: VM,
    onSubscriptions: () -> Unit,
    onSlate: () -> Unit,
    onReruns: () -> Unit,
    onLatestEpisodes: () -> Unit,
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
                    "SYNDICATED PLAY",
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

            // Subscriptions
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

            // Slate
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

            // Reruns
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

            // Latest Episodes
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

            // Random Episode Button
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestEpisodesScreen(
    vm: VM,
    back: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSettings: () -> Unit
) {
    var items by remember { mutableStateOf<List<Pair<Podcast, com.example.rabbitear.data.Episode>>>(emptyList()) }
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
                    val favorites by remember { vm.repo.dao.favoriteEpisodes() }.collectAsState(emptyList())
                    val isFav = favorites.any { it.guid == episode.guid }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            AsyncImage(
                                model = episode.image ?: podcast.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onOpenPodcast(podcast) },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = podcast.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { onOpenPodcast(podcast) }
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = episode.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val shortDesc = getFirst15Words(episode.description)
                                if (shortDesc.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = shortDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            SlateButton(
                                inSlate = episode.inSlate,
                                onToggle = { vm.toggleSlate(episode) }
                            )
                            FavoriteButton(
                                isFavorite = isFav,
                                onToggle = { vm.toggleFavorite(episode) }
                            )
                            IconButton(
                                onClick = { vm.play(episode) },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play episode"
                                )
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
fun SlateScreen(
    vm: VM,
    back: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
    onSettings: () -> Unit
) {
    val slateEpisodes by remember { vm.repo.dao.slateEpisodes() }.collectAsState(emptyList())
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slate") },
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
                items(slateEpisodes, key = { it.guid }) { e ->
                    var podcast by remember { mutableStateOf<Podcast?>(null) }
                    LaunchedEffect(e.podcastUrl) {
                        podcast = vm.repo.dao.podcast(e.podcastUrl)
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = e.image ?: podcast?.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { podcast?.let { onOpenPodcast(it) } },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                podcast?.title?.let { podTitle ->
                                    Text(
                                        text = podTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { podcast?.let { onOpenPodcast(it) } }
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(
                                    text = e.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = df.format(Date(e.pubDate)) + if (e.localPath != null) " · Downloaded" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            SlateButton(
                                inSlate = true,
                                onToggle = { vm.toggleSlate(e) }
                            )
                            FavoriteButton(
                                isFavorite = e.favorite,
                                onToggle = { vm.toggleFavorite(e) }
                            )
                            IconButton(onClick = { vm.play(e) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play episode")
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
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }

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

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = e.image ?: podcast?.image,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { podcast?.let { onOpenPodcast(it) } },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                podcast?.title?.let { podTitle ->
                                    Text(
                                        text = podTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { podcast?.let { onOpenPodcast(it) } }
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Text(
                                    text = e.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = df.format(Date(e.pubDate)) + if (e.localPath != null) " · Downloaded" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            SlateButton(
                                inSlate = e.inSlate,
                                onToggle = { vm.toggleSlate(e) }
                            )
                            FavoriteButton(
                                isFavorite = true,
                                onToggle = { vm.toggleFavorite(e) }
                            )
                            IconButton(onClick = { vm.play(e) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play episode")
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
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<Podcast>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun doSearch() {
        if (q.isNotBlank()) {
            scope.launch { busy = true; res = vm.repo.search(q.trim()); busy = false }
        }
    }
    Column {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            OutlinedTextField(
                q, { q = it }, Modifier.weight(1f), singleLine = true,
                placeholder = { Text("Search, or paste an RSS URL") },
                trailingIcon = {
                    Row {
                        if (q.isNotEmpty()) {
                            IconButton(onClick = { q = "" }) { Icon(Icons.Default.Clear, "Clear") }
                        }
                        IconButton(onClick = ::doSearch) { Icon(Icons.Default.Search, "Search") }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { doSearch() })
            )
            Spacer(Modifier.width(8.dp))
            ToolboxButton(onClick = onSettings)
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn {
            items(res.distinctBy { it.feedUrl }, key = { it.feedUrl }) { p ->
                ListItem(
                    headlineContent = { Text(p.title, maxLines = 2) },
                    supportingContent = { Text(p.author, maxLines = 1) },
                    leadingContent = { AsyncImage(p.image, null, Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))) },
                    modifier = Modifier.clickable { open(p) }
                )
            }
        }
    }
}

@Composable
fun DetailScreen(vm: VM, p: Podcast, back: () -> Unit, onSettings: () -> Unit) {
    val dao = vm.repo.dao
    val eps by remember(p) { dao.episodes(p.feedUrl) }.collectAsState(emptyList())
    val pod by remember(p) { dao.podcastFlow(p.feedUrl) }.collectAsState(null)
    LaunchedEffect(p) { vm.repo.open(p) }
    val subscribed = pod?.subscribed == true
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
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
        items(eps.distinctBy { it.guid }, key = { it.guid }) { e ->
            ListItem(
                headlineContent = {
                    Text(e.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        color = if (e.played) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified)
                },
                supportingContent = { Text(df.format(Date(e.pubDate)) + if (e.localPath != null) " · Downloaded" else "") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlateButton(
                            inSlate = e.inSlate,
                            onToggle = { vm.toggleSlate(e) }
                        )
                        FavoriteButton(
                            isFavorite = e.favorite,
                            onToggle = { vm.toggleFavorite(e) }
                        )
                        IconButton({ vm.toggleDownload(e) }) { Icon(if (e.localPath == null) Icons.Default.Download else Icons.Default.Delete, "Download") }
                        IconButton({ vm.setPlayed(e, !e.played) }) {
                            Icon(Icons.Default.Check, "Mark played", tint = if (e.played) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                        }
                        IconButton({ vm.play(e) }) { Icon(Icons.Default.PlayArrow, "Play") }
                    }
                }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: VM, back: () -> Unit) {
    val context = LocalContext.current
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
    val bgColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.Handyman,
                            contentDescription = "Hammer",
                            tint = Color(0xFFBDAE93),
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            Icons.Default.Build,
                            contentDescription = "Wrench",
                            tint = Color(0xFFBDAE93),
                            modifier = Modifier.size(16.dp)
                        )
                        Icon(
                            Icons.Default.Construction,
                            contentDescription = "Screwdriver",
                            tint = Color(0xFFBDAE93),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        ToolboxButton(onClick = back, isOpen = true)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = gradientEdgeBorder(primaryColor = MaterialTheme.colorScheme.outline, bgColor = bgColor, width = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Check for new episodes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                            enabled = refreshHours > 1
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease interval")
                        }

                        Text(
                            text = formatRefreshInterval(refreshHours),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        IconButton(
                            onClick = {
                                val next = if (refreshHours >= 24) refreshHours + 24 else refreshHours + 1
                                val newHours = next.coerceAtMost(720)
                                refreshHours = newHours
                                vm.repo.setRefreshIntervalHours(newHours)
                                vm.repo.updateRefreshSchedule(context, newHours)
                            },
                            enabled = refreshHours < 720
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase interval")
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                border = gradientEdgeBorder(primaryColor = MaterialTheme.colorScheme.outline, bgColor = bgColor, width = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Sleep Timer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
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
                            }
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
                                enabled = timerMinutes > 1
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease sleep timer")
                            }

                            Text(
                                text = formatSleepTimerMinutes(timerMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = {
                                    val newMin = (timerMinutes + 5).coerceAtMost(60)
                                    timerMinutes = newMin
                                    vm.repo.setSleepTimerMinutes(newMin)
                                    vm.startSleepTimer(newMin, context)
                                },
                                enabled = timerMinutes < 60
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase sleep timer")
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Settings Backup & Restore", style = MaterialTheme.typography.titleMedium)

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Settings Backup & Restore", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = {
                    vm.exportSettingsDirect(context) { success ->
                        val msg = if (success) "rabbitear_settings.json was saved to Documents" else "Failed to export settings"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Export settings")
            }

            OutlinedButton(
                onClick = {
                    vm.importSettingsDirect(context) { success ->
                        val msg = if (success) "Settings imported successfully" else "Failed to import settings"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Import settings")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Support Development", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = {
                    try {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            "https://ko-fi.com/pueralbe".toUri()
                        )
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFE8019),
                    contentColor = Color(0xFF1D2021)
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                modifier = Modifier.fillMaxWidth().height(34.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Donate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
