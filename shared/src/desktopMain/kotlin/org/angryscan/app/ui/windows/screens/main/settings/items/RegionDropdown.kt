package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionDropdown(
    selectedRegion: MatcherCountry,
    onRegionSelected: (MatcherCountry) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selectedRegion.flag}  ${selectedRegion.getLocalizedName(useShort = false)}",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MatcherCountry.entries
                .filter { it != MatcherCountry.ALL }
                .forEach { region ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${region.flag}  ${region.getLocalizedName(useShort = false)}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onRegionSelected(region)
                        expanded = false
                    }
                )
            }
        }
    }
}
