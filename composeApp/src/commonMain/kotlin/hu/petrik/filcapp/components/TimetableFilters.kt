package hu.petrik.filcapp.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.petrik.filcapp.network.TimetableFilter
import hu.petrik.filcapp.settings.tr

@Composable
fun TimetableFilterChips(
    selected: TimetableFilter,
    onSelected: (TimetableFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == TimetableFilter.COHORT,
            onClick = { onSelected(TimetableFilter.COHORT) },
            label = { Text(tr("Osztály", "Class")) },
            leadingIcon = { Icon(Icons.Default.School, null) },
            colors = FilterChipDefaults.filterChipColors(),
        )
        FilterChip(
            selected = selected == TimetableFilter.TEACHER,
            onClick = { onSelected(TimetableFilter.TEACHER) },
            label = { Text(tr("Tanár", "Teacher")) },
            leadingIcon = { Icon(Icons.Default.Person, null) },
        )
        FilterChip(
            selected = selected == TimetableFilter.CLASSROOM,
            onClick = { onSelected(TimetableFilter.CLASSROOM) },
            label = { Text(tr("Terem", "Classroom")) },
            leadingIcon = { Icon(Icons.Default.MeetingRoom, null) },
        )
    }
}

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
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second.orEmpty()

    LaunchedEffect(selectedId, options, expanded) {
        if (!expanded) {
            query = selectedLabel
        }
    }

    val filtered =
        if (query.isBlank() || query == selectedLabel) {
            options
        } else {
            options.filter { (_, optionLabel) -> optionLabel.contains(query, ignoreCase = true) }
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = options.isNotEmpty(),
            singleLine = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                Row {
                    if (allowClear && selectedId != null) {
                        IconButton(
                            onClick = {
                                onSelected(null)
                                query = ""
                                expanded = false
                            },
                        ) {
                            Icon(Icons.Default.Clear, tr("Szűrés törlése", "Clear filter"))
                        }
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
            },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                query = selectedLabel
            },
            modifier = Modifier.widthIn(min = 280.dp, max = 420.dp),
        ) {
            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            tr("Nincs találat", "No results"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {},
                    enabled = false,
                )
            } else {
                filtered.forEach { (id, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            onSelected(id)
                            query = optionLabel
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
