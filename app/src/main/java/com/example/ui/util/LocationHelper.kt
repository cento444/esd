package com.example.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        if (!hasLocationPermission(context)) {
            onError("Permiso de ubicación no concedido")
            return
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val formatted = String.format(Locale.US, "%.6f, %.6f", location.latitude, location.longitude)
                        onSuccess(formatted)
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    val formatted = String.format(Locale.US, "%.6f, %.6f", lastLoc.latitude, lastLoc.longitude)
                                    onSuccess(formatted)
                                } else {
                                    // Try standard LocationManager
                                    val lmLocation = getLocationFromLocationManager(context)
                                    if (lmLocation != null) {
                                        val formatted = String.format(Locale.US, "%.6f, %.6f", lmLocation.latitude, lmLocation.longitude)
                                        onSuccess(formatted)
                                    } else {
                                        onError("No se pudo obtener la señal GPS actual. Asegúrate de tener el GPS activado.")
                                    }
                                }
                            }
                            .addOnFailureListener {
                                val lmLocation = getLocationFromLocationManager(context)
                                if (lmLocation != null) {
                                    val formatted = String.format(Locale.US, "%.6f, %.6f", lmLocation.latitude, lmLocation.longitude)
                                    onSuccess(formatted)
                                } else {
                                    onError("Error al obtener la ubicación GPS")
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    val lmLocation = getLocationFromLocationManager(context)
                    if (lmLocation != null) {
                        val formatted = String.format(Locale.US, "%.6f, %.6f", lmLocation.latitude, lmLocation.longitude)
                        onSuccess(formatted)
                    } else {
                        onError("Error al obtener la ubicación GPS")
                    }
                }
        } catch (e: Exception) {
            val lmLocation = getLocationFromLocationManager(context)
            if (lmLocation != null) {
                val formatted = String.format(Locale.US, "%.6f, %.6f", lmLocation.latitude, lmLocation.longitude)
                onSuccess(formatted)
            } else {
                onError("Error: ${e.localizedMessage ?: "No se pudo obtener la ubicación"}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationFromLocationManager(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passiveLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            listOfNotNull(gpsLoc, networkLoc, passiveLoc).maxByOrNull { it.time }
        } catch (e: Exception) {
            null
        }
    }
}
