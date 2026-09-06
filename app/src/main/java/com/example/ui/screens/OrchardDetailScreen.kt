package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CalendarTaskEntity
import com.example.data.model.IrrigationScheduleEntity
import com.example.data.model.MaterialsJsonHelper
import com.example.data.model.OrchardEntity
import com.example.data.model.WorkPartEntity
import com.example.ui.util.AppKeyboards
import com.example.ui.util.ImageStorageHelper
import com.example.ui.util.formatTimeInput
import com.example.ui.util.showAndroidTimePicker
import com.example.ui.MainViewModel
import com.example.ui.Screen
import android.widget.Toast
import com.example.ui.components.AppTopHeader
import com.example.ui.components.TaskAlarmConfigDialog
import com.example.ui.components.rememberPhotoPickerHandler
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OrchardDetailScreen(
    orchardId: Long,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val allWorkParts by viewModel.allWorkParts.collectAsStateWithLifecycle()
    val allCalendarTasks by viewModel.allCalendarTasks.collectAsStateWithLifecycle()
    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val orchard = allOrchards.find { it.id == orchardId } ?: allOrchards.firstOrNull()

    val activeAlertsForOrchard = remember(allPosts, orchard) {
        if (orchard == null) emptyList()
        else {
            allPosts.filter { post ->
                post.isAlert && !post.isAlertResolved && (
                    (post.orchardId != null && post.orchardId == orchard.id) ||
                    post.orchardName.trim().equals(orchard.name.trim(), ignoreCase = true) ||
                    post.orchardName.contains(orchard.name, ignoreCase = true) ||
                    orchard.name.contains(post.orchardName, ignoreCase = true)
                )
            }
        }
    }

    val photoPickerHandler = rememberPhotoPickerHandler(prefix = "orchard") { localPath ->
        if (orchard != null) {
            viewModel.updateOrchardPhoto(orchard.id, localPath)
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isFichaExpanded by remember { mutableStateOf(false) }

    val tabs = listOf("Actividad Reciente", "Tareas Pendientes", "Riego")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Deletion and Completion confirmation states
    var workPartToDelete by remember { mutableStateOf<WorkPartEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskToComplete by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskForAlarmDialog by remember { mutableStateOf<CalendarTaskEntity?>(null) }

    val daysList = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val savedSchedules by viewModel.getSchedulesForOrchard(orchard?.id ?: 0L).collectAsStateWithLifecycle(initialValue = emptyList())
    val schedulesMap = remember(orchard?.id) {
        mutableStateMapOf<String, Pair<String, String>>()
    }

    LaunchedEffect(savedSchedules, orchard?.id) {
        if (savedSchedules.isNotEmpty()) {
            savedSchedules.forEach { s ->
                schedulesMap[s.dayOfWeek] = Pair(s.startTime, s.endTime)
            }
        } else {
            daysList.forEach { day ->
                if (!schedulesMap.containsKey(day)) {
                    val defaultPair = when (day) {
                        "Lunes", "Miércoles", "Viernes" -> Pair("08:00", "10:00")
                        else -> Pair("", "")
                    }
                    schedulesMap[day] = defaultPair
                }
            }
        }
    }

    var showSavedScheduleToast by remember { mutableStateOf(false) }

    if (orchard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    val orchardWorkParts = allWorkParts.filter { it.orchardId == orchard.id || it.orchardName == orchard.name }
    val orchardTasks = allCalendarTasks.filter { task ->
        !task.isDone && (
            (task.orchardId != null && task.orchardId == orchard.id) ||
            task.orchardName.equals(orchard.name, ignoreCase = true) ||
            task.orchardName.startsWith(orchard.name, ignoreCase = true)
        )
    }

    val isCitrus = orchard.fruitType != "Aguacate"
    val accentColor = if (isCitrus) SecondaryOrange else PrimaryGreen

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "",
                showMenu = false,
                showBack = true,
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // "Parte" Button (Actividad / Partes on the left)
                        Button(
                            onClick = { viewModel.navigateTo(Screen.WorkParts("tareas", orchard.id)) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("add_part_orchard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Parte", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // "Tarea" Button (Tareas Pendientes on the right)
                        Button(
                            onClick = { showAddTaskDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("add_task_orchard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Tarea", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Box {
                            IconButton(onClick = { showMoreMenu = !showMoreMenu }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Más opciones",
                                    tint = accentColor
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Editar Huerto", color = OnSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.navigateTo(Screen.AddOrchard(editOrchardId = orchard.id))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryGreen)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Borrar Huerto", color = Color.Red) },
                                    onClick = {
                                        showMoreMenu = false
                                        showDeleteDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceBright),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Detalle de Huerto
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DETALLE DE HUERTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = orchard.name,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            fontSize = 22.sp
                        )
                    )
                }
            }

            // Priority Alerts for this Orchard
            if (activeAlertsForOrchard.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeAlertsForOrchard.forEach { alert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = alert.alertTitle ?: "Alerta Prioritaria Activa",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF991B1B)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "EN INICIO",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFDC2626),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = alert.content,
                                        fontSize = 13.sp,
                                        color = Color(0xFF7F1D1D)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.navigateTo(Screen.Social) },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Ver en Muro de Socios",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.resolveAlert(alert.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF16A34A),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "Resolver Alerta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hero Image
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp))
                        .background(SurfaceContainerHigh)
                ) {
                    if (!orchard.photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = orchard.photoUri,
                            contentDescription = "Foto del huerto",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback citrus landscape representation
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF3B6E32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Park,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = orchard.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Floating action overlay to upload/change photo
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                photoPickerHandler.openPicker()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceWhite.copy(alpha = 0.92f),
                                contentColor = PrimaryGreen
                            )
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (!orchard.photoUri.isNullOrEmpty()) "Cambiar Foto" else "Añadir Foto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!orchard.photoUri.isNullOrEmpty()) {
                            FilledTonalIconButton(
                                onClick = { viewModel.updateOrchardPhoto(orchard.id, null) },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Red.copy(alpha = 0.88f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar foto", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Technical Sheet (Ficha Técnica)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFichaExpanded = !isFichaExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Ficha Técnica y Datos",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                            Icon(
                                imageVector = if (isFichaExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = OnSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = isFichaExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FichaRow(label = "Propietario", value = orchard.ownerName)

                                if (orchard.locationGps.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Ubicación", fontSize = 13.sp, color = OnSurfaceVariant)
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            IconButton(
                                                onClick = {
                                                    val uri = Uri.parse("geo:0,0?q=${orchard.locationGps}(${orchard.name})")
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Map,
                                                    contentDescription = "Ver Mapa",
                                                    tint = PrimaryGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, "Ubicación de ${orchard.name}: https://maps.google.com/?q=${orchard.locationGps}")
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir Ubicación"))
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Compartir WhatsApp",
                                                    tint = WhatsAppGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    FichaRow(label = "Ubicación GPS", value = "-")
                                }

                                FichaRow(label = "Término", value = orchard.municipality.ifEmpty { "-" })
                                FichaRow(label = "Partida", value = orchard.partida.ifEmpty { "-" })
                                val polParc = when {
                                    orchard.polygon.isNotEmpty() && orchard.parcel.isNotEmpty() -> "${orchard.polygon} / ${orchard.parcel}"
                                    orchard.polygon.isNotEmpty() -> "Pol. ${orchard.polygon}"
                                    orchard.parcel.isNotEmpty() -> "Parc. ${orchard.parcel}"
                                    else -> "-"
                                }
                                FichaRow(label = "Pol. / Parc.", value = polParc)
                                FichaRow(label = "Hanegadas", value = if (orchard.hanegadas > 0) "${orchard.hanegadas} hg" else "-")
                                FichaRow(label = "Variedad", value = orchard.variety.ifEmpty { "-" })
                                FichaRow(label = "Portainjerto", value = orchard.rootstock.ifEmpty { "-" })
                                FichaRow(label = "Año de plantación", value = if (orchard.plantingYear > 0) "${orchard.plantingYear}" else "-")

                                HorizontalDivider(thickness = 1.dp, color = OutlineVariant, modifier = Modifier.padding(vertical = 4.dp))

                                if (orchard.regadorPhone.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Regador", fontSize = 13.sp, color = OnSurfaceVariant)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.clickable {
                                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${orchard.regadorPhone}"))
                                                context.startActivity(dialIntent)
                                            }
                                        ) {
                                            Text(
                                                text = if (orchard.regadorName.isNotBlank()) "Llamar a ${orchard.regadorName}" else "Llamar al regador",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryGreen
                                            )
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                } else {
                                    FichaRow(label = "Regador", value = orchard.regadorName.ifEmpty { "-" })
                                }

                                FichaRow(label = "Pozo", value = orchard.pozo.ifEmpty { "-" })
                                FichaRow(label = "Sector", value = orchard.sector.ifEmpty { "-" })
                                FichaRow(label = "Hidrante", value = orchard.hidrante.ifEmpty { "-" })
                            }
                        }
                    }
                }
            }

            // Sticky Tabs Selector: Actividad Reciente | Tareas Pendientes | Riego
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceWhite)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        DetailTabButton(
                            title = title,
                            selected = selectedTabIndex == index,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTabIndex = index }
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // TAB 1: Actividad Reciente
                    if (orchardWorkParts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "No hay actividades registradas aún", color = OnSurfaceVariant)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwipeLeft,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Desliza a la izquierda cualquier actividad para eliminarla",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        items(orchardWorkParts, key = { it.id }) { part ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SwipeableActivityPartItem(
                                    part = part,
                                    onDeleteRequest = { workPartToDelete = it },
                                    onEditRequest = {
                                        viewModel.navigateTo(
                                            Screen.WorkParts(editWorkPartId = it.id)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 2: Tareas Pendientes
                    if (orchardTasks.isEmpty()) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Sin tareas pendientes",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurface
                                        )
                                        Text(
                                            text = "No hay tareas programadas para este huerto actualmente.",
                                            fontSize = 13.sp,
                                            color = OnSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Button(
                                            onClick = { showAddTaskDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                            shape = RoundedCornerShape(50.dp),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Programar Tarea", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Swipe,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Desliza derecha (completar) o izquierda (eliminar)",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        items(orchardTasks, key = { it.id }) { task ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SwipeablePendingTaskItem(
                                    task = task,
                                    orchardName = orchard.name,
                                    onCompleteRequest = { taskToComplete = it },
                                    onDeleteRequest = { taskToDelete = it },
                                    onAlarmRequest = { taskForAlarmDialog = it }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 3: Riego (Programación Semanal de Riego)
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Programación Semanal de Riego",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )

                                    daysList.forEach { day ->
                                        val schedule = schedulesMap[day] ?: Pair("", "")
                                        val start = schedule.first
                                        val end = schedule.second

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = day,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = OnSurface,
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                ScheduleTimeText(
                                                    time = start,
                                                    onTimeChange = {
                                                        schedulesMap[day] = Pair(it, end)
                                                        showSavedScheduleToast = false
                                                    },
                                                    context = context
                                                )

                                                Text(
                                                    text = "-",
                                                    color = OnSurfaceVariant,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 2.dp)
                                                )

                                                ScheduleTimeText(
                                                    time = end,
                                                    onTimeChange = {
                                                        schedulesMap[day] = Pair(start, it)
                                                        showSavedScheduleToast = false
                                                    },
                                                    context = context
                                                )
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)
                                    }

                                    Button(
                                        onClick = {
                                            val entities = daysList.map { day ->
                                                val pair = schedulesMap[day] ?: Pair("", "")
                                                IrrigationScheduleEntity(
                                                    orchardId = orchard.id,
                                                    dayOfWeek = day,
                                                    startTime = pair.first.trim(),
                                                    endTime = pair.second.trim(),
                                                    isEnabled = pair.first.isNotBlank() || pair.second.isNotBlank()
                                                )
                                            }
                                            viewModel.saveIrrigationSchedules(orchard.id, entities)
                                            showSavedScheduleToast = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryGreenContainer,
                                            contentColor = OnPrimaryGreenContainer
                                        ),
                                        shape = RoundedCornerShape(50.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Guardar Horarios", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (showSavedScheduleToast) {
                                        Text(
                                            text = "✓ Horarios de riego guardados correctamente",
                                            color = PrimaryGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(top = 4.dp)
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

    // Delete Confirmation Dialog for Orchard
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(text = "¿Eliminar Huerto?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Esta acción no se puede deshacer. Se eliminarán todos los registros, historial de riego y datos asociados a \"${orchard.name}\"."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteOrchard(orchard.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = PrimaryGreen)
                }
            }
        )
    }

    // Delete Confirmation Dialog for Activity / Work Part
    if (workPartToDelete != null) {
        val part = workPartToDelete!!
        AlertDialog(
            onDismissRequest = { workPartToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar actividad?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas eliminar el parte de trabajo '${part.taskName}' registrado en ${part.orchardName}?",
                        fontSize = 14.sp,
                        color = OnSurface
                    )
                    if (part.totalCost > 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "📉 Se descontarán %.2f € de los gastos del huerto en Finanzas.".format(part.totalCost),
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Text(
                        text = "📢 Se registrará la notificación de eliminación en el Muro de Socios.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteWorkPart(part)
                        workPartToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { workPartToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Delete Confirmation Dialog for Pending Task
    if (taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar tarea pendiente?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Estás seguro de que deseas eliminar la tarea '${task.title}' programada para ${task.dateKey} (${task.timeText})?",
                        fontSize = 14.sp,
                        color = OnSurface
                    )
                    Text(
                        text = "📢 Se publicará el aviso de cancelación en el muro de Socios.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCalendarTask(task)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Complete Task Confirmation Dialog
    if (taskToComplete != null) {
        val task = taskToComplete!!
        AlertDialog(
            onDismissRequest = { taskToComplete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Completar Tarea",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas marcar como finalizada la tarea '${task.title}' programada para ${task.dateKey} (${task.timeText})?",
                        fontSize = 14.sp,
                        color = OnSurface
                    )
                    Text(
                        text = "Puedes completar la labor y abrir directamente el nuevo parte de trabajo con el huerto y la faena ya seleccionados.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val matchedTaskType = resolveFarmTaskType(task.title)
                            val targetOrchardId = task.orchardId
                                ?: orchard?.id
                                ?: allOrchards.find { it.name.equals(task.orchardName, ignoreCase = true) }?.id
                            viewModel.completeCalendarTask(task)
                            taskToComplete = null
                            viewModel.navigateTo(
                                Screen.WorkParts(
                                    initialTab = "tareas",
                                    selectedOrchardId = targetOrchardId,
                                    preselectedTaskType = matchedTaskType
                                )
                            )
                            Toast.makeText(context, "Tarea completada. Abriendo nuevo parte de trabajo...", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("orchard_detail_complete_and_create_workpart_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Sí, y crear parte de trabajo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { taskToComplete = null },
                            modifier = Modifier.testTag("orchard_detail_cancel_complete_task_button")
                        ) {
                            Text("Cancelar", color = OnSurfaceVariant)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.completeCalendarTask(task)
                                taskToComplete = null
                                Toast.makeText(context, "Tarea completada con éxito", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("orchard_detail_only_complete_task_button")
                        ) {
                            Text("Solo completar", fontSize = 13.sp, color = PrimaryGreenDark)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    // Add Task Dialog for current Orchard
    if (showAddTaskDialog) {
        val todayStr = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date()) }
        AddTaskDialog(
            allOrchards = allOrchards,
            initialOrchard = orchard,
            selectedDateKey = todayStr,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { oId, oName, title, time, desc, fruitType, dateKey, reminderEnabled, reminderDate, reminderTime ->
                viewModel.addCalendarTask(
                    orchardId = oId,
                    orchardName = oName,
                    title = title,
                    time = time,
                    description = desc,
                    fruitType = fruitType,
                    dateKey = dateKey,
                    reminderEnabled = reminderEnabled,
                    reminderDate = reminderDate,
                    reminderTime = reminderTime
                )
                showAddTaskDialog = false
            }
        )
    }

    // Task Alarm Configuration & Test Dialog for Orchard tasks
    if (taskForAlarmDialog != null) {
        val currentTask = taskForAlarmDialog!!
        TaskAlarmConfigDialog(
            task = currentTask,
            onDismiss = { taskForAlarmDialog = null },
            onSave = { reminderEnabled, reminderDate, reminderTime ->
                viewModel.updateCalendarTaskReminder(currentTask.id, reminderEnabled, reminderDate, reminderTime)
                taskForAlarmDialog = null
                val msg = if (reminderEnabled) "Alarma guardada para $reminderDate $reminderTime" else "Alarma desactivada"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onTestAlarm = { taskToTest ->
                viewModel.testTaskAlarm(taskToTest)
                Toast.makeText(context, "Pop-up de alarma lanzado al móvil", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableActivityPartItem(
    part: WorkPartEntity,
    onDeleteRequest: (WorkPartEntity) -> Unit,
    onEditRequest: (WorkPartEntity) -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest(part)
                false
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDC2626))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Eliminar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar actividad",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        ActivityPartCard(
            part = part,
            onDeleteClick = { onDeleteRequest(part) },
            onEditClick = { onEditRequest(part) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePendingTaskItem(
    task: CalendarTaskEntity,
    orchardName: String,
    onCompleteRequest: ((CalendarTaskEntity) -> Unit)? = null,
    onDeleteRequest: (CalendarTaskEntity) -> Unit,
    onAlarmRequest: ((CalendarTaskEntity) -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest(task)
                false
            } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd && onCompleteRequest != null) {
                onCompleteRequest(task)
                false
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.25f }
    )

    val taskVisual = getFarmTaskVisualInfo(task.title)

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onCompleteRequest != null && !task.isDone,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGreen)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completar",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Completar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDC2626))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Eliminar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar tarea",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) {
        PendingTaskDetailCard(
            task = task,
            title = task.title,
            tag = if (task.isDone) "Finalizada" else "Programada",
            tagColor = if (task.isDone) Color(0xFF64748B) else SecondaryOrangeDark,
            desc = task.description.ifEmpty { "Tarea asignada a $orchardName" },
            deadline = "${task.dateKey} • ${task.timeText}",
            icon = taskVisual.icon,
            iconColor = taskVisual.iconColor,
            onAlarmClick = if (onAlarmRequest != null) { { onAlarmRequest(task) } } else null
        )
    }
}

@Composable
private fun FichaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = OnSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OnSurface)
    }
}

@Composable
private fun DetailTabButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) SecondaryOrangeDark.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) SecondaryOrangeDark else OnSurfaceVariant,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ActivityPartCard(
    part: WorkPartEntity,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    val isRevenue = part.type == "produccion"
    val isGoteo = part.type == "goteo"
    val isVarios = part.type == "varios"

    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val formattedDate = remember(part.dateTimestamp) {
        if (part.dateTimestamp > 0) dateFormat.format(java.util.Date(part.dateTimestamp)) else ""
    }

    val taskVisual = getFarmTaskVisualInfo(part.taskName)

    val iconVector = when {
        isRevenue -> Icons.AutoMirrored.Filled.TrendingUp
        isGoteo -> Icons.Default.WaterDrop
        isVarios -> if (part.taskName.isNotBlank() && part.taskName != "Gasto Varios" && part.taskName != "Gastos Varios") taskVisual.icon else Icons.AutoMirrored.Filled.ReceiptLong
        else -> taskVisual.icon
    }

    val iconBgColor = when {
        isRevenue -> PrimaryGreenContainer
        isGoteo -> Color(0xFFE1F5FE)
        isVarios -> if (part.taskName.isNotBlank() && part.taskName != "Gasto Varios" && part.taskName != "Gastos Varios") taskVisual.containerColor else Color(0xFFFEF3C7)
        else -> taskVisual.containerColor
    }

    val iconTint = when {
        isRevenue -> PrimaryGreen
        isGoteo -> Color(0xFF0288D1)
        isVarios -> if (part.taskName.isNotBlank() && part.taskName != "Gasto Varios" && part.taskName != "Gastos Varios") taskVisual.iconColor else Color(0xFFD97706)
        else -> taskVisual.iconColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = part.taskName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = if (formattedDate.isNotEmpty()) {
                                "${if (isRevenue) "Ingreso / Producción" else "Gasto Operativo"} • $formattedDate"
                            } else {
                                if (isRevenue) "Ingreso / Producción" else "Gasto Operativo"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isRevenue) PrimaryGreen else Color(0xFFDC2626)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onEditClick != null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar actividad",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (part.observations.isNotEmpty()) {
                Text(
                    text = part.observations,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant
                )
            }

            if (!part.photoUri.isNullOrEmpty()) {
                AsyncImage(
                    model = part.photoUri,
                    contentDescription = "Foto adjunta",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            val materials = remember(part.materialsJson) {
                MaterialsJsonHelper.fromJson(part.materialsJson)
            }
            if (materials.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Materiales empleados:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant
                    )
                    materials.forEach { mat ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val qtyStr = if (mat.quantity % 1.0 == 0.0) mat.quantity.toInt().toString() else mat.quantity.toString()
                            Text(
                                text = "• ${mat.name} ($qtyStr un/L)",
                                fontSize = 12.sp,
                                color = OnSurface
                            )
                            Text(
                                text = "%.2f €".format(mat.totalCost),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

            if (isRevenue && (part.kilosDestrio > 0 || part.indemnizacionSeguro > 0)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceContainerLow)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (part.kilos > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Comercial: ${part.kilos.toInt()} kg (%.2f €/kg)".format(part.pricePerKg), fontSize = 11.sp, color = OnSurfaceVariant)
                            Text(text = "%.2f €".format(part.kilos * part.pricePerKg), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                        }
                    }
                    if (part.kilosDestrio > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Destrío: ${part.kilosDestrio.toInt()} kg (%.2f €/kg)".format(part.precioDestrio), fontSize = 11.sp, color = OnSurfaceVariant)
                            Text(text = "+%.2f €".format(part.kilosDestrio * part.precioDestrio), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryGreen)
                        }
                    }
                    if (part.indemnizacionSeguro > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Indemnización Seguro (granizo/siniestro):", fontSize = 11.sp, color = OnSurfaceVariant)
                            Text(text = "+%.2f €".format(part.indemnizacionSeguro), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isRevenue && part.kilos > 0 && part.kilosDestrio <= 0 && part.indemnizacionSeguro <= 0 -> "Cosecha: ${part.kilos.toInt()} kg (%.2f €/kg)".format(part.pricePerKg)
                        isRevenue -> "Liquidación Cosecha"
                        part.hours > 0 -> "Horas de campo: ${part.hours}h"
                        isGoteo -> "Gasto de Riego / Goteo"
                        else -> "Gasto Registrado"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurface
                )
                Text(
                    text = when {
                        isRevenue && part.totalCost > 0 -> "+%.2f €".format(part.totalCost)
                        !isRevenue && part.totalCost > 0 -> "-%.2f €".format(part.totalCost)
                        else -> "Coste: Pdte."
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isRevenue -> PrimaryGreen
                        part.totalCost > 0 -> Color(0xFFDC2626)
                        else -> SecondaryOrangeDark
                    }
                )
            }
        }
    }
}

@Composable
fun PendingTaskDetailCard(
    task: CalendarTaskEntity? = null,
    title: String,
    tag: String,
    tagColor: Color,
    desc: String,
    deadline: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onAlarmClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val isTaskDone = task?.isDone == true
    val isDark = LocalIsDarkTheme.current
    val cardBg = when {
        isTaskDone && isDark -> Color(0xFF142614)
        isTaskDone && !isDark -> Color(0xFFF0FDF4)
        else -> SurfaceWhite
    }
    val cardBorder = when {
        isTaskDone && isDark -> Color(0xFF166534)
        isTaskDone && !isDark -> Color(0xFF86EFAC)
        else -> OutlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(if (isTaskDone) 1.5.dp else 1.dp, cardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTaskDone) PrimaryGreenDark else OnSurface,
                        textDecoration = if (isTaskDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onAlarmClick != null) {
                        Surface(
                            color = if (task?.reminderEnabled == true) Color(0xFFFFF7ED) else SurfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (task?.reminderEnabled == true) Color(0xFFFED7AA) else OutlineVariant),
                            modifier = Modifier.clickable { onAlarmClick() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (task?.reminderEnabled == true) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                                    contentDescription = "Alarma",
                                    tint = if (task?.reminderEnabled == true) Color(0xFFEA580C) else OnSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (task?.reminderEnabled == true) "Alarma ON" else "+Alarma",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (task?.reminderEnabled == true) Color(0xFFEA580C) else OnSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!isTaskDone) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagColor
                            )
                        }
                    }
                }
            }

            Text(text = desc, fontSize = 13.sp, color = OnSurfaceVariant)

            if (task?.reminderEnabled == true) {
                Surface(
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = if (onAlarmClick != null) Modifier.clickable { onAlarmClick() } else Modifier
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Aviso programado",
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Aviso activo: ${task.reminderDate.ifBlank { task.dateKey }} a las ${task.reminderTime.ifBlank { task.timeText }} • Toca para editar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFC2410C)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

            Text(
                text = deadline,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
        }
    }
}

@Composable
private fun ScheduleTimeText(
    time: String,
    onTimeChange: (String) -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val hasTime = time.isNotBlank()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
                showAndroidTimePicker(context, time.ifBlank { "08:00" }) { picked ->
                    onTimeChange(picked)
                }
            }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = "Seleccionar hora",
            tint = if (hasTime) PrimaryGreen else OnSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (hasTime) time else "--:--",
            fontSize = 14.sp,
            fontWeight = if (hasTime) FontWeight.Bold else FontWeight.Normal,
            color = if (hasTime) PrimaryGreenDark else OnSurfaceVariant.copy(alpha = 0.6f)
        )
        if (hasTime) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Borrar hora",
                tint = OnSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(15.dp)
                    .clickable { onTimeChange("") }
            )
        }
    }
}


