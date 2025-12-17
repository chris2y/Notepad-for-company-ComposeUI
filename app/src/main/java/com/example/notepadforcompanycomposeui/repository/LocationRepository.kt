package com.example.notepadforcompanycomposeui.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.notepadforcompanycomposeui.data.dataclass.UploadedNote
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.views.MapView
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
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

    /**
     * IMPROVED: Streams location updates.
     * Removed the strict 20m filter which blocks updates indoors.
     */
    @SuppressLint("MissingPermission") // Permissions are handled in UI
    fun getLocationUpdates(interval: Long): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateDistanceMeters(2f) // Lowered to detect small movements
            .setMinUpdateIntervalMillis(interval / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Return the most recent location available
                result.lastLocation?.let { location ->
                    trySend(location)
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

    /**
     * MAJOR FIX: Replaced complex manual logic with modern 'getCurrentLocation'.
     * This API automatically handles "try cache, if old, get fresh".
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location = suspendCancellableCoroutine { continuation ->
        // Cancellation token allows the coroutine to cancel the location request if the user leaves the screen
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                continuation.resume(location)
            } else {
                // If location is null (services disabled/no fix), throw exception to stop loading spinner
                continuation.resumeWithException(Exception("Location not found. Ensure GPS is on."))
            }
        }.addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }

        // Clean up if the coroutine is cancelled
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
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

    private val firestore = FirebaseFirestore.getInstance()
    suspend fun getUploadedNotes(): List<UploadedNote> {
        return withContext(Dispatchers.IO) {
            val notes = mutableListOf<UploadedNote>()
            val snapshot = firestore.collection("notes")
                .get()
                .await()

            for (document in snapshot.documents) {
                val noteId = document.getLong("noteId") ?: 0L
                val dateId = document.getLong("dateId") ?: 0L
                val noteText = document.getString("noteText") ?: ""
                val phoneNumber = document.getString("phoneNumber") ?: ""
                val companyName = document.getString("companyName") ?: ""
                val email = document.getString("email") ?: ""
                val location = document.getString("location") ?: ""
                val additionalInfo = document.getString("additionalInfo") ?: ""
                val followUp = document.getString("followUp") ?: ""
                val interestRate = document.getString("interestRate") ?: ""
                val imageUrl = document.getString("imageUrl") ?: ""
                val latitude = document.getDouble("latitude") ?: 0.0
                val longitude = document.getDouble("longitude") ?: 0.0

                val note = UploadedNote(
                    noteId = noteId,
                    dateId = dateId,
                    noteText = noteText,
                    phoneNumber = phoneNumber,
                    companyName = companyName,
                    email = email,
                    location = location,
                    additionalInfo = additionalInfo,
                    followUp = followUp,
                    interestRate = interestRate,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,

                )
                notes.add(note)

                // Log the fetched note
                println("Fetched note: $note")
            }
            notes
        }
    }
}