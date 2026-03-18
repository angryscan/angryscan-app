package org.angryscan.app.ui.windows.screens.scans

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.windows.screens.scans.components.ScanFilterChipBox
import org.angryscan.app.ui.windows.screens.scans.components.ScanTaskCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ScansScreen(onTaskClick: (Int) -> Unit) {
    val scanService = koinInject<ScanService>()

    val filterTaskStates = remember { mutableListOf<TaskState>() }
    var active by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    val allTasks by scanService.tasks.tasks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val visibleTasks = allTasks.filter { it.state.value != TaskState.LOADING }
    val filteredTasks = visibleTasks
        .filter { task ->
            if (filterTaskStates.isEmpty()) true
            else task.state.value in filterTaskStates
        }
        .filter { task ->
            val q = searchQuery.trim()
            if (q.isEmpty()) true
            else {
                val queryVariants = rememberSearchQueryVariants(q)
                val haystack = rememberTaskSearchHaystack(task)
                queryVariants.any { variant -> haystack.contains(variant) }
            }
        }
        .sortedByDescending { it.finishedAt.value }
        .sortedByDescending { it.pausedAt.value }
        .sortedByDescending { it.startedAt.value }

    val countActive = visibleTasks.count { it.state.value == TaskState.SCANNING || it.state.value == TaskState.SEARCHING }
    val countPaused = visibleTasks.count { it.state.value == TaskState.STOPPED || it.state.value == TaskState.PENDING }
    val countError = visibleTasks.count { it.state.value == TaskState.FAILED }
    val countCompleted = visibleTasks.count { it.state.value == TaskState.COMPLETED }

    var currentTime by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(currentTime) {
        while (true) {
            currentTime = Clock.System.now()
            delay(1000)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(24.dp)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(containerShape)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.22f), containerShape)
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = containerShape
                )
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp),
                    placeholder = {
                        Text(
                            stringResource(Res.string.ScansPage_SearchPlaceholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(Res.string.ScansPage_SearchPlaceholder),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.6f),
                        unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.35f),
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        cursorColor = colorScheme.primary,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.ScansPage_FilterByStatus),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                ScanFilterChipBox(
                    active = active,
                    paused = paused,
                    error = error,
                    completed = completed,
                    totalCount = visibleTasks.size,
                    countActive = countActive,
                    countPaused = countPaused,
                    countError = countError,
                    countCompleted = countCompleted,
                    onAllClick = {
                        focusManager.clearFocus()
                        active = false
                        paused = false
                        error = false
                        completed = false
                        filterTaskStates.clear()
                    },
                    onActiveClick = {
                        focusManager.clearFocus()
                        active = !active
                        if (active) {
                            filterTaskStates.addAll(listOf(TaskState.SCANNING, TaskState.SEARCHING))
                        } else {
                            filterTaskStates.removeAll(listOf(TaskState.SCANNING, TaskState.SEARCHING))
                        }
                    },
                    onPausedClick = {
                        focusManager.clearFocus()
                        paused = !paused
                        if (paused) {
                            filterTaskStates.add(TaskState.STOPPED)
                            filterTaskStates.add(TaskState.PENDING)
                        } else {
                            filterTaskStates.remove(TaskState.STOPPED)
                            filterTaskStates.remove(TaskState.PENDING)
                        }
                    },
                    onErrorClick = {
                        focusManager.clearFocus()
                        error = !error
                        if (error) filterTaskStates.add(TaskState.FAILED)
                        else filterTaskStates.remove(TaskState.FAILED)
                    },
                    onCompletedClick = {
                        focusManager.clearFocus()
                        completed = !completed
                        if (completed) filterTaskStates.add(TaskState.COMPLETED)
                        else filterTaskStates.remove(TaskState.COMPLETED)
                    }
                )
            }

            if (allTasks.isNotEmpty() && visibleTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(Res.string.ScansPage_Loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredTasks.isEmpty()) {
                val isFiltered = filterTaskStates.isNotEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(Res.string.MainScreen_RecentScans_Empty),
                            modifier = Modifier.size(48.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (isFiltered)
                                stringResource(Res.string.ScansPage_NoMatches)
                            else
                                stringResource(Res.string.MainScreen_RecentScans_Empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.ScansPage_EmptyHint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 24.dp),
                        state = state,
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTasks) { task ->
                            ScanTaskCard(
                                taskEntity = task,
                                onClick = {
                                    focusManager.clearFocus()
                                    onTaskClick(task.id.value!!)
                                },
                                currentTime = currentTime,
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(state),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                            .width(8.dp)
                            .align(Alignment.CenterEnd),
                        style = LocalScrollbarStyle.current.copy(
                            unhoverColor = colorScheme.outlineVariant.copy(alpha = 0.6f),
                            hoverColor = colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSearchQueryVariants(raw: String): List<String> {
    // Keep this stable across recompositions for the same raw query.
    return remember(raw) {
        val base = normalizeForSearch(raw)
        if (base.isEmpty()) return@remember emptyList()

        buildList {
            add(base)
            add(normalizeForSearch(translitRuToLat(raw)))
            add(normalizeForSearch(translitLatToRu(raw)))
            // Handles cases where user typed with wrong keyboard layout
            add(normalizeForSearch(mapKeyboardEnToRu(raw)))
            add(normalizeForSearch(mapKeyboardRuToEn(raw)))
        }
            .filter { it.isNotBlank() }
            .distinct()
    }
}

@Composable
private fun rememberTaskSearchHaystack(task: org.angryscan.app.scan.TaskEntityViewModel): String {
    val name = task.name.value.orEmpty()
    val path = task.path.value.orEmpty()
    val attrNames = task.foundAttributes.value.keys.joinToString(" ") { it.name }
    val stateTokens = taskStateSearchTokens(task.state.value)

    return remember(name, path, attrNames, task.state.value) {
        normalizeForSearch(
            buildString {
                append(name).append(' ')
                append(path).append(' ')
                append(attrNames).append(' ')
                attributeSearchTokens.forEach { t -> append(t).append(' ') }
                stateTokens.forEach { t -> append(t).append(' ') }
            }
        )
    }
}

private fun taskStateSearchTokens(state: TaskState): List<String> = when (state) {
    TaskState.SCANNING, TaskState.SEARCHING -> listOf(
        "active", "running", "in progress", "scanning", "searching",
        "активные", "активный", "в работе", "сканирование", "поиск"
    )
    TaskState.STOPPED, TaskState.PENDING -> listOf(
        "paused", "stopped", "pending", "waiting", "queued",
        "пауза", "остановлено", "ожидание", "в очереди"
    )
    TaskState.FAILED -> listOf(
        "error", "failed", "failure",
        "ошибка", "ошибки", "сбой", "не удалось"
    )
    TaskState.COMPLETED -> listOf(
        "completed", "done", "finished", "success",
        "готово", "завершено", "выполнено"
    )
    TaskState.LOADING -> listOf("loading", "загрузка")
}

private val attributeSearchTokens = listOf(
    // EN
    "attribute", "attributes", "attr", "attrs", "found attributes", "match", "matches", "pattern", "patterns", "signature", "signatures",
    // RU
    "атрибут", "атрибуты", "аттр", "совпадение", "совпадения", "паттерн", "паттерны", "сигнатура", "сигнатуры"
)

private fun normalizeForSearch(input: String): String {
    // Lowercase, unify Ё/ё, keep only letters/digits/spaces, collapse whitespace.
    val sb = StringBuilder(input.length)
    var prevSpace = false
    for (ch in input.lowercase()) {
        val c = if (ch == 'ё') 'е' else ch
        val keep = c.isLetterOrDigit()
        if (keep) {
            sb.append(c)
            prevSpace = false
        } else if (!prevSpace) {
            sb.append(' ')
            prevSpace = true
        }
    }
    return sb.toString().trim()
}

private fun translitRuToLat(input: String): String {
    // Minimal, search-oriented transliteration (not for display).
    val map = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'е' to "e", 'ё' to "e", 'ж' to "zh", 'з' to "z", 'и' to "i",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
        'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
        'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "",
        'э' to "e", 'ю' to "yu", 'я' to "ya"
    )
    val out = StringBuilder(input.length)
    for (ch in input.lowercase()) {
        out.append(map[ch] ?: ch.toString())
    }
    return out.toString()
}

private fun translitLatToRu(input: String): String {
    // Best-effort reverse for search: handles common digraphs.
    val s = input.lowercase()
    val out = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        val next2 = s.substring(i, minOf(i + 2, s.length))
        val next3 = s.substring(i, minOf(i + 3, s.length))
        val next4 = s.substring(i, minOf(i + 4, s.length))
        when {
            next4 == "sch" -> { out.append('щ'); i += 3 }
            next3 == "zh" -> { out.append('ж'); i += 2 }
            next3 == "ts" -> { out.append('ц'); i += 2 }
            next3 == "ch" -> { out.append('ч'); i += 2 }
            next3 == "sh" -> { out.append('ш'); i += 2 }
            next2 == "yu" -> { out.append('ю'); i += 2 }
            next2 == "ya" -> { out.append('я'); i += 2 }
            else -> {
                out.append(
                    when (s[i]) {
                        'a' -> 'а'; 'b' -> 'б'; 'v' -> 'в'; 'g' -> 'г'; 'd' -> 'д'
                        'e' -> 'е'; 'z' -> 'з'; 'i' -> 'и'; 'y' -> 'й'; 'k' -> 'к'
                        'l' -> 'л'; 'm' -> 'м'; 'n' -> 'н'; 'o' -> 'о'; 'p' -> 'п'
                        'r' -> 'р'; 's' -> 'с'; 't' -> 'т'; 'u' -> 'у'; 'f' -> 'ф'
                        'h' -> 'х' // best-effort
                        else -> s[i]
                    }
                )
                i += 1
            }
        }
    }
    return out.toString()
}

private fun mapKeyboardEnToRu(input: String): String {
    val map = mapOf(
        'q' to 'й','w' to 'ц','e' to 'у','r' to 'к','t' to 'е','y' to 'н','u' to 'г','i' to 'ш','o' to 'щ','p' to 'з',
        '[' to 'х',']' to 'ъ',
        'a' to 'ф','s' to 'ы','d' to 'в','f' to 'а','g' to 'п','h' to 'р','j' to 'о','k' to 'л','l' to 'д',';' to 'ж','\'' to 'э',
        'z' to 'я','x' to 'ч','c' to 'с','v' to 'м','b' to 'и','n' to 'т','m' to 'ь',',' to 'б','.' to 'ю'
    )
    val out = StringBuilder(input.length)
    for (ch in input.lowercase()) out.append(map[ch] ?: ch)
    return out.toString()
}

private fun mapKeyboardRuToEn(input: String): String {
    val map = mapOf(
        'й' to 'q','ц' to 'w','у' to 'e','к' to 'r','е' to 't','н' to 'y','г' to 'u','ш' to 'i','щ' to 'o','з' to 'p',
        'х' to '[','ъ' to ']',
        'ф' to 'a','ы' to 's','в' to 'd','а' to 'f','п' to 'g','р' to 'h','о' to 'j','л' to 'k','д' to 'l','ж' to ';','э' to '\'',
        'я' to 'z','ч' to 'x','с' to 'c','м' to 'v','и' to 'b','т' to 'n','ь' to 'm','б' to ',','ю' to '.'
    )
    val out = StringBuilder(input.length)
    for (ch in input.lowercase()) out.append(map[ch] ?: ch)
    return out.toString()
}