package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CalendarTaskEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AppTopHeader
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.theme.*

@Composable
fun NotificationSettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.notificationSettings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val titularVr = remember(userProfile.titularVr) { userProfile.titularVr.ifBlank { "V&R C.B." } }
    val titularOtros = remember(userProfile.titularOtros) { userProfile.titularOtros.ifBlank { "2ª C.B." } }

    // Dialog state for modifying threshold values
    var editingFieldTitle by remember { mutableStateOf<String?>(null) }
    var editingFieldUnit by remember { mutableStateOf("") }
    var editingFieldValue by remember { mutableStateOf("") }
    var onSaveValue by remember { mutableStateOf<((String) -> Unit)?>(null) }

    if (editingFieldTitle != null && onSaveValue != null) {
        var tempValue by remember(editingFieldTitle) { mutableStateOf(editingFieldValue) }
        AlertDialog(
            onDismissRequest = { editingFieldTitle = null },
            title = {
                Text(
                    text = "Modificar $editingFieldTitle",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Introduce el valor de activación ($editingFieldUnit):",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { input ->
                            if (input.isEmpty() || input == "-" || input.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                                tempValue = input
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            if (editingFieldUnit.isNotEmpty()) {
                                Text(
                                    text = editingFieldUnit,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = appOutlinedTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = tempValue.trim()
                        if (cleaned.isNotEmpty() && cleaned != "-") {
                            onSaveValue?.invoke(cleaned)
                        }
                        editingFieldTitle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFieldTitle = null }) {
                    Text("Cancelar", color = OnSurfaceVariant, fontSize = 13.sp)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "Ajustes de Notificaciones",
                showMenu = false,
                showBack = true,
                onBackClick = { viewModel.navigateBack() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceWhite)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header: Alertas Climáticas
            item {
                SettingsSectionHeader(title = "ALERTAS CLIMÁTICAS")
            }

            // Card: Alertas Climáticas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(OutlineVariant))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Heladas
                        MinimalistClimateAlertItem(
                            icon = Icons.Default.AcUnit,
                            iconTint = Color(0xFF0284C7),
                            iconContainer = Color(0xFFE0F2FE),
                            title = "Aviso de Heladas",
                            checked = settings.frostAlert,
                            onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(frostAlert = it)) },
                            thresholdLabel = "Umbral mín.",
                            thresholdValue = "${settings.frostThresholdTemp} °C",
                            onEditThreshold = {
                                editingFieldTitle = "Aviso de Helada"
                                editingFieldUnit = "°C"
                                editingFieldValue = settings.frostThresholdTemp
                                onSaveValue = { newVal ->
                                    viewModel.updateNotificationSettings(settings.copy(frostThresholdTemp = newVal))
                                }
                            },
                            notify7Days = settings.frostNotify7Days,
                            notify2Days = settings.frostNotify2Days,
                            notifyRealtime = settings.frostNotifyRealtime,
                            realtimeThresholdText = "${settings.frostThresholdTemp} °C",
                            onToggle7Days = { viewModel.updateNotificationSettings(settings.copy(frostNotify7Days = !settings.frostNotify7Days)) },
                            onToggle2Days = { viewModel.updateNotificationSettings(settings.copy(frostNotify2Days = !settings.frostNotify2Days)) },
                            onToggleRealtime = { viewModel.updateNotificationSettings(settings.copy(frostNotifyRealtime = !settings.frostNotifyRealtime)) }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // 2. Viento Fuerte
                        MinimalistClimateAlertItem(
                            icon = Icons.Default.Air,
                            iconTint = Color(0xFF0D9488),
                            iconContainer = Color(0xFFCCFBF1),
                            title = "Viento Fuerte",
                            checked = settings.windAlert,
                            onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(windAlert = it)) },
                            thresholdLabel = "Racha máx.",
                            thresholdValue = "${settings.windThresholdKmh} km/h",
                            onEditThreshold = {
                                editingFieldTitle = "Velocidad del Viento"
                                editingFieldUnit = "km/h"
                                editingFieldValue = settings.windThresholdKmh
                                onSaveValue = { newVal ->
                                    viewModel.updateNotificationSettings(settings.copy(windThresholdKmh = newVal))
                                }
                            },
                            notify7Days = settings.windNotify7Days,
                            notify2Days = settings.windNotify2Days,
                            notifyRealtime = settings.windNotifyRealtime,
                            realtimeThresholdText = "${settings.windThresholdKmh} km/h",
                            onToggle7Days = { viewModel.updateNotificationSettings(settings.copy(windNotify7Days = !settings.windNotify7Days)) },
                            onToggle2Days = { viewModel.updateNotificationSettings(settings.copy(windNotify2Days = !settings.windNotify2Days)) },
                            onToggleRealtime = { viewModel.updateNotificationSettings(settings.copy(windNotifyRealtime = !settings.windNotifyRealtime)) }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // 3. Olas de Calor
                        MinimalistClimateAlertItem(
                            icon = Icons.Default.WbSunny,
                            iconTint = Color(0xFFEA580C),
                            iconContainer = Color(0xFFFFEDD5),
                            title = "Olas de Calor",
                            checked = settings.heatAlert,
                            onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(heatAlert = it)) },
                            thresholdLabel = "Temp. máx.",
                            thresholdValue = "${settings.heatThresholdTemp} °C",
                            onEditThreshold = {
                                editingFieldTitle = "Temperatura Ola de Calor"
                                editingFieldUnit = "°C"
                                editingFieldValue = settings.heatThresholdTemp
                                onSaveValue = { newVal ->
                                    viewModel.updateNotificationSettings(settings.copy(heatThresholdTemp = newVal))
                                }
                            },
                            notify7Days = settings.heatNotify7Days,
                            notify2Days = settings.heatNotify2Days,
                            notifyRealtime = settings.heatNotifyRealtime,
                            realtimeThresholdText = "${settings.heatThresholdTemp} °C",
                            onToggle7Days = { viewModel.updateNotificationSettings(settings.copy(heatNotify7Days = !settings.heatNotify7Days)) },
                            onToggle2Days = { viewModel.updateNotificationSettings(settings.copy(heatNotify2Days = !settings.heatNotify2Days)) },
                            onToggleRealtime = { viewModel.updateNotificationSettings(settings.copy(heatNotifyRealtime = !settings.heatNotifyRealtime)) }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // 4. Lluvia
                        MinimalistClimateAlertItem(
                            icon = Icons.Default.WaterDrop,
                            iconTint = Color(0xFF2563EB),
                            iconContainer = Color(0xFFDBEAFE),
                            title = "Probabilidad de Lluvia",
                            checked = settings.rainAlert,
                            onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(rainAlert = it)) },
                            thresholdLabel = "Probab. mín.",
                            thresholdValue = "${settings.rainProbabilityThreshold} %",
                            onEditThreshold = {
                                editingFieldTitle = "Probabilidad de Lluvia"
                                editingFieldUnit = "%"
                                editingFieldValue = settings.rainProbabilityThreshold
                                onSaveValue = { newVal ->
                                    viewModel.updateNotificationSettings(settings.copy(rainProbabilityThreshold = newVal))
                                }
                            },
                            notify7Days = settings.rainNotify7Days,
                            notify2Days = settings.rainNotify2Days,
                            notifyRealtime = settings.rainNotifyRealtime,
                            realtimeThresholdText = "${settings.rainProbabilityThreshold}%",
                            onToggle7Days = { viewModel.updateNotificationSettings(settings.copy(rainNotify7Days = !settings.rainNotify7Days)) },
                            onToggle2Days = { viewModel.updateNotificationSettings(settings.copy(rainNotify2Days = !settings.rainNotify2Days)) },
                            onToggleRealtime = { viewModel.updateNotificationSettings(settings.copy(rainNotifyRealtime = !settings.rainNotifyRealtime)) }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // 5. Granizo
                        MinimalistClimateAlertItem(
                            icon = Icons.Default.Grain,
                            iconTint = Color(0xFF7C3AED),
                            iconContainer = Color(0xFFEDE9FE),
                            title = "Probabilidad de Granizo",
                            checked = settings.hailAlert,
                            onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(hailAlert = it)) },
                            thresholdLabel = "Probab. mín.",
                            thresholdValue = "${settings.hailProbabilityThreshold} %",
                            onEditThreshold = {
                                editingFieldTitle = "Probabilidad de Granizo"
                                editingFieldUnit = "%"
                                editingFieldValue = settings.hailProbabilityThreshold
                                onSaveValue = { newVal ->
                                    viewModel.updateNotificationSettings(settings.copy(hailProbabilityThreshold = newVal))
                                }
                            },
                            notify7Days = settings.hailNotify7Days,
                            notify2Days = settings.hailNotify2Days,
                            notifyRealtime = settings.hailNotifyRealtime,
                            realtimeThresholdText = "${settings.hailProbabilityThreshold}%",
                            onToggle7Days = { viewModel.updateNotificationSettings(settings.copy(hailNotify7Days = !settings.hailNotify7Days)) },
                            onToggle2Days = { viewModel.updateNotificationSettings(settings.copy(hailNotify2Days = !settings.hailNotify2Days)) },
                            onToggleRealtime = { viewModel.updateNotificationSettings(settings.copy(hailNotifyRealtime = !settings.hailNotifyRealtime)) }
                        )
                    }
                }
            }

            // Header: Actividad y Comunidad
            item {
                SettingsSectionHeader(title = "ACTIVIDAD Y COMUNIDAD")
            }

            // Card: Actividad y Comunidad
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(OutlineVariant))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Publicaciones de Socios
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.newSocialPosts) Color(0xFFEEF2FF) else SurfaceWhite)
                                        .border(1.dp, if (settings.newSocialPosts) Color(0xFFEEF2FF) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = null,
                                        tint = if (settings.newSocialPosts) Color(0xFF4F46E5) else Color(0xFF4F46E5).copy(alpha = 0.55f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Avisos de Socios en el Móvil",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Pop-up cuando otros socios publiquen o completen tareas y partes",
                                        fontSize = 12.sp,
                                        color = if (settings.newSocialPosts) Color(0xFF4F46E5) else OnSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Switch(
                                checked = settings.newSocialPosts,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(newSocialPosts = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // Resumen Semanal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.weeklyCostSummary) Color(0xFFFEF3C7) else SurfaceWhite)
                                        .border(1.dp, if (settings.weeklyCostSummary) Color(0xFFFEF3C7) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = if (settings.weeklyCostSummary) Color(0xFFD97706) else Color(0xFFD97706).copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen Semanal de Costes",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Balance consolidado cada lunes por la mañana",
                                        fontSize = 12.sp,
                                        color = if (settings.weeklyCostSummary) Color(0xFFD97706) else OnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.weeklyCostSummary,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(weeklyCostSummary = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // Resumen Mensual
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.monthlyCostSummary) Color(0xFFD1FAE5) else SurfaceWhite)
                                        .border(1.dp, if (settings.monthlyCostSummary) Color(0xFFD1FAE5) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = if (settings.monthlyCostSummary) Color(0xFF059669) else Color(0xFF059669).copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen Mensual de Costes",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Informe financiero de labores y amortizaciones",
                                        fontSize = 12.sp,
                                        color = if (settings.monthlyCostSummary) Color(0xFF059669) else OnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.monthlyCostSummary,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(monthlyCostSummary = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // Resumen Semanal VYR CB
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.weeklyCostSummaryVyr) Color(0xFFFEF3C7) else SurfaceWhite)
                                        .border(1.dp, if (settings.weeklyCostSummaryVyr) Color(0xFFFEF3C7) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = if (settings.weeklyCostSummaryVyr) Color(0xFFD97706) else Color(0xFFD97706).copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen Semanal de Costes $titularVr",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Balance consolidado cada lunes por la mañana",
                                        fontSize = 12.sp,
                                        color = if (settings.weeklyCostSummaryVyr) Color(0xFFD97706) else OnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.weeklyCostSummaryVyr,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(weeklyCostSummaryVyr = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // Resumen Mensual 1ª CB (V&R CB)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.monthlyCostSummaryVyr) Color(0xFFD1FAE5) else SurfaceWhite)
                                        .border(1.dp, if (settings.monthlyCostSummaryVyr) Color(0xFFD1FAE5) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = if (settings.monthlyCostSummaryVyr) Color(0xFF059669) else Color(0xFF059669).copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen Mensual de Costes $titularVr",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Informe financiero de labores y amortizaciones",
                                        fontSize = 12.sp,
                                        color = if (settings.monthlyCostSummaryVyr) Color(0xFF059669) else OnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.monthlyCostSummaryVyr,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(monthlyCostSummaryVyr = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // Resumen Semanal 2ª CB / Otros
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.weeklyCostSummaryOtros) Color(0xFFFEF3C7) else SurfaceWhite)
                                        .border(1.dp, if (settings.weeklyCostSummaryOtros) Color(0xFFFEF3C7) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = if (settings.weeklyCostSummaryOtros) Color(0xFFD97706) else Color(0xFFD97706).copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen Semanal de Costes $titularOtros",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Balance consolidado cada lunes por la mañana",
                                        fontSize = 12.sp,
                                        color = if (settings.weeklyCostSummaryOtros) Color(0xFFD97706) else OnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.weeklyCostSummaryOtros,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(weeklyCostSummaryOtros = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        // Resumen Mensual 2ª CB / Otros
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (settings.monthlyCostSummaryOtros) Color(0xFFD1FAE5) else SurfaceWhite)
                                        .border(1.dp, if (settings.monthlyCostSummaryOtros) Color(0xFFD1FAE5) else OutlineVariant, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = if (settings.monthlyCostSummaryOtros) Color(0xFF059669) else Color(0xFF059669).copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Resumen Mensual de Costes $titularOtros",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Informe financiero de labores y amortizaciones",
                                        fontSize = 12.sp,
                                        color = if (settings.monthlyCostSummaryOtros) Color(0xFF059669) else OnSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.monthlyCostSummaryOtros,
                                onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(monthlyCostSummaryOtros = it)) },
                                modifier = Modifier.scale(0.82f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryGreen,
                                    checkedTrackColor = PrimaryGreenContainer,
                                    uncheckedThumbColor = Color(0xFFD1D5DB),
                                    uncheckedTrackColor = SurfaceContainerLow,
                                    uncheckedBorderColor = OutlineVariant
                                )
                            )
                        }
                    }
                }
            }

            // Header: Simulador
            item {
                SettingsSectionHeader(title = "SIMULADOR DE NOTIFICACIONES")
            }

            // Card: Simulador Unificado y Minimalista
            item {
                UnifiedSimulatorCard(
                    settings = settings,
                    titularVr = titularVr,
                    titularOtros = titularOtros,
                    onSimulatePartnerActivity = { action, author -> viewModel.simulatePartnerActivity(action, author) },
                    onSimulateClimateAlert = { type, stage -> viewModel.simulateClimateAlert(type, stage) },
                    onTestTaskAlarm = {
                        val todayKey = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())
                        val testTask = CalendarTaskEntity(
                            id = 999999L,
                            orchardName = "Huerto El Realengo",
                            title = "Tratamiento fitosanitario",
                            timeText = "08:00",
                            dateKey = todayKey,
                            description = "Revisar aplicación y dosis fitosanitaria en cítricos",
                            reminderEnabled = true,
                            reminderDate = todayKey,
                            reminderTime = "08:00"
                        )
                        viewModel.testTaskAlarm(testTask)
                    },
                    onTestLonjaWeekly = {
                        viewModel.testLonjaWeeklyNotification()
                    },
                    onTestWeeklyCostSummary = {
                        viewModel.testWeeklyCostSummaryNotification()
                    },
                    onTestMonthlyCostSummary = {
                        viewModel.testMonthlyCostSummaryNotification()
                    },
                    onTestWeeklyCostSummaryVyr = {
                        viewModel.testWeeklyCostSummaryVyrNotification()
                    },
                    onTestMonthlyCostSummaryVyr = {
                        viewModel.testMonthlyCostSummaryVyrNotification()
                    },
                    onTestWeeklyCostSummaryOtros = {
                        viewModel.testWeeklyCostSummaryOtrosNotification()
                    },
                    onTestMonthlyCostSummaryOtros = {
                        viewModel.testMonthlyCostSummaryOtrosNotification()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Item individual minimalista para cada Alerta Climática, manteniendo idénticos todos los parámetros
 * y los 3 selectores pero perfectamente integrado con la estética limpia de V&R Agro.
 */
@Composable
private fun MinimalistClimateAlertItem(
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    thresholdLabel: String,
    thresholdValue: String,
    onEditThreshold: () -> Unit,
    notify7Days: Boolean,
    notify2Days: Boolean,
    notifyRealtime: Boolean,
    realtimeThresholdText: String,
    onToggle7Days: () -> Unit,
    onToggle2Days: () -> Unit,
    onToggleRealtime: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Row: Icon + Title + Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (checked) iconContainer else SurfaceWhite)
                        .border(1.dp, if (checked) iconContainer else OutlineVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) iconTint else iconTint.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface
                    )
                    Text(
                        text = if (checked) "$thresholdLabel: $thresholdValue" else "Desactivado",
                        fontSize = 12.sp,
                        color = if (checked) iconTint else OnSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(0.82f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PrimaryGreen,
                    checkedTrackColor = PrimaryGreenContainer,
                    uncheckedThumbColor = Color(0xFFD1D5DB),
                    uncheckedTrackColor = SurfaceContainerLow,
                    uncheckedBorderColor = OutlineVariant
                )
            )
        }

        // Subpanel desplegable si la alerta está activa
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Fila de edición del umbral numérico
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Parámetro crítico:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(6.dp))
                            .clickable { onEditThreshold() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = thresholdValue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar valor",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                // Selector de 3 momentos de notificación estilo píldora minimalista
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Momentos de aviso configurados:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MinimalistTimingPill(
                            label = "7 Días",
                            isSelected = notify7Days,
                            onClick = onToggle7Days,
                            modifier = Modifier.weight(1f)
                        )
                        MinimalistTimingPill(
                            label = "2 Días (48h)",
                            isSelected = notify2Days,
                            onClick = onToggle2Days,
                            modifier = Modifier.weight(1f)
                        )
                        MinimalistTimingPill(
                            label = "Tiempo Real",
                            isSelected = notifyRealtime,
                            onClick = onToggleRealtime,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Píldora de selección minimalista con el naranja del botón de Parte difuminado
 */
@Composable
private fun MinimalistTimingPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Naranja exacto del botón "Parte" (SecondaryOrangeDark) difuminado para fondo suave y máxima legibilidad interior
    val diffusedOrangeBg = SecondaryOrangeDark.copy(alpha = 0.15f)
    val diffusedOrangeBorder = SecondaryOrangeDark.copy(alpha = 0.45f)
    val diffusedOrangeText = Color(0xFF8C3400) // Tono cítrico profundo del mismo naranja para leer los datos nítidamente
    val diffusedOrangeIcon = SecondaryOrangeDark // Icono con el tono naranja vivo del botón

    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) diffusedOrangeBg else SurfaceContainerLow)
            .border(
                1.dp,
                if (isSelected) diffusedOrangeBorder else OutlineVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = diffusedOrangeIcon,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) diffusedOrangeText else OnSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Simulador unificado y elegante alineado con la interfaz del resto de la app
 */
