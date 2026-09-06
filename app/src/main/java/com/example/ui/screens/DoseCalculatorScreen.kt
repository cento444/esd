package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AppTopHeader
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.theme.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DoseCalculatorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var doseInput by remember { mutableStateOf("4") }
    var isLiquid by remember { mutableStateOf(true) } // true: ml/L, false: g/L
    var customLitersInput by remember { mutableStateOf("200") }

    val focusManager = LocalFocusManager.current

    // Parse numeric dose (accepting both comma and dot)
    val doseValue = remember(doseInput) {
        val sanitized = doseInput.trim().replace(',', '.')
        sanitized.toDoubleOrNull() ?: 0.0
    }

    val customLitersValue = remember(customLitersInput) {
        val sanitized = customLitersInput.trim().replace(',', '.')
        sanitized.toDoubleOrNull() ?: 0.0
    }

    // Fixed default tanks
    val mochilaLiters = 16.0
    val turboLiters = 1000.0

    // Calculations
    val mochilaResult = doseValue * mochilaLiters
    val turboResult = doseValue * turboLiters
    val customResult = doseValue * customLitersValue

    val unitPerLiterLabel = if (isLiquid) "ml/L" else "g/L"
    val unitBaseName = if (isLiquid) "líquido" else "polvo/sólido"

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "Calculadora de dosis",
                showMenu = false,
                showBack = true,
                onBackClick = { viewModel.navigateBack() },
                actions = {
                    IconButton(
                        onClick = {
                            doseInput = ""
                            customLitersInput = "200"
                        },
                        modifier = Modifier.testTag("clear_calculator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Limpiar campos",
                            tint = PrimaryGreen
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceBright)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // 1. Dosis del Producto Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, OutlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1. Dosis del producto",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Surface(
                                color = PrimaryGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = unitPerLiterLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreenDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Unit selector toggle (ml/L vs g/L)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLow)
                                .border(1.dp, OutlineVariant, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // ml/L button
                            Button(
                                onClick = { isLiquid = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("unit_ml_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLiquid) PrimaryGreen else Color.Transparent,
                                    contentColor = if (isLiquid) Color.White else OnSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = null,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "💧 ml/L (Líquido)",
                                    fontSize = 14.sp,
                                    fontWeight = if (isLiquid) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            // g/L button
                            Button(
                                onClick = { isLiquid = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("unit_g_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isLiquid) PrimaryGreen else Color.Transparent,
                                    contentColor = if (!isLiquid) Color.White else OnSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = null,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "⚖️ g/L (Polvo/Sólido)",
                                    fontSize = 14.sp,
                                    fontWeight = if (!isLiquid) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Number input field
                        OutlinedTextField(
                            value = doseInput,
                            onValueChange = { doseInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dose_input_field"),
                            label = { Text("Dosis por litro") },
                            placeholder = { Text("Ej. 4 ó 1,5") },
                            trailingIcon = {
                                Text(
                                    text = unitPerLiterLabel,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreenDark,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            colors = appOutlinedTextFieldColors(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )

                        // Quick Dose Presets for dirty hands / fast field usage
                        Text(
                            text = "Valores frecuentes:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant
                        )
                        val commonDoses = listOf("0.5", "1", "1.5", "2", "2.5", "3", "4", "5")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(commonDoses) { dosePreset ->
                                val isSelected = doseInput == dosePreset || doseInput == dosePreset.replace('.', ',')
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            doseInput = dosePreset
                                            focusManager.clearFocus()
                                        },
                                    color = if (isSelected) PrimaryGreen else SurfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PrimaryGreen else OutlineVariant
                                    )
                                ) {
                                    Text(
                                        text = "$dosePreset $unitPerLiterLabel",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else OnSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Main Default Results: Mochila 16 L & Turbo 1.000 L
            item {
                Text(
                    text = "Cantidades habituales",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // Mochila de 16 L Card
            item {
                TankResultCard(
                    icon = { BackpackSprayerIcon(modifier = Modifier.size(44.dp), color = Color(0xFF16A34A)) },
                    tankTitle = "Mochila de 16 L",
                    tankSubtitle = "16 L × $doseInput $unitPerLiterLabel",
                    resultFormatted = formatPrimaryResult(mochilaResult, isLiquid),
                    secondaryFormatted = formatSecondaryResult(mochilaResult, isLiquid),
                    accentColor = Color(0xFF16A34A), // Green
                    backgroundColor = Color(0xFFF0FDF4),
                    borderColor = Color(0xFF86EFAC),
                    testTag = "mochila_result_card"
                )
            }

            // Turbo de 1.000 L Card
            item {
                TankResultCard(
                    icon = { Text("💨", fontSize = 32.sp) },
                    tankTitle = "Turbo de 1.000 L",
                    tankSubtitle = "1.000 L × $doseInput $unitPerLiterLabel",
                    resultFormatted = formatPrimaryResult(turboResult, isLiquid),
                    secondaryFormatted = formatSecondaryResult(turboResult, isLiquid),
                    accentColor = Color(0xFF0284C7), // Blue
                    backgroundColor = Color(0xFFF0F9FF),
                    borderColor = Color(0xFF7DD3FC),
                    testTag = "turbo_result_card"
                )
            }

            // 3. Custom Calculation: “Calcular otra cantidad”
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_calculation_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, OutlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🎛️",
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Calcular otra cantidad",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }

                        // Liters input field
                        OutlinedTextField(
                            value = customLitersInput,
                            onValueChange = { customLitersInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_liters_input"),
                            label = { Text("Volumen de agua (Litros)") },
                            placeholder = { Text("Ej. 200") },
                            trailingIcon = {
                                Text(
                                    text = "Litros",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreenDark,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            colors = appOutlinedTextFieldColors(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )

                        // Quick volume presets
                        val commonVolumes = listOf("50", "100", "200", "400", "500", "800", "1500", "2000")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(commonVolumes) { volumePreset ->
                                val isSelected = customLitersInput == volumePreset
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            customLitersInput = volumePreset
                                            focusManager.clearFocus()
                                        },
                                    color = if (isSelected) Color(0xFF6366F1) else SurfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF6366F1) else OutlineVariant
                                    )
                                ) {
                                    Text(
                                        text = "$volumePreset L",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else OnSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Formula breakdown & Result Box
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Formula line: e.g. 200 L × 4 ml/L = 800 ml
                                val litersDisplay = if (customLitersInput.isBlank()) "0" else customLitersInput
                                val doseDisplay = if (doseInput.isBlank()) "0" else doseInput
                                Text(
                                    text = "$litersDisplay L × $doseDisplay $unitPerLiterLabel",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                // Result highlight
                                Surface(
                                    color = PrimaryGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Necesitas:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimaryGreenDark
                                        )
                                        Text(
                                            text = formatPrimaryResult(customResult, isLiquid),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = PrimaryGreenDark,
                                            textAlign = TextAlign.Center
                                        )
                                        formatSecondaryResult(customResult, isLiquid)?.let { sec ->
                                            Text(
                                                text = sec,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "de producto fitosanitario ($unitBaseName)",
                                            fontSize = 12.sp,
                                            color = OnSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 2.dp)
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

/**
 * Mochila de huerto / sulfatadora agrícola con depósito, tapón, palanca de bombeo, manguera y lanza
 */
@Composable
fun BackpackSprayerIcon(
    modifier: Modifier = Modifier.size(44.dp),
    color: Color = Color(0xFF16A34A)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Depósito principal de la mochila (tanque)
        val tankLeft = w * 0.18f
        val tankTop = h * 0.22f
        val tankWidth = w * 0.44f
        val tankHeight = h * 0.62f
        val cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)

        drawRoundRect(
            color = color,
            topLeft = Offset(tankLeft, tankTop),
            size = Size(tankWidth, tankHeight),
            cornerRadius = cornerRadius
        )

        // 2. Base / patas de apoyo
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(tankLeft - w * 0.02f, tankTop + tankHeight - h * 0.06f),
            size = Size(tankWidth + w * 0.04f, h * 0.08f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // 3. Tapón superior de llenado
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(tankLeft + tankWidth * 0.22f, h * 0.12f),
            size = Size(tankWidth * 0.56f, h * 0.11f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // 4. Asa de transporte superior
        drawArc(
            color = Color(0xFF334155),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(tankLeft + tankWidth * 0.15f, h * 0.04f),
            size = Size(tankWidth * 0.70f, h * 0.16f),
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round)
        )

        // 5. Palanca de bombeo manual (lado izquierdo)
        val leverPath = Path().apply {
            moveTo(tankLeft, tankTop + tankHeight * 0.35f)
            lineTo(w * 0.07f, tankTop + tankHeight * 0.25f)
            lineTo(w * 0.07f, tankTop + tankHeight * 0.82f)
        }
        drawPath(
            path = leverPath,
            color = Color(0xFF334155),
            style = Stroke(width = w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 6. Manguera flexible
        val hosePath = Path().apply {
            moveTo(tankLeft + tankWidth * 0.8f, tankTop + tankHeight * 0.85f)
            cubicTo(
                tankLeft + tankWidth + w * 0.09f, tankTop + tankHeight + h * 0.06f,
                w * 0.70f, h * 0.86f,
                w * 0.72f, h * 0.68f
            )
        }
        drawPath(
            path = hosePath,
            color = Color(0xFF475569),
            style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
        )

        // 7. Lanza de pulverizar con empuñadura
        val lancePath = Path().apply {
            moveTo(w * 0.72f, h * 0.72f) // empuñadura
            lineTo(w * 0.86f, h * 0.24f) // caña / tubo
            lineTo(w * 0.93f, h * 0.18f) // boquilla acodada
        }
        drawPath(
            path = lancePath,
            color = Color(0xFF0F172A),
            style = Stroke(width = w * 0.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Gatillo de la lanza
        drawLine(
            color = Color(0xFFEF4444),
            start = Offset(w * 0.72f, h * 0.64f),
            end = Offset(w * 0.67f, h * 0.60f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
        )

        // Gotas de pulverización
        drawCircle(
            color = color,
            radius = w * 0.038f,
            center = Offset(w * 0.95f, h * 0.09f)
        )
        drawCircle(
            color = color.copy(alpha = 0.85f),
            radius = w * 0.028f,
            center = Offset(w * 0.84f, h * 0.07f)
        )
        drawCircle(
            color = color.copy(alpha = 0.7f),
            radius = w * 0.026f,
            center = Offset(w * 0.98f, h * 0.22f)
        )
    }
}

@Composable
private fun TankResultCard(
    icon: @Composable () -> Unit,
    tankTitle: String,
    tankSubtitle: String,
    resultFormatted: String,
    secondaryFormatted: String?,
    accentColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Column {
                    Text(
                        text = tankTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = tankSubtitle,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = resultFormatted,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                if (secondaryFormatted != null) {
                    Text(
                        text = secondaryFormatted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Format helpers for high readability in field conditions
 */
private fun formatPrimaryResult(amount: Double, isLiquid: Boolean): String {
    if (amount <= 0.0) return if (isLiquid) "0 ml" else "0 g"

    if (isLiquid) {
        return if (amount >= 1000.0) {
            val liters = amount / 1000.0
            "${formatDecimal(liters)} L"
        } else {
            "${formatDecimal(amount)} ml"
        }
    } else {
        return if (amount >= 1000.0) {
            val kg = amount / 1000.0
            "${formatDecimal(kg)} kg"
        } else {
            "${formatDecimal(amount)} g"
        }
    }
}

private fun formatSecondaryResult(amount: Double, isLiquid: Boolean): String? {
    if (amount >= 1000.0) {
        return if (isLiquid) {
            "(${formatDecimal(amount)} ml)"
        } else {
            "(${formatDecimal(amount)} g)"
        }
    }
    return null
}

private fun formatDecimal(value: Double): String {
    val spanishLocale = Locale.forLanguageTag("es-ES")
    if (value == value.toLong().toDouble()) {
        val longVal = value.toLong()
        return NumberFormat.getIntegerInstance(spanishLocale).format(longVal)
    }
    val df = DecimalFormat("#,##0.##", DecimalFormatSymbols(spanishLocale))
    return df.format(value)
}
