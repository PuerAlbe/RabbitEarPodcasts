package com.example.rabbitear.ui

import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rabbitear.data.Mode
import com.example.rabbitear.data.Podcast
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val GruvboxDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFE8019),          // Gruvbox Bright Orange
    onPrimary = Color(0xFF1D2021),
    primaryContainer = Color(0xFFD65D0E),  // Gruvbox Dark Orange
    onPrimaryContainer = Color(0xFFFBF1C7),
    secondary = Color(0xFFB8BB26),        // Gruvbox Bright Green
    onSecondary = Color(0xFF1D2021),
    secondaryContainer = Color(0xFF98971A),
    onSecondaryContainer = Color(0xFFFBF1C7),
    tertiary = Color(0xFF8EC07C),         // Gruvbox Aqua
    onTertiary = Color(0xFF1D2021),
    background = Color(0xFF282828),       // Gruvbox Dark0
    onBackground = Color(0xFFEBDBB2),     // Gruvbox Light1
    surface = Color(0xFF3C3836),          // Gruvbox Dark1
    onSurface = Color(0xFFEBDBB2),
    surfaceVariant = Color(0xFF504945),   // Gruvbox Dark2
    onSurfaceVariant = Color(0xFFA89984), // Gruvbox Gray
    outline = Color(0xFF928374)
)

private val GruvboxLightColorScheme = lightColorScheme(
    primary = Color(0xFFAF3A03),          // Gruvbox Light Mode Dark Orange
    onPrimary = Color(0xFFFBF1C7),
    primaryContainer = Color(0xFFFE8019),
    onPrimaryContainer = Color(0xFF282828),
    secondary = Color(0xFF79740E),        // Gruvbox Light Mode Green
    onSecondary = Color(0xFFFBF1C7),
    secondaryContainer = Color(0xFFB8BB26),
    onSecondaryContainer = Color(0xFF282828),
    tertiary = Color(0xFF427B58),
    onTertiary = Color(0xFFFBF1C7),
    background = Color(0xFFFBF1C7),       // Gruvbox Light0 Cream
    onBackground = Color(0xFF3C3836),     // Gruvbox Dark1
    surface = Color(0xFFEBDBB2),          // Gruvbox Light1
    onSurface = Color(0xFF282828),
    surfaceVariant = Color(0xFFD5C4A1),   // Gruvbox Light2
    onSurfaceVariant = Color(0xFF665C54),
    outline = Color(0xFF928374)
)

@Composable
fun RabbitEarTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) GruvboxDarkColorScheme else GruvboxLightColorScheme
    MaterialTheme(colorScheme = scheme, content = content)
}

sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data class Detail(val p: Podcast) : Screen
    data object Settings : Screen
}

