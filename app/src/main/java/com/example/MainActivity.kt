package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppBottomNavBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryGreen
import com.example.ui.util.InAppNotificationData
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val pendingIntentRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingIntentRoute.value = intent?.getStringExtra("navigateTo")

        setContent {
            val viewModel: MainViewModel = viewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val activeTab by viewModel.activeBottomTab.collectAsStateWithLifecycle()
            val inAppNotification by viewModel.inAppNotification.collectAsStateWithLifecycle()

            // Manejar navegación si el usuario pulsó sobre la notificación del móvil
            val route = pendingIntentRoute.value
            LaunchedEffect(route) {
                if (route == "social") {
                    viewModel.navigateTo(Screen.Social)
                    pendingIntentRoute.value = null
                }
            }

            // Solicitar permiso de notificaciones POST_NOTIFICATIONS en Android 13+ (Tiramisu)
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* Permiso concedido o denegado */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val isInitialProfileSetupRequired by viewModel.isInitialProfileSetupRequired.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = userProfile.isDarkMode) {
                if (isInitialProfileSetupRequired) {
                    InitialProfileSetupScreen(viewModel = viewModel)
                } else {
                    BackHandler(enabled = currentScreen !is Screen.Dashboard) {
                        viewModel.navigateBack()
                    }

                    Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val showBottomBar = currentScreen is Screen.Dashboard ||
                                currentScreen is Screen.CalendarView ||
                                currentScreen is Screen.WorkParts ||
                                currentScreen is Screen.Finances ||
                                currentScreen is Screen.Social

                        if (showBottomBar) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    AppBottomNavBar(
                                        activeTab = activeTab,
                                        onTabSelected = { tab ->
                                            when (tab) {
                                                "inicio" -> viewModel.navigateTo(Screen.Dashboard)
                                                "partes" -> viewModel.navigateTo(Screen.WorkParts())
                                                "tareas" -> viewModel.navigateTo(Screen.CalendarView)
                                                "finanzas" -> viewModel.navigateTo(Screen.Finances)
                                                "socios" -> viewModel.navigateTo(Screen.Social)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                            is Screen.OrchardDetail -> OrchardDetailScreen(
                                orchardId = screen.orchardId,
                                viewModel = viewModel
                            )
                            is Screen.AddOrchard -> AddOrchardScreen(
                                editOrchardId = screen.editOrchardId,
                                viewModel = viewModel
                            )
                            is Screen.WorkParts -> WorkPartsScreen(
                                initialTab = screen.initialTab,
                                preselectedOrchardId = screen.selectedOrchardId,
                                preselectedTaskType = screen.preselectedTaskType,
                                editWorkPartId = screen.editWorkPartId,
                                preselectedProductName = screen.preselectedProductName,
                                preselectedProductDose = screen.preselectedProductDose,
                                preselectedProductSafetyDays = screen.preselectedProductSafetyDays,
                                preselectedActiveSubstance = screen.preselectedActiveSubstance,
                                viewModel = viewModel
                            )
                            is Screen.Finances -> FinancesScreen(viewModel = viewModel)
                            is Screen.Social -> SocialScreen(viewModel = viewModel)
                            is Screen.NewPost -> NewPostScreen(
                                preselectedOrchardName = screen.preselectedOrchardName,
                                viewModel = viewModel
                            )
                            is Screen.CalendarView -> CalendarScreen(viewModel = viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                            is Screen.NotificationSettings -> NotificationSettingsScreen(viewModel = viewModel)
                            is Screen.DoseCalculator -> DoseCalculatorScreen(viewModel = viewModel)
                        }

                        // Pop-up flotante interactivo y compacto en pantalla cuando salta una alerta o novedad
                        AnimatedVisibility(
                            visible = inAppNotification != null,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .widthIn(max = 500.dp)
                        ) {
                            inAppNotification?.let { notif ->
                                InAppHeadsUpBanner(
                                    notification = notif,
                                    onDismiss = { viewModel.dismissInAppNotification() },
                                    onClick = {
                                        viewModel.dismissInAppNotification()
                                        viewModel.navigateTo(Screen.Social)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getStringExtra("navigateTo")
        if (target != null) {
            pendingIntentRoute.value = target
        }
    }
}

@Composable
fun InAppHeadsUpBanner(
    notification: InAppNotificationData,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetailsDialog by remember(notification.id) { mutableStateOf(false) }

    // Auto-cierre del pop-up flotante si el usuario no abre el diálogo de lectura completa
    LaunchedEffect(notification.id, showDetailsDialog) {
        if (!showDetailsDialog) {
            delay(6000)
            onDismiss()
        }
    }

    val isFrost = notification.iconType == "frost"
    val isRain = notification.iconType == "rain"
    val isWind = notification.iconType == "wind"
    val isHeat = notification.iconType == "heat"
    val isHail = notification.iconType == "hail"
    val isMarket = notification.iconType == "market"
    val isWeeklyCosts = notification.iconType == "weekly_costs"
    val isMonthlyCosts = notification.iconType == "monthly_costs"
    val isWeeklyCostsVyr = notification.iconType == "weekly_costs_vyr"
    val isMonthlyCostsVyr = notification.iconType == "monthly_costs_vyr"
    val isAlert = notification.iconType == "alert" || isFrost || isRain || isWind || isHeat || isHail

    val accentColor = when {
        isFrost -> Color(0xFF0284C7) // Cyan / Azul helada
        isRain -> Color(0xFF2563EB) // Azul lluvia
        isWind -> Color(0xFF0D9488) // Verde azulado / Viento
        isHeat -> Color(0xFFEA580C) // Naranja intenso / Calor
        isHail -> Color(0xFF7C3AED) // Púrpura / Granizo
        isMarket -> Color(0xFFD97706) // Ámbar Lonja
        isWeeklyCosts || isWeeklyCostsVyr -> Color(0xFFD97706) // Ámbar costes
        isMonthlyCosts || isMonthlyCostsVyr -> Color(0xFF059669) // Esmeralda balance
        isAlert -> Color(0xFFDC2626) // Rojo peligro
        else -> PrimaryGreen
    }

    val badgeLabel = when {
        isFrost -> "HELADA"
        isRain -> "LLUVIA"
        isWind -> "VIENTO"
        isHeat -> "CALOR"
        isHail -> "GRANIZO"
        isMarket -> "LONJA"
        isWeeklyCosts || isWeeklyCostsVyr -> "COSTES SEMANALES"
        isMonthlyCosts || isMonthlyCostsVyr -> "BALANCE MENSUAL"
        isAlert -> "ALERTA"
        notification.iconType == "new_task" -> "TAREA"
        notification.iconType == "task_completed" -> "TAREA"
        notification.iconType == "work_part" -> "PARTE"
        notification.iconType == "new_orchard" -> "HUERTO"
        notification.iconType == "orchard_modified" -> "CAMBIO"
        else -> "SOCIO"
    }

    val icon = when (notification.iconType) {
        "frost" -> Icons.Default.AcUnit
        "rain" -> Icons.Default.WaterDrop
        "wind" -> Icons.Default.Air
        "heat" -> Icons.Default.WbSunny
        "hail" -> Icons.Default.Grain
        "market" -> Icons.Default.TrendingUp
        "weekly_costs" -> Icons.Default.DateRange
        "monthly_costs" -> Icons.Default.Assessment
        "weekly_costs_vyr" -> Icons.Default.DateRange
        "monthly_costs_vyr" -> Icons.Default.Assessment
        "new_task" -> Icons.Default.Event
        "task_completed" -> Icons.Default.Event
        "work_part" -> Icons.Default.Build
        "new_orchard" -> Icons.Default.Yard
        "orchard_modified" -> Icons.Default.Edit
        "alert" -> Icons.Default.Warning
        else -> Icons.Default.NotificationsActive
    }

    val cleanTitle = remember(notification.title) {
        notification.title
            .replace(Regex("^[❄️🌧️💨☀️⛈️📢⚠️🚨✅🚜🌱✏️💧🍊📊\\s]+"), "")
            .trim()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDetailsDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icono compacto circular
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(17.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = badgeLabel,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }

                    Text(
                        text = cleanTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = notification.message,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Botón de cerrar compacto
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = {
                showDetailsDialog = false
                onDismiss()
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            title = {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!notification.orchardName.isNullOrBlank() && notification.orchardName != "General") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "📍 Parcela / Huerto: ${notification.orchardName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = if (notification.fullContent.isNotBlank()) notification.fullContent else notification.message,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDetailsDialog = false
                        onClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ver en Socios", fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDetailsDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Cerrar", fontSize = 13.sp)
                }
            }
        )
    }
}


