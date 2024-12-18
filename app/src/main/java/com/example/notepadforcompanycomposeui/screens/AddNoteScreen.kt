package com.example.notepadforcompanycomposeui.screens


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.example.notepadforcompanycomposeui.ViewModels.NotesViewModel
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import com.example.notepadforcompanycomposeui.util.rememberLocationHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
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

    // Function to start location updates
    val startLocationUpdates = {
        locationUpdateJob?.cancel()
        locationUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                locationHandler.getCurrentLocation()?.let { location ->
                    currentLocation = location
                    showLocationRetryButton = false
                } ?: run {
                    showLocationRetryButton = true
                }
                delay(5000) // Update every 3 seconds
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
                    Toast.makeText(context, "${note.longitude + note.latitude}", Toast.LENGTH_SHORT).show()
                }

                isUploaded = note.isUploaded
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
                .fillMaxSize()  // Ensure the Column occupies the whole screen
                .background(MaterialTheme.colorScheme.background)
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
            if (showErrors && (companyName.isEmpty() || phoneNumber.isEmpty() || location.isEmpty())) {
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
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number*") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                isError = showErrors && phoneNumber.isEmpty()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location*") },
                modifier = Modifier.fillMaxWidth(),
                isError = showErrors && location.isEmpty()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
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
                    if (companyName.isEmpty() || phoneNumber.isEmpty() || location.isEmpty()) {
                        showErrors = true
                    }
                    else {
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
                            isUploaded = isUploaded
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