@Composable
fun Root(vm: VM) {
    val context = LocalContext.current
    val navStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = navStack.lastOrNull() ?: Screen.Home
    var showFullscreenPlayer by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

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
                    onSearch = { navStack.add(Screen.Search) },
                    onOpen = { navStack.add(Screen.Detail(it)) },
                    onSettings = { navStack.add(Screen.Settings) },
                    onSleepTimer = { showSleepTimerDialog = true }
                )
                Screen.Search -> SearchScreen(
                    vm,
                    back = { navStack.removeAt(navStack.lastIndex) },
                    open = { navStack.add(Screen.Detail(it)) }
                )
                is Screen.Detail -> DetailScreen(
                    vm,
                    s.p,
                    back = { navStack.removeAt(navStack.lastIndex) }
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
            onDismiss = { showFullscreenPlayer = false },
            onSleepTimer = { showSleepTimerDialog = true }
        )
    }

    if (showSleepTimerDialog) {
        var minutesInput by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        val isTimerActive = vm.sleepTimerRemainingSec > 0

        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("60 minutes or less", style = MaterialTheme.typography.bodyMedium)
                    if (isTimerActive) {
                        val min = vm.sleepTimerRemainingSec / 60
                        val sec = vm.sleepTimerRemainingSec % 60
                        Text(
                            text = String.format(Locale.US, "Active timer: %02d:%02d remaining", min, sec),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            minutesInput = digits
                            val num = digits.toIntOrNull()
                            errorMsg = when {
                                digits.isEmpty() -> null
                                num == null || num < 1 -> "Enter at least 1 minute"
                                num > 60 -> "60 minutes or less"
                                else -> null
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        isError = errorMsg != null,
                        supportingText = errorMsg?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = minutesInput.toIntOrNull()
                        if (num != null && num in 1..60) {
                            vm.startSleepTimer(num, context)
                            showSleepTimerDialog = false
                        }
                    },
                    enabled = minutesInput.toIntOrNull()?.let { it in 1..60 } == true
                ) {
                    Text("Set Timer")
                }
            },
            dismissButton = {
                Row {
                    if (isTimerActive) {
                        TextButton(onClick = {
                            vm.cancelSleepTimer(context)
                            showSleepTimerDialog = false
                        }) {
                            Text("Turn Off")
                        }
                    }
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun MiniPlayer(vm: VM, onClick: () -> Unit) {
    if (vm.nowTitle.isEmpty()) return
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
            IconButton({ vm.skipBack() }) { Icon(Icons.Default.Replay30, "Skip back 30 seconds") }
            IconButton({ vm.togglePlay() }) { Icon(if (vm.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/pause") }
            IconButton({ vm.skipForward() }) { Icon(Icons.Default.Forward30, "Skip forward 30 seconds") }
            IconButton({ vm.skip() }) { Icon(Icons.Default.SkipNext, "Next episode") }
        }
    }
}

@Composable
fun FullscreenPlayer(vm: VM, onDismiss: () -> Unit, onSleepTimer: () -> Unit) {
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
                IconButton(onClick = onSleepTimer) {
                    if (vm.sleepTimerRemainingSec > 0) {
                        val label = if (vm.sleepTimerRemainingSec >= 60) "${(vm.sleepTimerRemainingSec + 59) / 60}m" else "${vm.sleepTimerRemainingSec}s"
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Icon(Icons.Default.Timer, "Sleep Timer")
                    }
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

            Text(
                text = vm.nowTitle,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = vm.nowShow,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

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
fun Home(
    vm: VM,
    onSearch: () -> Unit,
    onOpen: (Podcast) -> Unit,
    onSettings: () -> Unit,
    onSleepTimer: () -> Unit
) {
    val subs by remember { vm.repo.dao.subscriptions() }.collectAsState(emptyList())
    LaunchedEffect(Unit) { vm.repo.refreshAll() }
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    LazyVerticalGrid(
        GridCells.Adaptive(120.dp), contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = full) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RabbitEar", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                IconButton(onSearch) { Icon(Icons.Default.Search, "Search") }
                IconButton(onClick = onSleepTimer) {
                    if (vm.sleepTimerRemainingSec > 0) {
                        val label = if (vm.sleepTimerRemainingSec >= 60) "${(vm.sleepTimerRemainingSec + 59) / 60}m" else "${vm.sleepTimerRemainingSec}s"
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Icon(Icons.Default.Timer, "Sleep Timer")
                    }
                }
                IconButton(onSettings) { Icon(Icons.Default.Settings, "Settings") }
            }
        }
        item(span = full) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Syndication Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { vm.startSchedule(Mode.OLDEST) },
                        enabled = subs.isNotEmpty(),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD65D0E),
                            contentColor = Color(0xFFFBF1C7)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Oldest", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { vm.startSchedule(Mode.NEWEST) },
                        enabled = subs.isNotEmpty(),
                        shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB8BB26),
                            contentColor = Color(0xFF1D2021)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Latest", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (subs.isEmpty()) item(span = full) { Text("Search for a podcast and subscribe to get started.") }
        items(subs, key = { it.feedUrl }) { p ->
            Column(Modifier.clickable { onOpen(p) }) {
                AsyncImage(p.image, null, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Text(p.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun SearchScreen(vm: VM, back: () -> Unit, open: (Podcast) -> Unit) {
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
fun DetailScreen(vm: VM, p: Podcast, back: () -> Unit) {
    val dao = vm.repo.dao
    val eps by remember(p) { dao.episodes(p.feedUrl) }.collectAsState(emptyList())
    val pod by remember(p) { dao.podcastFlow(p.feedUrl) }.collectAsState(null)
    LaunchedEffect(p) { vm.repo.open(p) }
    val subscribed = pod?.subscribed == true
    val df = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    LazyColumn {
        item { IconButton(back, Modifier.padding(8.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
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
                    Row {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: VM, back: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Settings Backup & Restore", style = MaterialTheme.typography.titleMedium)

            Button(
                onClick = {
                    vm.exportSettingsDirect(context) { success ->
                        val msg = if (success) "rabbitear_settings.json was saved to Documents" else "Failed to export settings"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Export settings")
            }

            OutlinedButton(
                onClick = {
                    vm.importSettingsDirect(context) { success ->
                        val msg = if (success) "Settings imported successfully" else "Failed to import settings"
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Import settings")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

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
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Donate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
