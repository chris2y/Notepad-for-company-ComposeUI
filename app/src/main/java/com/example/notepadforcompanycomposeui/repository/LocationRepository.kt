package com.example.notepadforcompanycomposeui.repository

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.views.MapView
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

class LocationRepository @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private var cacheManager: CacheManager? = null

    fun setupCache(mapView: MapView) {
        // Set up cache directory
        val cacheDir = File(context.getExternalFilesDir(null), "osmdroid")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // Initialize cache manager
        cacheManager = CacheManager(mapView)
    }

    fun getLocationUpdates(interval: Long): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateDistanceMeters(5f) // Minimum distance for updates
            .setMinUpdateIntervalMillis(5000) // Minimum time between updates
            .setMaxUpdateDelayMillis(interval) // Maximum time between updates
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    if (location.accuracy <= 20f) { // Only accept locations with accuracy better than 20 meters
                        trySend(location)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun getLastLocation(): Location = suspendCancellableCoroutine { continuation ->
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .setMaxUpdateDelayMillis(5000)
            .build()

        // First try to get the very last location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null && location.accuracy <= 20f) {
                    continuation.resume(location, null)
                } else {
                    // If last location is null or inaccurate, request a fresh location
                    requestFreshLocation(locationRequest) { newLocation ->
                        continuation.resume(newLocation, null)
                    }
                }
            }
            .addOnFailureListener {
                requestFreshLocation(locationRequest) { newLocation ->
                    continuation.resume(newLocation, null)
                }
            }
    }

    private fun requestFreshLocation(
        locationRequest: LocationRequest,
        onLocationResult: (Location) -> Unit
    ) {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.firstOrNull { it.accuracy <= 20f }?.let { location ->
                    fusedLocationClient.removeLocationUpdates(this)
                    onLocationResult(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun downloadMapArea(mapView: MapView, zoomMin: Int, zoomMax: Int) {
        cacheManager?.downloadAreaAsync(
            context,
            mapView.boundingBox,
            zoomMin,
            zoomMax,
            object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    // Cache download completed
                }
                override fun onTaskFailed(error: Int) {
                    // Handle download failure
                }
                override fun updateProgress(progress: Int, currentZoom: Int, zoomMin: Int, zoomMax: Int) {
                    // Update download progress
                }

                override fun downloadStarted() {
                    //
                }

                override fun setPossibleTilesInArea(total: Int) {
                    TODO("Not yet implemented")
                }
            }
        )
    }

    fun clearCache() {
        try {
            // Clear the tile cache using the cache directory
            val cacheDir = File(context.getExternalFilesDir(null), "osmdroid")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }

            // Cancel any ongoing downloads
            cacheManager?.cancelAllJobs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}