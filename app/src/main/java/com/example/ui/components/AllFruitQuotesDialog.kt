package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LonjaPriceDatabase
import com.example.data.VarietyPrice
import com.example.data.model.OrchardEntity
import com.example.ui.theme.*

/**
 * Diálogo modal para consultar el desglose completo de todas las cotizaciones
 * de frutas, cítricos y aguacates de la Lonja de Cítricos de Valencia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFruitQuotesDialog(
    userOrchards: List<OrchardEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }

    val categories = remember {
        listOf(
            "Todas",
            "Mis Frutas",
            "Mandarinas / Clementinas",
            "Naranjas - Navel",
            "Naranjas - Blancas",
            "Aguacates",
            "Limones",
            "Pomelos"
        )
    }

    val filteredVarieties = remember(searchQuery, selectedCategory, userOrchards) {
        LonjaPriceDatabase.varieties
            .distinctBy { it.varietyName }
            .filter { item ->
                val matchesCategory = when (selectedCategory) {
                    "Todas" -> true
                    "Mis Frutas", "Mis Huertos" -> isVarietyInUserOrchards(item, userOrchards)
                    "Mandarinas / Clementinas" -> item.category.contains("Mandarinas", ignoreCase = true) || item.category.contains("Clementinas", ignoreCase = true) || item.category.contains("Híbridos", ignoreCase = true) || item.category.contains("Satsuma", ignoreCase = true)
                    "Naranjas - Navel" -> item.category.contains("Navel", ignoreCase = true)
                    "Naranjas - Blancas" -> item.category.contains("Blancas", ignoreCase = true) || item.category.contains("Sanguinas", ignoreCase = true)
                    "Aguacates" -> item.category.contains("Aguacate", ignoreCase = true)
                    "Limones" -> item.category.contains("Limón", ignoreCase = true) || item.category.contains("Limones", ignoreCase = true)
                    "Pomelos" -> item.category.contains("Pomelo", ignoreCase = true)
                    else -> item.category.contains(selectedCategory, ignoreCase = true)
                }

                val query = searchQuery.trim().lowercase()
                val matchesQuery = query.isEmpty() ||
                        item.varietyName.lowercase().contains(query) ||
                        item.category.lowercase().contains(query)

                matchesCategory && matchesQuery
            }
    }

    val isDark = LocalIsDarkTheme.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .fillMaxHeight(0.88f)
                .testTag("all_fruit_quotes_dialog"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF3D2A10) else Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Cotizaciones de Frutas",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Lonja de Cítricos de Valencia • Semanal",
                                fontSize = 11.5.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_quotes_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = OnSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(thickness = 0.8.dp, color = OutlineVariant)

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("quotes_search_field"),
                    placeholder = {
                        Text("Buscar variedad (ej. Clemenules, Hass...)", fontSize = 13.sp, color = OnSurfaceVariant.copy(alpha = 0.6f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Borrar búsqueda", tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = appOutlinedTextFieldColors(
                        focusedBorderColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                        containerColor = SurfaceBright
                    )
                )

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        val isMyFruits = category == "Mis Frutas"
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            modifier = Modifier.testTag("quotes_chip_${category.lowercase().replace(" ", "_")}"),
                            leadingIcon = if (isMyFruits) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Agriculture,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = category,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isMyFruits) {
                                    if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
                                } else {
                                    if (isDark) Color(0xFF4D350A) else Color(0xFFFEF3C7)
                                },
                                selectedLabelColor = if (isMyFruits) {
                                    if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                } else {
                                    if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                },
                                selectedLeadingIconColor = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D),
                                containerColor = SurfaceBright,
                                labelColor = OnSurfaceVariant
                            ),
                            border = BorderStroke(
                                0.8.dp,
                                if (isSelected) {
                                    if (isMyFruits) (if (isDark) Color(0xFF22C55E) else Color(0xFF16A34A))
                                    else (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B))
                                } else OutlineVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Subtitle Info Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceContainerLow,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.6.dp, OutlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedCategory == "Mis Frutas") {
                                "${filteredVarieties.size} de tus variedades"
                            } else {
                                "${filteredVarieties.size} variedades listadas"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = "Precios en árbol (€/kg)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        )
                    }
                }

                // List of Varieties
                if (filteredVarieties.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedCategory == "Mis Frutas") Icons.Default.Agriculture else Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = OnSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = if (selectedCategory == "Mis Frutas") {
                                    "No hay frutas registradas de tus huertos"
                                } else {
                                    "No se encontraron cotizaciones"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                text = if (selectedCategory == "Mis Frutas") {
                                    "Configura las variedades de tus huertos (ej. Clemenules, Navelina, Hass) para ver sus cotizaciones directas."
                                } else {
                                    "Prueba con otro término de búsqueda o categoría"
                                },
                                fontSize = 12.sp,
                                color = OnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredVarieties, key = { index, item -> "${item.varietyName}_$index" }) { _, varietyPrice ->
                            VarietyPriceCard(
                                variety = varietyPrice,
                                userOrchards = userOrchards
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.8.dp, color = OutlineVariant)

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fuente: precioscitricos.com",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Cerrar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VarietyPriceCard(
    variety: VarietyPrice,
    userOrchards: List<OrchardEntity>
) {
    val matchingOrchards = remember(variety.varietyName, userOrchards) {
        userOrchards.filter { orchard ->
            orchard.variety.equals(variety.varietyName, ignoreCase = true) ||
                    variety.varietyName.contains(orchard.variety, ignoreCase = true) ||
                    orchard.variety.contains(variety.varietyName, ignoreCase = true)
        }
    }
    val isDark = LocalIsDarkTheme.current
    val hasMyOrchard = matchingOrchards.isNotEmpty()

    val isPositive = variety.variationPercent > 0
    val isNegative = variety.variationPercent < 0

    val badgeBg = when {
        isPositive -> if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
        isNegative -> if (isDark) Color(0xFF4C0519) else Color(0xFFFFE4E6)
        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    }
    val badgeColor = when {
        isPositive -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        isNegative -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
        else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }
    val variationText = when {
        isPositive -> "+${variety.variationPercent}% ↗"
        isNegative -> "${variety.variationPercent}% ↘"
        else -> "= 0.0%"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasMyOrchard) {
                if (isDark) Color(0xFF132E1B) else Color(0xFFF0FDF4)
            } else {
                SurfaceWhite
            }
        ),
        border = BorderStroke(
            1.dp,
            if (hasMyOrchard) {
                if (isDark) Color(0xFF166534) else Color(0xFF86EFAC)
            } else {
                OutlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // "En tu huerto" Tag (if applicable)
            if (hasMyOrchard) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "En tus huertos: ${matchingOrchards.joinToString { it.name }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Top Row: Variety Name & Current Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = variety.varietyName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = variety.category,
                        fontSize = 11.5.sp,
                        color = OnSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = variety.priceText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (hasMyOrchard) {
                            if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                        } else {
                            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        }
                    )
                    // Variation Pill
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = variationText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Bottom Row: Price Range (Min - Max)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Horquilla: ${String.format(java.util.Locale.US, "%.2f", variety.minPrice)}€ - ${String.format(java.util.Locale.US, "%.2f", variety.maxPrice)}€/kg",
                    fontSize = 11.5.sp,
                    color = OnSurfaceVariant.copy(alpha = 0.8f)
                )

                Text(
                    text = variety.source,
                    fontSize = 10.5.sp,
                    color = OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * Comprueba con total precisión si una variedad cotizada en la Lonja coincide exactamente
 * con las variedades cultivadas en los huertos registrados del usuario.
 */
