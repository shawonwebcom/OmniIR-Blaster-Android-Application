package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.IrProtocolDecoder
import com.example.viewmodel.IrViewModel
import com.example.viewmodel.IrViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(dynamicColor = false) {
                val context = LocalContext.current
                val database = remember { IrDatabase.getDatabase(context) }
                val repository = remember { IrRepository(database.irDao()) }
                val viewModel: IrViewModel = viewModel(
                    factory = IrViewModelFactory(context.applicationContext as Application, repository)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1B1F) // Sleek base background #1C1B1F
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: IrViewModel) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // State Collectors
    val remotes by viewModel.allRemotes.collectAsState()
    val selectedRemoteId by viewModel.selectedRemoteId.collectAsState()
    val activeRemote by viewModel.selectedRemote.collectAsState()
    val activeButtons by viewModel.currentButtons.collectAsState()
    val lastSignal by viewModel.lastTransmittedSignal.collectAsState()
    val logs by viewModel.transmissionLogs.collectAsState()

    // Dialog/Form triggers
    var showAddRemoteDialog by remember { mutableStateOf(false) }
    var showAddButtonDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var keyToEdit by remember { mutableStateOf<IrButton?>(null) }

    // Screen navigation state
    var activeTab by remember { mutableStateOf(0) } // 0 = Dashboard, 1 = Direct Input, 2 = Logs & Waveform

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF1C1B1F)) // Flat sleek base bg
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Modern Branding Header matching the Sleek Interface design
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "OmniIR Blaster",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        // Dynamic combined subtitle & creator link
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Precision Controller v2.1 • ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCAC4D0),
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = "Shawon Web",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFD0BCFF),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            uriHandler.openUri("https://shawonweb.com")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Opening browser...", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            )
                        }
                    }

                    // Sleek Interface header icon button instead of standard info circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF49454F), CircleShape)
                            .clickable { showAboutDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .border(2.dp, Color(0xFFD0BCFF), RoundedCornerShape(2.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom IR Detector Banner
                DetectorBanner(
                    hasBlaster = viewModel.hasPhysicalBlaster,
                    supportedFreq = viewModel.supportedFrequenciesText
                )
            }
        },
        bottomBar = {
            // Sleek high-fidelity dark navigation bar matching the design specs
            NavigationBar(
                containerColor = Color(0xFF1C1B1F), // Sleek background #1C1B1F
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD0BCFF), // SleekAccent
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFF49454F), // Selected background
                        unselectedIconColor = Color(0xFFCAC4D0), // SleekSecondaryText
                        unselectedTextColor = Color(0xFFCAC4D0)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Direct Input") },
                    label = { Text("Direct Code") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD0BCFF),
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFF49454F),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Trace Console") },
                    label = { Text("Trace Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFD0BCFF),
                        selectedTextColor = Color(0xFFD0BCFF),
                        indicatorColor = Color(0xFF49454F),
                        unselectedIconColor = Color(0xFFCAC4D0),
                        unselectedTextColor = Color(0xFFCAC4D0)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (activeTab) {
                0 -> DashboardTab(
                    remotes = remotes,
                    activeRemoteId = selectedRemoteId,
                    activeRemote = activeRemote,
                    activeButtons = activeButtons,
                    onSelectRemote = { viewModel.selectRemote(it) },
                    onDeleteRemote = { viewModel.deleteCurrentRemote() },
                    onAddRemoteClick = { showAddRemoteDialog = true },
                    onAddButtonClick = { showAddButtonDialog = true },
                    onButtonTransmit = { viewModel.transmitSignal(it) },
                    onButtonLongClick = { keyToEdit = it }
                )
                1 -> DirectInputTab(
                    onTransmitDirect = { protocol, hex, raw, freq, bits ->
                        viewModel.transmitCustomRaw(protocol, hex, raw, freq, bits)
                    }
                )
                2 -> TraceAndWaveformTab(
                    signal = lastSignal,
                    logs = logs,
                    onClearLog = { viewModel.addLog("Telemetry cleared.") }
                )
            }

            // Always display a subtle, elegant, responsive branding tag at the bottom of standard screens
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Created by Shawon Web (shawonweb.com)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.clickable {
                        try {
                            uriHandler.openUri("https://shawonweb.com")
                        } catch (e: Exception) {
                            Log.e("BrandingClick", "Failed to launch logo URL")
                        }
                    }
                )
            }
        }
    }

    // Modal Forms

    // 1. Add Remote Dialog
    if (showAddRemoteDialog) {
        var remoteName by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("TV") } // TV, AC, Custom

        AlertDialog(
            onDismissRequest = { showAddRemoteDialog = false },
            containerColor = Color(0xFF2B2930), // SleekCard background #2B2930
            title = {
                Text("➕ Add Custom Remote Controller", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = remoteName,
                        onValueChange = { remoteName = it },
                        label = { Text("Remote Name", color = Color.Gray) },
                        placeholder = { Text("e.g. Master living Room AC") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_remote_name_input")
                    )

                    Text("Device Category", color = Color.White, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("TV", "AC", "AUDIO", "CUSTOM").forEach { cat ->
                            Button(
                                onClick = { category = cat },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (category == cat) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                    contentColor = if (category == cat) Color(0xFF381E72) else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (remoteName.isNotBlank()) {
                            viewModel.addNewRemote(remoteName.trim(), category)
                            showAddRemoteDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a remote name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Create Remote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRemoteDialog = false }) {
                    Text("Dismiss", color = Color(0xFFCAC4D0))
                }
            }
        )
    }

    // 2. Add Button Dialog
    if (showAddButtonDialog) {
        AddOrEditButtonDialog(
            button = null,
            onDismiss = { showAddButtonDialog = false },
            onSave = { label, protocol, hex, raw, freq, bits, icon, color ->
                viewModel.addNewButton(label, protocol, hex, raw, freq, bits, icon, color)
                showAddButtonDialog = false
            }
        )
    }

    // 3. Edit / Delete Button Dialog
    if (keyToEdit != null) {
        val btn = keyToEdit!!
        AddOrEditButtonDialog(
            button = btn,
            onDismiss = { keyToEdit = null },
            onSave = { label, protocol, hex, raw, freq, bits, icon, color ->
                val updated = btn.copy(
                    label = label,
                    protocol = protocol,
                    hexCode = hex,
                    rawPattern = raw,
                    frequency = freq,
                    bitLength = bits,
                    iconName = icon,
                    colorHex = color
                )
                viewModel.updateButton(updated)
                keyToEdit = null
            },
            onDelete = {
                viewModel.deleteButton(btn)
                keyToEdit = null
            }
        )
    }

    // 4. About dialog with clicking website link
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = Color(0xFF2B2930), // SleekCard background #2B2930
            title = {
                Text("ℹ️ OmniIR Blaster Console", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This application is designed as an all-encompassing hub for operating devices via built-in Infrared blaster transmitters.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Features:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text("• Hardware IR Emitter Automatic Detection\n• Simulator Sandbox for Non-IR Devices\n• Precise Protocol Compiler (NEC, Sony, RC5)\n• Custom timing pattern sender & Pronto Hex decoder\n• Live Dynamic Waves Oscilloscope Canvas Flow", color = Color.LightGray, fontSize = 13.sp)

                    HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Created & Owned by:",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Shawon Web\nWeb: https://shawonweb.com",
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            try {
                                uriHandler.openUri("https://shawonweb.com")
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Proceed")
                }
            }
        )
    }
}