@Composable
private fun UnifiedSimulatorCard(
    settings: com.example.data.model.NotificationSettingsEntity,
    titularVr: String = "V&R C.B.",
    titularOtros: String = "2ª C.B.",
    onSimulatePartnerActivity: (action: String, author: String) -> Unit,
    onSimulateClimateAlert: (type: String, stage: Int) -> Unit,
    onTestTaskAlarm: () -> Unit,
    onTestLonjaWeekly: () -> Unit,
    onTestWeeklyCostSummary: () -> Unit,
    onTestMonthlyCostSummary: () -> Unit,
    onTestWeeklyCostSummaryVyr: () -> Unit,
    onTestMonthlyCostSummaryVyr: () -> Unit,
    onTestWeeklyCostSummaryOtros: () -> Unit,
    onTestMonthlyCostSummaryOtros: () -> Unit
) {
    var simulatorTab by remember { mutableStateOf(0) } // 0: Clima, 1: Socios
    var selectedClimateType by remember { mutableStateOf("frost") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(OutlineVariant))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Comprobar Pop-ups en el Móvil",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface
                )
            }

            Text(
                text = "Lanza una notificación simulada para verificar cómo aparece el pop-up según tus ajustes guardados:",
                fontSize = 12.sp,
                color = OnSurfaceVariant,
                lineHeight = 16.sp
            )

            // Selector de pestaña: Clima vs Socios (estilo pill bar de V&R Agro)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineVariant, RoundedCornerShape(50.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Alertas Climáticas", "Avisos de Socios").forEachIndexed { index, tabTitle ->
                    val isSelected = simulatorTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) PrimaryGreen else Color.Transparent)
                            .clickable { simulatorTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else OnSurfaceVariant
                        )
                    }
                }
            }

            if (simulatorTab == 0) {
                // Pestaña Clima: Selector de tipo de clima
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "frost" to "❄️ Helada",
                        "wind" to "💨 Viento",
                        "heat" to "☀️ Calor",
                        "rain" to "🌧️ Lluvia",
                        "hail" to "⛈️ Granizo"
                    ).forEach { (type, label) ->
                        val isSelected = selectedClimateType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryGreenContainer else SurfaceWhite)
                                .border(1.dp, if (isSelected) PrimaryGreen else OutlineVariant, RoundedCornerShape(6.dp))
                                .clickable { selectedClimateType = type },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryGreen else OnSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                val (currentLabel, isEnabled, has7d, has2d, hasRt, currentThresh) = when (selectedClimateType) {
                    "wind" -> Hexa("Viento Fuerte", settings.windAlert, settings.windNotify7Days, settings.windNotify2Days, settings.windNotifyRealtime, "${settings.windThresholdKmh} km/h")
                    "heat" -> Hexa("Ola de Calor", settings.heatAlert, settings.heatNotify7Days, settings.heatNotify2Days, settings.heatNotifyRealtime, "${settings.heatThresholdTemp} °C")
                    "rain" -> Hexa("Lluvia", settings.rainAlert, settings.rainNotify7Days, settings.rainNotify2Days, settings.rainNotifyRealtime, "${settings.rainProbabilityThreshold}%")
                    "hail" -> Hexa("Granizo", settings.hailAlert, settings.hailNotify7Days, settings.hailNotify2Days, settings.hailNotifyRealtime, "${settings.hailProbabilityThreshold}%")
                    else -> Hexa("Helada", settings.frostAlert, settings.frostNotify7Days, settings.frostNotify2Days, settings.frostNotifyRealtime, "${settings.frostThresholdTemp} °C")
                }

                // 3 Botones de prueba minimalistas
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulatorActionButton(
                        title = "1. Previsión a 7 Días",
                        subtitle = if (has7d) "Aviso preventivo en la semana" else "Desactivado en tus ajustes",
                        isActive = isEnabled && has7d,
                        onClick = { onSimulateClimateAlert(selectedClimateType, 1) }
                    )
                    SimulatorActionButton(
                        title = "2. A 2 Días (48 horas)",
                        subtitle = if (has2d) "Alta fiabilidad para labores" else "Desactivado en tus ajustes",
                        isActive = isEnabled && has2d,
                        onClick = { onSimulateClimateAlert(selectedClimateType, 2) }
                    )
                    SimulatorActionButton(
                        title = "3. Parámetro alcanzado ($currentThresh)",
                        subtitle = if (hasRt) "Alcanzado en tiempo real en parcela" else "Desactivado en tus ajustes",
                        isActive = isEnabled && hasRt,
                        onClick = { onSimulateClimateAlert(selectedClimateType, 3) }
                    )
                }

                // Nota informativa minimalista
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, OutlineVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (isEnabled)
                            "Aviso de $currentLabel: Saltará en móvil y socios según las etapas activadas arriba."
                        else
                            "Aviso de $currentLabel apagado en el interruptor principal.",
                        fontSize = 11.sp,
                        color = OnSurfaceVariant
                    )
                }
            } else {
                // Pestaña Socios
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SimulatorActionButton(
                        title = "🔔 Probar Pop-up de Alarma de Tarea",
                        subtitle = "Dispara aviso prioritario flotante con sonido y vibración",
                        isActive = true,
                        onClick = onTestTaskAlarm
                    )
                    SimulatorActionButton(
                        title = "📊 Probar Cotización Semanal Lonja",
                        subtitle = "Simula el pop-up semanal cuando la Lonja publica nuevos precios",
                        isActive = true,
                        onClick = onTestLonjaWeekly
                    )
                    SimulatorActionButton(
                        title = "📊 Probar Resumen Semanal de Costes",
                        subtitle = "Simula el balance consolidado con horas e inversión en parcelas",
                        isActive = settings.weeklyCostSummary,
                        onClick = onTestWeeklyCostSummary
                    )
                    SimulatorActionButton(
                        title = "📈 Probar Balance Mensual de Costes",
                        subtitle = "Simula el balance de cierre mensual con mano de obra y materiales",
                        isActive = settings.monthlyCostSummary,
                        onClick = onTestMonthlyCostSummary
                    )
                    SimulatorActionButton(
                        title = "📊 Probar Resumen Semanal de Costes $titularVr",
                        subtitle = "Simula el balance consolidado con horas e inversión en parcelas de $titularVr",
                        isActive = settings.weeklyCostSummaryVyr,
                        onClick = onTestWeeklyCostSummaryVyr
                    )
                    SimulatorActionButton(
                        title = "📈 Probar Balance Mensual de Costes $titularVr",
                        subtitle = "Simula el balance de cierre mensual con mano de obra y materiales de $titularVr",
                        isActive = settings.monthlyCostSummaryVyr,
                        onClick = onTestMonthlyCostSummaryVyr
                    )
                    SimulatorActionButton(
                        title = "📊 Probar Resumen Semanal de Costes $titularOtros",
                        subtitle = "Simula el balance consolidado con horas e inversión en parcelas de $titularOtros",
                        isActive = settings.weeklyCostSummaryOtros,
                        onClick = onTestWeeklyCostSummaryOtros
                    )
                    SimulatorActionButton(
                        title = "📈 Probar Balance Mensual de Costes $titularOtros",
                        subtitle = "Simula el balance de cierre mensual con mano de obra y materiales de $titularOtros",
                        isActive = settings.monthlyCostSummaryOtros,
                        onClick = onTestMonthlyCostSummaryOtros
                    )
                    SimulatorActionButton(
                        title = "Simular Nueva Tarea",
                        subtitle = "Publicada por Ramón Ripoll en el huerto",
                        isActive = settings.newSocialPosts,
                        onClick = { onSimulatePartnerActivity("new_task", "Ramón Ripoll") }
                    )
                    SimulatorActionButton(
                        title = "Simular Tarea Completada",
                        subtitle = "Finalizada por María Valero",
                        isActive = settings.newSocialPosts,
                        onClick = { onSimulatePartnerActivity("task_completed", "María Valero") }
                    )
                    SimulatorActionButton(
                        title = "Simular Parte de Campo",
                        subtitle = "Registrado por Paco López",
                        isActive = settings.newSocialPosts,
                        onClick = { onSimulatePartnerActivity("work_part", "Paco López") }
                    )
                    SimulatorActionButton(
                        title = "Simular Huerto Modificado",
                        subtitle = "Actualizado por Vicente Martí",
                        isActive = settings.newSocialPosts,
                        onClick = { onSimulatePartnerActivity("orchard_modified", "Vicente Martí") }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceWhite)
                            .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Lonja de Cítricos: El pop-up salta únicamente 1 vez por semana tras publicarse la cotización oficial en su web (nunca en cada inicio de la app).",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatorActionButton(
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceWhite)
            .border(1.dp, if (isActive) PrimaryGreenContainer else OutlineVariant, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) OnSurface else OnSurfaceVariant
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = OnSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isActive) PrimaryGreen else OnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private data class Hexa<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = OnSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
    )
}
