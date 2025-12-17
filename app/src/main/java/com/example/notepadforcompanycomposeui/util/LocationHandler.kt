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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationHandler(private val context: Context) {
    private val locationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Configuration for Real-time updates.
     * High Accuracy + Fast Interval ensures the location 'snaps' to the correct spot quickly.
     */
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000) // Update every 3s
        .setMinUpdateIntervalMillis(1000) // Don't update faster than 1s
        .setWaitForAccurateLocation(false) // Don't wait forever, give us what you have and improve later
        .build()

    /**
     * Returns a stream (Flow) of location updates.
     * This is the BEST way to get accurate location. The first emission might be inaccurate (100m),
     * but subsequent emissions (every second) will zoom in (10m, 5m).
     */
    fun getLocationUpdates(): Flow<Location> = callbackFlow {
        // 1. Check permissions
        if (!checkLocationPermission()) {
            close() // Stop the flow if no permission
            return@callbackFlow
        }

        // 2. Define the callback
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                // Emit the latest location to the UI
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        // 3. Request updates
        try {
            locationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e) // Close flow on error
        }

        // 4. Clean up when the UI stops collecting (e.g., screen closed)
        awaitClose {
            locationClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Single shot location request.
     * Uses the modern 'getCurrentLocation' API which handles freshness automatically.
     */
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            if (!checkLocationPermission() || !isGPSEnabled()) {
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

    /**
     * Checks if the user has granted Fine Location permission.
     */
    fun checkLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Checks if the System GPS provider is actually on.
     */
    private fun isGPSEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Shows the Google Play Services dialog to turn on GPS without leaving the app.
     */
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

/**
 * Helper to remember the handler in Compose
 */
@Composable
fun rememberLocationHandler(context: Context): LocationHandler {
    return remember { LocationHandler(context) }
}