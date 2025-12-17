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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.notepadforcompanycomposeui.R
import com.example.notepadforcompanycomposeui.data.dataclass.UploadedNote

@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userLocation by viewModel.userLocation.collectAsState() // Use 'by' for direct access
    val locationError = viewModel.locationError.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val isDarkTheme = isSystemInDarkTheme()

    var hasZoomedToUser by rememberSaveable { mutableStateOf(false) }
    var lastZoomLevel by rememberSaveable { mutableStateOf(6.0) }
    var lastCenterLat by rememberSaveable { mutableStateOf(9.145) }
    var lastCenterLon by rememberSaveable { mutableStateOf(40.489673) }

    var selectedNote by remember { mutableStateOf<UploadedNote?>(null) }

    val uploadedNotes by viewModel.uploadedNotes.collectAsState()




    // 1. OPTIMIZATION: Increase cache size for smoother panning
    Configuration.getInstance().apply {
        load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))
        cacheMapTileCount = 30.toShort() // Cache more tiles in memory (default is 9)
        cacheMapTileOvershoot = 30.toShort() // Pre-load tiles just outside the screen
    }


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

    // 2. OPTIMIZATION: Fast Dark Mode (No pixel loops!)
    class FastDarkModeOverlay(mapView: MapView) : TilesOverlay(mapView.tileProvider, context) {
        init {
            // Use a simple high-contrast ColorMatrix for dark mode
            // This runs on the GPU and is ZERO lag compared to manual pixel manipulation
            val matrix = ColorMatrix()
            matrix.setSaturation(0f) // Remove color (grayscale)

            // Invert colors (Make white roads dark, black text white)
            val invert = floatArrayOf(
                -1f,  0f,  0f, 0f, 255f,
                0f, -1f,  0f, 0f, 255f,
                0f,  0f, -1f, 0f, 255f,
                0f,  0f,  0f, 1f,   0f
            )
            matrix.postConcat(ColorMatrix(invert))

            // Darken everything slightly so it's not too harsh
            val darken = ColorMatrix()
            darken.setScale(0.8f, 0.8f, 0.8f, 1f)
            matrix.postConcat(darken)

            setColorFilter(ColorMatrixColorFilter(matrix))
        }
    }

    val mapView = remember {
        MapView(context).apply {
            // 3. DETAIL SETTINGS
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            // --- FIX FOR MULTIPLE WORLDS ---
            // 1. Disable the wrapping/repeating of the map
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false

            // 2. Limit scrolling so the user can't scroll into the gray void
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude,
                MapView.getTileSystem().minLatitude,
                0
            )
            setScrollableAreaLimitLongitude(
                -180.0,
                180.0,
                0
            )

            // 3. Prevent zooming out too far (optional, keeps map filling the screen)
            minZoomLevel = 3.0
            maxZoomLevel = 20.0

            // Text scaling
            isTilesScaledToDpi = true

            // 4. PERFORMANCE SETTINGS
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setHasTransientState(true)
        }
    }

    /*val mapView = remember {
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
    }*/

    // 1. UPDATE LIFECYCLE OBSERVER
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    // Start tracking real-time location
                    viewModel.startLocationUpdates()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    // Stop tracking to save battery
                    viewModel.stopLocationUpdates()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLocationUpdates()
        }
    }

    // Apply Theme Changes
    LaunchedEffect(isDarkTheme) {
        mapView.overlayManager.tilesOverlay = if (isDarkTheme) {
            FastDarkModeOverlay(mapView)
        } else {
            TilesOverlay(mapView.tileProvider, context)
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

    var userMarker by remember { mutableStateOf<Marker?>(null) }

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


// Update User Location Marker Logic
        if (userLocation != null) {
            if (userMarker == null) {
                // Create the marker ONLY once
                userMarker = Marker(mv).apply {
                    title = "Your location"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = AppCompatResources.getDrawable(context, R.drawable.my_location) // Ensure you have this drawable
                    // Prevent this marker from being cleared when other notes update
                    id = "MY_LOCATION_MARKER"
                }
                mv.overlays.add(userMarker)
            }

            // Just update the position of the existing marker
            userMarker?.position = userLocation
            userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Force redraw
            mv.invalidate()
        }

        // Zoom logic (First time only)
        if (!hasZoomedToUser && userLocation != null) {
            userLocation?.let { location ->
                mv.controller.animateTo(location, 15.0, 1000L) // animated for smooth look
                hasZoomedToUser = true
            }
        }
    }


    // 3. ADD A "RE-CENTER" BUTTON (Optional but recommended)
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = {
                userLocation?.let {
                    mapView.controller.animateTo(it, 16.0, 1000L)
                } ?: viewModel.startLocationUpdates() // Retry if null
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .padding(bottom = 70.dp) // Adjust based on your Nav bar
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center Map")
        }
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
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = note.companyName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Divider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Details
                Text(
                    text = "Phone: ${note.phoneNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Location: ${note.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AsyncImage(
                    model = note.imageUrl,
                    contentDescription = "Note Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // import androidx.compose.foundation.layout.height
                        .padding(bottom = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                ) {
                    Text("Okay", color = MaterialTheme.colorScheme.onPrimary)
                }

            }
        }
    }
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