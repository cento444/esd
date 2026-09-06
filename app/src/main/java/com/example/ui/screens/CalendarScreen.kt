package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalendarTaskEntity
import com.example.data.model.OrchardEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppTopHeader
import com.example.ui.components.TaskAlarmConfigDialog
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.theme.*
import com.example.ui.util.AppKeyboards
import com.example.ui.util.TaskAlarmScheduler
import com.example.ui.util.formatDateToDisplay
import com.example.ui.util.formatTimeInput
import com.example.ui.util.showAndroidTimePicker
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Predefined agricultural tasks definition (sorted alphabetically)
val PREDEFINED_FARM_TASKS = listOf(
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

data class TaskVisualInfo(
    val icon: ImageVector,
    val iconColor: Color,
    val containerColor: Color
)

fun getFarmTaskVisualInfo(title: String): TaskVisualInfo {
    return when {
        title.contains("Abon", ignoreCase = true) || title.contains("Fertiliz", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.Eco, Color(0xFF2E7D32), Color(0xFFE8F5E9))
        title.contains("Desbroce", ignoreCase = true) || title.contains("Suelo", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.Grass, Color(0xFF558B2F), Color(0xFFF1F8E9))
        title.contains("Injerto", ignoreCase = true) || title.contains("Replant", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.Park, Color(0xFF00897B), Color(0xFFE0F2F1))
        title.contains("Limpieza", ignoreCase = true) || title.contains("Acondicionamiento", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.CleaningServices, Color(0xFF00ACC1), Color(0xFFE0F7FA))
        title.contains("Goteo", ignoreCase = true) || title.contains("Tubería", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.Plumbing, Color(0xFF0277BD), Color(0xFFE1F5FE))
        title.contains("Plaga", ignoreCase = true) || title.contains("Muestreo", ignoreCase = true) || title.contains("Pulgón", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.BugReport, Color(0xFF8E24AA), Color(0xFFF3E5F5))
        title.contains("Poda", ignoreCase = true) || title.contains("Aclareo", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.ContentCut, Color(0xFF8D6E63), Color(0xFFEFEBE9))
        title.contains("Recolección", ignoreCase = true) || title.contains("Cosecha", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.Agriculture, Color(0xFFF57F17), Color(0xFFFFF9C4))
        title.contains("Riego", ignoreCase = true) || title.contains("Humedad", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.WaterDrop, Color(0xFF0288D1), Color(0xFFE1F5FE))
        title.contains("Seguro", ignoreCase = true) || title.contains("Póliza", ignoreCase = true) || title.contains("Agroseguro", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.Security, Color(0xFF1565C0), Color(0xFFE3F2FD))
        title.contains("Fitosanitario", ignoreCase = true) || title.contains("Tratamiento", ignoreCase = true) ->
            TaskVisualInfo(Icons.Default.PestControl, Color(0xFFE53935), Color(0xFFFFEBEE))
        else ->
            TaskVisualInfo(Icons.Default.Assignment, Color(0xFF546E7A), Color(0xFFECEFF1))
    }
}

fun resolveFarmTaskType(taskTitle: String): String {
    val clean = taskTitle.trim()
    val exact = PREDEFINED_FARM_TASKS.find { it.equals(clean, ignoreCase = true) }
    if (exact != null) return exact

    val partial = PREDEFINED_FARM_TASKS.find {
        clean.contains(it, ignoreCase = true) || it.contains(clean, ignoreCase = true)
    }
    if (partial != null) return partial

    return when {
        clean.contains("Abon", ignoreCase = true) || clean.contains("Fertiliz", ignoreCase = true) -> "Abonado y fertirrigación"
        clean.contains("Desbroce", ignoreCase = true) || clean.contains("Suelo", ignoreCase = true) -> "Desbroce y mantenimiento de suelo"
        clean.contains("Injerto", ignoreCase = true) || clean.contains("Replant", ignoreCase = true) -> "Injerto y replantación"
        clean.contains("Limpieza", ignoreCase = true) || clean.contains("Acondicionamiento", ignoreCase = true) -> "Limpieza y acondicionamiento"
        clean.contains("Goteo", ignoreCase = true) || clean.contains("Tubería", ignoreCase = true) -> "Mantenimiento goteo y tuberías"
        clean.contains("Plaga", ignoreCase = true) || clean.contains("Muestreo", ignoreCase = true) || clean.contains("Pulgón", ignoreCase = true) -> "Muestreo y control de plagas"
        clean.contains("Poda", ignoreCase = true) || clean.contains("Aclareo", ignoreCase = true) -> "Poda y aclareo"
        clean.contains("Recolección", ignoreCase = true) || clean.contains("Cosecha", ignoreCase = true) -> "Recolección / Cosecha"
        clean.contains("Riego", ignoreCase = true) || clean.contains("Humedad", ignoreCase = true) -> "Riego / Control de humedad"
        clean.contains("Seguro", ignoreCase = true) || clean.contains("Póliza", ignoreCase = true) || clean.contains("Agroseguro", ignoreCase = true) -> "Seguro agrícola"
        clean.contains("Fitosanitario", ignoreCase = true) || clean.contains("Tratamiento", ignoreCase = true) -> "Tratamiento fitosanitario"
        clean.isNotBlank() -> clean
        else -> PREDEFINED_FARM_TASKS.first()
    }
}

private val SPANISH_MONTHS = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val SPANISH_WEEKDAYS = listOf(
    "Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"
)

private fun formatTaskDateBadge(dateKey: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateKey)
        if (date != null) {
            val cal = Calendar.getInstance().apply { time = date }
            val dayOfWeek = SPANISH_WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK) - 1].take(3)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = SPANISH_MONTHS[cal.get(Calendar.MONTH)].take(3).lowercase()
            "$dayOfWeek $day $month"
        } else formatDateToDisplay(dateKey)
    } catch (e: Exception) {
        formatDateToDisplay(dateKey)
    }
}

