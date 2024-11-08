package com.example.notepadforcompanycomposeui.screens

import android.content.Context
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.notepadforcompanycomposeui.ViewModels.MapViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.MapView.getTileSystem
import org.osmdroid.views.overlay.Marker

import org.osmdroid.events.MapListener
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.TilesOverlay

import android.graphics.*
import androidx.compose.foundation.isSystemInDarkTheme
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.toArgb
import com.example.notepadforcompanycomposeui.R
import com.example.notepadforcompanycomposeui.data.dataclass.UploadedNote

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userLocation = viewModel.userLocation.collectAsState().value
    val locationError = viewModel.locationError.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val isDarkTheme = isSystemInDarkTheme()

    var hasZoomedToUser by rememberSaveable { mutableStateOf(false) }
    var lastZoomLevel by rememberSaveable { mutableStateOf(6.0) }
    var lastCenterLat by rememberSaveable { mutableStateOf(9.145) }
    var lastCenterLon by rememberSaveable { mutableStateOf(40.489673) }

    var selectedNote by remember { mutableStateOf<UploadedNote?>(null) }

    val uploadedNotes by viewModel.uploadedNotes.collectAsState()




    // Initialize map configuration
    Configuration.getInstance().load(
        context,
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
    )


    class InvertedTilesOverlay(mapView: MapView, context: Context) : TilesOverlay(mapView.tileProvider, context) {
        private val paint = Paint()
        init {

            // Create inverse matrix
            val inverseMatrix = ColorMatrix(floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255f,
                0.0f, -1.0f, 0.0f, 0.0f, 255f,
                0.0f, 0.0f, -1.0f, 0.0f, 255f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            ))

            // Define destination color (dark gray)
            val destinationColor = android.graphics.Color.parseColor("#FF2A2A2A")

            // Calculate luminance ratios
            val lr = (255.0f - android.graphics.Color.red(destinationColor)) / 255.0f
            val lg = (255.0f - android.graphics.Color.green(destinationColor)) / 255.0f
            val lb = (255.0f - android.graphics.Color.blue(destinationColor)) / 255.0f

            // Create grayscale matrix
            val grayscaleMatrix = ColorMatrix(floatArrayOf(
                lr, lg, lb, 0f, 0f,
                lr, lg, lb, 0f, 0f,
                lr, lg, lb, 0f, 0f,
                0f, 0f, 0f, 0f, 255f
            ))
            grayscaleMatrix.preConcat(inverseMatrix)

            // Calculate destination color components
            val dr = android.graphics.Color.red(destinationColor)
            val dg = android.graphics.Color.green(destinationColor)
            val db = android.graphics.Color.blue(destinationColor)
            val drf = dr / 255f
            val dgf = dg / 255f
            val dbf = db / 255f

            // Create tint matrix
            val tintMatrix = ColorMatrix(floatArrayOf(
                drf, 0f, 0f, 0f, 0f,
                0f, dgf, 0f, 0f, 0f,
                0f, 0f, dbf, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            tintMatrix.preConcat(grayscaleMatrix)

            // Calculate scale and translate values
            val lDestination = drf * lr + dgf * lg + dbf * lb
            val scale = 1f - lDestination
            val translate = 1f - scale * 0.5f

            // Create scale matrix
            val scaleMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, dr * translate,
                0f, scale, 0f, 0f, dg * translate,
                0f, 0f, scale, 0f, db * translate,
                0f, 0f, 0f, 1f, 0f
            ))
            scaleMatrix.preConcat(tintMatrix)

            // Apply the color filter
            paint.colorFilter = ColorMatrixColorFilter(scaleMatrix)
        }

        override fun onTileReadyToDraw(
            c: Canvas?,
            currentTile: Drawable,
            tileRect: Rect
        ) {
            if (currentTile is BitmapDrawable && c != null) {
                // Additional water color adjustment
                val bitmap = currentTile.bitmap
                val modifiedBitmap = Bitmap.createBitmap(
                    bitmap.width,
                    bitmap.height,
                    Bitmap.Config.ARGB_8888
                )

                // Draw with the color filter we set up in init
                val canvas = Canvas(modifiedBitmap)
                val paint = Paint().apply {
                    colorFilter = this@InvertedTilesOverlay.paint.colorFilter
                }
                canvas.drawBitmap(bitmap, 0f, 0f, paint)

                // Additional water color enhancement
                val pixels = IntArray(bitmap.width * bitmap.height)
                modifiedBitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                val waterColor = android.graphics.Color.parseColor("#FF1B2838") // Deep blue for water
                for (i in pixels.indices) {
                    val pixel = pixels[i]
                    val red = android.graphics.Color.red(pixel)
                    val green = android.graphics.Color.green(pixel)
                    val blue = android.graphics.Color.blue(pixel)

                    // Detect water pixels and enhance them
                    if (blue > red && blue > green && blue > 150) {
                        val blendFactor = 0.5f
                        pixels[i] = android.graphics.Color.argb(
                            android.graphics.Color.alpha(pixel),
                            ((1 - blendFactor) * red + blendFactor * android.graphics.Color.red(waterColor)).toInt(),
                            ((1 - blendFactor) * green + blendFactor * android.graphics.Color.green(waterColor)).toInt(),
                            ((1 - blendFactor) * blue + blendFactor * android.graphics.Color.blue(waterColor)).toInt()
                        )
                    }
                }

                modifiedBitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                val finalDrawable = BitmapDrawable(context.resources, modifiedBitmap)
                super.onTileReadyToDraw(c, finalDrawable, tileRect)
            } else {
                super.onTileReadyToDraw(c, currentTile, tileRect)
            }
        }
    }


    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 2.0
            maxZoomLevel = 20.0
            setScrollableAreaLimitLatitude(
                getTileSystem().maxLatitude,
                getTileSystem().minLatitude,
                0
            )

            // Set the last known position and zoom
            controller.setCenter(GeoPoint(lastCenterLat, lastCenterLon))
            controller.setZoom(lastZoomLevel)
        }
    }

    LaunchedEffect(isDarkTheme) {
        if (isDarkTheme) {
            // Apply dark theme
            val darkTilesOverlay = InvertedTilesOverlay(mapView, context)
            darkTilesOverlay.loadingBackgroundColor = android.graphics.Color.BLACK
            darkTilesOverlay.loadingLineColor = android.graphics.Color.rgb(180, 180, 180)
            mapView.overlayManager.tilesOverlay = darkTilesOverlay
        } else {
            // Reset to default theme
            val defaultTilesOverlay = TilesOverlay(mapView.tileProvider, context)
            defaultTilesOverlay.loadingBackgroundColor = android.graphics.Color.rgb(238, 238, 238)
            defaultTilesOverlay.loadingLineColor = android.graphics.Color.rgb(100, 100, 100)
            mapView.overlayManager.tilesOverlay = defaultTilesOverlay
        }
        mapView.invalidate()
    }

    LaunchedEffect(uploadedNotes) {
        // Clear previous overlays (existing markers)
        //mapView.overlays.clear()

        uploadedNotes.forEach { note ->
            // Create a marker with custom settings
            val marker = Marker(mapView).apply {
                position = GeoPoint(note.latitude, note.longitude)
                title = note.companyName
                snippet = "Contact: ${note.phoneNumber}\nLocation: ${note.location}"

                // Set custom icon for the marker
                icon = AppCompatResources.getDrawable(mapView.context, R.drawable.campany)

                /*if (isDarkTheme) {
                    // Tint the marker icon for dark mode
                    icon?.setTint(android.graphics.Color.WHITE)
                }*/

                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Position icon's anchor at bottom center
            }

            // Set up an info window to display additional details
            marker.setOnMarkerClickListener { marker, mapView ->
                selectedNote = note // Set the selected note to show in the dialog
                true
            }

            mapView.overlays.add(marker)
        }

        // Refresh map to show new markers
        mapView.invalidate()
    }


    // Save map state when it changes
    DisposableEffect(mapView) {
        val mapListener = object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                mapView.getMapCenter()?.let { center ->
                    lastCenterLat = center.latitude
                    lastCenterLon = center.longitude
                }
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                lastZoomLevel = mapView.zoomLevelDouble
                return true
            }
        }

        mapView.addMapListener(mapListener)

        onDispose {
            mapView.removeMapListener(mapListener)
        }
    }

    // Lifecycle handling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    lastZoomLevel = mapView.zoomLevelDouble
                    mapView.getMapCenter()?.let { center ->
                        lastCenterLat = center.latitude
                        lastCenterLon = center.longitude
                    }
                    mapView.onPause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getLastLocation()
        viewModel.getUploadedNotes()
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    ) { mv ->


        /*viewModel.uploadedNotes.value.forEach { note ->
            Marker(mv).apply {
                position = GeoPoint(note.latitude, note.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = note.companyName
                snippet = "Phone: ${note.phoneNumber}\nLocation: ${note.location}"
                icon = context.getDrawable(org.osmdroid.library.R.drawable.ic_menu_compass)
                mv.overlays.add(this)
                setOnMarkerClickListener { _, _ ->
                    selectedNote = note // Set the selected note to show in the dialog
                    true
                }
            }
        }*/

        // Only zoom to user location if it's the first time and we have a location
        if (!hasZoomedToUser && userLocation != null) {
            userLocation.let { location ->
                mv.controller.apply {
                    setZoom(6.0)
                    setCenter(location)
                }
                hasZoomedToUser = true
                lastZoomLevel = 6.0
                lastCenterLat = location.latitude
                lastCenterLon = location.longitude
            }
        }

        // Always update the location marker
        if (userLocation != null) {
            //mv.overlays.clear()
            Marker(mv).apply {
                position = userLocation
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Your location"
                icon = context.getDrawable(R.drawable.my_location)
                if (isDarkTheme) {
                    // Tint the marker icon for dark mode
                    //icon?.setTint(android.graphics.Color.WHITE)
                }
                mv.overlays.add(this)
            }
        }

        mv.setScrollableAreaLimitDouble(
            BoundingBox(
                85.0,  // North
                180.0, // East
                -85.0, // South
                -180.0 // West
            )
        )
    }

    selectedNote?.let { note ->
        ShowNoteDetailsDialog(note) {
            selectedNote = null // Hide the dialog when dismissed
        }
    }

    // Error dialog
    if (locationError != null) {
        RetryLocationDialog(
            error = locationError,
            isLoading = isLoading,
            onRetry = { viewModel.retryLocationFetch() },
            onDismiss = { viewModel.clearLocationError() }
        )
    }
}

@Composable
fun ShowNoteDetailsDialog(note: UploadedNote, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(note.companyName) },
        text = { Text("Phone: ${note.phoneNumber}\nLocation: ${note.location}") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Okay")
            }
        }
    )
}
@Composable
private fun RetryLocationDialog(
    error: String,
    isLoading: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Error") },
        text = { Text(error) },
        confirmButton = {
            Button(
                onClick = {
                    onRetry()
                    //FirebaseCrashlytics.getInstance().log("Location fetch retry attempted")
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                //FirebaseCrashlytics.getInstance().log("Location error dialog dismissed")
            }) {
                Text("Dismiss")
            }
        }
    )
}