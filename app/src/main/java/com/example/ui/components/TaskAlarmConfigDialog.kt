package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarTaskEntity
import com.example.ui.theme.*
import com.example.ui.util.formatDateToDisplay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskAlarmConfigDialog(
    task: CalendarTaskEntity,
    onDismiss: () -> Unit,
    onSave: (reminderEnabled: Boolean, reminderDate: String, reminderTime: String) -> Unit,
    onTestAlarm: (CalendarTaskEntity) -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(task.reminderEnabled) }
    var selectedDate by remember { mutableStateOf(task.reminderDate.ifBlank { task.dateKey }) }
    var selectedTime by remember { mutableStateOf(task.reminderTime.ifBlank { task.timeText.ifBlank { "08:00" } }) }

    // Dialog state for pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val orangeColor = Color(0xFFEA580C)
    val lightOrange = Color(0xFFFFF7ED)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (enabled) orangeColor.copy(alpha = 0.15f) else SurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Outlined.NotificationsNone,
                        contentDescription = null,
                        tint = if (enabled) orangeColor else OnSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Alarma de Tarea",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = task.title,
                        fontSize = 13.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Orchard info banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${task.orchardName} • Programada: ${formatDateToDisplay(task.dateKey)} (${task.timeText})",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Switch row: Activar Alarma / Pop-up en el móvil
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (enabled) lightOrange else SurfaceWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (enabled) orangeColor.copy(alpha = 0.5f) else OutlineVariant)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Aviso Pop-up en el Móvil",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (enabled) orangeColor else OnSurface
                            )
                            Text(
                                text = "Salto de alerta con sonido y vibración en tu dispositivo",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = { isChecked ->
                                enabled = isChecked
                                if (isChecked && selectedTime.isBlank()) {
                                    selectedTime = task.timeText.ifBlank { "08:00" }
                                }
                                if (isChecked && selectedDate.isBlank()) {
                                    selectedDate = task.dateKey
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = orangeColor
                            )
                        )
                    }
                }

                if (enabled) {
                    // Quick presets row
                    Text(
                        text = "Preajustes rápidos de aviso:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(
                            label = "A la hora",
                            selected = selectedDate == task.dateKey && selectedTime == task.timeText,
                            onClick = {
                                selectedDate = task.dateKey
                                selectedTime = task.timeText.ifBlank { "08:00" }
                            }
                        )
                        PresetChip(
                            label = "30 min antes",
                            selected = false,
                            onClick = {
                                selectedDate = task.dateKey
                                selectedTime = calculateOffsetTime(task.timeText, -30)
                            }
                        )
                        PresetChip(
                            label = "1 hora antes",
                            selected = false,
                            onClick = {
                                selectedDate = task.dateKey
                                selectedTime = calculateOffsetTime(task.timeText, -60)
                            }
                        )
                        PresetChip(
                            label = "Día antes (20:00)",
                            selected = false,
                            onClick = {
                                selectedDate = calculateOffsetDate(task.dateKey, -1)
                                selectedTime = "20:00"
                            }
                        )
                    }

                    // Date & Time selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date picker trigger
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceWhite)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Fecha aviso",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
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
                                        text = formatDateToDisplay(selectedDate),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                            }
                        }

                        // Time picker trigger
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showTimePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceWhite)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Hora aviso",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = orangeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedTime,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                            }
                        }
                    }

                    // Test Alarm Button
                    Button(
                        onClick = {
                            val tempTask = task.copy(
                                reminderEnabled = true,
                                reminderDate = selectedDate,
                                reminderTime = selectedTime
                            )
                            onTestAlarm(tempTask)
                            Toast.makeText(context, "¡Pop-up de prueba lanzado al móvil!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = orangeColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Probar Pop-up en este móvil ahora",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(enabled, selectedDate, selectedTime)
                    val msg = if (enabled) {
                        "Alarma programada: ${formatDateToDisplay(selectedDate)} a las $selectedTime"
                    } else {
                        "Alarma desactivada para esta tarea"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (enabled) orangeColor else PrimaryGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (enabled) "Guardar Alarma" else "Guardar Cambios",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = OnSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    )

    // Sub-dialogs for Date & Time
    if (showDatePicker) {
        SimpleDatePickerDialog(
            initialDateKey = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                selectedDate = newDate
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        SimpleTimePickerDialog(
            initialTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { newTime ->
                selectedTime = newTime
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (selected) Color(0xFFEA580C) else OutlineVariant,
                RoundedCornerShape(16.dp)
            ),
        color = if (selected) Color(0xFFFFF7ED) else SurfaceWhite
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFFEA580C) else OnSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun calculateOffsetTime(timeText: String, offsetMinutes: Int): String {
    return try {
        val parts = timeText.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            add(Calendar.MINUTE, offsetMinutes)
        }
        String.format(Locale.ROOT, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    } catch (e: Exception) {
        timeText
    }
}

private fun calculateOffsetDate(dateKey: String, offsetDays: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
        val d = sdf.parse(dateKey) ?: return dateKey
        val cal = Calendar.getInstance().apply {
            time = d
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }
        sdf.format(cal.time)
    } catch (e: Exception) {
        dateKey
    }
}

@Composable
fun SimpleDatePickerDialog(
    initialDateKey: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val initialCal = remember {
        Calendar.getInstance().apply {
            try {
                val parts = initialDateKey.split("-")
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            } catch (e: Exception) {}
        }
    }

    var year by remember { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar Fecha de Aviso", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val c = Calendar.getInstance().apply {
                            set(year, month, day)
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        year = c.get(Calendar.YEAR)
                        month = c.get(Calendar.MONTH)
                        day = c.get(Calendar.DAY_OF_MONTH)
                    }) {
                        Text("◀ Día anterior", fontSize = 12.sp)
                    }

                    Text(
                        text = String.format(Locale.ROOT, "%02d/%02d/%04d", day, month + 1, year),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )

                    TextButton(onClick = {
                        val c = Calendar.getInstance().apply {
                            set(year, month, day)
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        year = c.get(Calendar.YEAR)
                        month = c.get(Calendar.MONTH)
                        day = c.get(Calendar.DAY_OF_MONTH)
                    }) {
                        Text("Día siguiente ▶", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val today = Calendar.getInstance()
                            year = today.get(Calendar.YEAR)
                            month = today.get(Calendar.MONTH)
                            day = today.get(Calendar.DAY_OF_MONTH)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLow),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hoy", color = OnSurface, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                            year = tomorrow.get(Calendar.YEAR)
                            month = tomorrow.get(Calendar.MONTH)
                            day = tomorrow.get(Calendar.DAY_OF_MONTH)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerLow),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mañana", color = OnSurface, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDateSelected(String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day))
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 13.sp)
            }
        }
    )
}

