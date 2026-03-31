package org.angryscan.app.ui.windows.screens.scans

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.windows.screens.scans.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ScansScreen(onTaskClick: (Int) -> Unit) {
    val scanService = koinInject<ScanService>()

    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    var statusFilterExpanded by remember { mutableStateOf(false) }

    val allTasks by scanService.tasks.tasks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filterTaskStates = statusFilter.states

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
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 36.dp)
                .padding(bottom = 8.dp),
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
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.8f),
                unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.4f),
                focusedContainerColor = colorScheme.surface,
                unfocusedContainerColor = colorScheme.surface.copy(alpha = 0.92f)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.16f))
                .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(start = 10.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
        ) {
            if (allTasks.isNotEmpty() && visibleTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
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
                val isFiltered = filterTaskStates.isNotEmpty() || searchQuery.isNotBlank()
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(Res.string.MainScreen_RecentScans_Empty),
                            modifier = Modifier.size(36.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Text(
                            text = if (isFiltered) stringResource(Res.string.ScansPage_NoMatches)
                            else stringResource(Res.string.MainScreen_RecentScans_Empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.ScansPage_EmptyHint),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                val state = rememberLazyListState()
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = ScanListRowHorizontalPadding,
                                end = 16.dp + ScanListRowHorizontalPadding,
                                top = 4.dp,
                                bottom = 4.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.ScansPage_ColumnScan),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        )
                        Box(
                            modifier = Modifier.width(ScanListAttributesColumnWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.ScansPage_ColumnAttributes),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                            )
                        }
                        Spacer(modifier = Modifier.width(ScanListMainToMetricsGap))
                        Row(
                            modifier = Modifier.width(ScanListMetricsWidth),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(ScanListStatusColumnWidth)) {
                                OutlinedButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        statusFilterExpanded = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(30.dp)
                                        .align(Alignment.Center),
                                    border = BorderStroke(
                                        1.dp,
                                        colorScheme.outlineVariant.copy(alpha = 0.45f)
                                    ),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = colorScheme.surfaceVariant.copy(alpha = 0.24f),
                                        contentColor = colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(Res.string.ScansPage_StatusLabel, statusFilter.label()),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.95f),
                                        maxLines = 1
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                    )
                                }
                                DropdownMenu(
                                    expanded = statusFilterExpanded,
                                    onDismissRequest = { statusFilterExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("${stringResource(Res.string.ScansPage_FilterAll)} (${visibleTasks.size})") },
                                        onClick = {
                                            statusFilter = StatusFilter.ALL
                                            statusFilterExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${stringResource(Res.string.TaskStateChipFilter_Active)} (${countActive})") },
                                        onClick = {
                                            statusFilter = StatusFilter.ACTIVE
                                            statusFilterExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${stringResource(Res.string.TaskStateChipFilter_Paused)} (${countPaused})") },
                                        onClick = {
                                            statusFilter = StatusFilter.PAUSED
                                            statusFilterExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${stringResource(Res.string.TaskStateChipFilter_Error)} (${countError})") },
                                        onClick = {
                                            statusFilter = StatusFilter.ERROR
                                            statusFilterExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("${stringResource(Res.string.TaskStateChipFilter_Completed)} (${countCompleted})") },
                                        onClick = {
                                            statusFilter = StatusFilter.COMPLETED
                                            statusFilterExpanded = false
                                        }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.width(ScanListFoundColumnWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.ScansPage_ColumnFound),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                                )
                            }
                            Box(
                                modifier = Modifier.width(ScanListScoreColumnWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.ScansPage_ColumnScore),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                                )
                            }
                            Box(
                                modifier = Modifier.width(ScanListTimeColumnWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(Res.string.ScansPage_ColumnTime),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                                )
                            }
                            Spacer(modifier = Modifier.width(ScanListChevronColumnWidth))
                        }
                    }
                    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.22f))
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp),
                            state = state,
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                .padding(end = 4.dp)
                                .width(8.dp)
                                .align(Alignment.CenterEnd),
                            style = LocalScrollbarStyle.current.copy(
                                unhoverColor = colorScheme.outlineVariant.copy(alpha = 0.55f),
                                hoverColor = colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}

private enum class StatusFilter(val states: Set<TaskState>) {
    ALL(emptySet()),
    ACTIVE(setOf(TaskState.SCANNING, TaskState.SEARCHING)),
    PAUSED(setOf(TaskState.STOPPED, TaskState.PENDING)),
    ERROR(setOf(TaskState.FAILED)),
    COMPLETED(setOf(TaskState.COMPLETED))
}

@Composable
private fun StatusFilter.label(): String = when (this) {
    StatusFilter.ALL -> stringResource(Res.string.ScansPage_FilterAll)
    StatusFilter.ACTIVE -> stringResource(Res.string.TaskStateChipFilter_Active)
    StatusFilter.PAUSED -> stringResource(Res.string.TaskStateChipFilter_Paused)
    StatusFilter.ERROR -> stringResource(Res.string.TaskStateChipFilter_Error)
    StatusFilter.COMPLETED -> stringResource(Res.string.TaskStateChipFilter_Completed)
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