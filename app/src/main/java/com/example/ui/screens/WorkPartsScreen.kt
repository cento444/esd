package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.MaterialItem
import com.example.data.model.MaterialsJsonHelper
import com.example.ui.util.AppKeyboards
import com.example.ui.util.ImageStorageHelper
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppTopHeader
import com.example.ui.components.FruitVarietyDropdown
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.components.rememberPhotoPickerHandler
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.DatePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkPartsScreen(
    initialTab: String = "tareas",
    preselectedOrchardId: Long? = null,
    preselectedTaskType: String? = null,
    editWorkPartId: Long? = null,
    preselectedProductName: String? = null,
    preselectedProductDose: String? = null,
    preselectedProductSafetyDays: Int? = null,
    preselectedActiveSubstance: String? = null,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val allWorkParts by viewModel.allWorkParts.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    val editingPart = remember(editWorkPartId, allWorkParts) {
        if (editWorkPartId != null) allWorkParts.find { it.id == editWorkPartId } else null
    }
    val isEditing = editingPart != null

    val tabs = listOf("Tareas", "Goteo", "Producción", "Varios")
    val initialPageIndex = when {
        editingPart != null -> when (editingPart.type.lowercase()) {
            "goteo" -> 1
            "produccion" -> 2
            "varios" -> 3
            else -> 0
        }
        else -> when (initialTab.lowercase()) {
            "goteo" -> 1
            "produccion" -> 2
            "varios" -> 3
            else -> 0
        }
    }
    val pagerState = rememberPagerState(initialPage = initialPageIndex) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(initialTab, editWorkPartId) {
        if (editingPart != null) {
            val targetPage = when (editingPart.type.lowercase()) {
                "goteo" -> 1
                "produccion" -> 2
                "varios" -> 3
                else -> 0
            }
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        } else {
            val targetPage = when (initialTab.lowercase()) {
                "goteo" -> 1
                "produccion" -> 2
                "varios" -> 3
                else -> 0
            }
            if (pagerState.currentPage != targetPage) {
                pagerState.scrollToPage(targetPage)
            }
        }
    }

    var selectedOrchardId by remember(preselectedOrchardId, editingPart?.orchardId) {
        mutableStateOf(editingPart?.orchardId ?: preselectedOrchardId ?: allOrchards.firstOrNull()?.id ?: 1L)
    }

    LaunchedEffect(preselectedOrchardId) {
        if (preselectedOrchardId != null && editingPart == null) {
            selectedOrchardId = preselectedOrchardId
        }
    }

    val selectedOrchard = allOrchards.find { it.id == selectedOrchardId } ?: allOrchards.firstOrNull()
    val orchardName = selectedOrchard?.name ?: "Seleccionar Huerto"
    val orchardOwnerCategory = when (selectedOrchard?.ownerType) {
        "v_y_r_cb" -> userProfile.titularVr.ifEmpty { "V&R" }
        "otros" -> userProfile.titularOtros.ifEmpty { "Otros" }
        else -> userProfile.titularPropios.ifEmpty { "Mío" }
    }

    // Find latest goteo for the currently selected orchard
    val latestGoteoForOrchard = remember(selectedOrchardId, allWorkParts) {
        allWorkParts
            .filter { it.orchardId == selectedOrchardId && it.type == "goteo" }
            .maxByOrNull { it.dateTimestamp }
    }

    // Tab 1: Tareas State
    var tipoTarea by remember(preselectedTaskType) {
        mutableStateOf(preselectedTaskType ?: "Poda y aclareo")
    }

    LaunchedEffect(preselectedTaskType) {
        if (!preselectedTaskType.isNullOrBlank() && editingPart == null) {
            tipoTarea = preselectedTaskType
        }
    }

    var tipoTareaExpanded by remember { mutableStateOf(false) }
    var horas by remember { mutableStateOf("") }
    var precioHora by remember { mutableStateOf("") }
    var isOwnLabor by remember { mutableStateOf(selectedOrchard?.ownerType?.lowercase() in listOf("propios", "propiedad")) }

    // Auto-marcar Horas Propias si se cambia de huerto a uno de Propiedad (solo en partes nuevos)
    LaunchedEffect(selectedOrchardId) {
        if (editingPart == null) {
            val isProp = selectedOrchard?.ownerType?.lowercase() in listOf("propios", "propiedad")
            isOwnLabor = isProp
        }
    }
    var costeDirectoTareas by remember { mutableStateOf("") }
    var addedMaterials by remember { mutableStateOf<List<MaterialItem>>(emptyList()) }
    var productoMaterial by remember { mutableStateOf("") }
    var cantidadMaterial by remember { mutableStateOf("") }
    var precioMaterial by remember { mutableStateOf("") }
    var materialAdelantadoPorMi by remember { mutableStateOf(false) }
    var materialInputError by remember { mutableStateOf<String?>(null) }
    var materialFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var observacionesTareas by remember { mutableStateOf("") }
    var selectedPhotoTareas by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(preselectedProductName) {
        if (!preselectedProductName.isNullOrBlank() && editingPart == null) {
            productoMaterial = preselectedProductName
            tipoTarea = "Tratamiento fitosanitario"
            val dose = preselectedProductDose ?: "Consultar ficha"
            val safety = preselectedProductSafetyDays ?: 0
            val plazoStr = if (safety == 0) "0 días" else "$safety días"
            materialFeedbackMessage = "$preselectedProductName • Dosis: $dose • Plazo: $plazoStr"
            val substanceStr = if (!preselectedActiveSubstance.isNullOrBlank()) " ($preselectedActiveSubstance)" else ""
            if (observacionesTareas.isBlank()) {
                observacionesTareas = "Tratamiento fitosanitario: $preselectedProductName$substanceStr. Dosis: $dose. Plazo seg.: $plazoStr."
            } else if (!observacionesTareas.contains(preselectedProductName)) {
                observacionesTareas = "$observacionesTareas\n[Fitosanitario: $preselectedProductName, Dosis: $dose, Plazo: $plazoStr]"
            }
        }
    }

    // Tab 2: Goteo State
    var gastoMensual by remember { mutableStateOf("") }
    var repetirMensual by remember { mutableStateOf(false) }
    var observacionesGoteo by remember { mutableStateOf("") }
    var selectedPhotoGoteo by remember { mutableStateOf<String?>(null) }

    // Automatic pre-fill of recurring monthly goteo per specific orchard (when not editing)
    LaunchedEffect(selectedOrchardId, latestGoteoForOrchard?.id) {
        if (editingPart == null) {
            if (latestGoteoForOrchard != null && latestGoteoForOrchard.isMonthlyRepeat) {
                if (latestGoteoForOrchard.totalCost > 0) {
                    gastoMensual = if (latestGoteoForOrchard.totalCost % 1.0 == 0.0) {
                        latestGoteoForOrchard.totalCost.toInt().toString()
                    } else {
                        "%.2f".format(Locale.US, latestGoteoForOrchard.totalCost)
                    }
                }
                repetirMensual = true
            } else {
                gastoMensual = ""
                repetirMensual = false
            }
            observacionesGoteo = ""
            selectedPhotoGoteo = null
        }
    }

    // Tab 3: Varios State
    var conceptoVarios by remember { mutableStateOf("") }
    var importeVarios by remember { mutableStateOf("") }
    var observacionesVarios by remember { mutableStateOf("") }
    var selectedPhotoVarios by remember { mutableStateOf<String?>(null) }
    var conceptoError by remember { mutableStateOf<String?>(null) }

    // Tab 4: Producción State
    val harvestDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var fechaCosechaTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var fechaCosechaText by remember { mutableStateOf(harvestDateFormat.format(Date())) }
    var variedadRecolectada by remember { mutableStateOf(selectedOrchard?.variety ?: "Clemenules") }
    LaunchedEffect(selectedOrchardId) {
        if (editingPart == null) {
            val orchard = allOrchards.find { it.id == selectedOrchardId }
            if (orchard != null) {
                variedadRecolectada = orchard.variety
            }
        }
    }
    var kilosTotales by remember { mutableStateOf("") }
    var precioPorKilo by remember { mutableStateOf("") }
    var tieneDestrio by remember { mutableStateOf(false) }
    var kilosDestrio by remember { mutableStateOf("") }
    var precioDestrio by remember { mutableStateOf("") }
    var tieneSeguro by remember { mutableStateOf(false) }
    var indemnizacionSeguro by remember { mutableStateOf("") }
    var observacionesProduccion by remember { mutableStateOf("") }
    var selectedPhotoProduccion by remember { mutableStateOf<String?>(null) }

    // Populate data when in edit mode
    LaunchedEffect(editingPart?.id) {
        if (editingPart != null) {
            if (editingPart.orchardId > 0) {
                selectedOrchardId = editingPart.orchardId
            }
            when (editingPart.type.lowercase()) {
                "tareas" -> {
                    tipoTarea = editingPart.taskName
                    horas = if (editingPart.hours > 0) {
                        if (editingPart.hours % 1.0 == 0.0) editingPart.hours.toInt().toString() else editingPart.hours.toString()
                    } else ""
                    precioHora = if (editingPart.pricePerHour > 0) {
                        if (editingPart.pricePerHour % 1.0 == 0.0) editingPart.pricePerHour.toInt().toString() else "%.2f".format(Locale.US, editingPart.pricePerHour)
                    } else ""
                    val laborAndMat = (editingPart.hours * editingPart.pricePerHour) + MaterialsJsonHelper.fromJson(editingPart.materialsJson).sumOf { it.totalCost }
                    val diff = editingPart.totalCost - laborAndMat
                    costeDirectoTareas = if (diff > 0.01) {
                        if (diff % 1.0 == 0.0) diff.toInt().toString() else "%.2f".format(Locale.US, diff)
                    } else ""
                    observacionesTareas = editingPart.observations
                    selectedPhotoTareas = editingPart.photoUri
                    addedMaterials = MaterialsJsonHelper.fromJson(editingPart.materialsJson)
                    isOwnLabor = editingPart.observations.contains("[HORAS_PROPIAS]") ||
                            (editingPart.ownerCategory.lowercase() in listOf("propios", "propiedad") && !editingPart.observations.contains("[HORAS_CONTRATADAS]"))
                }
                "goteo" -> {
                    gastoMensual = if (editingPart.totalCost > 0) {
                        if (editingPart.totalCost % 1.0 == 0.0) editingPart.totalCost.toInt().toString() else "%.2f".format(Locale.US, editingPart.totalCost)
                    } else ""
                    repetirMensual = editingPart.isMonthlyRepeat
                    observacionesGoteo = editingPart.observations
                    selectedPhotoGoteo = editingPart.photoUri
                }
                "produccion" -> {
                    fechaCosechaTimestamp = editingPart.dateTimestamp
                    fechaCosechaText = harvestDateFormat.format(Date(editingPart.dateTimestamp))
                    val varietyClean = editingPart.taskName.removePrefix("Producción ").trim()
                    if (varietyClean.isNotBlank()) {
                        variedadRecolectada = varietyClean
                    }
                    kilosTotales = if (editingPart.kilos > 0) {
                        if (editingPart.kilos % 1.0 == 0.0) editingPart.kilos.toInt().toString() else editingPart.kilos.toString()
                    } else ""
                    precioPorKilo = if (editingPart.pricePerKg > 0) {
                        if (editingPart.pricePerKg % 1.0 == 0.0) editingPart.pricePerKg.toInt().toString() else "%.2f".format(Locale.US, editingPart.pricePerKg)
                    } else ""
                    tieneDestrio = editingPart.kilosDestrio > 0
                    kilosDestrio = if (editingPart.kilosDestrio > 0) {
                        if (editingPart.kilosDestrio % 1.0 == 0.0) editingPart.kilosDestrio.toInt().toString() else editingPart.kilosDestrio.toString()
                    } else ""
                    precioDestrio = if (editingPart.precioDestrio > 0) {
                        if (editingPart.precioDestrio % 1.0 == 0.0) editingPart.precioDestrio.toInt().toString() else "%.2f".format(Locale.US, editingPart.precioDestrio)
                    } else ""
                    tieneSeguro = editingPart.indemnizacionSeguro > 0
                    indemnizacionSeguro = if (editingPart.indemnizacionSeguro > 0) {
                        if (editingPart.indemnizacionSeguro % 1.0 == 0.0) editingPart.indemnizacionSeguro.toInt().toString() else "%.2f".format(Locale.US, editingPart.indemnizacionSeguro)
                    } else ""
                    observacionesProduccion = editingPart.observations
                    selectedPhotoProduccion = editingPart.photoUri
                }
                "varios" -> {
                    conceptoVarios = editingPart.taskName
                    importeVarios = if (editingPart.totalCost > 0) {
                        if (editingPart.totalCost % 1.0 == 0.0) editingPart.totalCost.toInt().toString() else "%.2f".format(Locale.US, editingPart.totalCost)
                    } else ""
                    observacionesVarios = editingPart.observations
                    selectedPhotoVarios = editingPart.photoUri
                }
            }
        }
    }

    var showToastMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerHandlerTareas = rememberPhotoPickerHandler(prefix = "tarea") { localPath ->
        selectedPhotoTareas = localPath
    }

    val photoPickerHandlerGoteo = rememberPhotoPickerHandler(prefix = "goteo") { localPath ->
        selectedPhotoGoteo = localPath
    }

    val photoPickerHandlerVarios = rememberPhotoPickerHandler(prefix = "varios") { localPath ->
        selectedPhotoVarios = localPath
    }

    val photoPickerHandlerProduccion = rememberPhotoPickerHandler(prefix = "produccion") { localPath ->
        selectedPhotoProduccion = localPath
    }

    val taskTypes = listOf(
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

    Scaffold(
        topBar = {
            AppTopHeader(
                title = if (isEditing) "Editar Parte" else "Partes",
                showBack = isEditing,
                onBackClick = { viewModel.navigateBack() },
                showMenu = !isEditing,
                onMenuClick = { viewModel.navigateTo(Screen.Settings) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceBright)
        ) {
            // Edit Banner
            if (isEditing) {
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Modificando parte existente: ${editingPart?.taskName ?: ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = { viewModel.navigateBack() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // Tabs Bar: Tareas | Goteo | Producción | Varios (Píldoras redondeadas igual que Inicio y Finanzas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineVariant, RoundedCornerShape(50.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    WorkPartTab(
                        title = title,
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Toast Message notification
            if (showToastMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PrimaryFixedDim)
                        Text(text = showToastMessage ?: "", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            // HorizontalPager for swipeable tab content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> {
                        // TAB 1: TAREAS
                        val h = horas.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val p = precioHora.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val directC = costeDirectoTareas.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val laborCost = h * p
                        val materialsListCost = addedMaterials.sumOf { it.totalCost }
                        val currentInputQty = cantidadMaterial.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val currentInputPrice = precioMaterial.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val currentInputCost = if (productoMaterial.isNotBlank()) currentInputQty * currentInputPrice else 0.0
                        val totalCost = laborCost + directC + materialsListCost + currentInputCost

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                        ) {
                            item {
                                OrchardSelectorCard(
                                    orchardName = orchardName,
                                    allOrchards = allOrchards,
                                    titularPropios = userProfile.titularPropios,
                                    titularVr = userProfile.titularVr,
                                    titularOtros = userProfile.titularOtros,
                                    onOrchardSelected = { orchard ->
                                        selectedOrchardId = orchard.id
                                        variedadRecolectada = orchard.variety
                                    }
                                )
                            }
                            item {
                                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Tipo de Tarea Dropdown
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Tipo de Tarea", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                ExposedDropdownMenuBox(
                                    expanded = tipoTareaExpanded,
                                    onExpandedChange = { tipoTareaExpanded = !tipoTareaExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = tipoTarea,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoTareaExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = appOutlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = tipoTareaExpanded,
                                        onDismissRequest = { tipoTareaExpanded = false }
                                    ) {
                                        val displayTaskTypes = if (tipoTarea.isNotBlank() && !taskTypes.contains(tipoTarea)) {
                                            listOf(tipoTarea) + taskTypes
                                        } else {
                                            taskTypes
                                        }
                                        displayTaskTypes.forEach { item ->
                                            val vis = getFarmTaskVisualInfo(item)
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
                                                    tipoTarea = item
                                                    tipoTareaExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Selector de Mano de Obra: Horas Propias (Tu trabajo / No pagadas en caja) vs Contratada
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tipo de mano de obra:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceVariant
                                    )
                                    if (selectedOrchard?.ownerType?.lowercase() in listOf("propios", "propiedad")) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0F2FE)
                                        ) {
                                            Text(
                                                text = "Huerto en propiedad",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF0369A1),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Opción 1: Horas Propias (Tu trabajo personal)
                                    OutlinedButton(
                                        onClick = { isOwnLabor = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isOwnLabor) (if (isDark) Color(0xFF1E3A5F) else Color(0xFFE0F2FE)) else Color.Transparent,
                                            contentColor = if (isOwnLabor) (if (isDark) Color(0xFF93C5FD) else Color(0xFF0284C7)) else OnSurfaceVariant
                                        ),
                                        border = BorderStroke(
                                            width = if (isOwnLabor) 1.5.dp else 1.dp,
                                            color = if (isOwnLabor) (if (isDark) Color(0xFF60A5FA) else Color(0xFF0284C7)) else OutlineVariant
                                        ),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isOwnLabor) Icons.Default.CheckCircle else Icons.Default.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "Horas Propias",
                                                    fontWeight = if (isOwnLabor) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Text(
                                                text = "Mi trabajo (rentabilidad)",
                                                fontSize = 9.sp,
                                                color = if (isOwnLabor) (if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1)) else OnSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    // Opción 2: Mano de Obra Contratada / Externa (Gasto real de caja)
                                    OutlinedButton(
                                        onClick = { isOwnLabor = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (!isOwnLabor) (if (isDark) Color(0xFF332014) else Color(0xFFFEF3C7)) else Color.Transparent,
                                            contentColor = if (!isOwnLabor) (if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)) else OnSurfaceVariant
                                        ),
                                        border = BorderStroke(
                                            width = if (!isOwnLabor) 1.5.dp else 1.dp,
                                            color = if (!isOwnLabor) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else OutlineVariant
                                        ),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (!isOwnLabor) Icons.Default.CheckCircle else Icons.Default.Group,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "Contratadas",
                                                    fontWeight = if (!isOwnLabor) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Text(
                                                text = "Pagadas (gasto de caja)",
                                                fontSize = 9.sp,
                                                color = if (!isOwnLabor) (if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)) else OnSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Horas & Precio/Hora
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = horas,
                                    onValueChange = { horas = it },
                                    label = { Text("Horas de trabajo", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("Ej. 4", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Decimal
                                )

                                OutlinedTextField(
                                    value = precioHora,
                                    onValueChange = { precioHora = it },
                                    label = { Text(if (isOwnLabor) "Valor/Hora estim. (€)" else "Precio/Hora (€)", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("Ej. 15.00", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Decimal
                                )
                            }

                             // Coste directo / Importe fijo (para seguros, etc. sin necesidad de horas)
                             Column(modifier = Modifier.fillMaxWidth()) {
                                 Text(text = "Coste / Importe fijo (€) (opcional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                 Spacer(modifier = Modifier.height(4.dp))
                                 OutlinedTextField(
                                     value = costeDirectoTareas,
                                     onValueChange = { costeDirectoTareas = it },
                                     placeholder = { Text("Ej. 150.00 (ideal para Seguro agrícola)", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                     modifier = Modifier.fillMaxWidth(),
                                     shape = RoundedCornerShape(8.dp),
                                     singleLine = true,
                                     colors = appOutlinedTextFieldColors(),
                                     keyboardOptions = AppKeyboards.Decimal
                                 )
                             }

                            HorizontalDivider(thickness = 1.dp, color = OutlineVariant)

                            // MATERIALES SECTION CARD
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OutlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Eco,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Materiales / Fitosanitarios",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnSurface
                                            )
                                        }
                                        if (addedMaterials.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = PrimaryGreenContainer
                                            ) {
                                                Text(
                                                    text = "${addedMaterials.size} en lista (%.2f €)".format(materialsListCost),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PrimaryGreen,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // List of already added materials with clear cards and Edit/Delete buttons
                                    if (addedMaterials.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Materiales incluidos en este parte:",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = OnSurfaceVariant
                                            )
                                            addedMaterials.forEachIndexed { index, mat ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, if (mat.isAdvancedByMe) Color(0xFFF59E0B) else PrimaryGreen.copy(alpha = 0.4f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = mat.name,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = OnSurface,
                                                                    modifier = Modifier.weight(1f, fill = false),
                                                                    maxLines = 1,
                                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                )
                                                                if (mat.isAdvancedByMe) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7)
                                                                    ) {
                                                                        Text(
                                                                            text = "Adelantado por mí",
                                                                            fontSize = 9.sp,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            val qtyStr = if (mat.quantity % 1.0 == 0.0) mat.quantity.toInt().toString() else mat.quantity.toString()
                                                            Text(
                                                                text = "$qtyStr un/L × %.2f € = %.2f €".format(mat.unitPrice, mat.totalCost),
                                                                fontSize = 11.sp,
                                                                color = if (mat.isAdvancedByMe) (if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)) else PrimaryGreen,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            IconButton(
                                                                onClick = {
                                                                    // Put back into fields for editing
                                                                    productoMaterial = mat.name
                                                                    val qStr = if (mat.quantity % 1.0 == 0.0) mat.quantity.toInt().toString() else mat.quantity.toString()
                                                                    cantidadMaterial = qStr
                                                                    precioMaterial = mat.unitPrice.toString()
                                                                    materialAdelantadoPorMi = mat.isAdvancedByMe
                                                                    addedMaterials = addedMaterials.filterIndexed { i, _ -> i != index }
                                                                    materialFeedbackMessage = "Modificando '${mat.name}'. Pulsa añadir cuando termines."
                                                                },
                                                                modifier = Modifier.size(30.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = "Editar material",
                                                                    tint = OnSurfaceVariant,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    addedMaterials = addedMaterials.filterIndexed { i, _ -> i != index }
                                                                    materialFeedbackMessage = null
                                                                },
                                                                modifier = Modifier.size(30.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Eliminar material",
                                                                    tint = Color(0xFFDC2626),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Feedback message when a material is added
                                    if (materialFeedbackMessage != null) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = PrimaryGreenContainer.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = PrimaryGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = materialFeedbackMessage!!,
                                                    fontSize = 11.sp,
                                                    color = PrimaryGreen,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    // Input fields for product
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(text = "Producto / Material", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                        OutlinedTextField(
                                            value = productoMaterial,
                                            onValueChange = {
                                                productoMaterial = it
                                                if (it.isNotBlank()) {
                                                    materialInputError = null
                                                }
                                            },
                                            placeholder = { Text("Ej. Abono foliar / Sulfato / Fitosanitario", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            isError = materialInputError != null,
                                            colors = appOutlinedTextFieldColors(),
                                            keyboardOptions = AppKeyboards.Words
                                        )
                                        if (materialInputError != null) {
                                            Text(
                                                text = materialInputError!!,
                                                color = Color(0xFFDC2626),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = cantidadMaterial,
                                            onValueChange = { cantidadMaterial = it },
                                            label = { Text("Cantidad") },
                                            placeholder = { Text("Ej. 5", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            colors = appOutlinedTextFieldColors(),
                                            keyboardOptions = AppKeyboards.Decimal
                                        )

                                        OutlinedTextField(
                                            value = precioMaterial,
                                            onValueChange = { precioMaterial = it },
                                            label = { Text("Precio (€)") },
                                            placeholder = { Text("Ej. 12.50", color = OnSurfaceVariant.copy(alpha = 0.5f), fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            colors = appOutlinedTextFieldColors(),
                                            keyboardOptions = AppKeyboards.Decimal
                                        )
                                    }

                                    // Switch / Checkbox "¿Adelantado de mi bolsillo?"
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { materialAdelantadoPorMi = !materialAdelantadoPorMi },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (materialAdelantadoPorMi) (if (isDark) Color(0xFF3D2A10) else Color(0xFFFEF3C7)) else SurfaceContainerLow,
                                        border = BorderStroke(1.dp, if (materialAdelantadoPorMi) Color(0xFFF59E0B) else OutlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Checkbox(
                                                    checked = materialAdelantadoPorMi,
                                                    onCheckedChange = { materialAdelantadoPorMi = it },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFFD97706),
                                                        checkmarkColor = Color.White
                                                    )
                                                )
                                                Column {
                                                    Text(
                                                        text = "Adelantado por mí (pagado de mi bolsillo)",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (materialAdelantadoPorMi) Color(0xFF92400E) else OnSurface
                                                    )
                                                    Text(
                                                        text = if (materialAdelantadoPorMi)
                                                            "Se sumará al total a cobrar a la C.B."
                                                        else
                                                            "Pagado directamente con la cuenta bancaria de la C.B.",
                                                        fontSize = 10.sp,
                                                        color = OnSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Button to Add to list
                                    Button(
                                        onClick = {
                                            val name = productoMaterial.trim()
                                            if (name.isEmpty()) {
                                                materialInputError = "Introduce el nombre del producto o material antes de añadirlo"
                                                return@Button
                                            }
                                            val qty = cantidadMaterial.replace(',', '.').toDoubleOrNull() ?: 1.0
                                            val price = precioMaterial.replace(',', '.').toDoubleOrNull() ?: 0.0
                                            val qtyStr = if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString()

                                            addedMaterials = addedMaterials + MaterialItem(
                                                name = name,
                                                quantity = qty,
                                                unitPrice = price,
                                                isAdvancedByMe = materialAdelantadoPorMi
                                            )
                                            val pagoTipo = if (materialAdelantadoPorMi) "Adelantado por ti" else "Cuenta C.B."
                                            val priceFormatted = "%.2f".format(Locale.US, price)
                                            materialFeedbackMessage = "✓ Guardado: '$name' ($qtyStr un/L × $priceFormatted € • $pagoTipo). Puedes añadir otro material si lo deseas."
                                            productoMaterial = ""
                                            cantidadMaterial = ""
                                            precioMaterial = ""
                                            materialAdelantadoPorMi = false
                                            materialInputError = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (productoMaterial.isNotBlank()) PrimaryGreen else SurfaceWhite,
                                            contentColor = if (productoMaterial.isNotBlank()) Color.White else PrimaryGreen
                                        ),
                                        border = if (productoMaterial.isBlank()) BorderStroke(1.dp, PrimaryGreen) else null
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (addedMaterials.isEmpty()) "+ Añadir este material a la lista" else "+ Añadir otro material a la lista",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Observaciones
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Observaciones", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = observacionesTareas,
                                    onValueChange = { observacionesTareas = it },
                                    placeholder = { Text("Añadir notas adicionales...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Text
                                )
                            }

                            // Total and Action Footer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = "Coste Total Estimado", fontSize = 13.sp, color = OnSurfaceVariant)
                                            val totalMats = addedMaterials.size + if (productoMaterial.isNotBlank()) 1 else 0
                                            if (totalMats > 0) {
                                                Text(
                                                    text = "Mano de obra (%.2f €) + $totalMats material(es)".format(laborCost),
                                                    fontSize = 11.sp,
                                                    color = OnSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "%.2f €".format(totalCost),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }

                                    if (selectedPhotoTareas != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = selectedPhotoTareas,
                                                contentDescription = "Foto Tarea",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        photoPickerHandlerTareas.openPicker()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(50.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = SurfaceWhite.copy(alpha = 0.9f),
                                                        contentColor = PrimaryGreen
                                                    )
                                                ) {
                                                    Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                FilledTonalIconButton(
                                                    onClick = { selectedPhotoTareas = null },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = Color.Red.copy(alpha = 0.9f),
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerHandlerTareas.openPicker()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(50.dp),
                                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                                brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = PrimaryGreen
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Añadir Foto / Actualización",
                                                color = PrimaryGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Las fotos añadidas aquí se publicarán automáticamente en el Área Social de Socios",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val finalMaterials = addedMaterials.toMutableList()
                                            if (productoMaterial.isNotBlank()) {
                                                val qty = cantidadMaterial.replace(',', '.').toDoubleOrNull() ?: 1.0
                                                val price = precioMaterial.replace(',', '.').toDoubleOrNull() ?: 0.0
                                                finalMaterials.add(
                                                    MaterialItem(
                                                        name = productoMaterial.trim(),
                                                        quantity = qty,
                                                        unitPrice = price,
                                                        isAdvancedByMe = materialAdelantadoPorMi
                                                    )
                                                )
                                            }
                                            val finalMaterialsJson = MaterialsJsonHelper.toJson(finalMaterials)
                                            val cleanObs = observacionesTareas
                                                .replace("[HORAS_PROPIAS]", "")
                                                .replace("[HORAS_CONTRATADAS]", "")
                                                .trim()
                                            val tag = if (isOwnLabor) "[HORAS_PROPIAS]" else "[HORAS_CONTRATADAS]"
                                            val finalObservations = if (cleanObs.isBlank()) tag else "$cleanObs $tag"

                                            viewModel.saveWorkPart(
                                                id = editWorkPartId ?: 0L,
                                                orchardId = selectedOrchardId,
                                                orchardName = orchardName,
                                                type = "tareas",
                                                taskName = tipoTarea,
                                                hours = h,
                                                pricePerHour = p,
                                                totalCost = totalCost,
                                                observations = finalObservations,
                                                materialsJson = finalMaterialsJson,
                                                photoUri = selectedPhotoTareas,
                                                ownerCategory = orchardOwnerCategory,
                                                publishToSocial = selectedPhotoTareas != null
                                            )
                                            showToastMessage = if (isEditing) "Parte actualizado correctamente" else "Parte guardado correctamente"
                                            if (isEditing) {
                                                viewModel.navigateBack()
                                            } else {
                                                horas = ""
                                                precioHora = ""
                                                val isProp = selectedOrchard?.ownerType?.lowercase() in listOf("propios", "propiedad")
                                                isOwnLabor = isProp
                                                productoMaterial = ""
                                                cantidadMaterial = ""
                                                precioMaterial = ""
                                                materialAdelantadoPorMi = false
                                                addedMaterials = emptyList()
                                                materialInputError = null
                                                materialFeedbackMessage = null
                                                observacionesTareas = ""
                                                selectedPhotoTareas = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("submit_work_part_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryGreen,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Text(
                                            text = if (isEditing) "Guardar Cambios" else "Guardar Parte",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        1 -> {
                        // TAB 2: GOTEO
                        val g = gastoMensual.replace(',', '.').toDoubleOrNull() ?: 0.0

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                        ) {
                            item {
                                OrchardSelectorCard(
                                    orchardName = orchardName,
                                    allOrchards = allOrchards,
                                    titularPropios = userProfile.titularPropios,
                                    titularVr = userProfile.titularVr,
                                    titularOtros = userProfile.titularOtros,
                                    onOrchardSelected = { orchard ->
                                        selectedOrchardId = orchard.id
                                        variedadRecolectada = orchard.variety
                                    }
                                )
                            }
                            item {
                                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Gasto Mensual", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = gastoMensual,
                                    onValueChange = { gastoMensual = it },
                                    placeholder = { Text("Ej. 120.00", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Decimal
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable { repetirMensual = !repetirMensual }
                            ) {
                                Checkbox(
                                    checked = repetirMensual,
                                    onCheckedChange = { repetirMensual = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                                )
                                Column {
                                    Text(
                                        text = "Repetir mensualmente",
                                        fontSize = 13.sp,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Aplica únicamente a $orchardName",
                                        fontSize = 11.sp,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Observaciones", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = observacionesGoteo,
                                    onValueChange = { observacionesGoteo = it },
                                    placeholder = { Text("Añadir notas sobre incidencias en la red de riego...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Text
                                )
                            }

                            // Total and Action Footer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Coste Agua Estimado", fontSize = 13.sp, color = OnSurfaceVariant)
                                        Text(
                                            text = "%.2f €".format(g),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }

                                    if (selectedPhotoGoteo != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = selectedPhotoGoteo,
                                                contentDescription = "Foto Riego",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        photoPickerHandlerGoteo.openPicker()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(50.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = SurfaceWhite.copy(alpha = 0.9f),
                                                        contentColor = PrimaryGreen
                                                    )
                                                ) {
                                                    Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                FilledTonalIconButton(
                                                    onClick = { selectedPhotoGoteo = null },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = Color.Red.copy(alpha = 0.9f),
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerHandlerGoteo.openPicker()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryGreen)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Añadir Foto / Actualización", color = PrimaryGreen)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.saveWorkPart(
                                                id = editWorkPartId ?: 0L,
                                                orchardId = selectedOrchardId,
                                                orchardName = orchardName,
                                                type = "goteo",
                                                taskName = "Gasto de Goteo",
                                                totalCost = g,
                                                observations = observacionesGoteo,
                                                isMonthlyRepeat = repetirMensual,
                                                photoUri = selectedPhotoGoteo,
                                                ownerCategory = orchardOwnerCategory,
                                                publishToSocial = selectedPhotoGoteo != null
                                            )
                                            showToastMessage = if (isEditing) {
                                                "Gasto de goteo actualizado"
                                            } else if (repetirMensual) {
                                                "Gasto guardado para $orchardName (se recordará mensualmente para este huerto)"
                                            } else {
                                                "Gasto de goteo guardado para $orchardName"
                                            }
                                            if (isEditing) {
                                                viewModel.navigateBack()
                                            } else {
                                                gastoMensual = ""
                                                observacionesGoteo = ""
                                                selectedPhotoGoteo = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Text(
                                            text = if (isEditing) "Guardar Cambios" else "Guardar Gasto",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        2 -> {
            // TAB 3: PRODUCCIÓN
            val k = kilosTotales.replace(',', '.').toDoubleOrNull() ?: 0.0
            val p = precioPorKilo.replace(',', '.').toDoubleOrNull() ?: 0.0
            val kd = if (tieneDestrio) (kilosDestrio.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
            val pd = if (tieneDestrio) (precioDestrio.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0
            val indSeguro = if (tieneSeguro) (indemnizacionSeguro.replace(',', '.').toDoubleOrNull() ?: 0.0) else 0.0

            val valorComercial = k * p
            val valorDestrio = kd * pd
            val valorEstimado = valorComercial + valorDestrio + indSeguro

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                item {
                    OrchardSelectorCard(
                        orchardName = orchardName,
                        allOrchards = allOrchards,
                        titularPropios = userProfile.titularPropios,
                        titularVr = userProfile.titularVr,
                        titularOtros = userProfile.titularOtros,
                        onOrchardSelected = { orchard ->
                            selectedOrchardId = orchard.id
                            variedadRecolectada = orchard.variety
                        }
                    )
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Fecha de Cosecha
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Fecha de Cosecha", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val cal = Calendar.getInstance().apply { timeInMillis = fechaCosechaTimestamp }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val newCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    }
                                                    fechaCosechaTimestamp = newCal.timeInMillis
                                                    fechaCosechaText = harvestDateFormat.format(newCal.time)
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                ) {
                                    OutlinedTextField(
                                        value = fechaCosechaText,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = fechaCosechaTimestamp }
                                                    DatePickerDialog(
                                                        context,
                                                        { _, year, month, dayOfMonth ->
                                                            val newCal = Calendar.getInstance().apply {
                                                                set(Calendar.YEAR, year)
                                                                set(Calendar.MONTH, month)
                                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                            }
                                                            fechaCosechaTimestamp = newCal.timeInMillis
                                                            fechaCosechaText = harvestDateFormat.format(newCal.time)
                                                        },
                                                        cal.get(Calendar.YEAR),
                                                        cal.get(Calendar.MONTH),
                                                        cal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = "Seleccionar fecha de cosecha",
                                                    tint = PrimaryGreen
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            FruitVarietyDropdown(
                                selectedVariety = variedadRecolectada,
                                onVarietySelected = { variedadRecolectada = it },
                                label = "Variedad Recolectada",
                                placeholder = "Seleccionar variedad de cítrico / aguacate...",
                                testTag = "work_part_harvest_variety_dropdown"
                            )

                            // Cosecha Comercial
                            Text(
                                text = "Kilos Comerciales (Almacén / Venta directa)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreenDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = kilosTotales,
                                    onValueChange = { kilosTotales = it },
                                    label = { Text("Kilos comerciales", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("Ej. 3500", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Decimal
                                )

                                OutlinedTextField(
                                    value = precioPorKilo,
                                    onValueChange = { precioPorKilo = it },
                                    label = { Text("Precio (€/kg)", maxLines = 1, softWrap = false) },
                                    placeholder = { Text("Ej. 0.32", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Decimal
                                )
                            }

                            HorizontalDivider(thickness = 0.8.dp, color = OutlineVariant)

                            // SECCIÓN DESTRÍO / MERMA / INDUSTRIA
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tieneDestrio = !tieneDestrio }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Destrío / Industria / Defectos",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Fruta pequeña, golpeada por granizo o enviada a zumo",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = tieneDestrio,
                                    onCheckedChange = { tieneDestrio = it }
                                )
                            }

                            if (tieneDestrio) {
                                Surface(
                                    color = SurfaceContainerLow,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = kilosDestrio,
                                                onValueChange = { kilosDestrio = it },
                                                label = { Text("Kilos destrío", maxLines = 1, softWrap = false) },
                                                placeholder = { Text("Ej. 450", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                colors = appOutlinedTextFieldColors(),
                                                keyboardOptions = AppKeyboards.Decimal
                                            )

                                            OutlinedTextField(
                                                value = precioDestrio,
                                                onValueChange = { precioDestrio = it },
                                                label = { Text("Precio (€/kg)", maxLines = 1, softWrap = false) },
                                                placeholder = { Text("Ej. 0.08 o 0", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true,
                                                colors = appOutlinedTextFieldColors(),
                                                keyboardOptions = AppKeyboards.Decimal
                                            )
                                        }

                                        if (kd > 0) {
                                            Text(
                                                text = if (pd > 0) "Importe destrío/zumo: +%.2f €".format(valorDestrio) else "Destrío sin abono (merma pura)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (pd > 0) PrimaryGreen else OnSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(thickness = 0.8.dp, color = OutlineVariant)

                            // SECCIÓN INDEMNIZACIÓN SEGURO (AGROSEGURO / GRANIZO)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tieneSeguro = !tieneSeguro }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Indemnización de Seguro (Siniestro / Granizo)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Pago de Agroseguro por pérdida de cosecha",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = tieneSeguro,
                                    onCheckedChange = { tieneSeguro = it }
                                )
                            }

                            if (tieneSeguro) {
                                Surface(
                                    color = SurfaceContainerLow,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Importe abonado por Agroseguro / Compañía (€)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurfaceVariant
                                        )
                                        OutlinedTextField(
                                            value = indemnizacionSeguro,
                                            onValueChange = { indemnizacionSeguro = it },
                                            placeholder = { Text("Ej. 1800.00", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            keyboardOptions = AppKeyboards.Decimal,
                                            colors = appOutlinedTextFieldColors(),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "Se sumará al ingreso del huerto como compensación por el siniestro.",
                                            fontSize = 11.sp,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Observaciones
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Observaciones", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = observacionesProduccion,
                                    onValueChange = { observacionesProduccion = it },
                                    placeholder = { Text("Añadir notas sobre la cosecha, calibre, comprador, peritaje de seguro...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Text
                                )
                            }

                            // Total and Action Footer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Desglose
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (valorComercial > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = "Cosecha Comercial (${k.toInt()} kg):", fontSize = 12.sp, color = OnSurfaceVariant)
                                                Text(text = "%.2f €".format(valorComercial), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                            }
                                        }
                                        if (tieneDestrio && valorDestrio > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = "Destrío / Zumo (${kd.toInt()} kg):", fontSize = 12.sp, color = OnSurfaceVariant)
                                                Text(text = "+%.2f €".format(valorDestrio), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                                            }
                                        }
                                        if (tieneSeguro && indSeguro > 0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = "Indemnización Seguro:", fontSize = 12.sp, color = OnSurfaceVariant)
                                                Text(text = "+%.2f €".format(indSeguro), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Total Liquidación / Ingreso", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnSurface)
                                        Text(
                                            text = "%.2f €".format(valorEstimado),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }

                                    if (selectedPhotoProduccion != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = selectedPhotoProduccion,
                                                contentDescription = "Foto Producción",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        photoPickerHandlerProduccion.openPicker()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(50.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = SurfaceWhite.copy(alpha = 0.9f),
                                                        contentColor = PrimaryGreen
                                                    )
                                                ) {
                                                    Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                FilledTonalIconButton(
                                                    onClick = { selectedPhotoProduccion = null },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = Color.Red.copy(alpha = 0.9f),
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerHandlerProduccion.openPicker()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryGreen)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Añadir Foto / Actualización", color = PrimaryGreen)
                                        }
                                    }

                                    Text(
                                        text = "Se publicará automáticamente en el Área Social de Socios y en Actividad Reciente",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            viewModel.saveWorkPart(
                                                id = editWorkPartId ?: 0L,
                                                orchardId = selectedOrchardId,
                                                orchardName = orchardName,
                                                type = "produccion",
                                                taskName = "Producción $variedadRecolectada",
                                                observations = observacionesProduccion,
                                                dateTimestamp = fechaCosechaTimestamp,
                                                kilos = k,
                                                pricePerKg = p,
                                                kilosDestrio = kd,
                                                precioDestrio = pd,
                                                indemnizacionSeguro = indSeguro,
                                                totalCost = valorEstimado,
                                                photoUri = selectedPhotoProduccion,
                                                ownerCategory = orchardOwnerCategory,
                                                publishToSocial = true
                                            )
                                            showToastMessage = if (isEditing) "Producción actualizada correctamente" else "Producción guardada en Actividad Reciente y Socios"
                                            if (isEditing) {
                                                viewModel.navigateBack()
                                            } else {
                                                kilosTotales = ""
                                                precioPorKilo = ""
                                                tieneDestrio = false
                                                kilosDestrio = ""
                                                precioDestrio = ""
                                                tieneSeguro = false
                                                indemnizacionSeguro = ""
                                                observacionesProduccion = ""
                                                selectedPhotoProduccion = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Text(
                                            text = if (isEditing) "Guardar Cambios" else "Guardar Parte",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        3 -> {
            // TAB 4: VARIOS
            val imp = importeVarios.replace(',', '.').toDoubleOrNull() ?: 0.0

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Concepto
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Concepto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = conceptoVarios,
                                    onValueChange = {
                                        conceptoVarios = it
                                        if (conceptoError != null && it.isNotBlank()) conceptoError = null
                                    },
                                    placeholder = { Text("Ej. Reparación maquinaria, compra herramientas, abono foliar...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    isError = conceptoError != null,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Text
                                )
                                if (conceptoError != null) {
                                    Text(
                                        text = conceptoError ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Importe
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Importe (€)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = importeVarios,
                                    onValueChange = { importeVarios = it },
                                    placeholder = { Text("Ej. 85.50", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Decimal
                                )
                            }

                            // Observaciones
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Observaciones", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                OutlinedTextField(
                                    value = observacionesVarios,
                                    onValueChange = { observacionesVarios = it },
                                    placeholder = { Text("Añadir notas o detalles adicionales sobre el gasto o labor...", color = OnSurfaceVariant.copy(alpha = 0.5f)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = appOutlinedTextFieldColors(),
                                    keyboardOptions = AppKeyboards.Text
                                )
                            }

                            // Total and Action Footer
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceContainerLow)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Importe Total", fontSize = 13.sp, color = OnSurfaceVariant)
                                        Text(
                                            text = "%.2f €".format(imp),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }

                                    // Botón de subir fotos
                                    if (selectedPhotoVarios != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, PrimaryGreen, RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = selectedPhotoVarios,
                                                contentDescription = "Foto Varios",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        photoPickerHandlerVarios.openPicker()
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(50.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = SurfaceWhite.copy(alpha = 0.9f),
                                                        contentColor = PrimaryGreen
                                                    )
                                                ) {
                                                    Text("Cambiar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                FilledTonalIconButton(
                                                    onClick = { selectedPhotoVarios = null },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = Color.Red.copy(alpha = 0.9f),
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                photoPickerHandlerVarios.openPicker()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryGreen)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = "Subir Fotos / Actualización", color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Text(
                                        text = "Se publicará automáticamente en el Área Social de Socios y en Finanzas",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Botón de guardar
                                    Button(
                                        onClick = {
                                            val conceptoFinal = conceptoVarios.trim()
                                            if (conceptoFinal.isBlank()) {
                                                conceptoError = "Introduce el concepto antes de guardar"
                                                return@Button
                                            }

                                            viewModel.saveWorkPart(
                                                id = editWorkPartId ?: 0L,
                                                orchardId = -1L,
                                                orchardName = "Gasto General",
                                                type = "varios",
                                                taskName = conceptoFinal,
                                                totalCost = imp,
                                                observations = observacionesVarios,
                                                photoUri = selectedPhotoVarios,
                                                ownerCategory = "Varios",
                                                publishToSocial = true
                                            )
                                            showToastMessage = if (isEditing) "Parte de varios actualizado correctamente" else "Parte de varios guardado correctamente"
                                            if (isEditing) {
                                                viewModel.navigateBack()
                                            } else {
                                                conceptoVarios = ""
                                                importeVarios = ""
                                                observacionesVarios = ""
                                                selectedPhotoVarios = null
                                                conceptoError = null
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Text(
                                            text = if (isEditing) "Guardar Cambios" else "Guardar",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}
}

@Composable
private fun WorkPartTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (selected) SurfaceContainerHigh else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OnSurface else OnSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrchardSelectorCard(
    orchardName: String,
    allOrchards: List<com.example.data.model.OrchardEntity>,
    onOrchardSelected: (com.example.data.model.OrchardEntity) -> Unit,
    titularPropios: String = "Propiedad",
    titularVr: String = "V&R C.B.",
    titularOtros: String = "Otros",
    modifier: Modifier = Modifier
) {
    var ownerExpanded by remember { mutableStateOf(false) }
    var orchardExpanded by remember { mutableStateOf(false) }
    var selectedOwnerKey by remember { mutableStateOf("all") }

    val ownerOptions = remember(titularPropios, titularVr, titularOtros) {
        listOf(
            "all" to "Todos los Propietarios",
            "propios" to titularPropios.ifEmpty { "Propiedad" },
            "vr" to titularVr.ifEmpty { "V&R C.B." },
            "otros" to titularOtros.ifEmpty { "Otros" }
        )
    }

    val selectedOwnerLabel = ownerOptions.find { it.first == selectedOwnerKey }?.second ?: "Todos los Propietarios"

    val filteredOrchards = remember(allOrchards, selectedOwnerKey) {
        when (selectedOwnerKey) {
            "propios" -> allOrchards.filter { it.ownerType.lowercase() in listOf("propios", "propiedad") }
            "vr" -> allOrchards.filter { it.ownerType.lowercase() in listOf("v_y_r_cb", "vr", "cb", "v&r") }
            "otros" -> allOrchards.filter { it.ownerType.lowercase() == "otros" }
            else -> allOrchards
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Selector de Propietario / Comunidad
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Propietario / Comunidad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariant
                )
                ExposedDropdownMenuBox(
                    expanded = ownerExpanded,
                    onExpandedChange = { ownerExpanded = !ownerExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedOwnerLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(8.dp),
                        colors = appOutlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = ownerExpanded,
                        onDismissRequest = { ownerExpanded = false }
                    ) {
                        ownerOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (key == selectedOwnerKey) FontWeight.Bold else FontWeight.Normal,
                                        color = OnSurface
                                    )
                                },
                                onClick = {
                                    selectedOwnerKey = key
                                    ownerExpanded = false
                                    val newFiltered = when (key) {
                                        "propios" -> allOrchards.filter { it.ownerType.lowercase() in listOf("propios", "propiedad") }
                                        "vr" -> allOrchards.filter { it.ownerType.lowercase() in listOf("v_y_r_cb", "vr", "cb", "v&r") }
                                        "otros" -> allOrchards.filter { it.ownerType.lowercase() == "otros" }
                                        else -> allOrchards
                                    }
                                    if (newFiltered.isNotEmpty() && newFiltered.none { it.name == orchardName }) {
                                        onOrchardSelected(newFiltered.first())
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Selector de Huerto
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Huerto",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = orchardExpanded,
                    onExpandedChange = { orchardExpanded = !orchardExpanded }
                ) {
                    OutlinedTextField(
                        value = orchardName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orchardExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(8.dp),
                        colors = appOutlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = orchardExpanded,
                        onDismissRequest = { orchardExpanded = false }
                    ) {
                        if (filteredOrchards.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay huertos para este propietario", color = OnSurfaceVariant) },
                                onClick = { orchardExpanded = false }
                            )
                        } else {
                            filteredOrchards.forEach { orchard ->
                                DropdownMenuItem(
                                    text = { Text(orchard.name, fontWeight = FontWeight.Medium, color = OnSurface) },
                                    onClick = {
                                        onOrchardSelected(orchard)
                                        orchardExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
