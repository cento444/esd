package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.OrchardEntity
import com.example.data.model.SocialPostEntity
import com.example.network.OrchardWeather
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AllFruitQuotesDialog
import com.example.ui.components.AppTopHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

fun hasActiveAlertForOrchard(post: SocialPostEntity, orchard: OrchardEntity): Boolean {
    if (!post.isAlert || post.isAlertResolved) return false
    if (post.orchardId != null && post.orchardId == orchard.id) return true
    val pName = post.orchardName.trim().lowercase()
    val oName = orchard.name.trim().lowercase()
    if (pName.isBlank() || pName == "general") return false
    return pName == oName || pName.contains(oName) || oName.contains(pName)
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val activeFilter by viewModel.dashboardFilter.collectAsStateWithLifecycle()
    val weatherMap by viewModel.weatherMap.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showAllFruitQuotesDialog by remember { mutableStateOf(false) }

    val tabs = remember(userProfile) {
        listOf(
            userProfile.titularPropios.ifEmpty { "Propiedad" },
            userProfile.titularVr.ifEmpty { "V&R C.B." },
            userProfile.titularOtros.ifEmpty { "Otros" }
        )
    }
    val tabFilterKeys = listOf("propios", "vr", "otros")

    val initialIndex = remember(activeFilter) {
        when (activeFilter) {
            "vr", "v_y_r_cb" -> 1
            "otros" -> 2
            else -> 0
        }
    }

    val pagerState = rememberPagerState(initialPage = initialIndex) { tabs.size }
    val coroutineScope = rememberCoroutineScope()

    // Sync active filter on pager slide
    LaunchedEffect(pagerState.currentPage) {
        val filterKey = tabFilterKeys[pagerState.currentPage]
        if (activeFilter != filterKey) {
            viewModel.setDashboardFilter(filterKey)
        }
    }

    // Trigger weather fetch
    LaunchedEffect(allOrchards) {
        if (allOrchards.isNotEmpty()) {
            viewModel.loadWeatherForOrchards(allOrchards)
        }
    }

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "Agro work",
                showMenu = true,
                onMenuClick = { viewModel.navigateTo(Screen.Settings) },
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.AddOrchard()) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen)
                            .testTag("add_orchard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Alta de Huerto",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceBright)
        ) {
            val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
            val allActiveAlerts = remember(allPosts) {
                allPosts.filter { it.isAlert && !it.isAlertResolved }
            }
            val alertedOrchards = remember(allActiveAlerts, allOrchards) {
                allOrchards.filter { orchard ->
                    allActiveAlerts.any { post -> hasActiveAlertForOrchard(post, orchard) }
                }
            }

            if (alertedOrchards.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFF87171))
                ) {
                    val firstAlerted = alertedOrchards.first()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.navigateTo(Screen.OrchardDetail(firstAlerted.id))
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${alertedOrchards.size} ${if (alertedOrchards.size == 1) "huerto con alerta prioritaria activa" else "huertos con alertas prioritarias activas"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Text(
                                    text = alertedOrchards.joinToString(", ") { it.name },
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.navigateTo(Screen.Social) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = "Ver en Muro",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB91C1C)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Button(
                                onClick = { viewModel.navigateTo(Screen.OrchardDetail(firstAlerted.id)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = "Ver huerto (${firstAlerted.name})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Filter Pills Bar (Full width, swipe synchronized)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, OutlineVariant, RoundedCornerShape(50.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    FilterPill(
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

            // Horizontal Pager for swiping between tabs
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentCategoryKey = tabFilterKeys[page]
                val pageOrchards = remember(allOrchards, currentCategoryKey) {
                    allOrchards.filter { orchard ->
                        when (currentCategoryKey) {
                            "propios" -> orchard.ownerType.equals("propiedad", ignoreCase = true) ||
                                    orchard.ownerType.equals("propios", ignoreCase = true) ||
                                    orchard.ownerType.equals("all", ignoreCase = true)
                            "vr" -> orchard.ownerType.equals("vr", ignoreCase = true) ||
                                    orchard.ownerType.equals("v_y_r_cb", ignoreCase = true)
                            "otros" -> orchard.ownerType.equals("otros", ignoreCase = true)
                            else -> true
                        }
                    }
                }

                ReorderableOrchardsTabContent(
                    tabTitle = tabs[page],
                    orchards = pageOrchards,
                    allOrchards = allOrchards,
                    weatherMap = weatherMap,
                    viewModel = viewModel,
                    onOpenAllPrices = { showAllFruitQuotesDialog = true }
                )
            }
        }

        if (showAllFruitQuotesDialog) {
            AllFruitQuotesDialog(
                userOrchards = allOrchards,
                onDismiss = { showAllFruitQuotesDialog = false }
            )
        }
    }
}

@Composable
fun ReorderableOrchardsTabContent(
    tabTitle: String,
    orchards: List<OrchardEntity>,
    allOrchards: List<OrchardEntity> = emptyList(),
    weatherMap: Map<String, OrchardWeather>,
    viewModel: MainViewModel,
    onOpenAllPrices: (() -> Unit)? = null
) {
    var isReorderMode by remember { mutableStateOf(false) }
    var currentList by remember(orchards) { mutableStateOf(orchards) }
    val viewMode by viewModel.orchardCardViewMode.collectAsStateWithLifecycle()

    LaunchedEffect(orchards) {
        currentList = orchards
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Mis Huertos Title, Minimalist View Mode Buttons & Reorder Toggle
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mis Huertos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            fontSize = 18.sp
                        )
                    )
                    Text(
                        text = "${currentList.size} ${if (currentList.size == 1) "huerto" else "huertos"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = OnSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botones minimalistas de cambio de vista: Cuadros / Barras
                    Surface(
                        color = SurfaceContainerHigh,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.5.dp, OutlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cuadros
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (viewMode == MainViewModel.OrchardCardViewMode.CUADROS) PrimaryGreen else Color.Transparent)
                                    .clickable { viewModel.setOrchardCardViewMode(MainViewModel.OrchardCardViewMode.CUADROS) }
                                    .testTag("view_mode_cuadros_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Vista de cuadros",
                                    tint = if (viewMode == MainViewModel.OrchardCardViewMode.CUADROS) Color.White else OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Barras
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (viewMode == MainViewModel.OrchardCardViewMode.BARRAS) PrimaryGreen else Color.Transparent)
                                    .clickable { viewModel.setOrchardCardViewMode(MainViewModel.OrchardCardViewMode.BARRAS) }
                                    .testTag("view_mode_barras_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableRows,
                                    contentDescription = "Vista de barras",
                                    tint = if (viewMode == MainViewModel.OrchardCardViewMode.BARRAS) Color.White else OnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (currentList.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { isReorderMode = !isReorderMode },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isReorderMode) PrimaryGreen else SurfaceContainerHigh,
                                contentColor = if (isReorderMode) Color.White else OnSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isReorderMode) "Listo" else "Reordenar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (isReorderMode) {
            item {
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (currentList.size > 1) "Usa las flechas de cada ficha para subir o bajar el orden de tus huertos." else "Añade más huertos en esta categoría para poder reordenarlos.",
                            fontSize = 12.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Orchard Cards
        if (currentList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay huertos en esta categoría",
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(currentList, key = { it.id }) { orchard ->
                val weather = weatherMap[orchard.locationGps]
                    ?: com.example.network.WeatherService.getDefaultFallbackWeather(
                        if (orchard.fruitType == "Aguacate") 39.12 else 39.15
                    )
                val currentIndex = currentList.indexOfFirst { it.id == orchard.id }
                val canMoveUp = currentIndex > 0
                val canMoveDown = currentIndex != -1 && currentIndex < currentList.size - 1

                val onMoveUpAction = {
                    if (currentIndex > 0) {
                        val mutable = currentList.toMutableList()
                        val item = mutable.removeAt(currentIndex)
                        mutable.add(currentIndex - 1, item)
                        currentList = mutable

                        val fullList = if (allOrchards.isNotEmpty()) allOrchards.toMutableList() else mutable
                        if (allOrchards.isNotEmpty()) {
                            val itemToMove = fullList.find { it.id == orchard.id }
                            val targetItem = fullList.find { it.id == mutable[currentIndex].id }
                            if (itemToMove != null && targetItem != null) {
                                val targetIdx = fullList.indexOf(targetItem)
                                fullList.remove(itemToMove)
                                val insertIdx = if (targetIdx < fullList.size) targetIdx else fullList.size
                                fullList.add(insertIdx, itemToMove)
                            }
                        }
                        viewModel.reorderOrchards(fullList)
                    }
                }

                val onMoveDownAction = {
                    if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                        val mutable = currentList.toMutableList()
                        val item = mutable.removeAt(currentIndex)
                        mutable.add(currentIndex + 1, item)
                        currentList = mutable

                        val fullList = if (allOrchards.isNotEmpty()) allOrchards.toMutableList() else mutable
                        if (allOrchards.isNotEmpty()) {
                            val itemToMove = fullList.find { it.id == orchard.id }
                            val targetItem = fullList.find { it.id == mutable[currentIndex].id }
                            if (itemToMove != null && targetItem != null) {
                                val targetIdx = fullList.indexOf(targetItem)
                                fullList.remove(itemToMove)
                                val insertIdx = if (targetIdx + 1 <= fullList.size) targetIdx + 1 else fullList.size
                                fullList.add(insertIdx, itemToMove)
                            }
                        }
                        viewModel.reorderOrchards(fullList)
                    }
                }

                val onCardClickAction = {
                    if (!isReorderMode) {
                        viewModel.navigateTo(Screen.OrchardDetail(orchard.id))
                    }
                }

                val onRegisterPartClickAction = {
                    if (!isReorderMode) {
                        viewModel.navigateTo(Screen.WorkParts(selectedOrchardId = orchard.id))
                    }
                }

                val onRegisterTaskClickAction = {
                    if (!isReorderMode) {
                        viewModel.navigateTo(Screen.CalendarView)
                    }
                }

                if (viewMode == MainViewModel.OrchardCardViewMode.BARRAS) {
                    OrchardDashboardBarCard(
                        orchard = orchard,
                        weather = weather,
                        viewModel = viewModel,
                        isReorderMode = isReorderMode,
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        onMoveUp = onMoveUpAction,
                        onMoveDown = onMoveDownAction,
                        onCardClick = onCardClickAction,
                        onRegisterPartClick = onRegisterPartClickAction,
                        onRegisterTaskClick = onRegisterTaskClickAction
                    )
                } else {
                    OrchardDashboardCard(
                        orchard = orchard,
                        weather = weather,
                        viewModel = viewModel,
                        isReorderMode = isReorderMode,
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        onMoveUp = onMoveUpAction,
                        onMoveDown = onMoveDownAction,
                        onCardClick = onCardClickAction,
                        onRegisterPartClick = onRegisterPartClickAction,
                        onRegisterTaskClick = onRegisterTaskClickAction,
                        onOpenAllPrices = onOpenAllPrices
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
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

@Composable
fun OrchardDashboardCard(
    orchard: OrchardEntity,
    weather: OrchardWeather,
    viewModel: MainViewModel,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onRegisterPartClick: () -> Unit = {},
    onRegisterTaskClick: () -> Unit,
    onOpenAllPrices: (() -> Unit)? = null
) {
    val isCitrus = orchard.fruitType != "Aguacate"
    val accentColor = if (isCitrus) SecondaryOrange else PrimaryGreen
    val priceInfo = viewModel.getPriceForVariety(orchard.variety)
    val allCalendarTasks by viewModel.allCalendarTasks.collectAsStateWithLifecycle()
    val orchardTasks = allCalendarTasks.filter { task ->
        !task.isDone && (
            (task.orchardId != null && task.orchardId == orchard.id) ||
            task.orchardName.equals(orchard.name, ignoreCase = true) ||
            task.orchardName.startsWith(orchard.name, ignoreCase = true)
        )
    }

    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val activeAlerts = remember(allPosts, orchard) {
        allPosts.filter { post -> hasActiveAlertForOrchard(post, orchard) }
    }
    val hasActiveAlert = activeAlerts.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isReorderMode || hasActiveAlert) 1.5.dp else 1.dp,
                color = when {
                    isReorderMode -> PrimaryGreen
                    hasActiveAlert -> Color(0xFFDC2626)
                    else -> OutlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onCardClick)
            .testTag("orchard_card_${orchard.id}"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasActiveAlert) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Accent Bar (Red if priority alert active)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasActiveAlert) 8.dp else 6.dp)
                    .background(if (hasActiveAlert) Color(0xFFDC2626) else accentColor)
            )

            if (isReorderMode) {
                // Quick reorder bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Posición del huerto",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = onMoveUp,
                            enabled = canMoveUp,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (canMoveUp) PrimaryGreen else SurfaceContainerHigh,
                                contentColor = if (canMoveUp) Color.White else OnSurfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Subir huerto",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onMoveDown,
                            enabled = canMoveDown,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (canMoveDown) PrimaryGreen else SurfaceContainerHigh,
                                contentColor = if (canMoveDown) Color.White else OnSurfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Bajar huerto",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Name, Location, Thumbnail & Weather
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!orchard.photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = orchard.photoUri,
                                contentDescription = "Foto ${orchard.name}",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, OutlineVariant, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = orchard.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface,
                                        fontSize = 18.sp
                                    )
                                )
                                if (hasActiveAlert) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = if (activeAlerts.size > 1) "${activeAlerts.size} ALERTAS" else "ALERTA",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFDC2626),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = null,
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                val locationLabel = when {
                                    orchard.municipality.isNotEmpty() -> {
                                        val polyPart = if (orchard.polygon.isNotEmpty()) ", Pol. ${orchard.polygon}" else ""
                                        val parcelPart = if (orchard.parcel.isNotEmpty()) ", Parc. ${orchard.parcel}" else ""
                                        "${orchard.municipality}$polyPart$parcelPart"
                                    }
                                    orchard.locationGps.isNotEmpty() -> orchard.locationGps
                                    else -> "Sin ubicación especificada"
                                }
                                Text(
                                    text = locationLabel,
                                    fontSize = 12.sp,
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Weather Icon & Temp (Right Side of Orchard Header)
                    val (currentIcon, currentIconColor) = getWeatherVisual(weather.iconType)
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = currentIcon,
                                contentDescription = weather.conditionDesc,
                                tint = currentIconColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "${weather.currentTemp}°",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                        }

                        val uvBg = when {
                            weather.uvIndex >= 8 -> Color(0xFFFEE2E2)
                            weather.uvIndex >= 6 -> Color(0xFFFFEDD5)
                            weather.uvIndex >= 3 -> Color(0xFFFEF3C7)
                            else -> Color(0xFFECFDF5)
                        }
                        val uvTextColor = when {
                            weather.uvIndex >= 8 -> Color(0xFFDC2626)
                            weather.uvIndex >= 6 -> Color(0xFFC2410C)
                            weather.uvIndex >= 3 -> Color(0xFFB45309)
                            else -> Color(0xFF047857)
                        }

                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(uvBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = weather.uvLevelText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = uvTextColor
                            )
                        }
                    }
                }

                // Priority Alerts Section for this Orchard
                if (hasActiveAlert) {
                    activeAlerts.forEach { alert ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDC2626)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = alert.alertTitle ?: "Alerta en ${orchard.name}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = alert.timestampText.substringBefore(" •"),
                                        fontSize = 10.sp,
                                        color = Color(0xFFB91C1C),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!alert.photoUri.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = alert.photoUri,
                                            contentDescription = "Foto de alerta",
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(0.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Text(
                                        text = alert.content,
                                        fontSize = 12.sp,
                                        color = Color(0xFF7F1D1D),
                                        lineHeight = 16.sp,
                                        maxLines = 3,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFFCA5A5).copy(alpha = 0.6f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.navigateTo(Screen.Social) }
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Ver en Muro de Socios",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.resolveAlert(alert.id) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Resolver",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 7-Day Forecast Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceContainerLowest)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = if (weather.dailyForecast.isNotEmpty()) weather.dailyForecast else listOf(
                        com.example.network.DailyForecast("Hoy", "", weather.currentTemp, weather.currentTemp - 8, 0, "Despejado", "sunny"),
                        com.example.network.DailyForecast("Mar", "", weather.currentTemp + 1, weather.currentTemp - 7, 0, "Soleado", "sunny"),
                        com.example.network.DailyForecast("Mié", "", weather.currentTemp - 1, weather.currentTemp - 8, 2, "Intervalos", "partly_cloudy"),
                        com.example.network.DailyForecast("Jue", "", weather.currentTemp - 2, weather.currentTemp - 9, 3, "Nublado", "cloudy"),
                        com.example.network.DailyForecast("Vie", "", weather.currentTemp - 4, weather.currentTemp - 10, 61, "Lluvia", "rainy"),
                        com.example.network.DailyForecast("Sáb", "", weather.currentTemp - 1, weather.currentTemp - 9, 2, "Intervalos", "partly_cloudy"),
                        com.example.network.DailyForecast("Dom", "", weather.currentTemp, weather.currentTemp - 8, 0, "Soleado", "sunny")
                    )

                    days.take(7).forEach { dayForecast ->
                        val (dayIcon, dayTint) = getWeatherVisual(dayForecast.iconType)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = dayForecast.dayOfWeek.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Icon(
                                imageVector = dayIcon,
                                contentDescription = dayForecast.conditionDesc,
                                tint = dayTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${dayForecast.maxTemp}°",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Text(
                                text = "${dayForecast.minTemp}°",
                                fontSize = 10.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = OutlineVariant)

                // Lonja Fruit Price Box
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Lonja de Cítricos de Valencia • Semanal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = OnSurfaceVariant
                            )
                        }

                        if (onOpenAllPrices != null) {
                            Text(
                                text = "Ver todas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenAllPrices() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = orchard.variety,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = OnSurface,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = priceInfo.priceText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPositive = priceInfo.variationPercent >= 0
                        val variationBg = if (isPositive) PrimaryGreenContainer else Color(0xFFFFE4E6)
                        val variationColor = if (isPositive) PrimaryGreen else Color(0xFFDC2626)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(variationBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = variationColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${if (isPositive) "+" else ""}${priceInfo.variationPercent}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = variationColor
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Mín: ",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "%.2f".format(priceInfo.minPrice),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorLight
                            )
                            Text(
                                text = "Máx: ",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant
                            )
                            Text(
                                text = "%.2f".format(priceInfo.maxPrice),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }

                // Tareas Pendientes Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerLow)
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Tareas Pendientes",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        if (orchardTasks.isEmpty()) {
                            Text(
                                text = "Sin tareas pendientes",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        } else {
                            orchardTasks.take(2).forEach { task ->
                                val taskVisual = getFarmTaskVisualInfo(task.title)
                                PendingTaskRow(
                                    icon = taskVisual.icon,
                                    iconColor = taskVisual.iconColor,
                                    text = if (task.timeText.isNotBlank()) "${task.title} (${task.timeText})" else task.title
                                )
                            }
                        }
                    }
                }

                // Split buttons: "+ Parte" and "+ Tarea"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "+ Parte" Button
                    Button(
                        onClick = onRegisterPartClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("register_part_button_${orchard.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Parte",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    // "Tarea" Button
                    Button(
                        onClick = onRegisterTaskClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("register_task_button_${orchard.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tarea",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrchardDashboardBarCard(
    orchard: OrchardEntity,
    weather: OrchardWeather,
    viewModel: MainViewModel,
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onCardClick: () -> Unit = {},
    onRegisterPartClick: () -> Unit = {},
    onRegisterTaskClick: () -> Unit = {}
) {
    val isCitrus = orchard.fruitType != "Aguacate"
    val accentColor = if (isCitrus) SecondaryOrange else PrimaryGreen
    val priceInfo = viewModel.getPriceForVariety(orchard.variety)
    val allCalendarTasks by viewModel.allCalendarTasks.collectAsStateWithLifecycle()
    val orchardTasks = allCalendarTasks.filter { task ->
        !task.isDone && (
            (task.orchardId != null && task.orchardId == orchard.id) ||
            task.orchardName.equals(orchard.name, ignoreCase = true) ||
            task.orchardName.startsWith(orchard.name, ignoreCase = true)
        )
    }

    val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
    val activeAlerts = remember(allPosts, orchard) {
        allPosts.filter { post -> hasActiveAlertForOrchard(post, orchard) }
    }
    val hasActiveAlert = activeAlerts.isNotEmpty()
    val (currentIcon, currentIconColor) = getWeatherVisual(weather.iconType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isReorderMode || hasActiveAlert) 1.5.dp else 1.dp,
                color = when {
                    isReorderMode -> PrimaryGreen
                    hasActiveAlert -> Color(0xFFDC2626)
                    else -> OutlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onCardClick)
            .testTag("orchard_bar_card_${orchard.id}"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasActiveAlert) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color accent vertical bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (hasActiveAlert) Color(0xFFDC2626) else accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top row: Photo/Icon + Name + Badges + Weather Temp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!orchard.photoUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = orchard.photoUri,
                                contentDescription = "Foto ${orchard.name}",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(0.8.dp, OutlineVariant, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCitrus) Icons.Default.Eco else Icons.Default.Yard,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = orchard.name,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = OnSurface,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                if (hasActiveAlert) {
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.8.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Text(
                                            text = "ALERTA",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFDC2626),
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Subtitle: Variety & Location
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = SurfaceContainerHigh,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = orchard.variety,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }

                                val loc = when {
                                    orchard.municipality.isNotEmpty() -> {
                                        val poly = if (orchard.polygon.isNotEmpty()) " P.${orchard.polygon}" else ""
                                        "${orchard.municipality}$poly"
                                    }
                                    orchard.locationGps.isNotEmpty() -> "GPS"
                                    else -> ""
                                }
                                if (loc.isNotEmpty()) {
                                    Text(
                                        text = "• $loc",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Weather in header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = currentIcon,
                            contentDescription = weather.conditionDesc,
                            tint = currentIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${weather.currentTemp}°",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }
                }

                // Bottom bar row: Price & Tasks chips (Left) + Actions / Reorder (Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left chips: Lonja & Tasks
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Lonja price tag
                        Surface(
                            color = Color(0xFFFEF3C7).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Lonja: ${priceInfo.priceText}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        // Tasks tag (if any)
                        if (orchardTasks.isNotEmpty()) {
                            Surface(
                                color = PrimaryGreenContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${orchardTasks.size} ${if (orchardTasks.size == 1) "tarea" else "tareas"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreenDark,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Right controls: Reorder or Quick Actions
                    if (isReorderMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = onMoveUp,
                                enabled = canMoveUp,
                                modifier = Modifier.size(28.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (canMoveUp) PrimaryGreen else SurfaceContainerHigh,
                                    contentColor = if (canMoveUp) Color.White else OnSurfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Subir",
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onMoveDown,
                                enabled = canMoveDown,
                                modifier = Modifier.size(28.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (canMoveDown) PrimaryGreen else SurfaceContainerHigh,
                                    contentColor = if (canMoveDown) Color.White else OnSurfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Bajar",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onRegisterPartClick,
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Parte",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            FilledTonalButton(
                                onClick = onRegisterTaskClick,
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = accentColor.copy(alpha = 0.15f),
                                    contentColor = accentColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Tarea",
                                    fontSize = 11.sp,
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

@Composable
private fun PendingTaskRow(
    icon: ImageVector,
    iconColor: Color = PrimaryGreen,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface
        )
    }
}

fun getWeatherVisual(iconType: String): Pair<ImageVector, Color> {
    return when (iconType) {
        "sunny" -> Pair(Icons.Default.WbSunny, Color(0xFFF59E0B)) // Amber sun
        "partly_cloudy" -> Pair(Icons.Default.WbCloudy, Color(0xFFF97316)) // Sun behind cloud warm orange
        "cloudy" -> Pair(Icons.Default.Cloud, Color(0xFF64748B)) // Slate cloud
        "fog" -> Pair(Icons.Default.Dehaze, Color(0xFF94A3B8)) // Fog grey
        "drizzle" -> Pair(Icons.Default.Grain, Color(0xFF38BDF8)) // Sky blue
        "rainy" -> Pair(Icons.Default.WaterDrop, Color(0xFF0284C7)) // Blue
        "showers" -> Pair(Icons.Default.WaterDrop, Color(0xFF2563EB)) // Deep blue
        "storm" -> Pair(Icons.Default.Bolt, Color(0xFF8B5CF6)) // Violet electric storm
        "snow" -> Pair(Icons.Default.AcUnit, Color(0xFF38BDF8))
        else -> Pair(Icons.Default.WbSunny, Color(0xFFF59E0B))
    }
}

