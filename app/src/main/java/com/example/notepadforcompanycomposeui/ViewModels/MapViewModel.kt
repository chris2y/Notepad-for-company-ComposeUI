package com.example.notepadforcompanycomposeui.ViewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepadforcompanycomposeui.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import javax.inject.Inject
// MapViewModel.kt
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _userLocation = MutableStateFlow<GeoPoint?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError = _locationError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var locationRetryCount = 0
    private val maxRetries = 3

    fun getLastLocation() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastLocation = locationRepository.getLastLocation()
                _userLocation.value = GeoPoint(lastLocation.latitude, lastLocation.longitude)
                _locationError.value = null
                locationRetryCount = 0
            } catch (e: Exception) {
                handleLocationError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLocationFetch() {
        if (locationRetryCount < maxRetries) {
            locationRetryCount++
            getLastLocation()
        } else {
            _locationError.value = "Maximum retry attempts reached. Please check your location settings and try again later."
        }
    }

    private fun handleLocationError(error: Exception) {
        val errorMessage = when {
            error.message?.contains("permission") == true ->
                "Location permission denied. Please grant location permission in settings."
            error.message?.contains("disabled") == true ->
                "Location services are disabled. Please enable location services."
            else -> "Unable to get location. Please check your settings and internet connection."
        }
        _locationError.value = errorMessage
    }

    fun clearLocationError() {
        _locationError.value = null
    }
}