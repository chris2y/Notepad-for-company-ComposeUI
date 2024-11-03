package com.example.notepadforcompanycomposeui.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHandler(private val context: Context) {
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 100)
        .setWaitForAccurateLocation(true)
        .setMinUpdateIntervalMillis(100)
        .setMaxUpdateDelayMillis(200)
        .build()

    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            if (!checkLocationPermission()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            if (!isGPSEnabled()) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    continuation.resume(location)
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    fun checkLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isGPSEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun turnOnGPS(activity: Activity, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val task: Task<LocationSettingsResponse> =
            LocationServices.getSettingsClient(context).checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            when ((e as ApiException).statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                    try {
                        val resolvable = e as ResolvableApiException
                        resolvable.startResolutionForResult(activity, 2)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        onFailure()
                    }
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    onFailure()
                }
            }
        }
    }
}

@Composable
fun rememberLocationHandler(context: Context): LocationHandler {
    return remember { LocationHandler(context) }
}

// Location permission composable
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}