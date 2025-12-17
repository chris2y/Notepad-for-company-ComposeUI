package com.example.notepadforcompanycomposeui.screens


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.notepadforcompanycomposeui.ViewModels.NotesViewModel
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import com.example.notepadforcompanycomposeui.util.rememberLocationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
// Add these imports
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import com.example.notepadforcompanycomposeui.R // Import your R file for the icon


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    dateId: Long,
    noteId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    var noteText by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var additionalInfo by remember { mutableStateOf("") }
    var followUp by remember { mutableStateOf("") }
    var interestRate by remember { mutableStateOf("") }
    var isUploaded by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }

    val uploadingNotes by viewModel.uploadingNotes.collectAsState()


    // Location states
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isLocationPermissionGranted by remember { mutableStateOf(false) }
    var isGPSEnabled by remember { mutableStateOf(false) }
    var showLocationRetryButton by remember { mutableStateOf(false) }
    var showPermissionRetryButton by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val locationHandler = rememberLocationHandler(context)
    val scrollState = rememberScrollState()

    // Location update job reference
    var locationUpdateJob by remember { mutableStateOf<Job?>(null) }

    // NEW IMAGE STATES
    var currentImagePath by remember { mutableStateOf<String?>(null) } // Path from DB
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) } // Temporarily selected URI

    var locationAccuracy by remember { mutableStateOf<Float?>(null) }



    // PHOTO PICKER LAUNCHER
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )

    val startLocationUpdates = remember {
        {
            locationUpdateJob?.cancel()
            locationUpdateJob = CoroutineScope(Dispatchers.Main).launch {
                locationHandler.getLocationUpdates() // Use the new FLOW
                    .collect { loc ->
                        currentLocation = loc
                        locationAccuracy = loc.accuracy // Get accuracy in meters
                        showLocationRetryButton = false

                        /*// Update the text field automatically if it's empty or looks like coordinates
                        if (location.isEmpty() || location.contains("Lat:")) {
                            location = "Lat: ${loc.latitude}, Lon: ${loc.longitude}"
                        }*/
                    }
            }
        }
    }
    // Function to handle GPS enable request
    val handleGPSEnable = {
        locationHandler.turnOnGPS(
            activity = activity,
            onSuccess = {
                isGPSEnabled = true
                showLocationRetryButton = false
                startLocationUpdates()
            },
            onFailure = {
                isGPSEnabled = false
                showLocationRetryButton = true
                Toast.makeText(context, "GPS is required for accurate location", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationPermissionGranted = isGranted
        if (isGranted) {
            showPermissionRetryButton = false
            handleGPSEnable()
        } else {
            showPermissionRetryButton = true
            Toast.makeText(context, "Allow location permission", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to request location permission
    val requestLocationPermission = {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Initial setup for new notes
    LaunchedEffect(Unit) {
        if (noteId == null) {
            if (locationHandler.checkLocationPermission()) {
                isLocationPermissionGranted = true
                handleGPSEnable()
            } else {
                requestLocationPermission()
            }
        }
    }

    // Load existing note data
    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.getNoteById(noteId)?.let { note ->
                noteText = note.noteText
                phoneNumber = note.phoneNumber
                companyName = note.companyName
                email = note.email
                location = note.location
                additionalInfo = note.additionalInfo
                followUp = note.followUp
                interestRate = note.interestRate
                currentLocation = Location("").apply {
                    latitude = note.latitude
                    longitude = note.longitude
                   // Toast.makeText(context, "${note.longitude + note.latitude}", Toast.LENGTH_SHORT).show()
                }

                isUploaded = note.isUploaded

                currentImagePath = note.localImagePath
            }
        }
    }

    // Cleanup location updates when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            locationUpdateJob?.cancel()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Note") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentLocation != null) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location acquired",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Location status and retry buttons
            if (noteId == null) {
                when {
                    !isLocationPermissionGranted && showPermissionRetryButton -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Location permission required",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = requestLocationPermission) {
                                Text("Grant Permission")
                            }
                        }
                    }
                    isLocationPermissionGranted && !isGPSEnabled && !showLocationRetryButton -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GPS is disabled",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = handleGPSEnable) {
                                Text("Enable GPS")
                            }
                        }
                    }
                    isLocationPermissionGranted && isGPSEnabled && showLocationRetryButton -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Unable to get location",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = startLocationUpdates) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            // Error message for required fields
            if (showErrors && (companyName.isEmpty() || location.isEmpty())) {
                Text(
                    text = "Please fill in all required fields",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            OutlinedTextField(
                value = companyName,
                onValueChange = { companyName = it },
                label = { Text("Company Name*") },
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && companyName.isEmpty()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location*") },
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && location.isEmpty(),

            )
            // Show Accuracy Text
            if (currentLocation != null && noteId == null) {
                Text(
                    text = "GPS Accuracy: ±${locationAccuracy?.toInt() ?: "?"} meters",
                    style = MaterialTheme.typography.bodySmall,
                    color = if ((locationAccuracy ?: 100f) < 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }



            // --- 1. IMAGE PREVIEW SECTION ---
            // Logic: Show selected URI if available (new pic), otherwise show saved path (edit mode)
            val imageModel = if (selectedImageUri != null) {
                selectedImageUri // User just picked a new photo
            } else if (currentImagePath != null) {
                File(currentImagePath!!) // Loading from storage
            } else {
                null
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Note Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // import androidx.compose.foundation.layout.height
                        .padding(bottom = 8.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                // Optional: Remove Image Button
                TextButton(onClick = {
                    selectedImageUri = null
                    currentImagePath = null
                }) {
                    Text("Remove Image", color = MaterialTheme.colorScheme.error)
                }
            }



            // --- 2. ADD IMAGE BUTTON ---
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null) // Using CloudUpload as generic icon
                Spacer(Modifier.width(8.dp))
                Text("Add Image")
            }

            val latToShow = currentLocation?.latitude
            val lonToShow = currentLocation?.longitude

            // Show map if we have valid coordinates (from GPS or DB)
            if (latToShow != null && lonToShow != null && latToShow != 0.0) {
                Text(
                    text = "Location Preview",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )

                MiniMapPreview(
                    latitude = latToShow,
                    longitude = lonToShow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // Small height for preview
                        .padding(bottom = 8.dp)
                )
            } else {
                // Optional: Placeholder if no location yet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Fetching Location...", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
            )


            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = followUp,
                onValueChange = { followUp = it },
                label = { Text("Follow-up") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = interestRate,
                onValueChange = { interestRate = it },
                label = { Text("Interest Rate") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = additionalInfo,
                onValueChange = { additionalInfo = it },
                label = { Text("Additional Info") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Inside AddNoteScreen, modify the Button's onClick:


            Button(
                onClick = {
                    if (companyName.isEmpty() || location.isEmpty()) {
                        showErrors = true
                    }
                    else {
                        // 1. DETERMINE FINAL IMAGE PATH
                        var finalImagePath = currentImagePath

                        // If user selected a NEW image, save it to internal storage now
                        if (selectedImageUri != null) {
                            finalImagePath = viewModel.saveImageToInternalStorage(context, selectedImageUri!!)
                        }
                        val note = NotesByDateEntity(
                            noteId = noteId ?: System.currentTimeMillis(),
                            dateId = dateId,
                            noteText = noteText,
                            phoneNumber = phoneNumber,
                            companyName = companyName,
                            email = email,
                            location = location,
                            additionalInfo = additionalInfo,
                            followUp = followUp,
                            interestRate = interestRate,
                            latitude = currentLocation?.latitude ?: 0.0,
                            longitude = currentLocation?.longitude ?: 0.0,
                            isUploaded = false,
                            localImagePath = finalImagePath
                        )

                        if (noteId != null) {
                            viewModel.updateNote(note)
                            onNavigateBack()
                        } else {
                            if(!isGPSEnabled){
                                Toast.makeText(context,"Location not found",Toast.LENGTH_SHORT).show()
                            }
                            else{
                                viewModel.insertNote(note)
                                onNavigateBack()
                            }

                        }

                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(if (noteId != null) "Update" else "Save")
            }
        }
    }
}


@SuppressLint("RememberReturnType")
@Composable
fun MiniMapPreview(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Initialize OSM configuration
    remember {
        Configuration.getInstance().load(
            context,
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
    }

    val geoPoint = remember(latitude, longitude) { GeoPoint(latitude, longitude) }

    AndroidView(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true) // Allow user to zoom in/out if they want
                controller.setZoom(17.0)
                // Disable scrolling to prevent it from eating scroll events of the column
                // setBuiltInZoomControls(false)
            }
        },
        update = { mapView ->
            mapView.controller.setCenter(geoPoint)

            mapView.overlays.clear()
            val marker = Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Selected Location"
                // Use your custom icon or a default one
                icon = AppCompatResources.getDrawable(context, R.drawable.my_location)
            }
            mapView.overlays.add(marker)
            mapView.invalidate()
        }
    )
}