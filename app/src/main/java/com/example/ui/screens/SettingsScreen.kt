package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import com.example.ui.util.AppDataBackupHelper
import com.example.ui.util.AppKeyboards
import com.example.ui.util.CbWorkExcelExportHelper
import com.example.ui.util.ExcelExportHelper
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppTopHeader
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.components.rememberPhotoPickerHandler
import com.example.ui.theme.*

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val workParts by viewModel.allWorkParts.collectAsStateWithLifecycle()
    val orchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()

    var showFirebaseHelpDialog by remember { mutableStateOf(false) }
    var isCheckingSync by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(userProfile.name) }
    var editedPhotoUri by remember { mutableStateOf<String?>(userProfile.photoUri) }

    var showEditTitularesDialog by remember { mutableStateOf(false) }
    var editedTitularPropios by remember { mutableStateOf(userProfile.titularPropios) }
    var editedTitularVr by remember { mutableStateOf(userProfile.titularVr) }
    var editedTitularOtros by remember { mutableStateOf(userProfile.titularOtros) }
    var isExcelSamplesExpanded by remember { mutableStateOf(false) }
    var showClearAlertsDialog by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberPhotoPickerHandler(prefix = "profile_avatar") { localPath ->
        editedPhotoUri = localPath
    }

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "Ajustes",
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
            // Profile Card
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
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val initials = userProfile.name.trim().split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifEmpty { "U" }

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreenContainer)
                                .clickable {
                                    editedName = userProfile.name
                                    editedPhotoUri = userProfile.photoUri
                                    showEditProfileDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userProfile.photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userProfile.photoUri,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = initials,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }

                            // Camera badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cambiar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        )

                        OutlinedButton(
                            onClick = {
                                editedName = userProfile.name
                                editedPhotoUri = userProfile.photoUri
                                showEditProfileDialog = true
                            },
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.height(36.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Editar Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Group: Herramientas de Campo
            item {
                SettingsSectionHeader(title = "HERRAMIENTAS DE CAMPO")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickableRow(
                            icon = Icons.Default.Calculate,
                            title = "Calculadora de dosis",
                            onClick = { viewModel.navigateTo(Screen.DoseCalculator) }
                        )
                    }
                }
            }

            // Group: Aplicación
            item {
                SettingsSectionHeader(title = "CONFIGURACIÓN")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsClickableRow(
                            icon = Icons.Default.Notifications,
                            title = "Ajustes de Notificaciones",
                            onClick = { viewModel.navigateTo(Screen.NotificationSettings) }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.Handshake,
                            title = "Propietarios y Comunidades de Bienes",
                            onClick = {
                                editedTitularPropios = userProfile.titularPropios
                                editedTitularVr = userProfile.titularVr
                                editedTitularOtros = userProfile.titularOtros
                                showEditTitularesDialog = true
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.AccountCircle,
                            title = "Reconfigurar bienvenida de perfil",
                            onClick = {
                                viewModel.resetInitialProfileSetupForTesting()
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        SettingsClickableRow(
                            icon = Icons.Default.NotificationsOff,
                            title = "Borrar alertas de la pantalla de socios",
                            onClick = {
                                showClearAlertsDialog = true
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = PrimaryGreen)
                                Text(text = "Modo Oscuro", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurface)
                            }
                            Switch(
                                checked = userProfile.isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen, checkedTrackColor = PrimaryGreenContainer)
                            )
                        }
                    }
                }
            }

            // Group: Conexión entre Móviles y Sincronización
            item {
                SettingsSectionHeader(title = "SINCRONIZACIÓN EN LA NUBE")
            }

            // Card 1: Estado de Sincronización Firebase
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (firebaseSyncStatus.isConnected) PrimaryGreen.copy(alpha = 0.4f)
                            else WarningOrange.copy(alpha = 0.5f)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (firebaseSyncStatus.isConnected) PrimaryGreen.copy(alpha = 0.12f)
                                        else WarningOrange.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (firebaseSyncStatus.isConnected) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (firebaseSyncStatus.isConnected) PrimaryGreen else WarningOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Estado de Sincronización Nube",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = if (firebaseSyncStatus.isConnected) {
                                        "🟢 Conectado en tiempo real"
                                    } else if (!firebaseSyncStatus.isConfigured) {
                                        "🟡 Modo Local (Sin sincronizar en la nube)"
                                    } else {
                                        "🔴 Sin conexión con la nube"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (firebaseSyncStatus.isConnected) PrimaryGreen else WarningOrange
                                )
                            }
                        }

                        Text(
                            text = if (!firebaseSyncStatus.isConfigured) {
                                "Ambos móviles están funcionando con base de datos local independiente porque el archivo google-services.json tiene la ID de plantilla ('${firebaseSyncStatus.projectId.ifBlank { "remixed-project-id" }}'). Para sincronizar automáticamente por internet, se requiere vincular un proyecto real de Firebase."
                            } else if (firebaseSyncStatus.isConnected) {
                                "Conectado al proyecto Firebase '${firebaseSyncStatus.projectId}'. Los cambios se comparten automáticamente entre dispositivos autorizados."
                            } else {
                                firebaseSyncStatus.errorMessage ?: firebaseSyncStatus.statusSummary
                            },
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isCheckingSync = true
                                    viewModel.forceCloudSync { success, message ->
                                        isCheckingSync = false
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isCheckingSync,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isCheckingSync) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Comprobar Nube", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            TextButton(
                                onClick = { showFirebaseHelpDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("¿Cómo conectar?", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Group: Copia de Seguridad y Exportación Completa
            item {
                SettingsSectionHeader(title = "COPIA DE SEGURIDAD Y EXPORTACIÓN (.XLSX)")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen.copy(alpha = 0.35f)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Copia de Seguridad Completa",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = "${workParts.size} partes registrados • ${orchards.size} parcelas",
                                    fontSize = 12.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "Genera un libro Excel (.xlsx) multichoja con todos los datos reales guardados en la app: parcelas, partes de trabajo, cosechas, costes y titulares. Podrás guardarlo en tu móvil, Google Drive o enviarlo por WhatsApp.",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = {
                                ExcelExportHelper.exportOperationsComprehensiveCsv(
                                    context = context,
                                    parts = workParts,
                                    orchards = orchards
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Descargar Copia de Seguridad (.xlsx)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Group: Plantillas e Informes Excel (Plegable)
            item {
                SettingsSectionHeader(title = "INFORMES Y MUESTRAS EXCEL")
            }

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isExcelSamplesExpanded = !isExcelSamplesExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Modelos y Muestras Excel (.xlsx)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = if (isExcelSamplesExpanded) "Toca para ocultar plantillas" else "Plantillas de ejemplo (C.B., Balances, etc.)",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isExcelSamplesExpanded = !isExcelSamplesExpanded }) {
                                Icon(
                                    imageVector = if (isExcelSamplesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExcelSamplesExpanded) "Ocultar" else "Mostrar",
                                    tint = PrimaryGreen
                                )
                            }
                        }

                        if (isExcelSamplesExpanded) {
                            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                            Text(
                                text = "Descarga libros de muestra predefinidos para comprobar el formato contable, las hojas de cálculo por huerto y el reparto de costes.",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            // Muestra 1: Balance y Operaciones General (1 Año Completo)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceWhite)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "1. Balance y Operaciones General (1 Año Completo)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                                Text(
                                    text = "12 meses con costes de mano de obra, riegos por goteo, gastos varios, cosechas y hojas desglosadas por huerto y comunidad.",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                OutlinedButton(
                                    onClick = {
                                        ExcelExportHelper.exportMock1YearExcel(context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Descargar Muestra Anual General (.xlsx)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Muestra 2: Liquidación C.B. (1 Año Mes a Mes - 12 Pestañas)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceWhite)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = SecondaryOrangeDark, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "2. Liquidación C.B. (1 Año Mes a Mes - 12 Pestañas)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                                Text(
                                    text = "12 liquidaciones individuales (Enero a Diciembre) de Comunidad de Bienes + pestaña de Balance y Resumen Anual.",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                OutlinedButton(
                                    onClick = {
                                        CbWorkExcelExportHelper.exportDemoOneYearCbExcel(context)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(SecondaryOrangeDark)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryOrangeDark)
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Descargar Muestra C.B. 12 Meses (.xlsx)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Muestra 3: Liquidación C.B. (1 Mes Individual)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceWhite)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Handshake, contentDescription = null, tint = SecondaryOrangeDark, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "3. Liquidación C.B. (1 Mes Individual de Muestra)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface
                                    )
                                }
                                Text(
                                    text = "Liquidación ficticia mensual completa para presentar al banco, gestoría o socios de la comunidad.",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                                OutlinedButton(
                                    onClick = {
                                        CbWorkExcelExportHelper.exportDemoSingleMonthCbExcel(context, 1, "Enero")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(SecondaryOrangeDark)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryOrangeDark)
                                ) {
                                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Descargar Muestra C.B. 1 Mes (.xlsx)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Editar Perfil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Photo Preview & Edit
                    val previewInitials = editedName.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .map { it.first().uppercase() }
                        .joinToString("")
                        .ifEmpty { "U" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreenContainer)
                                .border(2.dp, PrimaryGreen.copy(alpha = 0.5f), CircleShape)
                                .clickable { profilePhotoPicker.openPicker() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!editedPhotoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = editedPhotoUri,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = previewInitials,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }

                            // Camera Overlay Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Hacer o subir foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { profilePhotoPicker.openPicker() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (!editedPhotoUri.isNullOrEmpty()) "Cambiar Foto" else "Hacer o Subir Foto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!editedPhotoUri.isNullOrEmpty()) {
                                TextButton(
                                    onClick = { editedPhotoUri = null },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Quitar", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Name Input Field
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Nombre y Apellidos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            placeholder = { Text("Ej. Carlos Vicente") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = AppKeyboards.Words,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            name = editedName,
                            photoUri = editedPhotoUri,
                            updatePhoto = true
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar", color = PrimaryGreen)
                }
            }
        )
    }

    if (showEditTitularesDialog) {
        AlertDialog(
            onDismissRequest = { showEditTitularesDialog = false },
            title = {
                Column {
                    Text(
                        text = "Editar Nombres de Propietarios",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OnSurface
                    )
                    Text(
                        text = "Se actualizarán en huertos, partes de trabajo y liquidaciones",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Propios
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "1. Huertos Propios (Propiedad):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editedTitularPropios,
                            onValueChange = { editedTitularPropios = it },
                            placeholder = { Text("Ej. Propiedad o Carlos Vicente") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = AppKeyboards.Words,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                    }

                    // 2. 1ª CB
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "2. Primera Comunidad de Bienes (C.B.):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editedTitularVr,
                            onValueChange = { editedTitularVr = it },
                            placeholder = { Text("Ej. V&R C.B.") },
                            leadingIcon = {
                                Icon(Icons.Default.Handshake, contentDescription = null, tint = SecondaryOrangeDark, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = AppKeyboards.Words,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                    }

                    // 3. 2ª CB / Otros
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "3. Segunda Comunidad de Bienes / Otros:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editedTitularOtros,
                            onValueChange = { editedTitularOtros = it },
                            placeholder = { Text("Ej. AgroLevante C.B. u Otros") },
                            leadingIcon = {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = SecondaryOrangeDark, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            keyboardOptions = AppKeyboards.Words,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalPropios = editedTitularPropios.trim().ifEmpty { "Propiedad" }
                        val finalVr = editedTitularVr.trim().ifEmpty { "V&R C.B." }
                        val finalOtros = editedTitularOtros.trim().ifEmpty { "Otros" }
                        viewModel.updateTitulares(
                            titularPropios = finalPropios,
                            titularVr = finalVr,
                            titularOtros = finalOtros
                        )
                        showEditTitularesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitularesDialog = false }) {
                    Text("Cancelar", color = PrimaryGreen)
                }
            }
        )
    }

    if (showFirebaseHelpDialog) {
        AlertDialog(
            onDismissRequest = { showFirebaseHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryGreen)
                    Text("¿Por qué no se conectan?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "1. Causa de la desconexión:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OnSurface
                    )
                    Text(
                        text = "Cada instalación guarda sus datos en la memoria interna de su propio teléfono (SQLite/Room). Para que dos móviles se comuniquen por internet en tiempo real, ambos deben conectarse al mismo servidor de Google Firebase.\n\nActualmente la app incluye credenciales de plantilla ('remixed-project-id'), por lo que ambos dispositivos operan en modo local independiente.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Text(
                        text = "2. Solución Inmediata (Transferencia Directa):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SecondaryOrangeDark
                    )
                    Text(
                        text = "En la sección de arriba, pulsa 'Enviar al Móvil 2' y envía el archivo por WhatsApp o Drive. Luego en el otro móvil pulsa 'Cargar en este Móvil' y selecciona el archivo recibido. Todos los huertos, partes y tareas se sincronizarán al momento sin necesidad de internet ni servidores.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Text(
                        text = "3. Solución Automática en Tiempo Real (Firebase):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "Para que cada cambio que hagas se refleje automáticamente en el otro móvil por internet:\n• Crea un proyecto gratuito en https://console.firebase.google.com\n• Añade una app Android con el paquete com.example\n• Activa Firestore Database y Firebase Authentication (anónimo)\n• Descarga el archivo google-services.json y colócalo en la carpeta app/ del proyecto para generar el APK definitivo.",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFirebaseHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Entendido", color = Color.White)
                }
            }
        )
    }

    if (showClearAlertsDialog) {
        AlertDialog(
            onDismissRequest = { showClearAlertsDialog = false },
            title = {
                Text(
                    text = "¿Borrar alertas de socios?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSurface
                )
            },
            text = {
                Text(
                    text = "Se eliminarán todas las alertas de plagas, heladas y avisos del muro de socios para dejar la pantalla completamente limpia. Las tareas del calendario y los huertos registrados no se verán afectados.",
                    fontSize = 14.sp,
                    color = OnSurfaceVariant,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllAlerts()
                        showClearAlertsDialog = false
                        Toast.makeText(context, "Alertas eliminadas correctamente", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Borrar Alertas", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAlertsDialog = false }) {
                    Text("Cancelar", color = OnSurfaceVariant)
                }
            },
            containerColor = SurfaceWhite
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = OnSurfaceVariant,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryGreen)
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnSurface)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = OnSurfaceVariant)
    }
}
