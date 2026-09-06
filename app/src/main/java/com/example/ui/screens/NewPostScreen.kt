package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.components.rememberPhotoPickerHandler
import com.example.ui.util.AppKeyboards
import com.example.ui.util.ImageStorageHelper
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    preselectedOrchardName: String? = null,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()

    var content by remember { mutableStateOf("") }
    var selectedOrchardName by remember {
        mutableStateOf(preselectedOrchardName ?: allOrchards.firstOrNull()?.name ?: "Finca Norte")
    }
    var orchardDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var isAlert by remember { mutableStateOf(false) }
    var alertTitle by remember { mutableStateOf("") }

    val photoPickerHandler = rememberPhotoPickerHandler(prefix = "post") { localPath ->
        selectedPhotoUri = localPath
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                color = SurfaceWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = OnSurface)
                    }

                    Text(
                        text = "Nueva Actualización",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceWhite)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo Upload Area
            item {
                if (selectedPhotoUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, PrimaryGreen, RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Foto seleccionada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Floating action buttons over image (Change / Remove)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    photoPickerHandler.openPicker()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SurfaceWhite.copy(alpha = 0.9f),
                                    contentColor = PrimaryGreen
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalIconButton(
                                onClick = { selectedPhotoUri = null },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color.Red.copy(alpha = 0.9f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar foto", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, OutlineVariant, RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .clickable {
                                photoPickerHandler.openPicker()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Subir o Tomar Foto del Huerto",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "Toca para seleccionar de la galería o cámara",
                                fontSize = 12.sp,
                                color = OnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Orchard Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Huerto relacionado", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = orchardDropdownExpanded,
                        onExpandedChange = { orchardDropdownExpanded = !orchardDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedOrchardName,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orchardDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = orchardDropdownExpanded,
                            onDismissRequest = { orchardDropdownExpanded = false }
                        ) {
                            allOrchards.forEach { orchard ->
                                DropdownMenuItem(
                                    text = { Text(orchard.name, color = OnSurface) },
                                    onClick = {
                                        selectedOrchardName = orchard.name
                                        orchardDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Content Textarea
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Mensaje", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        placeholder = { Text("¿Qué está pasando en el huerto? Escribe novedades, incidencias o tareas realizadas...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = AppKeyboards.Text,
                        colors = appOutlinedTextFieldColors()
                    )
                }
            }

            // Quick Tag / Options
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isAlert,
                        onClick = { isAlert = !isAlert },
                        label = { Text("Marcar como alerta prioritaria", fontSize = 13.sp, fontWeight = if (isAlert) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF2F2),
                            selectedLabelColor = Color(0xFFDC2626),
                            selectedLeadingIconColor = Color(0xFFDC2626)
                        )
                    )
                }
            }

            if (isAlert) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = alertTitle,
                            onValueChange = { alertTitle = it },
                            label = { Text("Título de la alerta") },
                            placeholder = { Text("Ej. Alerta de Plaga / Mosca de la fruta") },
                            singleLine = true,
                            keyboardOptions = AppKeyboards.Words,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = appOutlinedTextFieldColors(
                                focusedBorderColor = Color(0xFFDC2626)
                            )
                        )
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Esta alerta se destacará con aviso prioritario en '$selectedOrchardName' en la pantalla de inicio.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            val matchedOrchard = allOrchards.find { it.name.equals(selectedOrchardName, ignoreCase = true) }
                            viewModel.createSocialPost(
                                orchardId = matchedOrchard?.id,
                                orchardName = selectedOrchardName,
                                content = content,
                                photoUri = selectedPhotoUri,
                                isAlert = isAlert,
                                alertTitle = if (isAlert) {
                                    if (alertTitle.isNotBlank()) alertTitle.trim() else "Alerta en $selectedOrchardName"
                                } else null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(text = "Publicar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
