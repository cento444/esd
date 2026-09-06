package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WorkPartEntity
import com.example.ui.util.AppKeyboards
import com.example.ui.util.ExcelExportHelper
import com.example.ui.util.formatDateInput
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppTopHeader
import com.example.ui.components.FruitVarietyDropdown
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val allWorkParts by viewModel.allWorkParts.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val labelPropios = remember(userProfile) { userProfile.titularPropios.ifEmpty { "Mío" } }
    val labelVr = remember(userProfile) { userProfile.titularVr.ifEmpty { "V&R" } }
    val labelOtros = remember(userProfile) { userProfile.titularOtros.ifEmpty { "Otros" } }

    var selectedHuertoFilter by remember { mutableStateOf("Todos los Huertos") }
    var huertoDropdownExpanded by remember { mutableStateOf(false) }

    var selectedVarietyFilter by remember { mutableStateOf("Todas las Variedades") }

    var selectedTaskFilter by remember { mutableStateOf("Todas las Tareas") }
    var taskDropdownExpanded by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }

    var selectedOwnerFilters by remember { mutableStateOf(setOf("Todos")) } // Set of: "Todos", "Mío", "V&R", "Otros", "Varios"
    var showExportToast by remember { mutableStateOf(false) }
    var partToDelete by remember { mutableStateOf<WorkPartEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    val taskTypes = listOf(
        "Todas las Tareas",
        "Abonado y fertirrigación",
        "Desbroce y mantenimiento de suelo",
        "Injerto y replantación",
        "Limpieza y acondicionamiento",
        "Mantenimiento goteo y tuberías",
        "Muestreo y control de plagas",
        "Poda y aclareo",
        "Recolección / Cosecha",
        "Riego / Control de humedad",
        "Seguro agrícola",
        "Tratamiento fitosanitario"
    )

    val availableVarieties = remember(allOrchards) {
        val orchardVarieties = allOrchards.map { it.variety.trim() }.filter { it.isNotBlank() }
        val orchardFruitTypes = allOrchards.map { it.fruitType.trim() }.filter { it.isNotBlank() }
        val defaultVarieties = listOf(
            "Clemenules", "Navelina", "Lane Late", "Valencia Late", "Nadorcott",
            "Tango", "Oronules", "Marisol", "Arrufatina", "Hernandina", "Satsuma",
            "Aguacate", "Aguacate Hass", "Cítricos", "Naranjo", "Mandarino", "Limonero"
        )
        (orchardVarieties + orchardFruitTypes + defaultVarieties).distinct().sorted()
    }

    val orchardMap = remember(allOrchards) { allOrchards.associateBy { it.id } }
    val orchardByNameMap = remember(allOrchards) { allOrchards.associateBy { it.name.trim().lowercase(Locale.getDefault()) } }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val hasActiveFilters = remember(selectedHuertoFilter, selectedVarietyFilter, selectedTaskFilter, selectedOwnerFilters, startDate, endDate) {
        selectedHuertoFilter != "Todos los Huertos" ||
                selectedVarietyFilter != "Todas las Variedades" ||
                selectedTaskFilter != "Todas las Tareas" ||
                (!selectedOwnerFilters.contains("Todos") && selectedOwnerFilters.isNotEmpty() && selectedOwnerFilters.size < 4) ||
                startDate.isNotBlank() ||
                endDate.isNotBlank()
    }

    // Filter work parts from all registered expenses and revenues
    val filteredParts = remember(allWorkParts, allOrchards, selectedHuertoFilter, selectedVarietyFilter, selectedTaskFilter, selectedOwnerFilters, startDate, endDate) {
        val startTimestamp = try { if (startDate.length == 10) dateFormat.parse(startDate)?.time else null } catch (e: Exception) { null }
        val endTimestamp = try { if (endDate.length == 10) (dateFormat.parse(endDate)?.time ?: 0L) + 86399999L else null } catch (e: Exception) { null }

        allWorkParts.filter { part ->
            val huertoMatch = selectedHuertoFilter == "Todos los Huertos" || part.orchardName.contains(selectedHuertoFilter, ignoreCase = true)

            val partVariety = orchardMap[part.orchardId]?.variety
                ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]?.variety
                ?: ""
            val partFruitType = orchardMap[part.orchardId]?.fruitType
                ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())]?.fruitType
                ?: ""
            val varietyMatch = selectedVarietyFilter == "Todas las Variedades" ||
                    partVariety.equals(selectedVarietyFilter, ignoreCase = true) ||
                    partFruitType.equals(selectedVarietyFilter, ignoreCase = true) ||
                    (selectedVarietyFilter.contains("Aguacate", ignoreCase = true) && (partVariety.contains("Aguacate", ignoreCase = true) || partFruitType.contains("Aguacate", ignoreCase = true))) ||
                    part.observations.contains(selectedVarietyFilter, ignoreCase = true) ||
                    part.taskName.contains(selectedVarietyFilter, ignoreCase = true)

            val taskMatch = selectedTaskFilter == "Todas las Tareas" ||
                    part.taskName.contains(selectedTaskFilter, ignoreCase = true) ||
                    (selectedTaskFilter == "Riego / Control de humedad" && (part.type == "goteo" || part.taskName.contains("Riego", ignoreCase = true))) ||
                    (selectedTaskFilter == "Recolección / Cosecha" && (part.type == "produccion" || part.taskName.contains("Recolección", ignoreCase = true) || part.taskName.contains("Cosecha", ignoreCase = true)))

            val resolvedOwner = ExcelExportHelper.getPartOwnerCategory(
                part = part,
                orchard = orchardMap[part.orchardId] ?: orchardByNameMap[part.orchardName.trim().lowercase(Locale.getDefault())],
                titularPropios = userProfile.titularPropios,
                titularVr = userProfile.titularVr,
                titularOtros = userProfile.titularOtros
            )

            val ownerMatch = if (selectedOwnerFilters.contains("Todos") || selectedOwnerFilters.isEmpty() || selectedOwnerFilters.size == 4) {
                true
            } else {
                selectedOwnerFilters.contains(resolvedOwner)
            }

            val afterStart = startTimestamp == null || part.dateTimestamp >= startTimestamp
            val beforeEnd = endTimestamp == null || part.dateTimestamp <= endTimestamp

            huertoMatch && varietyMatch && taskMatch && ownerMatch && afterStart && beforeEnd
        }.sortedByDescending { it.dateTimestamp }
    }

    // Dynamic calculation of hanegadas for the selected filter scope
    val filteredOrchards = remember(allOrchards, selectedHuertoFilter, selectedVarietyFilter, selectedOwnerFilters) {
        allOrchards.filter { orchard ->
            val huertoMatch = selectedHuertoFilter == "Todos los Huertos" ||
                    orchard.name.contains(selectedHuertoFilter, ignoreCase = true)

            val varietyMatch = selectedVarietyFilter == "Todas las Variedades" ||
                    orchard.variety.equals(selectedVarietyFilter, ignoreCase = true) ||
                    orchard.fruitType.equals(selectedVarietyFilter, ignoreCase = true) ||
                    (selectedVarietyFilter.contains("Aguacate", ignoreCase = true) && (orchard.variety.contains("Aguacate", ignoreCase = true) || orchard.fruitType.contains("Aguacate", ignoreCase = true)))

            val orchardOwner = ExcelExportHelper.getOrchardOwnerCategory(orchard)

            val ownerMatch = if (selectedOwnerFilters.contains("Todos") || selectedOwnerFilters.isEmpty() || selectedOwnerFilters.size == 4) {
                true
            } else {
                selectedOwnerFilters.contains(orchardOwner)
            }

            huertoMatch && varietyMatch && ownerMatch
        }
    }

    val totalFilteredHanegadas = remember(filteredOrchards, filteredParts, allOrchards, hasActiveFilters, selectedHuertoFilter, selectedVarietyFilter, selectedOwnerFilters) {
        if (filteredOrchards.isNotEmpty()) {
            filteredOrchards.sumOf { it.hanegadas }
        } else {
            val partOrchardIds = filteredParts.mapNotNull { it.orchardId }.filter { it > 0 }.distinct()
            val partOrchards = allOrchards.filter { it.id in partOrchardIds }
            if (partOrchards.isNotEmpty()) {
                partOrchards.sumOf { it.hanegadas }
            } else if (!hasActiveFilters || (selectedHuertoFilter == "Todos los Huertos" && selectedVarietyFilter == "Todas las Variedades" && (selectedOwnerFilters.contains("Todos") || selectedOwnerFilters.isEmpty()))) {
                allOrchards.sumOf { it.hanegadas }
            } else {
                0.0
            }
        }
    }

    // Dynamic calculations directly based on filtered records
    val currentCalendarYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    fun getYear(timestamp: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR)
    }

    val availableYears = remember(allWorkParts) {
        allWorkParts.map { getYear(it.dateTimestamp) }.distinct().sortedDescending()
    }
    val activeYear = if (availableYears.contains(currentCalendarYear) || availableYears.isEmpty()) {
        currentCalendarYear
    } else {
        availableYears.first()
    }
    val previousYear = activeYear - 1

    // State for Finance tab mode: 0 = "Mi Bolsillo (Caja Real)", 1 = "Rentabilidad Completa (Con mi trabajo)"
    var selectedFinanceTab by remember { mutableStateOf(0) }

    // Helper para determinar si un parte de trabajo corresponde a Horas Propias (mano de obra personal no pagada en caja)
    fun isPartOwnLabor(part: WorkPartEntity): Boolean {
        if (part.type != "tareas") return false
        if (part.observations.contains("[HORAS_PROPIAS]")) return true
        if (part.observations.contains("[HORAS_CONTRATADAS]")) return false
        // Si no tiene etiqueta explícita, los huertos en propiedad computan como horas propias por defecto
        return part.ownerCategory.lowercase() in listOf("propios", "propiedad")
    }

    // Coste de mano de obra propia (valor estimado del trabajo del usuario)
    val ownLaborParts = filteredParts.filter { isPartOwnLabor(it) }
    val ownLaborHours = ownLaborParts.sumOf { it.hours }
    val ownLaborValue = ownLaborParts.sumOf { it.hours * it.pricePerHour }
    
    // Gastos reales de caja (desembolsos efectivos: materiales, mano de obra contratada, goteo, varios)
    val currentCosts = filteredParts.filter { it.type != "produccion" }.sumOf { it.totalCost }
    val realCashCosts = currentCosts - ownLaborValue // Lo que realmente sale del bolsillo/banco

    val currentRevenue = filteredParts.filter { it.type == "produccion" }.sumOf { it.totalCost }
    
    // Beneficio en Caja (Dinero ganado en el bolsillo) = Ingresos - Gastos reales pagados
    val currentCashNet = currentRevenue - realCashCosts

    // Rentabilidad Real (Económica) = Ingresos - Gastos reales - Valor de tu trabajo
    val currentRealNet = currentRevenue - currentCosts
    val currentNet = currentRealNet

    val costPerHanegada = if (totalFilteredHanegadas > 0.0) currentCosts / totalFilteredHanegadas else 0.0
    val cashCostPerHanegada = if (totalFilteredHanegadas > 0.0) realCashCosts / totalFilteredHanegadas else 0.0
    val benefitPerHanegada = if (totalFilteredHanegadas > 0.0) currentRevenue / totalFilteredHanegadas else 0.0
    val netPerHanegada = if (totalFilteredHanegadas > 0.0) currentNet / totalFilteredHanegadas else 0.0
    val cashNetPerHanegada = if (totalFilteredHanegadas > 0.0) currentCashNet / totalFilteredHanegadas else 0.0

    // Comparison with previous year (only when no ad-hoc date filter is active)
    val previousYearParts = allWorkParts.filter { getYear(it.dateTimestamp) == previousYear }
    val previousCosts = previousYearParts.filter { it.type != "produccion" }.sumOf { it.totalCost }
    val previousRevenue = previousYearParts.filter { it.type == "produccion" }.sumOf { it.totalCost }
    val previousNet = previousRevenue - previousCosts
    val allHanegadas = allOrchards.sumOf { it.hanegadas }
    val prevCostPerHanegada = if (allHanegadas > 0 && previousCosts > 0) previousCosts / allHanegadas else 0.0
    val prevBenefitPerHanegada = if (allHanegadas > 0 && previousRevenue > 0) previousRevenue / allHanegadas else 0.0

    fun calculateDiff(current: Double, previous: Double): Double? {
        if (previous <= 0.0) return null
        return ((current - previous) / previous) * 100.0
    }

    val costDiff = if (!hasActiveFilters) calculateDiff(currentCosts, previousCosts) else null
    val costPerHanegadaDiff = if (!hasActiveFilters) calculateDiff(costPerHanegada, prevCostPerHanegada) else null
    val revenueDiff = if (!hasActiveFilters) calculateDiff(currentRevenue, previousRevenue) else null
    val benefitPerHanegadaDiff = if (!hasActiveFilters) calculateDiff(benefitPerHanegada, prevBenefitPerHanegada) else null
    val netDiff = if (!hasActiveFilters) calculateDiff(currentNet, previousNet) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        AppTopHeader(
            title = "Finanzas",
            showMenu = true,
            onMenuClick = { viewModel.navigateTo(Screen.Settings) }
        )
            // Píldoras de Navegación entre Pestañas: "General" y "Trabajo C.B." (Fijas arriba)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineVariant, RoundedCornerShape(50.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pestaña General
                    val isGeneral = pagerState.currentPage == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isGeneral) SurfaceContainerHigh else Color.Transparent)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = if (isGeneral) PrimaryGreenDark else OnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "General",
                                fontSize = 12.sp,
                                fontWeight = if (isGeneral) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGeneral) OnSurface else OnSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Pestaña Trabajo C.B. 1 (V&R)
                    val isCb1 = pagerState.currentPage == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isCb1) SurfaceContainerHigh else Color.Transparent)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Handshake,
                                contentDescription = null,
                                tint = if (isCb1) SecondaryOrangeDark else OnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = labelVr,
                                fontSize = 12.sp,
                                fontWeight = if (isCb1) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCb1) OnSurface else OnSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Pestaña Trabajo C.B. 2 (Otros / 2da CB)
                    val isCb2 = pagerState.currentPage == 2
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isCb2) SurfaceContainerHigh else Color.Transparent)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = if (isCb2) SecondaryOrangeDark else OnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = labelOtros,
                                fontSize = 12.sp,
                                fontWeight = if (isCb2) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCb2) OnSurface else OnSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // HorizontalPager para permitir deslizar horizontalmente entre pestañas
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
                    ) {
                        // Filters Section Card
                        item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "FILTROS GENERALES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        // Huerto Filter
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Huerto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            ExposedDropdownMenuBox(
                                expanded = huertoDropdownExpanded,
                                onExpandedChange = { huertoDropdownExpanded = !huertoDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedHuertoFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = huertoDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = appOutlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = huertoDropdownExpanded,
                                    onDismissRequest = { huertoDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todos los Huertos", color = OnSurface) },
                                        onClick = {
                                            selectedHuertoFilter = "Todos los Huertos"
                                            huertoDropdownExpanded = false
                                        }
                                    )
                                    allOrchards.forEach { o ->
                                        DropdownMenuItem(
                                            text = { Text(o.name, color = OnSurface) },
                                            onClick = {
                                                selectedHuertoFilter = o.name
                                                huertoDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Variedad de Fruta Filter (Entre Huerto y Tipo de Tarea)
                        FruitVarietyDropdown(
                            selectedVariety = selectedVarietyFilter,
                            onVarietySelected = { selectedVarietyFilter = it },
                            label = "Variedad de Fruta",
                            includeAllOption = true,
                            allOptionLabel = "Todas las Variedades",
                            extraVarieties = allOrchards.map { it.variety.trim() }.filter { it.isNotBlank() },
                            testTag = "finances_variety_filter_dropdown"
                        )

                        // Tipo de Tarea Filter
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Tipo de Tarea", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                            ExposedDropdownMenuBox(
                                expanded = taskDropdownExpanded,
                                onExpandedChange = { taskDropdownExpanded = !taskDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedTaskFilter,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskDropdownExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = appOutlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = taskDropdownExpanded,
                                    onDismissRequest = { taskDropdownExpanded = false }
                                ) {
                                    taskTypes.forEach { item ->
                                        val vis = if (item == "Todas las Tareas") {
                                            TaskVisualInfo(Icons.AutoMirrored.Filled.List, Color(0xFF546E7A), Color(0xFFECEFF1))
                                        } else {
                                            getFarmTaskVisualInfo(item)
                                        }
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(vis.containerColor),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = vis.icon,
                                                        contentDescription = null,
                                                        tint = vis.iconColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            text = { Text(item, color = OnSurface) },
                                            onClick = {
                                                selectedTaskFilter = item
                                                taskDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Dates From / To
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Desde", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = startDate,
                                    onValueChange = { startDate = formatDateInput(it) },
                                    placeholder = { Text("01/01/2023", color = OnSurfaceVariant.copy(alpha = 0.6f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    keyboardOptions = AppKeyboards.Date,
                                    colors = appOutlinedTextFieldColors()
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Hasta", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = endDate,
                                    onValueChange = { endDate = formatDateInput(it) },
                                    placeholder = { Text("31/12/2023", color = OnSurfaceVariant.copy(alpha = 0.6f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    keyboardOptions = AppKeyboards.Date,
                                    colors = appOutlinedTextFieldColors()
                                )
                            }
                        }
                    }
                }
            }

            // Propietario Filter Pill Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isAllSelected = selectedOwnerFilters.contains("Todos") || selectedOwnerFilters.isEmpty() || selectedOwnerFilters.size >= 4
                        val isMioSelected = !selectedOwnerFilters.contains("Todos") && selectedOwnerFilters.contains("Mío")
                        val isVyRSelected = !selectedOwnerFilters.contains("Todos") && selectedOwnerFilters.contains("V&R")
                        val isOtrosSelected = !selectedOwnerFilters.contains("Todos") && selectedOwnerFilters.contains("Otros")
                        val isVariosSelected = !selectedOwnerFilters.contains("Todos") && selectedOwnerFilters.contains("Varios")

                        Text(
                            text = "PROPIETARIOS / TITULARIDAD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        fun toggleOwnerFilter(category: String) {
                            if (category == "Todos") {
                                selectedOwnerFilters = setOf("Todos")
                            } else {
                                val current = if (selectedOwnerFilters.contains("Todos")) emptySet() else selectedOwnerFilters
                                selectedOwnerFilters = if (current.contains(category)) {
                                    val next = current - category
                                    if (next.isEmpty()) setOf("Todos") else next
                                } else {
                                    val next = current + category
                                    if (next.size >= 4) setOf("Todos") else next
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OwnerFilterTabButton(
                                label = "Todos",
                                selected = isAllSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { toggleOwnerFilter("Todos") }
                            )
                            OwnerFilterTabButton(
                                label = labelPropios,
                                selected = isMioSelected || (isAllSelected && selectedOwnerFilters.contains("Mío")),
                                modifier = Modifier.weight(1f),
                                onClick = { toggleOwnerFilter("Mío") }
                            )
                            OwnerFilterTabButton(
                                label = labelVr,
                                selected = isVyRSelected || (isAllSelected && selectedOwnerFilters.contains("V&R")),
                                modifier = Modifier.weight(1f),
                                onClick = { toggleOwnerFilter("V&R") }
                            )
                            OwnerFilterTabButton(
                                label = labelOtros,
                                selected = isOtrosSelected || (isAllSelected && selectedOwnerFilters.contains("Otros")),
                                modifier = Modifier.weight(1f),
                                onClick = { toggleOwnerFilter("Otros") }
                            )
                            OwnerFilterTabButton(
                                label = "Varios",
                                selected = isVariosSelected || (isAllSelected && selectedOwnerFilters.contains("Varios")),
                                modifier = Modifier.weight(1f),
                                onClick = { toggleOwnerFilter("Varios") }
                            )
                        }
                    }
                }
            }

            // Breakdown Section: 2 Tabs (Mi Bolsillo vs Rentabilidad Completa) with Exact 3-Card Stack Design
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Selector de 2 Pestañas Grandes y Claras
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Pestaña 1: Mi Bolsillo (Caja Real)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFinanceTab = 0 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedFinanceTab == 0) PrimaryGreen else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (selectedFinanceTab == 0) PrimaryGreenDark else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💵 Mi Bolsillo (Caja Real)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedFinanceTab == 0) Color.White else OnSurfaceVariant
                                )
                            }
                        }

                        // Pestaña 2: Rentabilidad Completa
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFinanceTab = 1 },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedFinanceTab == 1) (if (isDark) Color(0xFF0284C7) else Color(0xFF0369A1)) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (selectedFinanceTab == 1) (if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📈 Rentabilidad Completa",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedFinanceTab == 1) Color.White else OnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Valores dinámicos según la pestaña seleccionada
                    val isTabPocket = selectedFinanceTab == 0
                    
                    val displayCost = if (isTabPocket) realCashCosts else currentCosts
                    val displayCostPerHg = if (isTabPocket) cashCostPerHanegada else costPerHanegada
                    val displayCostTitle = if (isTabPocket) {
                        if (hasActiveFilters) "GASTOS REALES PAGADOS (FILTRADO)" else "COSTE TOTAL AÑO ($activeYear)"
                    } else {
                        if (hasActiveFilters) "COSTE TOTAL CON TRABAJO (FILTRADO)" else "COSTE TOTAL AÑO ($activeYear)"
                    }

                    val displayRevenue = currentRevenue
                    val displayRevenuePerHg = benefitPerHanegada
                    val displayRevenueTitle = if (hasActiveFilters) "BENEFICIO TOTAL (FILTRADO)" else "BENEFICIO TOTAL AÑO ($activeYear)"

                    val displayNet = if (isTabPocket) currentCashNet else currentRealNet
                    val displayNetPerHg = if (isTabPocket) cashNetPerHanegada else netPerHanegada
                    val displayNetTitle = if (isTabPocket) {
                        if (hasActiveFilters) "DINERO EN TU BOLSILLO (FILTRADO)" else "DINERO EN TU BOLSILLO ($activeYear)"
                    } else {
                        if (hasActiveFilters) "RENTABILIDAD REAL (FILTRADO)" else "RENTABILIDAD REAL ($activeYear)"
                    }

                    val isNetPositive = displayNet >= 0
                    val baseHgString = "Base: %.1f hg".format(totalFilteredHanegadas)

                    // =========================================================================
                    // TARJETA 1: COSTES (DISEÑO EXACTO DEL SCREENSHOT)
                    // =========================================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF261517) else Color(0xFFFBF3F2)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF4D2427) else Color(0xFFF1D3D3))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Fila Superior
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayCostTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFF8A80) else Color(0xFF8C292E),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (costDiff != null) {
                                        "${if (costDiff > 0) "+" else ""}${costDiff.toInt()}% vs $previousYear"
                                    } else {
                                        "Sin datos de $previousYear"
                                    },
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant
                                )
                            }

                            // Importe Grande
                            Text(
                                text = "- € %,.2f".format(displayCost),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828)
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = if (isDark) Color(0xFF3E1E21) else Color(0xFFEED0D2)
                            )

                            // Fila Inferior: Por Hanegada
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "COSTE / HANEGADA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFF8A80) else Color(0xFF8C292E),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = baseHgString,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            }

                            Text(
                                text = "- € %,.2f".format(displayCostPerHg),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828)
                            )
                        }
                    }

                    // =========================================================================
                    // TARJETA 2: BENEFICIO / INGRESOS (DISEÑO EXACTO DEL SCREENSHOT)
                    // =========================================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF152618) else Color(0xFFF4F8EE)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF264C28) else Color(0xFFD5E8C9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Fila Superior
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = displayRevenueTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E6B34),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (revenueDiff != null) {
                                        "${if (revenueDiff > 0) "+" else ""}${revenueDiff.toInt()}% vs $previousYear"
                                    } else {
                                        "Sin datos de $previousYear"
                                    },
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant
                                )
                            }

                            // Importe Grande
                            Text(
                                text = "+ € %,.2f".format(displayRevenue),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA4D659) else Color(0xFF2E7D32)
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                color = if (isDark) Color(0xFF1F3D22) else Color(0xFFD8E9CD)
                            )

                            // Fila Inferior: Por Hanegada
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BENEFICIO / HANEGADA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E6B34),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = baseHgString,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                            }

                            Text(
                                text = "+ € %,.2f".format(displayRevenuePerHg),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA4D659) else Color(0xFF2E7D32)
                            )
                        }
                    }

                    // =========================================================================
                    // TARJETA 3: RENTABILIDAD REAL / DINERO EN BOLSILLO (DISEÑO EXACTO DEL SCREENSHOT)
                    // =========================================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNetPositive) {
                                if (isDark) Color(0xFF152618) else Color(0xFFF4F8EE)
                            } else {
                                if (isDark) Color(0xFF261517) else Color(0xFFFBF3F2)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isNetPositive) {
                                if (isDark) Color(0xFF264C28) else Color(0xFFD5E8C9)
                            } else {
                                if (isDark) Color(0xFF4D2427) else Color(0xFFF1D3D3)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = displayNetTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetPositive) {
                                    if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E6B34)
                                } else {
                                    if (isDark) Color(0xFFFF8A80) else Color(0xFF8C292E)
                                },
                                letterSpacing = 0.5.sp,
                                textAlign = TextAlign.Center
                            )

                            val netPrefix = if (displayNet > 0) "+ € " else if (displayNet < 0) "- € " else "€ "
                            Text(
                                text = "$netPrefix%,.2f".format(kotlin.math.abs(displayNet)),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isNetPositive) {
                                    if (isDark) Color(0xFFA4D659) else Color(0xFF2E7D32)
                                } else {
                                    if (isDark) Color(0xFFFF6B6B) else Color(0xFFC62828)
                                },
                                textAlign = TextAlign.Center
                            )

                            val sign = if (displayNetPerHg >= 0) "+" else "-"
                            Text(
                                text = "Margen real: $sign € %.2f / hg (%.1f hg)".format(
                                    kotlin.math.abs(displayNetPerHg),
                                    totalFilteredHanegadas
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNetPositive) {
                                    if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E6B34)
                                } else {
                                    if (isDark) Color(0xFFFF8A80) else Color(0xFF8C292E)
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Botón Principal de Descargar Excel (Igual al de Trabajo C.B. y ubicado entre Beneficio Neto y Detalles de Operaciones)
            item {
                val isAllOwners = selectedOwnerFilters.contains("Todos") || selectedOwnerFilters.isEmpty() || selectedOwnerFilters.size >= 4
                val ownerExportLabel = if (isAllOwners) {
                    "Todos los Propietarios ($labelPropios, $labelVr, $labelOtros, Varios)"
                } else {
                    selectedOwnerFilters.map {
                        when (it) {
                            "Mío" -> labelPropios
                            "V&R" -> labelVr
                            "Otros" -> labelOtros
                            else -> it
                        }
                    }.sorted().joinToString(", ")
                }

                OutlinedButton(
                    onClick = {
                        ExcelExportHelper.exportOperationsComprehensiveCsv(
                            context = context,
                            parts = filteredParts,
                            orchards = allOrchards,
                            huertoFilter = selectedHuertoFilter,
                            varietyFilter = selectedVarietyFilter,
                            taskFilter = selectedTaskFilter,
                            ownerFilter = ownerExportLabel,
                            startDateStr = startDate,
                            endDateStr = endDate
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 54.dp)
                        .testTag("export_finances_excel_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceWhite,
                        contentColor = PrimaryGreenDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Descargar Excel",
                            tint = PrimaryGreenDark,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Descargar Excel de Finanzas y Operaciones",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreenDark
                            )
                            val filterInfo = if (hasActiveFilters) "Filtros aplicados" else "Año $activeYear"
                            Text(
                                text = "Informe .xlsx completo ($filterInfo • ${filteredParts.size} operaciones)",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Cost List Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Detalles de Operaciones (${filteredParts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                }
            }

            // Empty State
            if (filteredParts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No hay operaciones registradas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "Cada gasto (partes de trabajo, riego, gastos varios) o ingreso de cosecha registrado en la app aparecerá reflejado aquí en tiempo real.",
                                fontSize = 13.sp,
                                color = OnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            // List of Cost Items
            if (filteredParts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Swipe,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "⬅ Desliza a la izquierda para eliminar un registro",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            items(filteredParts, key = { it.id }) { part ->
                FinanceCostItemCard(
                    part = part,
                    onDeleteRequest = { partToDelete = part },
                    onEditRequest = {
                        viewModel.navigateTo(
                            Screen.WorkParts(editWorkPartId = part.id)
                        )
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else if (page == 1) {
        // Pestaña 1: Trabajo C.B. 1 (V&R)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            item {
                CbWorkTabContent(
                    viewModel = viewModel,
                    allOrchards = allOrchards,
                    allWorkParts = allWorkParts,
                    targetOwnerCategory = "V&R",
                    titularName = labelVr,
                    tabDisplayName = labelVr
                )
            }
        }
    } else {
        // Pestaña 2: Trabajo C.B. 2 (Otros / Segunda Comunidad de Bienes)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            item {
                CbWorkTabContent(
                    viewModel = viewModel,
                    allOrchards = allOrchards,
                    allWorkParts = allWorkParts,
                    targetOwnerCategory = "Otros",
                    titularName = labelOtros,
                    tabDisplayName = labelOtros
                )
            }
        }
    }
}
}

    if (partToDelete != null) {
        val part = partToDelete!!
        AlertDialog(
            onDismissRequest = { partToDelete = null },
            title = {
                Text(
                    text = "Eliminar Registro",
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar '${part.taskName}'" +
                            (if (part.totalCost > 0) " por importe de %.2f €".format(part.totalCost) else "") +
                            "? Esta operación se restará inmediatamente del balance de Finanzas.",
                    color = OnSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkPart(part)
                        partToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { partToDelete = null }) {
                    Text("Cancelar", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun OwnerFilterTabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor = if (selected) PrimaryGreenDark else if (isDark) SurfaceContainerHigh else SurfaceWhite
    val borderColor = if (selected) PrimaryGreenDark else OutlineVariant

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = when {
                label.length > 10 -> 9.sp
                label.length > 6 -> 10.sp
                else -> 11.sp
            },
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else OnSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FinanceCostItemCard(
    part: WorkPartEntity,
    onDeleteRequest: () -> Unit = {},
    onEditRequest: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe_offset_finance")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Red background when swiping left
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(
                    if (offsetX < -30) Color(0xFFDC2626) else SurfaceContainerLow
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (offsetX < -30) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Eliminar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                }
            }
        }

        // Foreground Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        // Only allow dragging to the left (negative delta)
                        offsetX = (offsetX + delta).coerceIn(-180f, 0f)
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (offsetX < -80f) {
                            onDeleteRequest()
                        }
                        offsetX = 0f
                    }
                )
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Row: Icon, Task Name, Type Badge & Amount
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
                        val taskVisual = getFarmTaskVisualInfo(part.taskName)
                        val icon = when {
                            part.type == "goteo" -> Icons.Default.WaterDrop
                            part.type == "varios" -> if (part.taskName.isNotBlank() && part.taskName != "Gasto Varios" && part.taskName != "Gastos Varios") taskVisual.icon else Icons.AutoMirrored.Filled.ReceiptLong
                            part.type == "produccion" -> Icons.Default.Agriculture
                            else -> taskVisual.icon
                        }
                        val iconBg = when {
                            part.type == "goteo" -> if (isDark) Color(0xFF0D324D) else Color(0xFFE1F5FE)
                            part.type == "varios" -> if (part.taskName.isNotBlank() && part.taskName != "Gasto Varios" && part.taskName != "Gastos Varios") taskVisual.containerColor else (if (isDark) Color(0xFF422405) else Color(0xFFFFE0B2))
                            part.type == "produccion" -> if (isDark) Color(0xFF1B3D1D) else Color(0xFFC8E6C9)
                            else -> taskVisual.containerColor
                        }
                        val iconTint = when {
                            part.type == "goteo" -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0288D1)
                            part.type == "varios" -> if (part.taskName.isNotBlank() && part.taskName != "Gasto Varios" && part.taskName != "Gastos Varios") taskVisual.iconColor else (if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100))
                            part.type == "produccion" -> if (isDark) Color(0xFFA4D659) else Color(0xFF2E7D32)
                            else -> taskVisual.iconColor
                        }

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(19.dp))
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = part.taskName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(part.dateTimestamp))
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    val isIncome = part.type == "produccion"
                    val badgeBg = if (isIncome) {
                        if (isDark) Color(0xFF1E3814) else Color(0xFFE8F5E9)
                    } else {
                        if (isDark) Color(0xFF3E1F1F) else Color(0xFFFFEBEE)
                    }
                    val amountColor = if (isIncome) {
                        if (isDark) Color(0xFFA4D659) else Color(0xFF2E7D32)
                    } else {
                        if (isDark) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
                    }
                    val badgeTextColor = amountColor

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isIncome) "Ingreso" else "Gasto",
                                fontSize = 11.sp,
                                color = badgeTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (isIncome) "+ € %,.2f".format(part.totalCost) else "- € %,.2f".format(part.totalCost),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )

                        IconButton(
                            onClick = onEditRequest,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Details Row: Huerto & Horas / Kilos
                val locationLabel = if (part.type == "varios" || part.orchardId <= 0L || part.orchardName.isBlank() || part.orchardName == "Gasto General") {
                    "Gasto General"
                } else {
                    part.orchardName
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Huerto
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Huerto",
                            tint = PrimaryGreenDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = locationLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface,
                            maxLines = 1
                        )
                    }

                    // Horas de la tarea o Kilos de producción
                    if (part.hours > 0.0) {
                        val hoursStr = if (part.hours % 1.0 == 0.0) part.hours.toInt().toString() else "%.1f".format(part.hours)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Horas",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = buildString {
                                    append("$hoursStr h")
                                    if (part.pricePerHour > 0.0) {
                                        append(" (%.2f €/h)".format(part.pricePerHour))
                                    }
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceVariant
                            )
                        }
                    } else if (part.kilos > 0.0) {
                        val kilosStr = if (part.kilos % 1.0 == 0.0) part.kilos.toInt().toString() else "%.1f".format(part.kilos)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Scale,
                                contentDescription = "Kilos",
                                tint = PrimaryGreenDark,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = buildString {
                                    append("$kilosStr kg")
                                    if (part.pricePerKg > 0.0) {
                                        append(" (%.2f €/kg)".format(part.pricePerKg))
                                    }
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryGreenDark
                            )
                        }
                    }
                }

                // Observaciones (si las hubiera)
                val displayObservations = part.observations
                    .replace("[HORAS_PROPIAS]", "")
                    .replace("[HORAS_CONTRATADAS]", "")
                    .trim()

                // Badge de Mano de Obra: Horas Propias vs Contratada
                val isOwnLaborPart = part.type == "tareas" && (
                    part.observations.contains("[HORAS_PROPIAS]") ||
                    (part.ownerCategory.lowercase() in listOf("propios", "propiedad") && !part.observations.contains("[HORAS_CONTRATADAS]"))
                )
                val isContratadaPart = part.type == "tareas" && part.hours > 0 && !isOwnLaborPart

                if (isOwnLaborPart || isContratadaPart) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isOwnLaborPart) {
                            if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0F2FE)
                        } else {
                            if (isDark) Color(0xFF332014) else Color(0xFFFEF3C7)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isOwnLaborPart) Icons.Default.Person else Icons.Default.Group,
                                contentDescription = null,
                                tint = if (isOwnLaborPart) (if (isDark) Color(0xFF93C5FD) else Color(0xFF0369A1)) else (if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isOwnLaborPart) "Horas Propias (mano de obra estimada, no pagada)" else "Mano de obra contratada (gasto de caja)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOwnLaborPart) (if (isDark) Color(0xFF93C5FD) else Color(0xFF0369A1)) else (if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309))
                            )
                        }
                    }
                }

                if (displayObservations.isNotBlank()) {
                    val obsBg = if (isDark) Color(0xFF2C2214) else Color(0xFFFFF8E1)
                    val obsBorder = if (isDark) Color(0xFF594320) else Color(0xFFFFE082)
                    val obsTextColor = if (isDark) Color(0xFFFFE082) else Color(0xFF5D4037)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(obsBg)
                            .border(1.dp, obsBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Notes,
                            contentDescription = "Observaciones",
                            tint = if (isDark) Color(0xFFFFB74D) else Color(0xFFF57F17),
                            modifier = Modifier.size(14.dp).padding(top = 1.dp)
                        )
                        Text(
                            text = displayObservations,
                            fontSize = 11.sp,
                            color = obsTextColor,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinanceMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    subtitle: String? = null
) {
    val isDark = LocalIsDarkTheme.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "€ %,.2f".format(amount),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    maxLines = 1
                )
            }
        }
    }
}

