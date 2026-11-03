package hu.petrik.filcapp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.data.TimetableFilter
import hu.petrik.filcapp.settings.tr

@Composable
fun TimetableFilterChips(
    selected: TimetableFilter,
    onSelected: (TimetableFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FilcFilterChip(
            text = tr("Osztály", "Class"),
            selected = selected == TimetableFilter.COHORT,
            onClick = { onSelected(TimetableFilter.COHORT) },
            icon = Icons.Default.School,
        )
        FilcFilterChip(
            text = tr("Tanár", "Teacher"),
            selected = selected == TimetableFilter.TEACHER,
            onClick = { onSelected(TimetableFilter.TEACHER) },
            icon = Icons.Default.Person,
        )
        FilcFilterChip(
            text = tr("Terem", "Classroom"),
            selected = selected == TimetableFilter.CLASSROOM,
            onClick = { onSelected(TimetableFilter.CLASSROOM) },
            icon = Icons.Default.MeetingRoom,
        )
    }
}

/**
 * Selects an item using an independently focused search field inside a bottom sheet.
 * The old DropdownMenu stole focus from the editable field and composed every option.
 * All timetable and substitution selectors share this component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableSelection(
    options: List<Pair<String, String>>,
    selectedId: String?,
    label: String,
    placeholder: String,
    onSelected: (String?) -> Unit,
    allowClear: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filtered = remember(options, query) { filterSelectionOptions(options, query) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    query = ""
                    expanded = true
                },
                enabled = options.isNotEmpty(),
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = selectedLabel ?: placeholder,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color =
                        if (selectedLabel == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            if (allowClear && selectedId != null) {
                IconButton(onClick = { onSelected(null) }) {
                    Icon(Icons.Default.Clear, contentDescription = tr("Kijelölés törlése", "Clear selection"))
                }
            }
        }
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = { expanded = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(tr("Keresés név alapján…", "Search by name…")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                if (filtered.isEmpty()) {
                    Text(
                        tr("Nincs találat", "No results"),
                        modifier = Modifier.padding(vertical = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(filtered) { (id, name) ->
                            ListItem(
                                headlineContent = { Text(name) },
                                modifier =
                                    Modifier.fillMaxWidth().clickable {
                                        onSelected(id)
                                        expanded = false
                                    },
                                trailingContent = {
                                    if (id == selectedId) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/** Pure search logic for the shared mobile picker, kept testable without Compose. */
internal fun filterSelectionOptions(
    options: List<Pair<String, String>>,
    query: String,
): List<Pair<String, String>> {
    val term = normalizeSelectionSearch(query.trim())
    return if (term.isEmpty()) {
        options
    } else {
        options.filter { (_, label) -> normalizeSelectionSearch(label).contains(term) }
    }
}
