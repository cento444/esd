package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.PostCommentEntity
import com.example.data.model.SocialPostEntity
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AllFruitQuotesDialog
import com.example.ui.components.AppTopHeader
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.theme.*

@Composable
fun SocialScreen(viewModel: MainViewModel) {
    val posts by viewModel.allPosts.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val orchards by viewModel.allOrchards.collectAsStateWithLifecycle()

    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    var activeCommentPost by remember { mutableStateOf<SocialPostEntity?>(null) }
    var activeLikersPost by remember { mutableStateOf<SocialPostEntity?>(null) }
    var showAllFruitQuotesDialog by remember { mutableStateOf(false) }
    var showDeleteAllAlertsDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<SocialPostEntity?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val hasPosts = remember(posts) { posts.isNotEmpty() }

    Scaffold(
        topBar = {
            AppTopHeader(
                title = "Socios",
                showMenu = true,
                onMenuClick = { viewModel.navigateTo(Screen.Settings) },
                actions = {
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = if (hasPosts) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Limpiar muro de socios (Dejar limpio)",
                                            color = if (hasPosts) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                enabled = hasPosts,
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteAllAlertsDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceBright)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // "Subir Foto / Actualización" Action Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.NewPost()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(48.dp)
                                .testTag("new_post_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(text = "Subir Foto / Actualización", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Posts List
                if (posts.isEmpty()) {
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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DynamicFeed,
                                        contentDescription = null,
                                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No hay actualizaciones en el muro de socios",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = "Las tareas de huerto y partes de trabajo aparecerán aquí automáticamente.",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        SocialPostCard(
                            post = post,
                            currentUserProfile = userProfile,
                            onLikeClick = { viewModel.toggleLikePost(post.id) },
                            onShowLikers = { activeLikersPost = post },
                            onCommentClick = { activeCommentPost = post },
                            onImageClick = { previewPhotoUri = it },
                            onNavigateToTasks = { viewModel.navigateTo(Screen.CalendarView) },
                            onResolveAlert = { id, resolved ->
                                if (resolved) viewModel.resolveAlert(id) else viewModel.unresolveAlert(id)
                            },
                            onOpenAllPrices = { showAllFruitQuotesDialog = true },
                            onDeleteClick = { postToDelete = post }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Image Zoom Preview Modal
            if (!previewPhotoUri.isNullOrEmpty()) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { previewPhotoUri = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Fotografía de Parcela",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                IconButton(onClick = { previewPhotoUri = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = OnSurface)
                                }
                            }
                            AsyncImage(
                                model = previewPhotoUri,
                                contentDescription = "Imagen completa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 420.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // Comments Bottom Sheet
            activeCommentPost?.let { post ->
                PostCommentsBottomSheet(
                    post = post,
                    viewModel = viewModel,
                    currentUserProfile = userProfile,
                    onDismiss = { activeCommentPost = null }
                )
            }

            // Likers Dialog
            activeLikersPost?.let { post ->
                PostLikersDialog(
                    post = post,
                    currentUserProfile = userProfile,
                    onDismiss = { activeLikersPost = null }
                )
            }

            // All Fruit Quotes Dialog from Lonja
            if (showAllFruitQuotesDialog) {
                AllFruitQuotesDialog(
                    userOrchards = orchards,
                    onDismiss = { showAllFruitQuotesDialog = false }
                )
            }

            // Dialog: Delete All Posts Confirmation
            if (showDeleteAllAlertsDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAllAlertsDialog = false },
                    title = {
                        Text(
                            text = "¿Limpiar todo el muro de socios?",
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    },
                    text = {
                        Text(
                            text = "Se eliminarán todas las publicaciones y notificaciones actuales del muro de socios para que puedas empezar a utilizar la app desde 0 con el muro completamente limpio. Esta acción no se puede deshacer.",
                            color = OnSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.clearAllSocialPosts()
                                showDeleteAllAlertsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Limpiar Todo", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAllAlertsDialog = false }) {
                            Text("Cancelar", color = OnSurfaceVariant)
                        }
                    },
                    containerColor = SurfaceWhite
                )
            }

            // Dialog: Delete Single Post Confirmation
            postToDelete?.let { targetPost ->
                AlertDialog(
                    onDismissRequest = { postToDelete = null },
                    title = {
                        Text(
                            text = if (targetPost.isAlert) "¿Borrar alerta?" else "¿Eliminar publicación?",
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    },
                    text = {
                        Text(
                            text = if (targetPost.isAlert) {
                                "Se eliminará esta alerta de '${targetPost.alertTitle ?: targetPost.orchardName}' de la pantalla de socios."
                            } else {
                                "Se eliminará esta publicación del muro de socios."
                            },
                            color = OnSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteSocialPost(targetPost.id)
                                postToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Eliminar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { postToDelete = null }) {
                            Text("Cancelar", color = OnSurfaceVariant)
                        }
                    },
                    containerColor = SurfaceWhite
                )
            }
        }
    }
}

@Composable
fun SocialPostCard(
    post: SocialPostEntity,
    currentUserProfile: com.example.data.model.UserProfileEntity? = null,
    onLikeClick: () -> Unit,
    onShowLikers: () -> Unit,
    onCommentClick: () -> Unit,
    onImageClick: ((String) -> Unit)? = null,
    onNavigateToTasks: (() -> Unit)? = null,
    onResolveAlert: ((Long, Boolean) -> Unit)? = null,
    onOpenAllPrices: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
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
            // Author & Timestamp Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Determine Avatar
                val effectiveAvatar = remember(post.authorAvatar, currentUserProfile?.photoUri, post.authorName, currentUserProfile?.name) {
                    when {
                        !post.authorAvatar.isNullOrBlank() -> post.authorAvatar
                        currentUserProfile?.photoUri != null && (
                            post.authorName.isBlank() ||
                            post.authorName.equals(currentUserProfile.name, ignoreCase = true) ||
                            post.authorName.equals("Carlos Vicente", ignoreCase = true) ||
                            post.authorName.equals("Usuario", ignoreCase = true)
                        ) -> currentUserProfile.photoUri
                        else -> currentUserProfile?.photoUri
                    }
                }

                val initials = post.authorName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (post.isAlert) Color(0xFFFFE4E6) else PrimaryGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!effectiveAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = effectiveAvatar,
                            contentDescription = "Avatar de ${post.authorName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (initials.isNotEmpty()) initials else "SA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (post.isAlert) Color(0xFFDC2626) else PrimaryGreen
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.authorName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = post.timestampText,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }

                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = OnSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Alert Box (if isAlert)
            if (post.isAlert) {
                val isResolved = post.isAlertResolved
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isResolved) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (isResolved) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                        )
                    )
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isResolved) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = post.alertTitle ?: "Alerta de Cultivo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isResolved) Color(0xFF15803D) else Color(0xFFDC2626)
                                )
                            }

                            Surface(
                                color = if (isResolved) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isResolved) "RESUELTA" else "ACTIVA EN INICIO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isResolved) Color(0xFF15803D) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = post.content,
                            fontSize = 13.sp,
                            color = if (isResolved) Color(0xFF166534) else Color(0xFF991B1B)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Comentar Acción",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isResolved) Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier
                                    .clickable(onClick = onCommentClick)
                                    .padding(vertical = 4.dp)
                            )

                            if (onResolveAlert != null) {
                                TextButton(
                                    onClick = { onResolveAlert(post.id, !isResolved) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isResolved) Icons.Default.Refresh else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (isResolved) Color(0xFF6B7280) else Color(0xFF16A34A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isResolved) "Reactivar" else "Marcar Resuelta",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isResolved) Color(0xFF6B7280) else Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (post.content.startsWith("📅") ||
                post.content.startsWith("✅ Tarea") ||
                post.content.startsWith("🔄 Tarea") ||
                post.content.startsWith("🗑️ Tarea") ||
                post.content.contains("tarea programada", ignoreCase = true) ||
                post.content.contains("tarea de huerto", ignoreCase = true) ||
                post.content.contains("tarea completada", ignoreCase = true)
            ) {
                // Agricultural Task Card in Feed
                val taskTitle = if (post.content.contains("'")) {
                    post.content.substringAfter("'").substringBefore("'").trim()
                } else {
                    post.content.lines().firstOrNull()?.replace("📅", "")?.replace("✅", "")?.replace("🔄", "")?.replace("🗑️", "")?.trim() ?: "Tarea de Huerto"
                }
                val taskVisual = getFarmTaskVisualInfo(taskTitle)
                val isCompleted = post.content.contains("completada", ignoreCase = true) || post.content.contains("finalizada", ignoreCase = true)
                val isCancelled = post.content.contains("cancelada", ignoreCase = true) || post.content.contains("eliminada", ignoreCase = true)
                val isReopened = post.content.contains("reabierta", ignoreCase = true)

                val badgeText = when {
                    isCompleted -> "Tarea"
                    isCancelled -> "🗑️ Tarea Cancelada"
                    isReopened -> "🔄 Tarea Reabierta"
                    else -> "📅 Tarea de Huerto"
                }
                val badgeBg = when {
                    isCompleted -> Color(0xFFF1F5F9)
                    isCancelled -> Color(0xFFFEE2E2)
                    isReopened -> Color(0xFFFEF3C7)
                    else -> Color(0xFFE0F2FE)
                }
                val badgeColor = when {
                    isCompleted -> Color(0xFF475569)
                    isCancelled -> Color(0xFFDC2626)
                    isReopened -> Color(0xFFD97706)
                    else -> Color(0xFF0284C7)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceBright),
                    shape = RoundedCornerShape(10.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant))
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeColor)
                            }
                            if (post.orchardName.isNotBlank() && post.orchardName != "General") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceWhite)
                                        .border(0.5.dp, OutlineVariant, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = post.orchardName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = OnSurface)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(taskVisual.containerColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = taskVisual.icon,
                                    contentDescription = null,
                                    tint = taskVisual.iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = taskTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface
                                )
                                Text(
                                    text = post.content,
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        if (onNavigateToTasks != null) {
                            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onNavigateToTasks)
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ver en calendario de tareas",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else if (post.authorName.contains("Lonja", ignoreCase = true) ||
                post.orchardName == "Mercado y Lonjas" ||
                post.content.contains("LONJA DE CÍTRICOS", ignoreCase = true) ||
                post.content.contains("COTIZACIÓN SEMANAL", ignoreCase = true)
            ) {
                // Lonja Market Bulletin Card
                val cleanedLonjaContent = remember(post.content) {
                    val bulletLines = post.content.lines().map { it.trim() }.filter { it.startsWith("•") }
                    if (bulletLines.isNotEmpty()) {
                        bulletLines.joinToString("\n") { line ->
                            line.replace("(= (", "(=").replace("%))", "%)")
                        }
                    } else {
                        post.content
                            .replace(Regex("📊 NUEVA COTIZACIÓN[^\n]*\n?"), "")
                            .replace(Regex("📅 Semana[^\n]*\n?"), "")
                            .replace("Cotizaciones oficiales para tus huertos:\n", "")
                            .replace("Cotizaciones oficiales para tus huertos:", "")
                            .replace("Precios de referencia oficiales destacados:\n", "")
                            .replace("Precios de referencia oficiales destacados:", "")
                            .replace("\n\nConsulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
                            .replace("Consulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
                            .trim()
                    }
                }

                val isDark = LocalIsDarkTheme.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF261E10) else Color(0xFFFFFBEB)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF664814) else Color(0xFFFDE68A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDark) Color(0xFF3D2A10) else Color(0xFFFEF3C7))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Lonja de Cítricos de Valencia",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                    )
                                }
                            }

                            Surface(
                                color = if (isDark) Color(0xFF382305) else Color(0xFFFEF9C3),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "OFICIAL",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFFFDE047) else Color(0xFF854D0E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = cleanedLonjaContent,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFF9FAFB) else Color(0xFF1E293B),
                            lineHeight = 20.sp
                        )

                        if (onOpenAllPrices != null) {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = if (isDark) Color(0xFF4D3710) else Color(0xFFFDE68A)
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenAllPrices)
                                    .testTag("lonja_all_prices_link"),
                                color = if (isDark) Color(0xFF36250B) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF6B4B15) else Color(0xFFFCD34D))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Ver todas las cotizaciones de las frutas",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Standard Text Content
                val cleanedText = remember(post.content) {
                    post.content
                        .replace("\n\nConsulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
                        .replace("Consulte el desglose completo de cotizaciones por variedad en el panel de inicio.", "")
                        .trimEnd()
                }
                Text(
                    text = cleanedText,
                    fontSize = 14.sp,
                    color = OnSurface,
                    lineHeight = 20.sp
                )
            }

            // Image (if any)
            if (!post.photoUri.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceContainerHigh)
                        .clickable { onImageClick?.invoke(post.photoUri) }
                ) {
                    AsyncImage(
                        model = post.photoUri,
                        contentDescription = "Foto publicación",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

            // Interaction Row (Likes, Comments)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onLikeClick)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Me gusta",
                            tint = if (post.isLiked) PrimaryGreen else OnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "${post.likesCount}",
                            fontSize = 13.sp,
                            color = if (post.isLiked) PrimaryGreen else OnSurfaceVariant,
                            fontWeight = if (post.isLiked) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    if (post.likesCount > 0) {
                        Text(
                            text = "• Ver quiénes",
                            fontSize = 12.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onShowLikers)
                                .padding(4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCommentClick)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comentarios",
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${post.commentsCount}",
                        fontSize = 13.sp,
                        color = OnSurfaceVariant,
                        fontWeight = if (post.commentsCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostCommentsBottomSheet(
    post: SocialPostEntity,
    viewModel: MainViewModel,
    currentUserProfile: com.example.data.model.UserProfileEntity?,
    onDismiss: () -> Unit
) {
    val comments by viewModel.getCommentsForPost(post.id).collectAsStateWithLifecycle(emptyList())
    var commentText by remember { mutableStateOf("") }
    var commentToDelete by remember { mutableStateOf<PostCommentEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Comentarios (${comments.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )
                    Text(
                        text = if (post.isAlert) "En alerta: ${post.alertTitle ?: post.orchardName}" else "En publicación de ${post.authorName} (${post.orchardName})",
                        fontSize = 12.sp,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = OnSurfaceVariant)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

            // Original Post Summary Snippet
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = if (post.isAlert) Color(0xFFFEF2F2) else SurfaceBright),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (post.isAlert) Icons.Default.Warning else Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        tint = if (post.isAlert) Color(0xFFDC2626) else PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = post.content,
                        fontSize = 12.sp,
                        color = if (post.isAlert) Color(0xFF991B1B) else OnSurfaceVariant,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = OnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Aún no hay comentarios",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OnSurface
                        )
                        Text(
                            text = "Sé el primer socio en responder o comentar esta publicación.",
                            fontSize = 12.sp,
                            color = OnSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwipeLeft,
                                contentDescription = null,
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Desliza a la izquierda para borrar un comentario",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    items(comments, key = { it.id }) { comment ->
                        SwipeableCommentItemRow(
                            comment = comment,
                            currentUserProfile = currentUserProfile,
                            onDeleteRequest = { commentToDelete = comment }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

            // Confirmation Dialog before deleting
            commentToDelete?.let { targetComment ->
                AlertDialog(
                    onDismissRequest = { commentToDelete = null },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Eliminar comentario",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = OnSurface
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "¿Estás seguro de que deseas eliminar este comentario?",
                                fontSize = 14.sp,
                                color = OnSurface
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceBright),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "\"${targetComment.content}\"",
                                    fontSize = 13.sp,
                                    color = OnSurfaceVariant,
                                    modifier = Modifier.padding(10.dp),
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteComment(targetComment.id, post.id)
                                commentToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Eliminar", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { commentToDelete = null }
                        ) {
                            Text("Cancelar", color = OnSurfaceVariant)
                        }
                    },
                    containerColor = SurfaceWhite,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // Input Bar
            val effectiveUserName = currentUserProfile?.name?.ifBlank { "Carlos Vicente" } ?: "Carlos Vicente"
            val effectiveUserAvatar = currentUserProfile?.photoUri
            val initials = effectiveUserName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Current User Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreenContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!effectiveUserAvatar.isNullOrBlank()) {
                        AsyncImage(
                            model = effectiveUserAvatar,
                            contentDescription = "Mi avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = if (initials.isNotEmpty()) initials else "CV",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }

                // Comment input
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            text = "Comentar como $effectiveUserName...",
                            fontSize = 13.sp,
                            color = OnSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = appOutlinedTextFieldColors(
                        containerColor = SurfaceBright
                    ),
                    maxLines = 4,
                    minLines = 1
                )

                IconButton(
                    onClick = {
                        if (commentText.isNotBlank()) {
                            viewModel.addComment(post.id, commentText)
                            commentText = ""
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = PrimaryGreen,
                        disabledContentColor = OnSurfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar comentario",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCommentItemRow(
    comment: PostCommentEntity,
    currentUserProfile: com.example.data.model.UserProfileEntity?,
    onDeleteRequest: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDeleteRequest()
                false
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.3f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isSwipingToDelete = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSwipingToDelete) Color(0xFFDC2626) else Color.Transparent)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwipingToDelete) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Eliminar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar comentario",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
        ) {
            CommentItemRow(
                comment = comment,
                currentUserProfile = currentUserProfile
            )
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: PostCommentEntity,
    currentUserProfile: com.example.data.model.UserProfileEntity?
) {
    val currentUserName = currentUserProfile?.name?.ifBlank { "Carlos Vicente" } ?: "Carlos Vicente"
    val isMyComment = comment.authorName.equals(currentUserName, ignoreCase = true) ||
                      (comment.authorName.equals("Carlos Vicente", ignoreCase = true) && (currentUserProfile?.name.isNullOrBlank() || currentUserProfile?.name == "Carlos Vicente"))

    val effectiveAvatar = remember(comment.authorAvatar, currentUserProfile?.photoUri, isMyComment) {
        if (!comment.authorAvatar.isNullOrBlank()) comment.authorAvatar
        else if (isMyComment) currentUserProfile?.photoUri
        else null
    }

    val initials = comment.authorName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("comment_item_${comment.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(PrimaryGreenContainer),
            contentAlignment = Alignment.Center
        ) {
            if (!effectiveAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = effectiveAvatar,
                    contentDescription = "Avatar de ${comment.authorName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = if (initials.isNotEmpty()) initials else "S",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceBright)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSurface
                )
                Text(
                    text = comment.timestampText,
                    fontSize = 11.sp,
                    color = OnSurfaceVariant
                )
            }

            Text(
                text = comment.content,
                fontSize = 13.sp,
                color = OnSurface,
                lineHeight = 18.sp
            )
        }
    }
}


fun getLikersForPost(post: SocialPostEntity, currentUserName: String): List<String> {
    val cooperativePartners = listOf(
        "María Dolores Giner",
        "José García Monzó",
        "Ana Belén Torres",
        "Vicente Martínez Pla",
        "Carmen Rosa Sivera",
        "Francisco Javier Ribera",
        "Rosa María Blasco",
        "Manuel Albors",
        "Teresa Soler"
    )
    val count = post.likesCount
    if (count <= 0) return emptyList()

    val list = mutableListOf<String>()
    if (post.isLiked) {
        list.add(currentUserName.ifEmpty { "Carlos Vicente" })
    }
    
    val seed = Math.abs(post.id.toInt())
    val shuffledPartners = cooperativePartners.shuffled(java.util.Random(seed.toLong()))
    
    var remaining = count - if (post.isLiked) 1 else 0
    for (partner in shuffledPartners) {
        if (remaining <= 0) break
        if (!list.contains(partner)) {
            list.add(partner)
            remaining--
        }
    }
    var i = 1
    while (remaining > 0) {
        val genericName = "Socio Colaborador $i"
        if (!list.contains(genericName)) {
            list.add(genericName)
            remaining--
        }
        i++
    }
    return list
}

@Composable
fun PostLikersDialog(
    post: SocialPostEntity,
    currentUserProfile: com.example.data.model.UserProfileEntity?,
    onDismiss: () -> Unit
) {
    val currentUserName = currentUserProfile?.name ?: "Carlos Vicente"
    val likers = remember(post.id, post.likesCount, post.isLiked, currentUserName) {
        getLikersForPost(post, currentUserName)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Me gusta (${post.likesCount})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = OnSurface)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = OutlineVariant)

                if (likers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ningún socio ha dado Me gusta todavía.",
                            fontSize = 13.sp,
                            color = OnSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(likers) { likerName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceBright)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = likerName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen,
                                        fontSize = 15.sp
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = likerName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OnSurface
                                    )
                                    Text(
                                        text = if (likerName == currentUserName) "Tú" else "Socio de la cooperativa",
                                        fontSize = 12.sp,
                                        color = OnSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
