package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LonjaPriceDatabase
import com.example.ui.theme.*
import java.text.Collator
import java.util.Locale

/**
 * Desplegable unificado de frutas y cítricos de la Lonja de Cítricos de Valencia.
 * Utilizado de manera idéntica en:
 * 1. Pantalla de Alta / Edición de Huerto (AddOrchardScreen)
 * 2. Pantalla de Partes de Trabajo - Recolección (WorkPartsScreen)
 * 3. Pantalla de Finanzas - Filtro por Variedad (FinancesScreen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitVarietyDropdown(
    selectedVariety: String,
    onVarietySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Variedad de la fruta",
    placeholder: String = "Seleccionar variedad...",
    includeAllOption: Boolean = false,
    allOptionLabel: String = "Todas las Variedades",
    extraVarieties: List<String> = emptyList(),
    testTag: String = "fruit_variety_dropdown",
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val spanishCollator = remember {
        Collator.getInstance(Locale("es", "ES")).apply {
            strength = Collator.PRIMARY
        }
    }

    // Unificación de todas las variedades oficiales de la Lonja de Cítricos de Valencia
    // ordenadas alfabéticamente
    val allVarieties = remember(extraVarieties) {
        val baseList = LonjaPriceDatabase.allVarietyNamesAlphabetical
        val combined = (baseList + extraVarieties.filter { it.isNotBlank() })
            .distinctBy { it.trim().lowercase(Locale.ROOT) }
            .sortedWith { a, b -> spanishCollator.compare(a, b) }
        combined
    }

    val filteredVarieties = remember(searchQuery, allVarieties) {
        val query = searchQuery.trim().lowercase(Locale.ROOT)
        if (query.isEmpty()) {
            allVarieties
        } else {
            allVarieties.filter { it.lowercase(Locale.ROOT).contains(query) }
        }
    }

    Column(
        modifier = modifier.testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                if (enabled) {
                    expanded = !expanded
                    if (!expanded) searchQuery = ""
                }
            }
        ) {
            val displayValue = when {
                selectedVariety.isBlank() -> ""
                includeAllOption && (selectedVariety == allOptionLabel || selectedVariety.isEmpty()) -> allOptionLabel
                else -> selectedVariety
            }

            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                placeholder = {
                    Text(
                        text = if (includeAllOption) allOptionLabel else placeholder,
                        fontSize = 14.sp,
                        color = OnSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                colors = appOutlinedTextFieldColors(),
                singleLine = true
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    searchQuery = ""
                },
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .heightIn(max = 380.dp)
            ) {
                // Buscador en el desplegable
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Buscar en Lonja de Valencia...",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Borrar",
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = appOutlinedTextFieldColors()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = OutlineVariant)

                // Opción "Todas las Variedades" para filtros (ej. Finanzas)
                if (includeAllOption && searchQuery.isEmpty()) {
                    val isAllSelected = selectedVariety == allOptionLabel || selectedVariety.isEmpty()
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = if (isAllSelected) PrimaryGreen else OnSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = allOptionLabel,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isAllSelected) PrimaryGreen else OnSurface,
                                        fontSize = 14.sp
                                    )
                                }
                                if (isAllSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onVarietySelected(allOptionLabel)
                            expanded = false
                            searchQuery = ""
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = OutlineVariant)
                }

                // Si no hay resultados para la búsqueda, permitir añadir texto libre
                if (filteredVarieties.isEmpty() && searchQuery.isNotBlank()) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = PrimaryGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Usar: \"${searchQuery.trim()}\"",
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreenDark,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = {
                            onVarietySelected(searchQuery.trim())
                            expanded = false
                            searchQuery = ""
                        }
                    )
                } else {
                    // Variedades de la Lonja de Cítricos en orden alfabético
                    filteredVarieties.forEach { item ->
                        val isSelected = selectedVariety.equals(item, ignoreCase = true)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = item,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryGreen else OnSurface,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Badge de categoría oficial de Lonja
                                    val (badgeText, badgeBg, badgeTextColor) = getVarietyBadgeInfo(item)
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = badgeTextColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onVarietySelected(item)
                                expanded = false
                                searchQuery = ""
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun getVarietyBadgeInfo(variety: String): Triple<String, Color, Color> {
    val lower = variety.lowercase(Locale.ROOT)
    return when {
        lower.contains("aguacate") || lower.contains("hass") || lower.contains("bacon") || lower.contains("fuerte") -> {
            Triple("🥑 Aguacate", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        }
        lower.contains("limón") || lower.contains("limon") || lower.contains("eureka") || lower.contains("verna") -> {
            Triple("🍋 Limón", Color(0xFFFFF9C4), Color(0xFFF57F17))
        }
        lower.contains("pomelo") || lower.contains("sanguinelli") -> {
            Triple("🍊 Pomelo/Sangre", Color(0xFFFFEBEE), Color(0xFFC62828))
        }
        lower.contains("clemen") || lower.contains("mandarina") || lower.contains("satsuma") ||
                lower.contains("orri") || lower.contains("tango") || lower.contains("nadorcott") ||
                lower.contains("marisol") || lower.contains("arrufatina") || lower.contains("okitsu") ||
                lower.contains("owari") || lower.contains("iwasaki") || lower.contains("clausellina") -> {
            Triple("🍊 Mandarina", Color(0xFFFFF3E0), Color(0xFFE65100))
        }
        else -> {
            Triple("🍊 Naranja", Color(0xFFFFEDE0), SecondaryOrangeDark)
        }
    }
}
