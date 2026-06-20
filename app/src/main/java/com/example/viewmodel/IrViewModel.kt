package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.IrProtocolDecoder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IrViewModel(
    application: Application,
    private val repository: IrRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private var irManager: ConsumerIrManager? = null

    // Blaster States
    var hasPhysicalBlaster = false
        private set

    var supportedFrequenciesText = ""
        private set

    // Selected Remote ID Flow
    private val _selectedRemoteId = MutableStateFlow<Long?>(null)
    val selectedRemoteId: StateFlow<Long?> = _selectedRemoteId.asStateFlow()

    // All available remotes
    val allRemotes: StateFlow<List<IrRemote>> = repository.allRemotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Remote Details
    val selectedRemote: StateFlow<IrRemote?> = _selectedRemoteId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getRemoteById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Buttons for selected remote
    val currentButtons: StateFlow<List<IrButton>> = _selectedRemoteId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getButtonsForRemote(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Interactive Console Logs / Signal Analysis
    private val _transmissionLogs = MutableStateFlow<List<String>>(listOf("System Ready."))
    val transmissionLogs: StateFlow<List<String>> = _transmissionLogs.asStateFlow()

    // Last sent pattern for drawing simulated waveform analyzer
    private val _lastTransmittedSignal = MutableStateFlow<IrProtocolDecoder.DecodedSignal?>(null)
    val lastTransmittedSignal: StateFlow<IrProtocolDecoder.DecodedSignal?> = _lastTransmittedSignal.asStateFlow()

    init {
        detectIrBlaster()
        prepopulateDefaultRemotesIfNeeded()
    }

    private fun detectIrBlaster() {
        try {
            irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
            val manager = irManager
            if (manager != null && manager.hasIrEmitter()) {
                hasPhysicalBlaster = true
                val ranges = manager.carrierFrequencies
                val sb = StringBuilder()
                if (ranges != null) {
                    for (range in ranges) {
                        sb.append("${range.minFrequency / 1000}kHz - ${range.maxFrequency / 1000}kHz, ")
                    }
                }
                supportedFrequenciesText = if (sb.isNotEmpty()) {
                    sb.substring(0, sb.length - 2)
                } else {
                    "30kHz - 60kHz (Standard Range)"
                }
                addLog("✨ Physical IR Blaster Detected! Supported: $supportedFrequenciesText")
            } else {
                hasPhysicalBlaster = false
                supportedFrequenciesText = "None"
                addLog("⚠️ No Physical IR Blaster detected. App running in 'Simulation / Virtual Core' mode.")
            }
        } catch (e: Exception) {
            Log.e("IrViewModel", "Error detecting IR blaster: ${e.message}", e)
            hasPhysicalBlaster = false
            supportedFrequenciesText = "Error"
            addLog("⚠️ Error detecting blaster: ${e.message}. Simulating instead.")
        }
    }

    private fun prepopulateDefaultRemotesIfNeeded() {
        viewModelScope.launch {
            // Check if remotes table is empty, if so, populate defaults
            allRemotes.first { true } // Wait for first pull
            val existing = repository.allRemotes.first()
            if (existing.isEmpty()) {
                addLog("🛠️ Pre-populating default remote controllers...")

                // 1. Sony TV Remote
                val sonyId = repository.insertRemote(
                    IrRemote(name = "Sony Bravia TV", category = "TV")
                )
                repository.insertButton(IrButton(remoteId = sonyId, label = "Power", protocol = "SONY", hexCode = "0xA90", frequency = 40000, bitLength = 12, iconName = "power", colorHex = "#f44336", orderIndex = 0))
                repository.insertButton(IrButton(remoteId = sonyId, label = "Vol +", protocol = "SONY", hexCode = "0x490", frequency = 40000, bitLength = 12, iconName = "volume_up", colorHex = "#2196F3", orderIndex = 1))
                repository.insertButton(IrButton(remoteId = sonyId, label = "Vol -", protocol = "SONY", hexCode = "0xC90", frequency = 40000, bitLength = 12, iconName = "volume_down", colorHex = "#2196F3", orderIndex = 2))
                repository.insertButton(IrButton(remoteId = sonyId, label = "Mute", protocol = "SONY", hexCode = "0x290", frequency = 40000, bitLength = 12, iconName = "volume_mute", colorHex = "#9C27B0", orderIndex = 3))
                repository.insertButton(IrButton(remoteId = sonyId, label = "Ch +", protocol = "SONY", hexCode = "0x090", frequency = 40000, bitLength = 12, iconName = "arrow_upward", colorHex = "#4CAF50", orderIndex = 4))
                repository.insertButton(IrButton(remoteId = sonyId, label = "Ch -", protocol = "SONY", hexCode = "0x890", frequency = 40000, bitLength = 12, iconName = "arrow_downward", colorHex = "#4CAF50", orderIndex = 5))

                // 2. Samsung TV
                val samsungId = repository.insertRemote(
                    IrRemote(name = "Samsung Smart TV", category = "TV")
                )
                repository.insertButton(IrButton(remoteId = samsungId, label = "Power", protocol = "NEC", hexCode = "0xE0E040BF", frequency = 38000, bitLength = 32, iconName = "power", colorHex = "#f44336", orderIndex = 0))
                repository.insertButton(IrButton(remoteId = samsungId, label = "Source", protocol = "NEC", hexCode = "0xE0E0807F", frequency = 38000, bitLength = 32, iconName = "settings", colorHex = "#FF9800", orderIndex = 1))
                repository.insertButton(IrButton(remoteId = samsungId, label = "Vol +", protocol = "NEC", hexCode = "0xE0E0E01F", frequency = 38000, bitLength = 32, iconName = "volume_up", colorHex = "#2196F3", orderIndex = 2))
                repository.insertButton(IrButton(remoteId = samsungId, label = "Vol -", protocol = "NEC", hexCode = "0xE0E0D02F", frequency = 38000, bitLength = 32, iconName = "volume_down", colorHex = "#2196F3", orderIndex = 3))
                repository.insertButton(IrButton(remoteId = samsungId, label = "Menu", protocol = "NEC", hexCode = "0xE0E058A7", frequency = 38000, bitLength = 32, iconName = "menu", colorHex = "#795548", orderIndex = 4))
                repository.insertButton(IrButton(remoteId = samsungId, label = "Exit", protocol = "NEC", hexCode = "0xE0E0B44B", frequency = 38000, bitLength = 32, iconName = "close", colorHex = "#9E9E9E", orderIndex = 5))

                // Auto select first remote
                _selectedRemoteId.value = sonyId
                addLog("👉 Active remote: Sony Bravia TV selected.")
            } else {
                // Select first existing remote
                _selectedRemoteId.value = existing.first().id
                addLog("📂 Loaded ${existing.size} existing remote configuration(s).")
            }
        }
    }

    fun selectRemote(remoteId: Long) {
        _selectedRemoteId.value = remoteId
        addLog("📺 Switched remote context.")
    }

    fun addLog(msg: String) {
        val current = _transmissionLogs.value.toMutableList()
        current.add(0, msg) // Newest log on top
        if (current.size > 50) current.removeAt(current.lastIndex)
        _transmissionLogs.value = current
    }

    // TRANSMIT SIGNAL
    fun transmitSignal(button: IrButton) {
        viewModelScope.launch {
            addLog("⚡ Transmitting '${button.label}' using ${button.protocol} protocol...")

            val decoded = IrProtocolDecoder.decode(
                protocol = button.protocol,
                hexCode = button.hexCode,
                rawPattern = button.rawPattern,
                frequency = button.frequency,
                bitLength = button.bitLength
            )

            if (decoded.pattern.isEmpty()) {
                addLog("❌ Error: Generated custom pattern timing array is empty.")
                return@launch
            }

            _lastTransmittedSignal.value = decoded

            val freq = decoded.frequency
            val pattern = decoded.pattern

            val totalDurationUs = pattern.sum()
            val totalDurationMs = totalDurationUs / 1000

            addLog("📊 Signal Specs: Freq = ${freq}Hz (${(freq/1000.0)}kHz) | Timing block count = ${pattern.size} | Total Pulse Stream Duration = ${totalDurationMs}ms")

            if (hasPhysicalBlaster) {
                try {
                    irManager?.transmit(freq, pattern)
                    addLog("🎯 Hardware signal fired successfully!")
                } catch (e: Exception) {
                    addLog("💥 Hardware blaster exception: ${e.message}")
                }
            } else {
                addLog("📱 Simulated transmission completed (Virtual Core).")
            }
        }
    }

    // Direct text / scratchpad output
    fun transmitCustomRaw(protocol: String, hexStr: String, rawPatternStr: String, freqHz: Int, bits: Int) {
        viewModelScope.launch {
            addLog("⚡ Firing Direct Input: Protocol: $protocol, Hex: $hexStr, Freq: ${freqHz}Hz")
            val decoded = IrProtocolDecoder.decode(
                protocol = protocol,
                hexCode = hexStr,
                rawPattern = rawPatternStr,
                frequency = freqHz,
                bitLength = bits
            )

            if (decoded.pattern.isEmpty()) {
                addLog("❌ Error: Decoded direct input pattern is empty.")
                return@launch
            }

            _lastTransmittedSignal.value = decoded
            val freq = decoded.frequency
            val pattern = decoded.pattern

            val totalDurationMs = pattern.sum() / 1000
            addLog("📊 Signal Specs: Freq = ${freq}Hz | Block count = ${pattern.size} | Total Duration = ${totalDurationMs}ms")

            if (hasPhysicalBlaster) {
                try {
                    irManager?.transmit(freq, pattern)
                    addLog("🎯 Direct hardware signal fired successfully!")
                } catch (e: Exception) {
                    addLog("💥 Direct hardware blaster exception: ${e.message}")
                }
            } else {
                addLog("📱 Simulated direct transmission completed.")
            }
        }
    }

    // CRUD Ops for UI bindings
    fun addNewRemote(name: String, category: String) {
        viewModelScope.launch {
            val newId = repository.insertRemote(IrRemote(name = name, category = category))
            _selectedRemoteId.value = newId
            addLog("➕ Added and switched to custom remote controller: '$name'")
        }
    }

    fun deleteCurrentRemote() {
        val currentId = _selectedRemoteId.value ?: return
        viewModelScope.launch {
            repository.deleteRemoteWithButtons(currentId)
            addLog("🗑️ Deleted current remote and all associated keys.")
            val rems = repository.allRemotes.first()
            if (rems.isNotEmpty()) {
                _selectedRemoteId.value = rems.first().id
            } else {
                _selectedRemoteId.value = null
            }
        }
    }

    fun addNewButton(
        label: String,
        protocol: String,
        hexCode: String,
        rawPattern: String,
        frequency: Int,
        bitLength: Int,
        iconName: String,
        colorHex: String
    ) {
        val remoteId = _selectedRemoteId.value ?: return
        viewModelScope.launch {
            val count = currentButtons.value.size
            val newBtn = IrButton(
                remoteId = remoteId,
                label = label,
                protocol = protocol,
                hexCode = hexCode,
                rawPattern = rawPattern,
                frequency = frequency,
                bitLength = bitLength,
                iconName = iconName,
                colorHex = colorHex,
                orderIndex = count
            )
            repository.insertButton(newBtn)
            addLog("➕ Created custom key: '$label' [$protocol]")
        }
    }

    fun updateButton(button: IrButton) {
        viewModelScope.launch {
            repository.updateButton(button)
            addLog("✏️ Updated key: '${button.label}'")
        }
    }

    fun deleteButton(button: IrButton) {
        viewModelScope.launch {
            repository.deleteButton(button)
            addLog("🗑️ Removed custom key: '${button.label}'")
        }
    }
}

class IrViewModelFactory(
    private val application: Application,
    private val repository: IrRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IrViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IrViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