private fun isVarietyInUserOrchards(variety: VarietyPrice, userOrchards: List<OrchardEntity>): Boolean {
    if (userOrchards.isEmpty()) return false

    return try {
        val lonjaKey = getCanonicalVarietyKey(variety.varietyName)
        if (lonjaKey.isEmpty()) return false

        userOrchards.any { orchard ->
            val orchardVariety = orchard.variety.trim()
            if (orchardVariety.isEmpty()) return@any false
            val orchardKey = getCanonicalVarietyKey(orchardVariety)
            orchardKey.isNotEmpty() && (orchardKey == lonjaKey || orchardKey.contains(lonjaKey) || lonjaKey.contains(orchardKey))
        }
    } catch (_: Exception) {
        false
    }
}

private fun getCanonicalVarietyKey(rawName: String): String {
    val clean = rawName.lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace(Regex("[()\\[\\]\\-–,/]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    if (clean.isEmpty()) return ""

    val words = clean.split(" ").filter { it.isNotBlank() }.toSet()

    return when {
        // Navel
        words.contains("powell") -> "powell"
        words.contains("navelina") -> "navelina"
        words.contains("lane") || clean.contains("lane late") -> "lane_late"
        words.contains("navelate") -> "navelate"
        words.contains("barnfield") -> "barnfield"
        words.contains("chislett") -> "chislett"
        words.contains("newhall") -> "newhall"
        words.contains("rohde") -> "rohde"
        words.contains("washington") -> "washington_navel"

        // Blancas / Tardías
        words.contains("valencia") || clean.contains("valencia late") -> "valencia_late"
        words.contains("salustiana") -> "salustiana"
        words.contains("barberina") -> "barberina"
        words.contains("midknight") -> "midknight"
        words.contains("delta") -> "delta_seedless"

        // Sanguinas
        words.contains("sanguinelli") || words.contains("sanguina") || words.contains("sanguinas") -> "sanguinelli"

        // Satsumas
        words.contains("clausellina") -> "clausellina"
        words.contains("iwasaki") -> "iwasaki"
        words.contains("okitsu") -> "okitsu"
        words.contains("owari") -> "owari"

        // Clementinas y Mandarinas
        words.contains("arrufatina") -> "arrufatina"
        words.contains("basol") -> "basol"
        words.contains("clemenpons") || (words.contains("pons") && !words.contains("clemen")) -> "clemenpons"
        words.contains("clemenrubi") || (words.contains("rubi") && !words.contains("star")) -> "clemenrubi"
        words.contains("clemenules") || words.contains("nules") -> "clemenules"
        words.contains("esbal") -> "esbal"
        words.contains("fina") -> "fina"
        words.contains("hernandina") -> "hernandina"
        words.contains("loretina") -> "loretina"
        words.contains("marisol") -> "marisol"
        words.contains("mioro") -> "mioro"
        words.contains("oronules") -> "oronules"
        words.contains("orogrande") -> "orogrande"
        words.contains("orogros") -> "orogros"
        words.contains("oroval") -> "oroval"
        words.contains("sando") -> "sando"
        words.contains("tomatera") -> "tomatera"

        // Híbridos
        words.contains("afourer") -> "afourer"
        words.contains("nova") || words.contains("clemenvilla") -> "nova"
        words.contains("garbi") -> "garbi"
        words.contains("leanri") -> "leanri"
        words.contains("murcott") -> "murcott"
        words.contains("nadorcott") -> "nadorcott"
        words.contains("orri") -> "orri"
        words.contains("ortanique") -> "ortanique"
        words.contains("safor") -> "safor"
        words.contains("sunshine") -> "spring_sunshine"
        words.contains("tango") -> "tango"

        // Limones / Pomelos
        words.contains("eureka") -> "eureka"
        words.contains("fino") -> "limon_fino"
        words.contains("verna") -> "limon_verna"
        words.contains("rio") && words.contains("red") -> "pomelo_rio_red"
        words.contains("star") || (words.contains("ruby") && words.contains("pomelo")) -> "pomelo_star_ruby"

        // Aguacates
        words.contains("lamb") || clean.contains("lamb hass") -> "aguacate_lamb_hass"
        words.contains("bacon") -> "aguacate_bacon"
        words.contains("fuerte") -> "aguacate_fuerte"
        words.contains("hass") && !words.contains("lamb") -> "aguacate_hass"

        // Fallback exact clean match
        else -> clean
    }
}
