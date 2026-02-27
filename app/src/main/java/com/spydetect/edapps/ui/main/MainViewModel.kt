package com.spydetect.edapps.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spydetect.edapps.data.model.SpyEvent
import com.spydetect.edapps.data.repository.PreferenceRepository
import com.spydetect.edapps.data.repository.ScannerStatusRepository
import com.spydetect.edapps.data.repository.SpyRepository
import com.spydetect.edapps.data.repository.TrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed interface MainUiState {
  data object Idle : MainUiState

  data object Scanning : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
  private val spyRepository: SpyRepository,
  private val scanningStatusRepository: ScannerStatusRepository,
  private val trackerRepository: TrackerRepository,
  private val preferencesManager: PreferenceRepository
) : ViewModel() {

  private val _sessionEvents = MutableStateFlow<List<SpyEvent>>(emptyList())

  init {
    viewModelScope.launch {
      scanningStatusRepository.isScanning.collect { scanning ->
        if (scanning) {
          if (!preferencesManager.loggingEnabled) {
            _sessionEvents.value = emptyList()
          }
        }
        _uiState.update { if (scanning) MainUiState.Scanning else MainUiState.Idle }
      }
    }
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  val allEvents: StateFlow<List<SpyEvent>> =
    preferencesManager.loggingEnabledFlow
      .flatMapLatest { enabled -> if (enabled) spyRepository.allDetections else _sessionEvents }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STATE_FLOW_STOP_TIMEOUT), emptyList())

  private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Idle)
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  private val _isServiceRunning = MutableStateFlow(false)
  val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

  fun updateServiceStatus(running: Boolean) {
    _isServiceRunning.value = running
  }

  fun addSpyEvent(event: SpyEvent) {
    viewModelScope.launch {
      if (preferencesManager.loggingEnabled) {
        spyRepository.insertDetection(event)
      } else {
        _sessionEvents.update { current ->
          val existingIndex =
            current.indexOfFirst {
              it.deviceAddress == event.deviceAddress && it.companyName == event.companyName
            }
          if (existingIndex != -1) {
            current.toMutableList().apply {
              this[existingIndex] = event.copy(hitCount = current[existingIndex].hitCount + 1)
            }
          } else {
            current + event
          }
        }
      }
    }
  }

  fun clearDetections() {
    viewModelScope.launch {
      spyRepository.clearAll()
      _sessionEvents.value = emptyList()
    }
  }

  val availableTrackerTypes: StateFlow<List<String>> =
    trackerRepository.availableTypes.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(STATE_FLOW_STOP_TIMEOUT),
      emptyList()
    )

  fun setTrackerTypeEnabled(type: String, enabled: Boolean) {
    viewModelScope.launch { trackerRepository.setTypeEnabled(type, enabled) }
  }

  companion object {
    private const val STATE_FLOW_STOP_TIMEOUT = 5000L
  }
}
