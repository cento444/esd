package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MaterialItem
import com.example.data.model.MaterialsJsonHelper
import com.example.data.model.OrchardEntity
import com.example.data.model.WorkPartEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.getFarmTaskVisualInfo
import com.example.ui.theme.*
import com.example.ui.util.CbWorkExcelExportHelper
import com.example.ui.util.ExcelExportHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pestaña especializada "Trabajo C.B." dentro de la pantalla de Finanzas.
 * Permite a Carlos Vicente registrar, visualizar y liquidar mensualmente todas
 * las horas de trabajo personal, tareas y materiales aportados a la sociedad
 * V&R C.B. (participada al 50% entre ambos hermanos) para pasarlo a la C.B. y cobrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CbWorkTabContent(
    viewModel: MainViewModel,
    allOrchards: List<OrchardEntity>,
    allWorkParts: List<WorkPartEntity>,
    targetOwnerCategory: String = "V&R",
    titularName: String = "V&R C.B.",
    tabDisplayName: String = "Trabajos V&R"
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    // Nombres de los meses en español
    val monthNames = remember {
        listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
    }

    val currentCal = remember { Calendar.getInstance() }
    var selectedMonthIndex by rememberSaveable { mutableIntStateOf(currentCal.get(Calendar.MONTH)) } // 0..11
    var selectedYear by rememberSaveable { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var isFullYearMode by rememberSaveable { mutableStateOf(false) }
    var isAllTimeMode by rememberSaveable { mutableStateOf(false) }

    val defaultAllHuertosLabel = remember(tabDisplayName) { "Todos los Huertos ($tabDisplayName)" }
    var selectedHuertoFilter by rememberSaveable(tabDisplayName) { mutableStateOf(defaultAllHuertosLabel) }
    var huertoDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var showMaterialsBreakdown by remember { mutableStateOf(false) }
    var partToDelete by remember { mutableStateOf<WorkPartEntity?>(null) }

    val orchardMap = remember(allOrchards) { allOrchards.associateBy { it.id } }

    // Identificar huertos que pertenecen a esta entidad (V&R u Otros / Comunidad de Bienes)
    val cbOrchards = remember(allOrchards, targetOwnerCategory, titularName) {
        val tNorm = titularName.trim().lowercase(Locale.getDefault())
        allOrchards.filter { orchard ->
            val ownerType = orchard.ownerType.lowercase().trim()
            val name = orchard.name.lowercase().trim()
            val ownerName = orchard.ownerName.lowercase().trim()
            val cat = ExcelExportHelper.getOrchardOwnerCategory(orchard)

            if (targetOwnerCategory == "Otros") {
                ownerType == "otros" ||
                        cat == "Otros" ||
                        (tNorm.isNotBlank() && ownerName.contains(tNorm)) ||
                        (tNorm.isNotBlank() && name.contains(tNorm))
            } else {
                ownerType in listOf("v_y_r_cb", "vr", "v&r", "cb") ||
                        cat == "V&R" ||
                        name.contains("v&r") || name.contains("cb") || name.contains("san jaime") ||
                        ownerName.contains("v&r") || ownerName.contains("cb") ||
                        (tNorm.isNotBlank() && ownerName.contains(tNorm))
            }
        }
    }

    // Identificar partes de trabajo pertenecientes a esta entidad
    val cbWorkParts = remember(allWorkParts, cbOrchards, targetOwnerCategory, titularName) {
        val cbOrchardIds = cbOrchards.map { it.id }.toSet()
        val cbOrchardNames = cbOrchards.map { it.name.trim().lowercase(Locale.getDefault()) }.toSet()
        val tNorm = titularName.trim().lowercase(Locale.getDefault())

        allWorkParts.filter { part ->
            val category = part.ownerCategory.trim()
            val catNormalized = category.lowercase(Locale.getDefault())
            val oName = part.orchardName.trim().lowercase(Locale.getDefault())
            val obs = part.observations.lowercase(Locale.getDefault())

            if (targetOwnerCategory == "Otros") {
                catNormalized in listOf("otros", "otro") ||
                        (tNorm.isNotBlank() && catNormalized.contains(tNorm)) ||
                        cbOrchardIds.contains(part.orchardId) ||
                        cbOrchardNames.contains(oName) ||
                        (tNorm.isNotBlank() && oName.contains(tNorm)) ||
                        (tNorm.isNotBlank() && obs.contains(tNorm))
            } else {
                catNormalized in listOf("v&r", "v&r c.b.", "vr", "v_y_r_cb", "cb") ||
                        catNormalized.contains("v&r") ||
                        (tNorm.isNotBlank() && catNormalized.contains(tNorm)) ||
                        cbOrchardIds.contains(part.orchardId) ||
                        cbOrchardNames.contains(oName) ||
                        oName.contains("v&r") || oName.contains("cb") || oName.contains("san jaime") ||
                        obs.contains("v&r") || obs.contains("c.b.") || obs.contains("cb ")
            }
        }
    }

    // Filtrar partes de trabajo según el mes/año o modo seleccionado y huerto
    val filteredCbParts = remember(
        cbWorkParts,
        selectedMonthIndex,
        selectedYear,
        isFullYearMode,
        isAllTimeMode,
        selectedHuertoFilter,
        defaultAllHuertosLabel
    ) {
        cbWorkParts.filter { part ->
            // Filtro de huerto
            val huertoMatch = selectedHuertoFilter == defaultAllHuertosLabel ||
                    selectedHuertoFilter == "Todos los Huertos C.B." ||
                    selectedHuertoFilter == "Todos los Huertos" ||
                    part.orchardName.contains(selectedHuertoFilter, ignoreCase = true)

            // Filtro de fecha
            val cal = Calendar.getInstance().apply { timeInMillis = part.dateTimestamp }
            val partMonth = cal.get(Calendar.MONTH)
            val partYear = cal.get(Calendar.YEAR)

            val dateMatch = when {
                isAllTimeMode -> true
                isFullYearMode -> partYear == selectedYear
                else -> partYear == selectedYear && partMonth == selectedMonthIndex
            }

            huertoMatch && dateMatch
        }.sortedByDescending { it.dateTimestamp }
    }

    // Cálculos económicos para la liquidación
    val totalHoursWorked = remember(filteredCbParts) {
        filteredCbParts.sumOf { it.hours }
    }

    val totalLaborCost = remember(filteredCbParts) {
        filteredCbParts.sumOf { part ->
            when {
                part.type == "varios" -> 0.0
                part.hours > 0.0 && part.pricePerHour > 0.0 -> part.hours * part.pricePerHour
                part.hours > 0.0 -> part.totalCost
                else -> 0.0
            }
        }
    }

    // Materiales detallados extraídos de los partes
    val allMaterialItemsWithOrigin = remember(filteredCbParts) {
        val list = mutableListOf<Pair<WorkPartEntity, MaterialItem>>()
        filteredCbParts.forEach { part ->
            val items = MaterialsJsonHelper.fromJson(part.materialsJson)
            if (items.isNotEmpty()) {
                items.forEach { list.add(part to it) }
            } else if (part.type == "varios" && part.totalCost > 0.0) {
                list.add(
                    part to MaterialItem(
                        name = part.taskName.ifBlank { "Gasto de Material / Varios C.B." },
                        quantity = 1.0,
                        unitPrice = part.totalCost,
                        isAdvancedByMe = false
                    )
                )
            }
        }
        list
    }

    val totalMaterialsCost = remember(allMaterialItemsWithOrigin) {
        allMaterialItemsWithOrigin.sumOf { it.second.totalCost }
    }

    // Desglose de materiales: Adelantados de mi bolsillo vs Pagados por cuenta de la C.B.
    val materialsAdelantadosCost = remember(allMaterialItemsWithOrigin) {
        allMaterialItemsWithOrigin.filter { it.second.isAdvancedByMe }.sumOf { it.second.totalCost }
    }

    val materialsCuentaCbCost = remember(allMaterialItemsWithOrigin) {
        allMaterialItemsWithOrigin.filter { !it.second.isAdvancedByMe }.sumOf { it.second.totalCost }
    }

    // Total a cobrar/liquidar a la C.B. (Mano de obra + Materiales adelantados de mi bolsillo)
    val totalToBillToCb = remember(totalLaborCost, materialsAdelantadosCost) {
        totalLaborCost + materialsAdelantadosCost
    }

    // Título descriptivo del período para exportación y encabezados
    val periodDescription = remember(selectedMonthIndex, selectedYear, isFullYearMode, isAllTimeMode) {
        when {
            isAllTimeMode -> "Histórico Completo"
            isFullYearMode -> "Año $selectedYear"
            else -> "${monthNames[selectedMonthIndex]} $selectedYear"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // SELECTOR DE PERÍODO Y MES (Específico para liquidación mensual)
        // =====================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
            shape = RoundedCornerShape(14.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fila de Chips Rápidos
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentMonth = currentCal.get(Calendar.MONTH)
                    val currentYear = currentCal.get(Calendar.YEAR)

                    // Chip Mes Actual
                    PeriodPill(
                        label = "Mes Actual",
                        selected = !isFullYearMode && !isAllTimeMode && selectedMonthIndex == currentMonth && selectedYear == currentYear,
                        onClick = {
                            isFullYearMode = false
                            isAllTimeMode = false
                            selectedMonthIndex = currentMonth
                            selectedYear = currentYear
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Chip Mes Anterior
                    val prevMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    val prevMonth = prevMonthCal.get(Calendar.MONTH)
                    val prevYear = prevMonthCal.get(Calendar.YEAR)
                    PeriodPill(
                        label = "Mes Anterior",
                        selected = !isFullYearMode && !isAllTimeMode && selectedMonthIndex == prevMonth && selectedYear == prevYear,
                        onClick = {
                            isFullYearMode = false
                            isAllTimeMode = false
                            selectedMonthIndex = prevMonth
                            selectedYear = prevYear
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Chip Todo el Año
                    PeriodPill(
                        label = "Año $selectedYear",
                        selected = isFullYearMode && !isAllTimeMode,
                        onClick = {
                            isFullYearMode = true
                            isAllTimeMode = false
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Chip Todo
                    PeriodPill(
                        label = "Histórico",
                        selected = isAllTimeMode,
                        onClick = {
                            isAllTimeMode = true
                            isFullYearMode = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Navegador Mes a Mes con Flechas
                if (!isAllTimeMode && !isFullYearMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceWhite)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonthIndex == 0) {
                                    selectedMonthIndex = 11
                                    selectedYear -= 1
                                } else {
                                    selectedMonthIndex -= 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Mes anterior",
                                tint = PrimaryGreen
                            )
                        }

                        // Menú desplegable para cambiar de mes directamente
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { monthDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${monthNames[selectedMonthIndex]} $selectedYear",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = monthDropdownExpanded,
                                onDismissRequest = { monthDropdownExpanded = false }
                            ) {
                                monthNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "$name $selectedYear",
                                                fontWeight = if (index == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal,
                                                color = if (index == selectedMonthIndex) PrimaryGreenDark else OnSurface
                                            )
                                        },
                                        onClick = {
                                            selectedMonthIndex = index
                                            monthDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                if (selectedMonthIndex == 11) {
                                    selectedMonthIndex = 0
                                    selectedYear += 1
                                } else {
                                    selectedMonthIndex += 1
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Mes siguiente",
                                tint = PrimaryGreen
                            )
                        }
                    }
                }

                // Filtro por Huerto
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Filtrar por Huerto ($tabDisplayName)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = huertoDropdownExpanded,
                        onExpandedChange = { huertoDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedHuertoFilter,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = huertoDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = OutlineVariant
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = huertoDropdownExpanded,
                            onDismissRequest = { huertoDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(defaultAllHuertosLabel) },
                                onClick = {
                                    selectedHuertoFilter = defaultAllHuertosLabel
                                    huertoDropdownExpanded = false
                                }
                            )
                            cbOrchards.forEach { orchard ->
                                DropdownMenuItem(
                                    text = { Text(orchard.name) },
                                    onClick = {
                                        selectedHuertoFilter = orchard.name
                                        huertoDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TARJETA PRINCIPAL KPI: TOTAL A COBRAR
        // =====================================================================
        val isPositive = totalToBillToCb >= 0
        val kpiContainerColor = if (isPositive) {
            if (isDark) Color(0xFF142E15) else PrimaryGreenContainer.copy(alpha = 0.35f)
        } else {
            if (isDark) Color(0xFF1F1618) else Color(0xFFFBF5F6)
        }
        val kpiBorderColor = if (isPositive) {
            if (isDark) Color(0xFF4CAF50) else PrimaryGreen.copy(alpha = 0.3f)
        } else {
            if (isDark) Color(0xFF5E333C) else Color(0xFFE6D5D8)
        }
        val kpiTitleColor = if (isPositive) {
            PrimaryGreenDark
        } else {
            if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
        }
        val kpiValueColor = if (isPositive) {
            PrimaryGreen
        } else {
            if (isDark) Color(0xFFFF5252) else Color(0xFFD32F2F)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = kpiContainerColor
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(kpiBorderColor)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL A COBRAR (${tabDisplayName.uppercase()})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = kpiTitleColor,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = periodDescription,
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }

                // Cifra grande destacada sin cortes en pantallas estrechas
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val signPrefix = if (totalToBillToCb > 0) "+ " else if (totalToBillToCb < 0) "- " else ""
                    Text(
                        text = "$signPrefix€ %,.2f".format(kotlin.math.abs(totalToBillToCb)),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = kpiValueColor
                    )
                    Text(
                        text = "A percibir por el socio",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                }

                Divider(color = OutlineVariant.copy(alpha = 0.5f))

                // Desglose limpio, legible y adaptativo
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Mano de Obra
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen)
                            )
                            val hoursFormatted = if (totalHoursWorked % 1.0 == 0.0) totalHoursWorked.toInt().toString() else "%.1f".format(totalHoursWorked)
                            Column {
                                Text(
                                    text = "Mano de Obra ($hoursFormatted h)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                                Text(
                                    text = "Trabajo propio dedicado",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "€ %,.2f".format(totalLaborCost),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }

                    // Adelantado por mí
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD97706))
                            )
                            val adelantadosCount = allMaterialItemsWithOrigin.count { it.second.isAdvancedByMe }
                            Column {
                                Text(
                                    text = "Adelantado por mí ($adelantadosCount ítems)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                )
                                Text(
                                    text = "Materiales pagados de mi bolsillo",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "€ %,.2f".format(materialsAdelantadosCost),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                        )
                    }

                    // Pagado cuenta C.B.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(OnSurfaceVariant.copy(alpha = 0.6f))
                            )
                            val cuentaCbCount = allMaterialItemsWithOrigin.count { !it.second.isAdvancedByMe }
                            Column {
                                Text(
                                    text = "Cuenta $titularName ($cuentaCbCount ítems)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariant
                                )
                                Text(
                                    text = "Pagado directo por la sociedad",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            text = "€ %,.2f".format(materialsCuentaCbCost),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }

        // =====================================================================
        // BOTÓN PRINCIPAL DE EXPORTACIÓN A EXCEL (PERÍODO ACTUAL)
        // =====================================================================
        OutlinedButton(
            onClick = {
                CbWorkExcelExportHelper.exportCbSettlementExcel(
                    context = context,
                    periodTitle = periodDescription,
                    parts = filteredCbParts,
                    orchards = cbOrchards,
                    laborCostTotal = totalLaborCost,
                    materialsCostTotal = materialsAdelantadosCost,
                    totalToBill = totalToBillToCb,
                    totalHours = totalHoursWorked,
                    entityName = tabDisplayName
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 54.dp)
                .testTag("export_cb_excel_button"),
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
                        text = "Descargar Excel Liquidación $tabDisplayName",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreenDark
                    )
                    Text(
                        text = "Informe .xlsx multichoja ($periodDescription)",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // =====================================================================
        // SECCIÓN DESPLEGABLE: MATERIALES E INSUMOS APORTADOS
        // =====================================================================
        if (allMaterialItemsWithOrigin.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMaterialsBreakdown = !showMaterialsBreakdown },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = SecondaryOrangeDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Materiales e Insumos Aportados",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = "${allMaterialItemsWithOrigin.size} ítems • Total: € %,.2f".format(totalMaterialsCost),
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = if (showMaterialsBreakdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showMaterialsBreakdown) "Ocultar" else "Mostrar",
                            tint = OnSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showMaterialsBreakdown) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Divider(color = OutlineVariant, modifier = Modifier.padding(vertical = 4.dp))
                            allMaterialItemsWithOrigin.forEach { (part, item) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceWhite)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OnSurface,
                                                modifier = Modifier.weight(1f, fill = false),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            if (item.isAdvancedByMe) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = "Adelantado",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${part.orchardName} • ${item.quantity} uds @ € %.2f • ${if (item.isAdvancedByMe) "A reembolsar" else "Cargo $titularName"}".format(item.unitPrice),
                                            fontSize = 10.sp,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "€ %,.2f".format(item.totalCost),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isAdvancedByMe) (if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)) else OnSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // LISTADO DE PARTES DE TRABAJO REGISTRADOS PARA LA C.B.
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(PrimaryGreen)
                )
                Text(
                    text = "Partes de Trabajo $tabDisplayName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${filteredCbParts.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreenDark
                    )
                }
            }

            if (filteredCbParts.isNotEmpty()) {
                Text(
                    text = "Desliza para eliminar",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant
                )
            }
        }

        if (filteredCbParts.isEmpty()) {
            // Estado Vacío
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Sin trabajos de $tabDisplayName en este período",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )

                    Text(
                        text = "No se han encontrado partes de trabajo o materiales asignados a $tabDisplayName para $periodDescription.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isAllTimeMode = true
                                isFullYearMode = false
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ver Histórico", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.navigateTo(Screen.WorkParts()) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nuevo Parte ($tabDisplayName)", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Lista de tarjetas
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredCbParts.forEach { part ->
                    key(part.id) {
                        CbWorkPartCard(
                            part = part,
                            onDeleteRequest = { partToDelete = part },
                            onEditRequest = {
                                viewModel.navigateTo(
                                    Screen.WorkParts(editWorkPartId = part.id)
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Diálogo de Confirmación de Eliminación
    if (partToDelete != null) {
        val part = partToDelete!!
        AlertDialog(
            onDismissRequest = { partToDelete = null },
            title = {
                Text(
                    text = "Eliminar Registro ($tabDisplayName)",
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar '${part.taskName}' de ${part.orchardName}" +
                            (if (part.totalCost > 0) " por importe de %.2f €".format(part.totalCost) else "") +
                            "? Esta operación se restará inmediatamente de la liquidación de $tabDisplayName.",
                    color = OnSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkPart(part)
                        partToDelete = null
                        Toast.makeText(context, "Parte eliminado", Toast.LENGTH_SHORT).show()
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
private fun PeriodPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryGreenDark else SurfaceWhite)
            .border(1.dp, if (selected) PrimaryGreenDark else OutlineVariant, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 8.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else OnSurface,
            maxLines = 1
        )
    }
}

/**
 * Tarjeta especializada de parte de trabajo para la C.B.
 */
@Composable
private fun CbWorkPartCard(
    part: WorkPartEntity,
    onDeleteRequest: () -> Unit,
    onEditRequest: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe_offset_cb")

    val materials = remember(part.materialsJson) {
        MaterialsJsonHelper.fromJson(part.materialsJson)
    }

    // Coste de mano de obra del parte
    val laborCost = remember(part) {
        when {
            part.type == "varios" -> 0.0
            part.hours > 0.0 && part.pricePerHour > 0.0 -> part.hours * part.pricePerHour
            part.hours > 0.0 -> part.totalCost
            else -> 0.0
        }
    }

    // Coste de materiales adelantados por mí en este parte
    val materialsAdelantadosPart = remember(materials) {
        materials.filter { it.isAdvancedByMe }.sumOf { it.totalCost }
    }

    // Importe que el socio cobra a la C.B. por este parte concreto
    val amountToBillForPart = remember(laborCost, materialsAdelantadosPart, part) {
        if (materials.isNotEmpty()) {
            laborCost + materialsAdelantadosPart
        } else {
            part.totalCost
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Fondo rojo al deslizar a la izquierda
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(if (offsetX < -30) Color(0xFFDC2626) else SurfaceContainerLow)
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

        // Tarjeta principal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
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
                // Fila Superior: Icono, Tarea, Fecha e Importe a Cobrar
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
                        val visualInfo = getFarmTaskVisualInfo(part.taskName)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) visualInfo.iconColor.copy(alpha = 0.2f) else visualInfo.containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = visualInfo.icon,
                                contentDescription = part.taskName,
                                tint = visualInfo.iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = part.taskName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(part.dateTimestamp))
                            Text(
                                text = formattedDate,
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }

                    // Importe a Cobrar y Botón Editar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "€ %,.2f".format(amountToBillForPart),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryGreenDark
                            )
                            Text(
                                text = "a cobrar",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceVariant
                            )
                        }

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

                // Fila de Huerto y Horas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Huerto C.B.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = part.orchardName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurface
                        )
                    }

                    // Horas de trabajo
                    if (part.hours > 0.0) {
                        val hoursStr = if (part.hours % 1.0 == 0.0) part.hours.toInt().toString() else "%.1f".format(part.hours)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(13.dp)
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
                    }
                }

                // Materiales asociados a este parte (si los hubiera)
                if (materials.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerLow.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = SecondaryOrangeDark,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Materiales incluidos (${materials.size}):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface
                            )
                        }

                        materials.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "• ${m.name} (${m.quantity} uds)",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                    if (m.isAdvancedByMe) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "Adelantado",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "€ %,.2f".format(m.totalCost),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (m.isAdvancedByMe) Color(0xFFB45309) else OnSurface
                                )
                            }
                        }
                    }
                }

                // Observaciones
                if (part.observations.isNotBlank()) {
                    Text(
                        text = part.observations,
                        fontSize = 11.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