@Composable
fun SimpleTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initialParts = remember {
        try {
            val p = initialTime.split(":")
            Pair(p[0].toInt(), p[1].toInt())
        } catch (e: Exception) {
            Pair(8, 0)
        }
    }

    var hour by remember { mutableStateOf(initialParts.first) }
    var minute by remember { mutableStateOf(initialParts.second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar Hora de Aviso", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = String.format(Locale.ROOT, "%02d:%02d", hour, minute),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFEA580C)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hora", fontSize = 12.sp, color = OnSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { hour = if (hour > 0) hour - 1 else 23 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos horas")
                            }
                            Text(
                                text = String.format(Locale.ROOT, "%02d", hour),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { hour = (hour + 1) % 24 }) {
                                Icon(Icons.Default.Add, contentDescription = "Más horas")
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minuto", fontSize = 12.sp, color = OnSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { minute = if (minute >= 5) minute - 5 else 55 }) {
                                Icon(Icons.Default.Remove, contentDescription = "Menos minutos")
                            }
                            Text(
                                text = String.format(Locale.ROOT, "%02d", minute),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { minute = (minute + 5) % 60 }) {
                                Icon(Icons.Default.Add, contentDescription = "Más minutos")
                            }
                        }
                    }
                }

                // Quick presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("07:00", "08:00", "09:00", "14:00", "18:00").forEach { preset ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val sp = preset.split(":")
                                    hour = sp[0].toInt()
                                    minute = sp[1].toInt()
                                },
                            color = SurfaceContainerLow
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeSelected(String.format(Locale.ROOT, "%02d:%02d", hour, minute))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar", fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 13.sp)
            }
        }
    )
}
