package com.example.notepadforcompanycomposeui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.notepadforcompanycomposeui.ViewModels.NotesViewModel
import com.example.notepadforcompanycomposeui.data.entities.NotesByDateEntity
import java.util.UUID

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.notepadforcompanycomposeui.util.LocationPermissionHandler
import com.example.notepadforcompanycomposeui.util.rememberLocationHandler
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

    // Location states
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var isLocationPermissionGranted by remember { mutableStateOf(false) }
    var isGPSEnabled by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val locationHandler = rememberLocationHandler(context)
    val scrollState = rememberScrollState()

    // Function to save note with current location
    val saveNote = {
        if (companyName.isEmpty() || phoneNumber.isEmpty() || location.isEmpty()) {
            showErrors = true
        } else {
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
            } else {
                viewModel.insertNote(note)
            }
            onNavigateBack()
        }
    }

    // Handle automatic location updates
    LaunchedEffect(Unit) {
        if (noteId == null) {  // Only for new notes
            locationHandler.checkLocationPermission().let { hasPermission ->
                isLocationPermissionGranted = hasPermission
                if (hasPermission) {
                    locationHandler.turnOnGPS(
                        activity = activity,
                        onSuccess = {
                            isGPSEnabled = true
                            // Launch a coroutine to continuously update location
                            CoroutineScope(Dispatchers.Main).launch {
                                while (true) {
                                    locationHandler.getCurrentLocation()?.let { location ->
                                        currentLocation = location
                                    }
                                    delay(30000) // Update every 30 seconds
                                }
                            }
                        },
                        onFailure = {
                            isGPSEnabled = false
                            Toast.makeText(context, "GPS is required for accurate location", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Load existing note data if editing
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
                }
                isUploaded = note.isUploaded
            }
        }
    }

    // Request location permission for new notes
    if (noteId == null && !isLocationPermissionGranted) {
        LocationPermissionHandler(
            onPermissionGranted = {
                isLocationPermissionGranted = true
                // Trigger GPS check after permission is granted
                locationHandler.turnOnGPS(
                    activity = activity,
                    onSuccess = {
                        isGPSEnabled = true
                        CoroutineScope(Dispatchers.Main).launch {
                            locationHandler.getCurrentLocation()?.let { location ->
                                currentLocation = location
                            }
                        }
                    },
                    onFailure = {
                        isGPSEnabled = false
                        Toast.makeText(context, "GPS is required for accurate location", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onPermissionDenied = {
                isLocationPermissionGranted = false
                Toast.makeText(context, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        )
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
            // Show location status
            if (noteId == null && (!isLocationPermissionGranted || !isGPSEnabled)) {
                Text(
                    text = when {
                        !isLocationPermissionGranted -> "Location permission required for accurate location"
                        !isGPSEnabled -> "Please enable GPS for accurate location"
                        else -> ""
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
               /* // Error message if the form is submitted with missing required fields
                if (showErrors && (companyName.isEmpty() || phoneNumber.isEmpty() || location.isEmpty())) {
                    Text(
                        text = "Please fill in all required fields",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }*/

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

                // Save Button at the end of the scrollable content
                Button(
                    onClick = saveNote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(if (noteId != null) "Update" else "Save")
                }
            }
        }
    }
