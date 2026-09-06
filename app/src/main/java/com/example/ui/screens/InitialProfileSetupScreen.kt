package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.components.rememberPhotoPickerHandler
import com.example.ui.theme.*
import com.example.ui.util.AppKeyboards

/**
 * Pantalla de bienvenida y configuración de perfil requerida la primera vez que se inicia la app.
 * Solicita de forma obligatoria el nombre de perfil y de forma opcional una fotografía del usuario.
 */
@Composable
fun InitialProfileSetupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()

    var name by remember {
        mutableStateOf(
            if (userProfile.name.isNotBlank() && userProfile.name != "Carlos Vicente") {
                userProfile.name
            } else {
                ""
            }
        )
    }
    var photoUri by remember { mutableStateOf(userProfile.photoUri) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val profilePhotoPicker = rememberPhotoPickerHandler(prefix = "profile_avatar") { localPath ->
        photoUri = localPath
    }

    val isNameValid = name.trim().isNotBlank()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Logo de la Aplicación
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(PrimaryGreenContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_agro_work),
                    contentDescription = "Logo Agro Work",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badge de bienvenida
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = PrimaryGreen.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Agriculture,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "BIENVENIDA A AGRO WORK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Configura tu Perfil",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Personaliza tus partes de campo, liquidaciones y avisos de la explotación agrícola.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Tarjeta principal de configuración
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // SELECCIÓN DE FOTO DE PERFIL (OPCIONAL)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val previewInitials = name.trim().split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercase() }
                            .joinToString("")
                            .ifEmpty { "U" }

                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(PrimaryGreenContainer)
                                .border(3.dp, PrimaryGreen.copy(alpha = 0.6f), CircleShape)
                                .clickable { profilePhotoPicker.openPicker() }
                                .testTag("initial_profile_photo_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!photoUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = "Foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (name.isNotBlank()) {
                                        Text(
                                            text = previewInitials,
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            // Badge de cámara flotante
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
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

                        // Botones para foto (cámara/galería o quitar)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { profilePhotoPicker.openPicker() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (!photoUri.isNullOrEmpty()) "Cambiar Foto" else "Añadir Foto (opcional)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (!photoUri.isNullOrEmpty()) {
                                TextButton(
                                    onClick = { photoUri = null },
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

                    HorizontalDivider(color = OutlineVariant.copy(alpha = 0.6f))

                    // CAMPO DE NOMBRE DE PERFIL (OBLIGATORIO)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nombre de perfil",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isNameValid) PrimaryGreen.copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isNameValid) "Completado" else "Obligatorio",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNameValid) PrimaryGreen else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Ej. Vicente Ripoll") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (name.isNotBlank()) PrimaryGreen else OnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (name.isNotEmpty()) {
                                    IconButton(onClick = { name = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Borrar texto",
                                            tint = OnSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            isError = hasAttemptedSubmit && !isNameValid,
                            singleLine = true,
                            keyboardOptions = AppKeyboards.Words,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("initial_profile_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = appOutlinedTextFieldColors()
                        )

                        if (hasAttemptedSubmit && !isNameValid) {
                            Text(
                                text = "Por favor, introduce tu nombre para continuar.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        } else {
                            Text(
                                text = "Este nombre identificará tus partes de trabajo, informes y titularidad.",
                                fontSize = 11.sp,
                                color = OnSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BOTÓN PRINCIPAL DE CONTINUAR / GUARDAR
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (isNameValid) {
                        viewModel.completeInitialProfile(
                            name = name,
                            photoUri = photoUri
                        )
                    }
                },
                enabled = isNameValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("complete_initial_profile_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    disabledContainerColor = PrimaryGreen.copy(alpha = 0.4f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Guardar y Comenzar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Podrás cambiar tu nombre y foto en cualquier momento desde los Ajustes.",
                fontSize = 11.sp,
                color = OnSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
