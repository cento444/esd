package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.util.AppKeyboards
import com.example.ui.util.ImageStorageHelper
import com.example.ui.util.LocationHelper
import com.example.data.LonjaPriceDatabase
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppTopHeader
import com.example.ui.components.FruitVarietyDropdown
import com.example.ui.components.appOutlinedTextFieldColors
import com.example.ui.components.rememberPhotoPickerHandler
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrchardScreen(
    editOrchardId: Long? = null,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allOrchards by viewModel.allOrchards.collectAsStateWithLifecycle()
    val orchardToEdit = remember(editOrchardId, allOrchards) {
        if (editOrchardId != null) allOrchards.find { it.id == editOrchardId } else null
    }
    val isEditing = orchardToEdit != null

    // Form state initialized with existing orchard values if editing
    var ownerType by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.ownerType ?: "propios")
    }
    var name by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.name ?: "")
    }
    var locationGps by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.locationGps ?: "")
    }
    var plantingYear by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.plantingYear?.takeIf { it > 0 }?.toString() ?: "")
    }

    var variety by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.variety ?: "Clemenules")
    }
    var rootstock by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.rootstock ?: "")
    }
    var varietyExpanded by remember { mutableStateOf(false) }
    var varietySearchQuery by remember { mutableStateOf("") }

    var municipality by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.municipality ?: "")
    }
    var partida by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.partida ?: "")
    }
    var polygon by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.polygon ?: "")
    }
    var parcel by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.parcel ?: "")
    }
    var hanegadas by remember(orchardToEdit) {
        mutableStateOf(
            orchardToEdit?.hanegadas?.takeIf { it > 0 }?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }

    var pozo by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.pozo ?: "")
    }
    var sector by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.sector ?: "")
    }
    var hidrante by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.hidrante ?: "")
    }
    var regadorName by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.regadorName ?: "")
    }
    var regadorPhone by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.regadorPhone ?: "")
    }

    var selectedPhotoUri by remember(orchardToEdit) {
        mutableStateOf(orchardToEdit?.photoUri)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isFetchingLocation = true
            LocationHelper.getCurrentLocation(
                context = context,
                onSuccess = { coords ->
                    locationGps = coords
                    isFetchingLocation = false
                    android.widget.Toast.makeText(context, "Ubicación obtenida: $coords", android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isFetchingLocation = false
                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        } else {
            isFetchingLocation = false
            android.widget.Toast.makeText(
                context,
                "Se requiere permiso de ubicación para obtener las coordenadas actuales.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestCurrentLocation() {
        if (LocationHelper.hasLocationPermission(context)) {
            isFetchingLocation = true
            LocationHelper.getCurrentLocation(
                context = context,
                onSuccess = { coords ->
                    locationGps = coords
                    isFetchingLocation = false
                    android.widget.Toast.makeText(context, "Ubicación obtenida: $coords", android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isFetchingLocation = false
                    android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
                }
            )
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val photoPickerHandler = rememberPhotoPickerHandler(prefix = "orchard") { localPath ->
        selectedPhotoUri = localPath
    }

    Scaffold(
        topBar = {
            AppTopHeader(
                title = if (isEditing) "Editar Huerto" else "Alta de Huerto",
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
            if (isEditing && !orchardToEdit?.name.isNullOrBlank()) {
                item {
                    Text(
                        text = "Editando: ${orchardToEdit?.name}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            // Section 1: Información General
            item {
                FormCard(title = "Información General", icon = Icons.Default.Info) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text(text = "Propietario", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OwnerTypeOption(
                                    label = userProfile.titularPropios.ifEmpty { "Propiedad" },
                                    selected = ownerType == "propios",
                                    modifier = Modifier.weight(1f),
                                    onClick = { ownerType = "propios" }
                                )
                                OwnerTypeOption(
                                    label = userProfile.titularVr.ifEmpty { "V&R C.B." },
                                    selected = ownerType == "v_y_r_cb",
                                    modifier = Modifier.weight(1f),
                                    onClick = { ownerType = "v_y_r_cb" }
                                )
                                OwnerTypeOption(
                                    label = userProfile.titularOtros.ifEmpty { "Otros" },
                                    selected = ownerType == "otros",
                                    modifier = Modifier.weight(1f),
                                    onClick = { ownerType = "otros" }
                                )
                            }
                        }

                        FormInputField(
                            label = "Nombre del Huerto",
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Ej. Finca Norte",
                            keyboardOptions = AppKeyboards.Words
                        )

                        FormInputField(
                            label = "Ubicación (GPS Google Maps)",
                            value = locationGps,
                            onValueChange = { locationGps = it },
                            placeholder = "Ej. 39.150, -0.433",
                            keyboardOptions = AppKeyboards.Text,
                            trailingIcon = {
                                IconButton(
                                    onClick = { requestCurrentLocation() },
                                    enabled = !isFetchingLocation,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Obtener ubicación actual",
                                        tint = if (locationGps.isNotEmpty()) PrimaryGreen else OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )

                        // Botón de obtener coordenadas actuales debajo del campo rellenable de ubicación
                        Button(
                            onClick = {
                                requestCurrentLocation()
                            },
                            enabled = !isFetchingLocation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("get_current_location_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryGreenContainer,
                                contentColor = OnPrimaryGreenContainer,
                                disabledContainerColor = PrimaryGreenContainer.copy(alpha = 0.6f),
                                disabledContentColor = OnPrimaryGreenContainer.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isFetchingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = PrimaryGreenDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Obteniendo ubicación GPS...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Obtener Coordenadas Actuales", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (locationGps.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryGreenContainer.copy(alpha = 0.4f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = PrimaryGreenDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Coordenadas fijadas: $locationGps",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreenDark
                                )
                            }
                        }

                        Text(
                            text = "Esta ubicación se utiliza para la organización del panel de control y la visualización en el mapa del inventario.",
                            fontSize = 11.sp,
                            color = OnSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Section 2: Detalles de Cultivo
            item {
                FormCard(title = "Detalles de Cultivo", icon = Icons.Default.Eco) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FruitVarietyDropdown(
                            selectedVariety = variety,
                            onVarietySelected = { variety = it },
                            label = "Variedad de la fruta",
                            placeholder = "Seleccionar variedad de cítrico / aguacate...",
                            testTag = "orchard_variety_dropdown"
                        )

                        FormInputField(
                            label = "Portainjerto de la variedad",
                            value = rootstock,
                            onValueChange = { rootstock = it },
                            placeholder = "Ej. Citrange Carrizo, Forner Alcaide 5, Macrophylla...",
                            keyboardOptions = AppKeyboards.Words
                        )

                        FormInputField(
                            label = "Año de plantación",
                            value = plantingYear,
                            onValueChange = { plantingYear = it.filter { ch -> ch.isDigit() }.take(4) },
                            placeholder = "Ej. 2018",
                            keyboardOptions = AppKeyboards.Number
                        )
                    }
                }
            }

            // Section 3: Datos Catastrales
            item {
                FormCard(title = "Datos Catastrales", icon = Icons.Default.Map) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FormInputField(
                                    label = "Término",
                                    value = municipality,
                                    onValueChange = { municipality = it },
                                    placeholder = "Municipio",
                                    keyboardOptions = AppKeyboards.Words
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FormInputField(
                                    label = "Partida",
                                    value = partida,
                                    onValueChange = { partida = it },
                                    placeholder = "Zona",
                                    keyboardOptions = AppKeyboards.Words
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FormInputField(
                                    label = "Polígono",
                                    value = polygon,
                                    onValueChange = { polygon = it.filter { ch -> ch.isDigit() } },
                                    placeholder = "000",
                                    keyboardOptions = AppKeyboards.Number
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FormInputField(
                                    label = "Parcela",
                                    value = parcel,
                                    onValueChange = { parcel = it.filter { ch -> ch.isDigit() } },
                                    placeholder = "000",
                                    keyboardOptions = AppKeyboards.Number
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FormInputField(
                                    label = "Hanegadas",
                                    value = hanegadas,
                                    onValueChange = { hanegadas = it },
                                    placeholder = "0.0",
                                    keyboardOptions = AppKeyboards.Decimal
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Datos de Riego
            item {
                FormCard(title = "Datos de Riego", icon = Icons.Default.WaterDrop) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FormInputField(label = "Pozo", value = pozo, onValueChange = { pozo = it }, placeholder = "Ej. Pozo A", keyboardOptions = AppKeyboards.Words)
                        FormInputField(label = "Sector", value = sector, onValueChange = { sector = it }, placeholder = "Ej. Sector 1", keyboardOptions = AppKeyboards.Words)
                        FormInputField(label = "Hidrante", value = hidrante, onValueChange = { hidrante = it }, placeholder = "Ej. H-12", keyboardOptions = AppKeyboards.Words)
                        FormInputField(label = "Regador", value = regadorName, onValueChange = { regadorName = it }, placeholder = "Nombre del regador", keyboardOptions = AppKeyboards.Words)
                        FormInputField(label = "Teléfono Regador", value = regadorPhone, onValueChange = { regadorPhone = it }, placeholder = "600 000 000", keyboardOptions = AppKeyboards.Phone)
                    }
                }
            }

            // Section 5: Subir o Tomar Foto del Huerto
            item {
                FormCard(title = "Subir o Tomar Foto del Huerto", icon = Icons.Default.PhotoCamera) {
                    if (selectedPhotoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, PrimaryGreen, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = selectedPhotoUri,
                                contentDescription = "Foto del Huerto",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Overlay action buttons
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
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(2.dp, OutlineVariant, RoundedCornerShape(12.dp))
                                .background(SurfaceContainerLow)
                                .clickable {
                                    photoPickerHandler.openPicker()
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Toca para abrir la cámara o galería",
                                    fontSize = 13.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Formatos soportados: JPG, PNG",
                                    fontSize = 11.sp,
                                    color = OnSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Por favor, introduce el nombre del huerto"
                            return@Button
                        }
                        val resolvedOwner = when (ownerType) {
                            "v_y_r_cb" -> userProfile.titularVr.ifEmpty { "V&R C.B." }
                            "propios" -> userProfile.titularPropios.ifEmpty { userProfile.name.ifEmpty { "Propiedad" } }
                            else -> userProfile.titularOtros.ifEmpty { "Otros" }
                        }
                        if (isEditing && editOrchardId != null) {
                            viewModel.updateOrchard(
                                id = editOrchardId,
                                name = name.trim(),
                                ownerType = ownerType,
                                ownerName = resolvedOwner,
                                variety = variety,
                                locationGps = locationGps.trim(),
                                municipality = municipality.trim(),
                                partida = partida.trim(),
                                polygon = polygon.trim(),
                                parcel = parcel.trim(),
                                hanegadas = hanegadas.toDoubleOrNull() ?: 0.0,
                                plantingYear = plantingYear.toIntOrNull() ?: 0,
                                pozo = pozo.trim(),
                                sector = sector.trim(),
                                hidrante = hidrante.trim(),
                                regadorName = regadorName.trim(),
                                regadorPhone = regadorPhone.trim(),
                                photoUri = selectedPhotoUri,
                                rootstock = rootstock.trim()
                            )
                        } else {
                            viewModel.addOrchard(
                                name = name.trim(),
                                ownerType = ownerType,
                                ownerName = resolvedOwner,
                                variety = variety,
                                locationGps = locationGps.trim(),
                                municipality = municipality.trim(),
                                partida = partida.trim(),
                                polygon = polygon.trim(),
                                parcel = parcel.trim(),
                                hanegadas = hanegadas.toDoubleOrNull() ?: 0.0,
                                plantingYear = plantingYear.toIntOrNull() ?: 0,
                                pozo = pozo.trim(),
                                sector = sector.trim(),
                                hidrante = hidrante.trim(),
                                regadorName = regadorName.trim(),
                                regadorPhone = regadorPhone.trim(),
                                photoUri = selectedPhotoUri,
                                rootstock = rootstock.trim()
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_orchard_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Text(
                            text = if (isEditing) "Guardar Cambios" else "Guardar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showMapPicker) {
            MapPickerDialog(
                initialLocation = locationGps,
                onDismiss = { showMapPicker = false },
                onSelectLocation = { selectedCoords ->
                    locationGps = selectedCoords
                    showMapPicker = false
                    android.widget.Toast.makeText(context, "Ubicación fijada: $selectedCoords", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun MapPickerDialog(
    initialLocation: String,
    onDismiss: () -> Unit,
    onSelectLocation: (String) -> Unit
) {
    val context = LocalContext.current
    val initLat = remember(initialLocation) {
        initialLocation.split(",").firstOrNull()?.trim()?.toDoubleOrNull() ?: 39.150000
    }
    val initLng = remember(initialLocation) {
        initialLocation.split(",").getOrNull(1)?.trim()?.toDoubleOrNull() ?: -0.433000
    }
    var selectedLat by remember { mutableDoubleStateOf(initLat) }
    var selectedLng by remember { mutableDoubleStateOf(initLng) }
    var isMapReady by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryGreenDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Buscar Huerto en el Mapa",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Toca o arrastra para marcar el huerto",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Interactive Map View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val mapHtml = remember {
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
                            <style>
                                * { -webkit-box-sizing: border-box; box-sizing: border-box; }
                                html, body {
                                    height: 100%;
                                    width: 100%;
                                    margin: 0;
                                    padding: 0;
                                    overflow: hidden;
                                    background: #2b3035;
                                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                }
                                #map {
                                    position: absolute;
                                    top: 0;
                                    bottom: 0;
                                    left: 0;
                                    right: 0;
                                    width: 100%;
                                    height: 100%;
                                    z-index: 1;
                                    background: #cad2c5;
                                }
                                .crosshair-tip {
                                    position: absolute;
                                    top: 10px;
                                    left: 50%;
                                    transform: translateX(-50%);
                                    z-index: 1000;
                                    background: rgba(255, 255, 255, 0.95);
                                    padding: 6px 14px;
                                    border-radius: 20px;
                                    font-size: 12px;
                                    font-weight: 600;
                                    color: #1b4332;
                                    box-shadow: 0 2px 8px rgba(0,0,0,0.25);
                                    pointer-events: none;
                                    white-space: nowrap;
                                }
                                .layer-switcher {
                                    position: absolute;
                                    bottom: 16px;
                                    right: 12px;
                                    z-index: 1000;
                                    background: #ffffff;
                                    color: #1b4332;
                                    border: 2px solid #2d6a4f;
                                    border-radius: 8px;
                                    padding: 6px 12px;
                                    font-size: 12px;
                                    font-weight: bold;
                                    box-shadow: 0 2px 8px rgba(0,0,0,0.25);
                                    cursor: pointer;
                                }
                            </style>
                            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
                        </head>
                        <body>
                            <div class="crosshair-tip">📍 Toca o arrastra para marcar tu huerto</div>
                            <div id="map"></div>
                            <button class="layer-switcher" id="layerBtn" onclick="toggleMapType()">🛰️ Satélite</button>
                            <script>
                                var map, marker, osmLayer, satLayer, currentMode = 'sat';

                                function startMap() {
                                    if (typeof L === 'undefined') {
                                        setTimeout(startMap, 100);
                                        return;
                                    }
                                    
                                    map = L.map('map', {
                                        zoomControl: true,
                                        attributionControl: false
                                    }).setView([$initLat, $initLng], 15);

                                    osmLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                        maxZoom: 19
                                    });

                                    satLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
                                        maxZoom: 19
                                    });

                                    // Capa satélite por defecto para ver las parcelas
                                    satLayer.addTo(map);

                                    marker = L.marker([$initLat, $initLng], {
                                        draggable: true
                                    }).addTo(map);

                                    function syncCoords(lat, lng) {
                                        marker.setLatLng([lat, lng]);
                                        if (window.AndroidInterface && window.AndroidInterface.onLocationChanged) {
                                            window.AndroidInterface.onLocationChanged(lat.toFixed(6), lng.toFixed(6));
                                        }
                                    }

                                    map.on('click', function(e) {
                                        syncCoords(e.latlng.lat, e.latlng.lng);
                                    });

                                    marker.on('dragend', function(e) {
                                        var pos = marker.getLatLng();
                                        syncCoords(pos.lat, pos.lng);
                                    });

                                    setTimeout(function() { map.invalidateSize(); }, 200);
                                    setTimeout(function() { map.invalidateSize(); }, 500);
                                    setTimeout(function() { map.invalidateSize(); }, 1000);
                                    window.addEventListener('resize', function() { map.invalidateSize(); });
                                }

                                function toggleMapType() {
                                    var btn = document.getElementById('layerBtn');
                                    if (currentMode === 'sat') {
                                        map.removeLayer(satLayer);
                                        osmLayer.addTo(map);
                                        currentMode = 'osm';
                                        btn.innerHTML = '🗺️ Callejero';
                                    } else {
                                        map.removeLayer(osmLayer);
                                        satLayer.addTo(map);
                                        currentMode = 'sat';
                                        btn.innerHTML = '🛰️ Satélite';
                                    }
                                }

                                if (document.readyState === 'complete' || document.readyState === 'interactive') {
                                    startMap();
                                } else {
                                    window.addEventListener('DOMContentLoaded', startMap);
                                }
                            </script>
                        </body>
                        </html>
                        """.trimIndent()
                    }

                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                }
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isMapReady = true
                                    }
                                }
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onLocationChanged(latStr: String, lngStr: String) {
                                            val lat = latStr.toDoubleOrNull()
                                            val lng = lngStr.toDoubleOrNull()
                                            if (lat != null && lng != null) {
                                                selectedLat = lat
                                                selectedLng = lng
                                            }
                                        }
                                    },
                                    "AndroidInterface"
                                )
                                loadDataWithBaseURL("https://www.openstreetmap.org", mapHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (!isMapReady) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    }
                }

                // Bottom Panel with selected coordinates and buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceContainerLow,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryGreenContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = PrimaryGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Coordenadas seleccionadas:",
                                        fontSize = 11.sp,
                                        color = OnSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.6f, %.6f", selectedLat, selectedLng),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreenDark
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val uri = Uri.parse("geo:$selectedLat,$selectedLng?q=$selectedLat,$selectedLng(Huerto)")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$selectedLat,$selectedLng")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.OpenInNew,
                                    contentDescription = "Ver en Google Maps",
                                    tint = PrimaryGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancelar", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    val formatted = String.format(java.util.Locale.US, "%.6f, %.6f", selectedLat, selectedLng)
                                    onSelectLocation(formatted)
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(44.dp)
                                    .testTag("confirm_map_location_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreenDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fijar Coordenadas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
            content()
        }
    }
}

@Composable
private fun OwnerTypeOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PrimaryGreenContainer else SurfaceContainerLow)
            .border(1.dp, if (selected) PrimaryGreen else OutlineVariant, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) OnPrimaryGreenContainer else OnSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = AppKeyboards.Text,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = OnSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            placeholder = { Text(text = placeholder, fontSize = 13.sp, color = OnSurfaceVariant.copy(alpha = 0.6f)) },
            singleLine = true,
            keyboardOptions = keyboardOptions,
            trailingIcon = trailingIcon,
            colors = appOutlinedTextFieldColors()
        )
    }
}