@Composable
fun DetectorBanner(hasBlaster: Boolean, supportedFreq: String) {
    Card(
        shape = RoundedCornerShape(24.dp), // Styled highly rounded corner matching template
        border = BorderStroke(1.dp, Color(0xFF49454F)), // Sleek border color
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2930) // Sleek Card bg
        ),
        modifier = Modifier.fillMaxWidth().testTag("ir_detector_card")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulse indicator from the Sleek Theme
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            (if (hasBlaster) Color(0xFF38E54D) else Color(0xFF38E54D)).copy(alpha = 0.2f),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (hasBlaster) Color(0xFF38E54D) else Color(0xFF38E54D),
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasBlaster) "IR SENSOR ACTIVE" else "IR SENSOR ACTIVE",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = if (hasBlaster) "Frequency: $supportedFreq" else "Hardware simulated and ready for transmission",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCAC4D0)),
                    fontSize = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF38E54D).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFF38E54D).copy(alpha = 0.3f),
                        RoundedCornerShape(100.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "READY",
                    color = Color(0xFF38E54D),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardTab(
    remotes: List<IrRemote>,
    activeRemoteId: Long?,
    activeRemote: IrRemote?,
    activeButtons: List<IrButton>,
    onSelectRemote: (Long) -> Unit,
    onDeleteRemote: () -> Unit,
    onAddRemoteClick: () -> Unit,
    onAddButtonClick: () -> Unit,
    onButtonTransmit: (IrButton) -> Unit,
    onButtonLongClick: (IrButton) -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(10.dp))

        // Remotes Row selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Remotes:",
                color = Color(0xFFCAC4D0), // SleekSecondaryText
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(remotes) { remote ->
                    val isSelected = remote.id == activeRemoteId
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), // Slate or Purple color
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectRemote(remote.id) }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = remote.name,
                            color = if (isSelected) Color(0xFF381E72) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onAddRemoteClick,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFD0BCFF), CircleShape) // SleekAccent primary circle
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Remote", tint = Color(0xFF381E72))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Remote Controller Panel Canvas
        if (activeRemote == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No controllers created, please click ➕ above", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)), // SleekCard background #2B2930
                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f)), // SleekBorder #49454F / 50%
                shape = RoundedCornerShape(28.dp), // Premium rounded-3xl style
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Title block of remote
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column {
                            Text(
                                text = activeRemote.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = "${activeRemote.category.uppercase()} LAYOUT ACTIVE",
                                color = Color(0xFFCAC4D0), // SleekSecondaryText
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalIconButton(
                                onClick = onAddButtonClick,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF49454F) // Sleek bg secondary element
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Key", tint = Color(0xFFD0BCFF))
                            }

                            FilledTonalIconButton(
                                onClick = onDeleteRemote,
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = Color(0xFF451A20).copy(alpha = 0.6f)
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Remote", tint = Color(0xFFEFB8C8))
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(bottom = 16.dp))

                    if (activeButtons.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Text(
                                    "No Keys on this controller yet.",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onAddButtonClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                                ) {
                                    Text("Add Your First Key ➕")
                                }
                            }
                        }
                    } else {
                        // Grid layout of interactive remote buttons
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(activeButtons) { btn ->
                                val btnColor = parseColorSafely(btn.colorHex)
                                val icon = getIconForName(btn.iconName)
                                val isPower = btn.label.lowercase().contains("power")
                                val actualBg = if (isPower) Color(0xFFD0BCFF) else Color(0xFF49454F) 
                                val actualContentColor = if (isPower) Color(0xFF381E72) else Color.White
                                val actualIconTint = if (isPower) Color(0xFF381E72) else btnColor

                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, if (isPower) Color.Transparent else btnColor.copy(alpha = 0.3f)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = actualBg
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(86.dp)
                                        .combinedClickable(
                                            onClick = { onButtonTransmit(btn) },
                                            onLongClick = { onButtonLongClick(btn) }
                                        )
                                        .testTag("remote_key_${btn.label.lowercase().replace(" ","_")}")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize().padding(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = btn.label,
                                            tint = actualIconTint,
                                            modifier = Modifier.size(26.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = btn.label,
                                            color = actualContentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = btn.protocol,
                                            color = if (isPower) Color(0xFF381E72).copy(alpha = 0.7f) else Color(0xFFCAC4D0),
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "💡 Tip: Tap button to transmit IR. Long-press key to Edit or Delete it.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun DirectInputTab(
    onTransmitDirect: (String, String, String, Int, Int) -> Unit
) {
    var protocol by remember { mutableStateOf("NEC") } // NEC, SONY, RC5, PRONTO, RAW
    var hexCode by remember { mutableStateOf("") }
    var rawPattern by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("38000") }
    var bitLength by remember { mutableStateOf("32") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Direct IR Protocol Signal Scratchpad",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Instantly experiment, compile, and fire signal codes without storing them permanently.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)), // SleekCard background #2B2930
                border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f)), // SleekBorder #49454F / 50%
                shape = RoundedCornerShape(24.dp), // Premium rounded-3xl corners
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Protocol Engine", color = Color.White, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NEC", "SONY", "RC5", "PRONTO", "RAW").forEach { item ->
                            val isSelected = protocol == item
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), // Slate/Purple Sleek palette
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        protocol = item
                                        if (item == "NEC") {
                                            frequency = "38000"
                                            bitLength = "32"
                                        } else if (item == "SONY") {
                                            frequency = "40000"
                                            bitLength = "12"
                                        } else if (item == "RC5") {
                                            frequency = "36000"
                                            bitLength = "14"
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    item,
                                    color = if (isSelected) Color(0xFF381E72) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (protocol == "NEC" || protocol == "SONY" || protocol == "RC5") {
                        // Hex String TextField
                        OutlinedTextField(
                            value = hexCode,
                            onValueChange = { hexCode = it },
                            label = { Text("Code Payload (Hex)", color = Color.Gray) },
                            placeholder = {
                                Text(
                                    text = if (protocol == "NEC") "e.g. 0xE0E040BF" else if (protocol == "SONY") "e.g. 0xA90" else "e.g. 0x1200",
                                    color = Color.DarkGray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("direct_hex_input")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = frequency,
                                onValueChange = { frequency = it },
                                label = { Text("Frequency (Hz)", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = bitLength,
                                onValueChange = { bitLength = it },
                                label = { Text("Bits length", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00E5FF),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (protocol == "PRONTO") {
                        OutlinedTextField(
                            value = rawPattern,
                            onValueChange = { rawPattern = it },
                            label = { Text("Pronto Hex Code blocks", color = Color.Gray) },
                            placeholder = { Text("e.g. 0000 006d 0022 0002 0157 ...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("direct_pronto_input")
                        )
                        Text(
                            "Pronto Hex automatically configures carrier frequency and signal timings.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    } else { // RAW timings
                        OutlinedTextField(
                            value = rawPattern,
                            onValueChange = { rawPattern = it },
                            label = { Text("Raw Pattern Delay timers (comma-separated μs)", color = Color.Gray) },
                            placeholder = { Text("e.g. 9000, 4500, 560, 560, 560, 1690") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("direct_raw_input")
                        )

                        OutlinedTextField(
                            value = frequency,
                            onValueChange = { frequency = it },
                            label = { Text("Target Frequency (Hz)", color = Color.Gray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00E5FF),
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val freq = frequency.toIntOrNull() ?: 38000
                            val bits = bitLength.toIntOrNull() ?: 32
                            onTransmitDirect(
                                protocol,
                                hexCode.trim(),
                                rawPattern.trim(),
                                freq,
                                bits
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF), // SleekAccent primary
                            contentColor = Color(0xFF381E72) // SleekAccentDark
                        ),
                        shape = RoundedCornerShape(24.dp), // Styled highly rounded
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("direct_transmit_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Transmit")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TRANSMIT COMPILATION DIRECT", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TraceAndWaveformTab(
    signal: IrProtocolDecoder.DecodedSignal?,
    logs: List<String>,
    onClearLog: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Waveform Visualizer on Canvas
        IrWaveformVisualizer(signal = signal)

        // Telemetry Logs Console
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)), // SleekCard background #2B2930
            border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f)), // SleekBorder #49454F / 50%
            shape = RoundedCornerShape(24.dp), // Premium rounded-3xl corners
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "📝 Infrared Telemetry Console Traces",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = onClearLog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear", color = Color(0xFFEFB8C8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color(0xFF49454F).copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF1C1B1F), shape = RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            color = if (log.startsWith("❌") || log.contains("Error")) Color(0xFFEFB8C8)
                            else if (log.contains("✨") || log.contains("Detected")) Color(0xFFD0BCFF)
                            else if (log.contains("📊")) Color(0xFF38E54D)
                            else Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IrWaveformVisualizer(signal: IrProtocolDecoder.DecodedSignal?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.testTag("oscilloscope_visualizer"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)), // SleekCard background #2B2930
        border = BorderStroke(1.dp, Color(0xFF49454F).copy(alpha = 0.5f)), // SleekBorder #49454F / 50%
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                Text(
                    text = "📡 Real-time Signal Oscilloscope",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFD0BCFF), // SleekAccent
                    fontWeight = FontWeight.Bold
                )
                if (signal != null) {
                    Text(
                        text = "${signal.frequency / 1000.0} kHz | ${signal.pattern.size} pulses",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCAC4D0) // SleekSecondaryText
                    )
                }
            }

            if (signal == null || signal.pattern.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(Color(0xFF1C1B1F), shape = RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Trigger any IR transmission key to view timing pulses",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                val pattern = signal.pattern
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(Color(0xFF1C1B1F), shape = RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Calculate total microsecond duration
                    val totalDuration = pattern.sum().toFloat()
                    if (totalDuration <= 0f) return@Canvas

                    val path = Path()
                    path.moveTo(0f, height * 0.82f) // Base line (low / gap space)

                    var currentX = 0f
                    var isPulse = true // Transmissions alternate starting with a pulse (high)

                    for (duration in pattern) {
                        val segmentWidth = (duration / totalDuration) * width
                        val nextX = currentX + segmentWidth
                        val y = if (isPulse) height * 0.18f else height * 0.82f

                        // Draw vertical rising/falling edge
                        path.lineTo(currentX, y)
                        // Draw flat level
                        path.lineTo(nextX, y)

                        currentX = nextX
                        isPulse = !isPulse
                    }

                    // Return to baseline
                    path.lineTo(width, height * 0.82f)

                    // Draw grid columns
                    val gridCols = 10
                    for (i in 1 until gridCols) {
                        val gridX = (width / gridCols) * i
                        drawLine(
                            color = Color(0xFF131D2F),
                            start = Offset(gridX, 0f),
                            end = Offset(gridX, height),
                            strokeWidth = 1f
                        )
                    }

                    // Draw signal path
                    drawPath(
                        path = path,
                        color = Color(0xFF00FF88), // High-frequency green pulse trace
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditButtonDialog(
    button: IrButton?, // Null if adding new
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int, Int, String, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var label by remember { mutableStateOf(button?.label ?: "") }
    var protocol by remember { mutableStateOf(button?.protocol ?: "NEC") }
    var hexCode by remember { mutableStateOf(button?.hexCode ?: "") }
    var rawPattern by remember { mutableStateOf(button?.rawPattern ?: "") }
    var frequencyText by remember { mutableStateOf(button?.frequency?.toString() ?: "38000") }
    var bitLengthText by remember { mutableStateOf(button?.bitLength?.toString() ?: "32") }
    var iconName by remember { mutableStateOf(button?.iconName ?: "play") }
    var colorHex by remember { mutableStateOf(button?.colorHex ?: "#2196F3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2930), // SleekCard background #2B2930
        title = {
            Text(
                text = if (button == null) "➕ Define New Control Key" else "✏️ Edit Control Key",
                color = Color(0xFFD0BCFF), // SleekAccent
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Key Label", color = Color.Gray) },
                        placeholder = { Text("e.g. Mute, Input, Temp Up") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD0BCFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_key_label_input")
                    )

                    Text("Signal Protocol Type", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("NEC", "SONY", "RC5", "PRONTO", "RAW").forEach { p ->
                            Button(
                                onClick = {
                                    protocol = p
                                    if (p == "NEC") {
                                        frequencyText = "38040"
                                        bitLengthText = "32"
                                    } else if (p == "SONY") {
                                        frequencyText = "40000"
                                        bitLengthText = "12"
                                    } else if (p == "RC5") {
                                        frequencyText = "36000"
                                        bitLengthText = "14"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (protocol == p) Color(0xFFD0BCFF) else Color(0xFF49454F),
                                    contentColor = if (protocol == p) Color(0xFF381E72) else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(p, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (protocol == "NEC" || protocol == "SONY" || protocol == "RC5") {
                        OutlinedTextField(
                            value = hexCode,
                            onValueChange = { hexCode = it },
                            label = { Text("Command Code (Hex)", color = Color.Gray) },
                            placeholder = { Text("e.g. 0x20DF10EF") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = frequencyText,
                                onValueChange = { frequencyText = it },
                                label = { Text("Carrier Freq (Hz)", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    focusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = bitLengthText,
                                onValueChange = { bitLengthText = it },
                                label = { Text("Bits length", color = Color.Gray) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    focusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (protocol == "PRONTO") {
                        OutlinedTextField(
                            value = hexCode,
                            onValueChange = { hexCode = it },
                            label = { Text("Pronto Hex payload String", color = Color.Gray) },
                            placeholder = { Text("e.g. 0000 006d 0022 0002 ...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else { // RAW
                        OutlinedTextField(
                            value = rawPattern,
                            onValueChange = { rawPattern = it },
                            label = { Text("Raw pattern intervals (comma μs)", color = Color.Gray) },
                            placeholder = { Text("e.g. 9000, 4500, 560, 560") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD0BCFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = frequencyText,
                            onValueChange = { frequencyText = it },
                            label = { Text("Freq (Hz)", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD0BCFF)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Icon selection
                    Text("Select Key Visual Shape Icon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("power", "source", "play", "pause", "volume_up", "volume_down", "volume_mute", "arrow_upward", "arrow_downward", "settings", "menu").forEach { iconKey ->
                            val isSelected = iconName == iconKey
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F), // Slate/Purple colors
                                        shape = CircleShape
                                    )
                                    .clickable { iconName = iconKey }
                            ) {
                                Icon(
                                    imageVector = getIconForName(iconKey),
                                    contentDescription = iconKey,
                                    tint = if (isSelected) Color(0xFF381E72) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Color selection
                    Text("Select Control Accent Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("#F44336", "#2196F3", "#4CAF50", "#9C27B0", "#FF9800", "#E91E63").forEach { hex ->
                            val isSelected = colorHex.uppercase() == hex.uppercase()
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(parseColorSafely(hex), CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = hex }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (label.isBlank()) {
                        return@Button
                    }
                    val freq = frequencyText.toIntOrNull() ?: 38040
                    val bits = bitLengthText.toIntOrNull() ?: 32
                    onSave(
                        label.trim(),
                        protocol,
                        hexCode.trim(),
                        rawPattern.trim(),
                        freq,
                        bits,
                        iconName,
                        colorHex
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF), contentColor = Color(0xFF381E72)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Save Key")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = Color(0xFFEFB8C8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFFCAC4D0))
                }
            }
        }
    )
}

// Helpers
fun parseColorSafely(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF2196F3) // Fallback standard Blue
    }
}

fun getIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "power" -> Icons.Default.PowerSettingsNew
        "source", "input" -> Icons.Default.Input
        "play" -> Icons.Default.PlayArrow
        "pause" -> Icons.Default.Refresh
        "volume_up", "arrow_upward" -> Icons.Default.KeyboardArrowUp
        "volume_down", "arrow_downward" -> Icons.Default.KeyboardArrowDown
        "arrow_left" -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        "arrow_right" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        "volume_mute", "close" -> Icons.Default.Close
        "menu" -> Icons.Default.Menu
        else -> Icons.Default.Settings
    }
}