private fun formatReminderBadgeText(reminderDate: String, reminderTime: String, taskDateKey: String): String {
    val cleanTime = reminderTime.trim()
    val cleanDate = reminderDate.trim()

    val formattedDate = if (cleanDate.isNotBlank()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(cleanDate)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val month = cal.get(Calendar.MONTH) + 1
                val year = cal.get(Calendar.YEAR)
                String.format(Locale.ROOT, "%02d/%02d/%04d", day, month, year)
            } else formatDateToDisplay(cleanDate)
        } catch (e: Exception) {
            formatDateToDisplay(cleanDate)
        }
    } else ""

    return when {
        formattedDate.isNotBlank() && cleanTime.isNotBlank() -> {
            if (cleanDate == taskDateKey) {
                "Aviso a las $cleanTime"
            } else {
                "Aviso: $formattedDate a las $cleanTime"
            }
        }
        cleanTime.isNotBlank() -> "Aviso a las $cleanTime"
        formattedDate.isNotBlank() -> "Aviso: $formattedDate"
        else -> "Aviso activo"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allTasks by viewModel.allCalendarTasks.collectAsStateWithLifecycle()
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Real-time dynamic calendar setup
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayDateKey = String.format(Locale.ROOT, "%04d-%02d-%02d", todayYear, todayMonth + 1, todayDay)

    var viewYear by remember { mutableIntStateOf(todayYear) }
    var viewMonth by remember { mutableIntStateOf(todayMonth) } // 0..11
    var selectedDateKey by remember { mutableStateOf(todayDateKey) }

    // Dialog state for adding task
    var showAddTaskDialog by remember { mutableStateOf(false) }

    // Confirmation dialog states
    var taskToComplete by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskToReactivate by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<CalendarTaskEntity?>(null) }
    var taskForAlarmDialog by remember { mutableStateOf<CalendarTaskEntity?>(null) }

    // Filters state
    var isFilterSectionExpanded by remember { mutableStateOf(false) }
    var filterOwner by remember { mutableStateOf("Todos") }
    var filterOrchardId by remember { mutableStateOf<Long?>(null) }
    var filterTaskType by remember { mutableStateOf("Todas") }
    var filterDateRange by remember { mutableStateOf("Todas") } // "Todas", "Hoy", "Esta Semana", "Este Mes", "Próximos 30 días"
    var filterStatus by remember { mutableStateOf("Todas") } // "Todas", "Solo Pendientes", "Solo Completadas"
    var filterReminder by remember { mutableStateOf("Todas") } // "Todas", "Con notificación", "Sin notificación"

    // Selected day tasks
    val dayTasks = remember(allTasks, selectedDateKey) {
        allTasks.filter { it.dateKey == selectedDateKey }
    }

    // All tasks of the currently viewed month, ordered chronologically from earliest to last day of month
    val monthTasks = remember(allTasks, viewYear, viewMonth) {
        val monthPrefix = String.format(Locale.ROOT, "%04d-%02d", viewYear, viewMonth + 1)
        allTasks
            .filter { it.dateKey.startsWith(monthPrefix) }
            .sortedWith(compareBy({ it.dateKey }, { it.timeText }))
    }

    // Filtered tasks computation across all tasks
    val filteredTasks = remember(allTasks, filterOwner, filterOrchardId, filterTaskType, filterDateRange, filterStatus, filterReminder, allOrchards) {
        val nowCal = Calendar.getInstance()
        val nowFormatted = String.format(Locale.ROOT, "%04d-%02d-%02d", nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH) + 1, nowCal.get(Calendar.DAY_OF_MONTH))

        allTasks.filter { task ->
            // Filter by orchard
            val matchesOrchard = if (filterOrchardId != null) {
                task.orchardId == filterOrchardId
            } else true

            // Filter by owner
            val matchesOwner = if (filterOwner != "Todos") {
                val orchard = allOrchards.find { it.id == task.orchardId || it.name.equals(task.orchardName, ignoreCase = true) }
                when (filterOwner) {
                    userProfile.titularPropios.ifEmpty { "Propiedad" }, "Propios", "Propiedad" ->
                        orchard?.ownerType?.lowercase() in listOf("propios", "propiedad", "mío")
                    userProfile.titularVr.ifEmpty { "V&R C.B." }, "V&R C.B.", "V&R" ->
                        orchard?.ownerType?.lowercase() in listOf("v_y_r_cb", "vyr", "v&r", "cb")
                    userProfile.titularOtros.ifEmpty { "Otros" }, "Otros" ->
                        orchard?.ownerType?.lowercase() == "otros"
                    else -> orchard?.ownerName.equals(filterOwner, ignoreCase = true)
                }
            } else true

            // Filter by task type
            val matchesTask = if (filterTaskType != "Todas") {
                task.title.equals(filterTaskType, ignoreCase = true) || task.title.contains(filterTaskType, ignoreCase = true)
            } else true

            // Filter by status
            val matchesStatus = when (filterStatus) {
                "Solo Pendientes" -> !task.isDone
                "Solo Completadas" -> task.isDone
                else -> true
            }

            // Filter by date range
            val matchesDateRange = when (filterDateRange) {
                "Hoy" -> task.dateKey == nowFormatted
                "Esta Semana" -> {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val taskDate = sdf.parse(task.dateKey)
                        val startCal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                            add(Calendar.DAY_OF_MONTH, 7)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                        }
                        taskDate != null && !taskDate.before(startCal.time) && !taskDate.after(endCal.time)
                    } catch (e: Exception) {
                        true
                    }
                }
                "Este Mes" -> {
                    val currentMonthKey = String.format(Locale.ROOT, "%04d-%02d", nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH) + 1)
                    task.dateKey.startsWith(currentMonthKey)
                }
                "Próximos 30 días" -> {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val taskDate = sdf.parse(task.dateKey)
                        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
                        val endCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }
                        taskDate != null && !taskDate.before(startCal.time) && !taskDate.after(endCal.time)
                    } catch (e: Exception) {
                        true
                    }
                }
                else -> true
            }

            // Filter by reminder / notification
            val matchesReminder = when (filterReminder) {
                "Con aviso", "Con notificación" -> task.reminderEnabled
                "Sin aviso", "Sin notificación" -> !task.reminderEnabled
                else -> true
            }

            matchesOrchard && matchesOwner && matchesTask && matchesStatus && matchesDateRange && matchesReminder
        }
    }

    // Formatted selected date text
    val selectedDateDisplay = remember(selectedDateKey) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(selectedDateKey)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val dayOfWeek = SPANISH_WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK) - 1]
                val day = cal.get(Calendar.DAY_OF_MONTH)
                val month = SPANISH_MONTHS[cal.get(Calendar.MONTH)]
                "$dayOfWeek, $day de $month"
            } else formatDateToDisplay(selectedDateKey)
        } catch (e: Exception) {
            formatDateToDisplay(selectedDateKey)
        }
    }

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "Tareas",
                showMenu = true,
                showBack = false,
                onMenuClick = { viewModel.navigateTo(Screen.Settings) },
                actions = {
                    // "Hoy" Button
                    TextButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            viewYear = c.get(Calendar.YEAR)
                            viewMonth = c.get(Calendar.MONTH)
                            selectedDateKey = String.format(Locale.ROOT, "%04d-%02d-%02d", viewYear, viewMonth + 1, c.get(Calendar.DAY_OF_MONTH))
                            viewModel.setSelectedCalendarDate(selectedDateKey)
                        },
                        modifier = Modifier.testTag("today_button")
                    ) {
                        Text(
                            text = "Hoy",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceBright)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Month Header & Navigation (< Mes Año >)
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
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (viewMonth == 0) {
                                        viewMonth = 11
                                        viewYear--
                                    } else {
                                        viewMonth--
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

                            Text(
                                text = "${SPANISH_MONTHS[viewMonth]} $viewYear",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface,
                                    fontSize = 17.sp
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (viewMonth == 11) {
                                        viewMonth = 0
                                        viewYear++
                                    } else {
                                        viewMonth++
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

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekdays Row (L, M, X, J, V, S, D)
                        val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekDays.forEach { dayLetter ->
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLetter,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dynamic Calendar Matrix Generator
                        val daysMatrix = remember(viewYear, viewMonth) {
                            generateCalendarMonthMatrix(viewYear, viewMonth)
                        }

                        daysMatrix.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { dayNum ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNum > 0) {
                                            val cellDateKey = String.format(Locale.ROOT, "%04d-%02d-%02d", viewYear, viewMonth + 1, dayNum)
                                            val isSelected = cellDateKey == selectedDateKey
                                            val isToday = cellDateKey == todayDateKey

                                            val cellTasks = allTasks.filter { it.dateKey == cellDateKey }
                                            val hasAvocado = cellTasks.any { it.fruitType == "avocado" }
                                            val hasOrange = cellTasks.any { it.fruitType == "orange" || it.fruitType != "avocado" }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isSelected -> PrimaryGreen
                                                            isToday -> PrimaryGreenContainer.copy(alpha = 0.5f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                        color = if (isToday && !isSelected) PrimaryGreen else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        selectedDateKey = cellDateKey
                                                        viewModel.setSelectedCalendarDate(cellDateKey)
                                                    }
                                                    .padding(2.dp)
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else if (isToday) PrimaryGreen else OnSurface
                                                )

                                                // Colored dots: Orange for Citrus, Green for Avocado
                                                if (cellTasks.isNotEmpty()) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    ) {
                                                        if (hasOrange) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color(0xFFFFD180) else SecondaryOrangeDark)
                                                            )
                                                        }
                                                        if (hasAvocado) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color(0xFFA7F3D0) else Color(0xFF16A34A))
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

                        // Legend for dots
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(SecondaryOrangeDark)
                                )
                                Text(text = "Cítricos / Naranjas", fontSize = 11.sp, color = OnSurfaceVariant)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A))
                                )
                                Text(text = "Aguacate", fontSize = 11.sp, color = OnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Selected Day Banner & Add Task Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = selectedDateDisplay,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                        Text(
                            text = "${dayTasks.size} ${if (dayTasks.size == 1) "tarea programada" else "tareas programadas"}",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddTaskDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_task_day_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(text = "Tarea", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Selected Day Tasks List
            if (dayTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventAvailable,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No hay tareas para este día",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurface
                                )
                                Text(
                                    text = "Pulsa '+' o 'Añadir Tarea' para programar una faena.",
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
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
                            text = "Desliza derecha: completar/reactivar ➔ | ⬅ Desliza izquierda: eliminar",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(dayTasks, key = { it.id }) { task ->
                    SwipeableCalendarTaskCard(
                        task = task,
                        onCompleteRequest = { taskToComplete = task },
                        onReactivateRequest = { taskToReactivate = task },
                        onDeleteRequest = { taskToDelete = task },
                        onAlarmRequest = { taskForAlarmDialog = task }
                    )
                }
            }

            // Search and Filter Expandable Section
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Section Header with Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isFilterSectionExpanded = !isFilterSectionExpanded },
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
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Buscar y Filtrar Tareas",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (filterOwner != "Todos" || filterOrchardId != null || filterTaskType != "Todas" || filterDateRange != "Todas" || filterStatus != "Todas" || filterReminder != "Todas") {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(PrimaryGreenContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "Filtro activo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                    }
                                }
                                Icon(
                                    imageVector = if (isFilterSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = isFilterSectionExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                                // Filter 1: Por Propietario
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Propietario del huerto:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                    var ownerMenuExpanded by remember { mutableStateOf(false) }
                                    val owners = listOf(
                                        "Todos",
                                        userProfile.titularPropios.ifEmpty { "Propiedad" },
                                        userProfile.titularVr.ifEmpty { "V&R C.B." },
                                        userProfile.titularOtros.ifEmpty { "Otros" }
                                    )
                                    ExposedDropdownMenuBox(
                                        expanded = ownerMenuExpanded,
                                        onExpandedChange = { ownerMenuExpanded = !ownerMenuExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = filterOwner,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerMenuExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = appOutlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = ownerMenuExpanded,
                                            onDismissRequest = { ownerMenuExpanded = false }
                                        ) {
                                            owners.forEach { o ->
                                                DropdownMenuItem(
                                                    text = { Text(o, color = OnSurface) },
                                                    onClick = {
                                                        filterOwner = o
                                                        ownerMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Filter 2: Por Huerto
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Huerto:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                    var orchardMenuExpanded by remember { mutableStateOf(false) }
                                    val selectedOrchardName = allOrchards.find { it.id == filterOrchardId }?.name ?: "Todos los huertos"

                                    ExposedDropdownMenuBox(
                                        expanded = orchardMenuExpanded,
                                        onExpandedChange = { orchardMenuExpanded = !orchardMenuExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedOrchardName,
                                            onValueChange = {},
                                            readOnly = true,
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orchardMenuExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = appOutlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = orchardMenuExpanded,
                                            onDismissRequest = { orchardMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Todos los huertos", color = OnSurface) },
                                                onClick = {
                                                    filterOrchardId = null
                                                    orchardMenuExpanded = false
                                                }
                                            )
                                            allOrchards.forEach { orch ->
                                                DropdownMenuItem(
                                                    text = { Text("${orch.name} (${orch.variety})", color = OnSurface) },
                                                    onClick = {
                                                        filterOrchardId = orch.id
                                                        orchardMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Filter 3: Por Tarea
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Tipo de tarea:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                    var taskMenuExpanded by remember { mutableStateOf(false) }

                                    ExposedDropdownMenuBox(
                                        expanded = taskMenuExpanded,
                                        onExpandedChange = { taskMenuExpanded = !taskMenuExpanded }
                                    ) {
                                        val filterVisual = if (filterTaskType != "Todas") getFarmTaskVisualInfo(filterTaskType) else null
                                        OutlinedTextField(
                                            value = filterTaskType,
                                            onValueChange = {},
                                            readOnly = true,
                                            leadingIcon = filterVisual?.let { vis ->
                                                {
                                                    Icon(
                                                        imageVector = vis.icon,
                                                        contentDescription = null,
                                                        tint = vis.iconColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskMenuExpanded) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .menuAnchor(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = appOutlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = taskMenuExpanded,
                                            onDismissRequest = { taskMenuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Assignment,
                                                        contentDescription = null,
                                                        tint = OnSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                },
                                                text = { Text("Todas", color = OnSurface) },
                                                onClick = {
                                                    filterTaskType = "Todas"
                                                    taskMenuExpanded = false
                                                }
                                            )
                                            PREDEFINED_FARM_TASKS.forEach { t ->
                                                val vis = getFarmTaskVisualInfo(t)
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = vis.icon,
                                                            contentDescription = null,
                                                            tint = vis.iconColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    },
                                                    text = { Text(t) },
                                                    onClick = {
                                                        filterTaskType = t
                                                        taskMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Filter 4: Rango de fecha
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "Rango de fecha:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                                    val dateRanges = listOf("Todas", "Hoy", "Esta Semana", "Este Mes", "Próximos 30 días")
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        dateRanges.take(3).forEach { r ->
                                            FilterChip(
                                                selected = filterDateRange == r,
                                                onClick = { filterDateRange = r },
                                                label = { Text(r, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryGreen,
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        dateRanges.drop(3).forEach { r ->
                                            FilterChip(
                                                selected = filterDateRange == r,
                                                onClick = { filterDateRange = r },
                                                label = { Text(r, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryGreen,
                                                    selectedLabelColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }

                                // Filter 5: Tareas con notificaciones
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Tareas con notificaciones:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceVariant
                                    )
                                    val reminderOptions = listOf("Todas", "Con aviso", "Sin aviso")
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        reminderOptions.forEach { opt ->
                                            val isSelected = when (opt) {
                                                "Todas" -> filterReminder == "Todas"
                                                "Con aviso" -> filterReminder == "Con aviso" || filterReminder == "Con notificación"
                                                "Sin aviso" -> filterReminder == "Sin aviso" || filterReminder == "Sin notificación"
                                                else -> false
                                            }
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { filterReminder = opt },
                                                label = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (opt == "Con aviso") {
                                                            Icon(
                                                                imageVector = Icons.Default.NotificationsActive,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                        }
                                                        Text(opt, fontSize = 11.5.sp)
                                                    }
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryGreen,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }
                                }

                                // Reset filters button
                                if (filterOwner != "Todos" || filterOrchardId != null || filterTaskType != "Todas" || filterDateRange != "Todas" || filterStatus != "Todas" || filterReminder != "Todas") {
                                    OutlinedButton(
                                        onClick = {
                                            filterOwner = "Todos"
                                            filterOrchardId = null
                                            filterTaskType = "Todas"
                                            filterDateRange = "Todas"
                                            filterStatus = "Todas"
                                            filterReminder = "Todas"
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(text = "Limpiar Filtros", fontSize = 13.sp, color = PrimaryGreen)
                                    }
                                }
                            }
                        }

                        // Filtered Results Summary
                        if (isFilterSectionExpanded) {
                            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resultados encontrados: ${filteredTasks.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                            }
                        }
                    }
                }
            }

            // If filter section is open, show filtered results
            if (isFilterSectionExpanded) {
                if (filteredTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No hay tareas que coincidan con los filtros seleccionados.",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredTasks, key = { "filter_${it.id}" }) { task ->
                        SwipeableCalendarTaskCard(
                            task = task,
                            showDate = true,
                            onCompleteRequest = { taskToComplete = task },
                            onReactivateRequest = { taskToReactivate = task },
                            onDeleteRequest = { taskToDelete = task },
                            onAlarmRequest = { taskForAlarmDialog = task }
                        )
                    }
                }
            } else {
                // When filter section is NOT open: show all tasks of the month from closest to last day of month
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tareas de ${SPANISH_MONTHS[viewMonth]} $viewYear",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = if (monthTasks.isNotEmpty()) {
                                    "${monthTasks.size} ${if (monthTasks.size == 1) "tarea" else "tareas"} • De la más próxima a fin de mes"
                                } else {
                                    "No hay tareas este mes"
                                },
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }

                        if (monthTasks.isNotEmpty()) {
                            Surface(
                                color = PrimaryGreenContainer,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "${monthTasks.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (monthTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.EventAvailable,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "No hay tareas programadas para ${SPANISH_MONTHS[viewMonth]}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Toca cualquier día del calendario para añadir una tarea.",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(monthTasks, key = { "month_${it.id}" }) { task ->
                        SwipeableCalendarTaskCard(
                            task = task,
                            showDate = true,
                            onCompleteRequest = { taskToComplete = task },
                            onReactivateRequest = { taskToReactivate = task },
                            onDeleteRequest = { taskToDelete = task },
                            onAlarmRequest = { taskForAlarmDialog = task }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddTaskDialog(
            allOrchards = allOrchards,
            selectedDateKey = selectedDateKey,
            titularPropios = userProfile.titularPropios,
            titularVr = userProfile.titularVr,
            titularOtros = userProfile.titularOtros,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { orchardId, orchardName, title, time, desc, fruitType, dateKey, reminderEnabled, reminderDate, reminderTime ->
                viewModel.addCalendarTask(
                    orchardId = orchardId,
                    orchardName = orchardName,
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
                val reminderMsg = if (reminderEnabled) " con aviso programado para ${formatDateToDisplay(reminderDate)}" else ""
                Toast.makeText(context, "Tarea '$title' guardada$reminderMsg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirm Complete Dialog
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
                        text = "¿Deseas marcar como completada la tarea '${task.title}' en el huerto '${task.orchardName}'?",
                        fontSize = 14.sp,
                        color = OnSurface
                    )
                    Text(
                        text = "Puedes completar la faena y abrir directamente el nuevo parte de trabajo con el huerto y la labor ya seleccionados.",
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
                            .testTag("complete_task_and_create_workpart_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            modifier = Modifier.testTag("cancel_complete_task_button")
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
                            modifier = Modifier.testTag("only_complete_task_button")
                        ) {
                            Text("Solo completar", fontSize = 13.sp, color = PrimaryGreenDark)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    // Confirm Reactivate Dialog
    if (taskToReactivate != null) {
        val task = taskToReactivate!!
        AlertDialog(
            onDismissRequest = { taskToReactivate = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PrimaryGreen)
                    Text(text = "Reactivar Tarea", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "¿Deseas volver a activar la tarea '${task.title}' en '${task.orchardName}' y marcarla de nuevo como pendiente?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleCalendarTaskDone(task, false)
                        taskToReactivate = null
                        Toast.makeText(context, "Tarea reactivada como pendiente", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Sí, Reactivar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToReactivate = null }) {
                    Text("Cancelar", color = OnSurfaceVariant)
                }
            }
        )
    }

    // Confirm Delete Dialog
    if (taskToDelete != null) {
        val task = taskToDelete!!
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626))
                    Text(text = "Eliminar Tarea", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar la tarea '${task.title}' de '${task.orchardName}'? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCalendarTask(task)
                        taskToDelete = null
                        Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancelar", color = OnSurfaceVariant)
                }
            }
        )
    }

    // Task Alarm Configuration & Test Dialog
    if (taskForAlarmDialog != null) {
        val currentTask = taskForAlarmDialog!!
        TaskAlarmConfigDialog(
            task = currentTask,
            onDismiss = { taskForAlarmDialog = null },
            onSave = { reminderEnabled, reminderDate, reminderTime ->
                viewModel.updateCalendarTaskReminder(currentTask.id, reminderEnabled, reminderDate, reminderTime)
                taskForAlarmDialog = null
                val msg = if (reminderEnabled) "Alarma guardada y programada para ${formatDateToDisplay(reminderDate)} $reminderTime" else "Alarma desactivada"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onTestAlarm = { taskToTest ->
                viewModel.testTaskAlarm(taskToTest)
                Toast.makeText(context, "Pop-up de alarma lanzado al móvil", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// Swipeable Task Card with Swipe Gestures and Confirmations
@Composable
fun SwipeableCalendarTaskCard(
    task: CalendarTaskEntity,
    showDate: Boolean = false,
    onCompleteRequest: () -> Unit,
    onReactivateRequest: () -> Unit = onCompleteRequest,
    onDeleteRequest: () -> Unit,
    onAlarmRequest: (() -> Unit)? = null
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")

    val isAvocado = task.fruitType == "avocado"
    val badgeBg = if (isAvocado) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
    val badgeColor = if (isAvocado) Color(0xFF16A34A) else SecondaryOrangeDark

    val taskVisual = getFarmTaskVisualInfo(task.title)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        // Background for Swipe Right (Complete - Green / Reactivate - Amber/Orange) & Swipe Left (Delete - Red)
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(
                    when {
                        offsetX > 30 -> if (task.isDone) Color(0xFFD97706) else Color(0xFF16A34A)
                        offsetX < -30 -> Color(0xFFDC2626)
                        else -> SurfaceContainerLow
                    }
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End
        ) {
            if (offsetX > 30) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (task.isDone) Icons.Default.Refresh else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = if (task.isDone) "Reactivar" else "Completar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            } else if (offsetX < -30) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Eliminar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
                }
            }
        }

        // Foreground Card
        val isDark = LocalIsDarkTheme.current
        val cardBgColor = when {
            task.isDone && isDark -> Color(0xFF142614)
            task.isDone && !isDark -> Color(0xFFF0FDF4)
            else -> SurfaceWhite
        }
        val cardBorderColor = when {
            task.isDone && isDark -> Color(0xFF166534)
            task.isDone && !isDark -> Color(0xFF86EFAC)
            else -> OutlineVariant
        }
        val cardBorderWidth = if (task.isDone) 1.5.dp else 1.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(-180f, 180f)
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (offsetX > 80f) {
                            if (task.isDone) {
                                onReactivateRequest()
                            } else {
                                onCompleteRequest()
                            }
                        } else if (offsetX < -80f) {
                            onDeleteRequest()
                        }
                        offsetX = 0f
                    }
                )
                .clip(RoundedCornerShape(10.dp))
                .border(cardBorderWidth, cardBorderColor, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = cardBgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(taskVisual.containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = taskVisual.icon,
                                contentDescription = null,
                                tint = taskVisual.iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = task.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isDone) PrimaryGreenDark else OnSurface,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (showDate) {
                                    Surface(
                                        color = PrimaryGreenContainer.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = formatTaskDateBadge(task.dateKey),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            maxLines = 1
                                        )
                                    }
                                    Text(text = "•", fontSize = 10.sp, color = OnSurfaceVariant)
                                }
                                Text(
                                    text = task.timeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Orchard badge and Status tag (stacked vertically so they never crowd the title or icon!)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (onAlarmRequest != null) {
                                Surface(
                                    color = if (task.reminderEnabled) Color(0xFFFFF7ED) else SurfaceContainerHigh,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, if (task.reminderEnabled) Color(0xFFFED7AA) else OutlineVariant),
                                    modifier = Modifier.clickable { onAlarmRequest() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (task.reminderEnabled) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                                            contentDescription = "Alarma",
                                            tint = if (task.reminderEnabled) Color(0xFFEA580C) else OnSurfaceVariant,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = if (task.reminderEnabled) "Alarma" else "+Alarma",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (task.reminderEnabled) Color(0xFFEA580C) else OnSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = task.orchardName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Notification Reminder Badge (clear, elegant pill with human-readable date & time)
                if (task.reminderEnabled) {
                    val isDark = LocalIsDarkTheme.current
                    Surface(
                        color = if (isDark) Color(0xFF431D05) else Color(0xFFFFF7ED),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF7C2D12) else Color(0xFFFED7AA)),
                        modifier = if (onAlarmRequest != null) Modifier.clickable { onAlarmRequest() } else Modifier
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Aviso programado",
                                tint = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formatReminderBadgeText(task.reminderDate, task.reminderTime, task.dateKey),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onAlarmRequest != null) {
                                Text(
                                    text = "• Editar",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
                                )
                            }
                        }
                    }
                }

                // Description
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// Add Task Dialog Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    allOrchards: List<OrchardEntity>,
    selectedDateKey: String = "",
    initialOrchardId: Long? = null,
    initialOrchard: OrchardEntity? = null,
    titularPropios: String = "Propiedad",
    titularVr: String = "V&R C.B.",
    titularOtros: String = "Otros",
    onDismiss: () -> Unit,
    onConfirm: (
        orchardId: Long?,
        orchardName: String,
        title: String,
        time: String,
        desc: String,
        fruitType: String,
        dateKey: String,
        reminderEnabled: Boolean,
        reminderDate: String,
        reminderTime: String
    ) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) }

    var ownerDropdownExpanded by remember { mutableStateOf(false) }
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

    var selectedOrchard by remember {
        mutableStateOf(
            initialOrchard 
                ?: (if (initialOrchardId != null) allOrchards.firstOrNull { it.id == initialOrchardId } else null)
                ?: allOrchards.firstOrNull()
        )
    }
    var isOwnLabor by remember {
        mutableStateOf(
            selectedOrchard?.ownerType?.lowercase() in listOf("propios", "propiedad")
        )
    }

    LaunchedEffect(selectedOrchard?.id) {
        val isProp = selectedOrchard?.ownerType?.lowercase() in listOf("propios", "propiedad")
        isOwnLabor = isProp
    }

    var selectedTaskName by remember {
        mutableStateOf(PREDEFINED_FARM_TASKS.first())
    }
    var taskTime by remember { mutableStateOf("08:00") }
    var observations by remember { mutableStateOf("") }
    var dateKey by remember { mutableStateOf(selectedDateKey.ifBlank { todayKey }) }

    // Reminder notification state
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderDate by remember { mutableStateOf(selectedDateKey.ifBlank { todayKey }) }
    var reminderTime by remember { mutableStateOf("07:30") }

    var orchardDropdownExpanded by remember { mutableStateOf(false) }
    var taskDropdownExpanded by remember { mutableStateOf(false) }

    // Compute fruit type based on selected orchard
    val fruitType = remember(selectedOrchard) {
        val o = selectedOrchard
        if (o != null) {
            if (o.fruitType.contains("Aguacate", ignoreCase = true) ||
                o.variety.contains("Aguacate", ignoreCase = true) ||
                o.variety.contains("Hass", ignoreCase = true) ||
                o.variety.contains("Bacon", ignoreCase = true) ||
                o.variety.contains("Fuerte", ignoreCase = true) ||
                o.variety.contains("Lamb", ignoreCase = true)
            ) "avocado" else "orange"
        } else "orange"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de Propietario / Comunidad
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Propietario / Comunidad:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = ownerDropdownExpanded,
                        onExpandedChange = { ownerDropdownExpanded = !ownerDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedOwnerLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = ownerDropdownExpanded,
                            onDismissRequest = { ownerDropdownExpanded = false }
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
                                        ownerDropdownExpanded = false
                                        val newFiltered = when (key) {
                                            "propios" -> allOrchards.filter { it.ownerType.lowercase() in listOf("propios", "propiedad") }
                                            "vr" -> allOrchards.filter { it.ownerType.lowercase() in listOf("v_y_r_cb", "vr", "cb", "v&r") }
                                            "otros" -> allOrchards.filter { it.ownerType.lowercase() == "otros" }
                                            else -> allOrchards
                                        }
                                        if (newFiltered.isNotEmpty() && (selectedOrchard == null || !newFiltered.contains(selectedOrchard))) {
                                            selectedOrchard = newFiltered.first()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 1. Selector de Huerto
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Huerto vinculado:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = orchardDropdownExpanded,
                        onExpandedChange = { orchardDropdownExpanded = !orchardDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedOrchard?.let { "${it.name} (${if (fruitType == "avocado") "🥑 Aguacate" else "🍊 Cítrico"})" } ?: "Seleccionar Huerto",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orchardDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = orchardDropdownExpanded,
                            onDismissRequest = { orchardDropdownExpanded = false }
                        ) {
                            if (filteredOrchards.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay huertos para este propietario", color = OnSurfaceVariant) },
                                    onClick = { orchardDropdownExpanded = false }
                                )
                            } else {
                                filteredOrchards.forEach { orch ->
                                    val isAvoc = orch.fruitType.contains("Aguacate", ignoreCase = true) || orch.variety.contains("Aguacate", ignoreCase = true) || orch.variety.contains("Hass", ignoreCase = true)
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isAvoc) Color(0xFF16A34A) else SecondaryOrangeDark)
                                                )
                                                Text("${orch.name} - ${orch.variety} (${if (isAvoc) "Aguacate" else "Cítricos"})", color = OnSurface)
                                            }
                                        },
                                        onClick = {
                                            selectedOrchard = orch
                                            orchardDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Selector de Tarea
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Tarea a realizar:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = taskDropdownExpanded,
                        onExpandedChange = { taskDropdownExpanded = !taskDropdownExpanded }
                    ) {
                        val currentVisual = getFarmTaskVisualInfo(selectedTaskName)
                        OutlinedTextField(
                            value = selectedTaskName,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(currentVisual.containerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = currentVisual.icon,
                                        contentDescription = null,
                                        tint = currentVisual.iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taskDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = taskDropdownExpanded,
                            onDismissRequest = { taskDropdownExpanded = false }
                        ) {
                            PREDEFINED_FARM_TASKS.forEach { farmTask ->
                                val vis = getFarmTaskVisualInfo(farmTask)
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
                                    text = { Text(farmTask, fontWeight = FontWeight.Medium, color = OnSurface) },
                                    onClick = {
                                        selectedTaskName = farmTask
                                        taskDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. Fecha y Hora en 2 columnas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Fecha
                    OutlinedTextField(
                        value = formatDateToDisplay(dateKey),
                        onValueChange = { },
                        label = { Text("Fecha") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    com.example.ui.util.showAndroidDatePicker(context, dateKey) { picked ->
                                        dateKey = picked
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Seleccionar fecha",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f),
                        colors = appOutlinedTextFieldColors()
                    )

                    // Hora
                    OutlinedTextField(
                        value = taskTime,
                        onValueChange = { taskTime = formatTimeInput(it) },
                        label = { Text("Hora") },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        keyboardOptions = AppKeyboards.Time,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showAndroidTimePicker(context, taskTime) { picked ->
                                        taskTime = picked
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "Seleccionar hora",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = appOutlinedTextFieldColors()
                    )
                }

                // 4. Tipo de Mano de Obra (Horas Propias vs Contratada)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mano de obra prevista:",
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
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOwnLabor) Icons.Default.CheckCircle else Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Horas Propias",
                                        fontWeight = if (isOwnLabor) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "Mi trabajo (sin gasto caja)",
                                    fontSize = 9.sp,
                                    color = if (isOwnLabor) (if (isDark) Color(0xFFBAE6FD) else Color(0xFF0369A1)) else OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

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
                            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (!isOwnLabor) Icons.Default.CheckCircle else Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Contratadas",
                                        fontWeight = if (!isOwnLabor) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "Mano de obra pagada",
                                    fontSize = 9.sp,
                                    color = if (!isOwnLabor) (if (isDark) Color(0xFFFEF3C7) else Color(0xFF92400E)) else OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // 5. Observaciones
                OutlinedTextField(
                    value = observations,
                    onValueChange = { observations = it },
                    label = { Text("Detalles / Notas") },
                    placeholder = { Text("Anotar detalles, cuadrilla, productos...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = AppKeyboards.Text,
                    colors = appOutlinedTextFieldColors()
                )

                // 6. Notificación / Recordatorio programado
                Column(
                    modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = if (reminderEnabled) PrimaryGreen else OnSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Aviso de notificación",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface
                                )
                                Text(
                                    text = if (reminderEnabled) "Se te notificará en la fecha y hora fijada" else "Activar para recibir aviso previo",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryGreen,
                                checkedTrackColor = PrimaryGreenContainer
                            )
                        )
                    }

                    // Selectores de fecha y hora del recordatorio si está activado
                    AnimatedVisibility(visible = reminderEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Fecha del aviso
                                OutlinedTextField(
                                    value = formatDateToDisplay(reminderDate.ifBlank { dateKey }),
                                    onValueChange = { },
                                    label = { Text("Día aviso", fontSize = 11.sp) },
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                com.example.ui.util.showAndroidDatePicker(context, reminderDate.ifBlank { dateKey }) { picked ->
                                                    reminderDate = picked
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "Fecha aviso",
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.2f),
                                    colors = appOutlinedTextFieldColors()
                                )

                                // Hora del aviso
                                OutlinedTextField(
                                    value = reminderTime,
                                    onValueChange = { reminderTime = formatTimeInput(it) },
                                    label = { Text("Hora aviso", fontSize = 11.sp) },
                                    placeholder = { Text("07:30") },
                                    singleLine = true,
                                    keyboardOptions = AppKeyboards.Time,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                showAndroidTimePicker(context, reminderTime) { picked ->
                                                    reminderTime = picked
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = "Hora aviso",
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    colors = appOutlinedTextFieldColors()
                                )
                            }

                            // Botón de prueba inmediata del pop-up en este móvil
                            OutlinedButton(
                                onClick = {
                                    val orchName = selectedOrchard?.name ?: allOrchards.firstOrNull()?.name ?: "Huerto"
                                    val testTask = CalendarTaskEntity(
                                        id = 999999L,
                                        orchardName = orchName,
                                        title = selectedTaskName.ifBlank { "Labor de campo" },
                                        timeText = taskTime,
                                        dateKey = dateKey,
                                        description = observations.ifBlank { "Aviso de alarma programada" },
                                        reminderEnabled = true,
                                        reminderDate = reminderDate.ifBlank { dateKey },
                                        reminderTime = reminderTime.ifBlank { taskTime }
                                    )
                                    TaskAlarmScheduler.triggerImmediateTaskAlarm(context, testTask)
                                    Toast.makeText(context, "🔔 ¡Pop-up de alarma disparado en este teléfono!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEA580C)),
                                border = BorderStroke(1.dp, Color(0xFFEA580C))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Probar Pop-up en este móvil ahora",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = if (selectedTaskName.isNotBlank()) selectedTaskName else "Faena general"
                    val orchName = selectedOrchard?.name ?: allOrchards.firstOrNull()?.name ?: "Huerto"
                    val orchId = selectedOrchard?.id ?: allOrchards.firstOrNull()?.id
                    val effectiveReminderDate = if (reminderEnabled) reminderDate.ifBlank { dateKey } else ""
                    val effectiveReminderTime = if (reminderEnabled) reminderTime.ifBlank { taskTime } else ""
                    val laborTag = if (isOwnLabor) "[HORAS_PROPIAS]" else "[HORAS_CONTRATADAS]"
                    val finalObservations = if (observations.isBlank()) laborTag else "$observations $laborTag"
                    onConfirm(
                        orchId,
                        orchName,
                        finalTitle,
                        taskTime,
                        finalObservations,
                        fruitType,
                        dateKey,
                        reminderEnabled,
                        effectiveReminderDate,
                        effectiveReminderTime
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Guardar Tarea", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OnSurfaceVariant)
            }
        }
    )
}

// Helper function to build dynamic monthly matrix starting with Monday (index 0)
private fun generateCalendarMonthMatrix(year: Int, month: Int): List<List<Int>> {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ... 7 = Saturday

    // Convert to Monday = 0, Tuesday = 1, ... Sunday = 6
    val offset = (firstDayOfWeek + 5) % 7

    val matrix = mutableListOf<List<Int>>()
    var currentDay = 1
    var week = mutableListOf<Int>()

    // First week fillers
    for (i in 0 until offset) {
        week.add(0)
    }

    while (currentDay <= maxDaysInMonth) {
        week.add(currentDay)
        if (week.size == 7) {
            matrix.add(week)
            week = mutableListOf()
        }
        currentDay++
    }

    // Last week fillers
    if (week.isNotEmpty()) {
        while (week.size < 7) {
            week.add(0)
        }
        matrix.add(week)
    }

    return matrix
}
