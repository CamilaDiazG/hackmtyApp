package com.example.smartintelligence

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.smartintelligence.ui.theme.SmartIntelligenceTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartIntelligenceTheme {
                var showScanner by remember { mutableStateOf(false) }
                var scannedValue by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (!showScanner) {
                            FloatingActionButton(onClick = { showScanner = true }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code")
                            }
                        }
                    }
                ) { innerPadding ->
                    if (showScanner) {
                        QRCodeScannerScreen(
                            modifier = Modifier.padding(innerPadding),
                            onQrCodeScanned = { result ->
                                scannedValue = result
                                showScanner = false
                                Log.d("QRCode", "Scanned value: $result")
                            },
                            onBack = { showScanner = false }
                        )
                    } else {
                        PackingTasksScreen(
                            modifier = Modifier.padding(innerPadding),
                            scannedValue = scannedValue
                        )
                    }
                }
            }
        }
    }
}

data class PackingTask(
    val id: String,
    val flightNumber: String,
    val destination: String,
    val status: String
)

@Composable
fun PackingTasksScreen(modifier: Modifier = Modifier, scannedValue: String?) {
    val tasks = listOf(
        PackingTask("1", "IB6845", "MEX", "Pending"),
        PackingTask("2", "UX0091", "JFK", "In Progress"),
        PackingTask("3", "BA0461", "LHR", "Completed"),
        PackingTask("4", "AF1005", "CDG", "Pending"),
        PackingTask("5", "LH2022", "FRA", "In Progress"),
    )

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "Packing Tasks",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        scannedValue?.let {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Last Scanned: $it",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tasks) { task ->
                PackingTaskItem(task = task, onClick = { })
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PackingTaskItem(task: PackingTask, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val statusColor = when (task.status) {
        "Completed" -> Color(0xFF4CAF50)
        "In Progress" -> Color(0xFFFF9800)
        "Pending" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
    val flightIcon: ImageVector = Icons.Filled.FlightLand

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(statusColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = flightIcon,
                    contentDescription = "Flight Icon",
                    tint = statusColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.flightNumber,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "to ${task.destination}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = task.status.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRCodeScannerScreen(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasScanned by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasCameraPermission = isGranted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                        if (!hasScanned) {
                                            processImageProxy(imageProxy) { result ->
                                                hasScanned = true
                                                onQrCodeScanned(result)
                                            }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalyzer
                                )
                            } catch (e: Exception) {
                                Log.e("QRCodeScanner", "Error binding camera lifecycle", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.size(250.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            3.dp,
                            MaterialTheme.colorScheme.primary
                        )
                    ) {}
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Camera permission is required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}

private fun processImageProxy(
    imageProxy: androidx.camera.core.ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    barcodes.first().rawValue?.let { qrValue ->
                        onSuccess(qrValue)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("QRCodeScanner", "Error scanning", it)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

@Preview(showBackground = true)
@Composable
fun PackingTasksScreenPreview() {
    SmartIntelligenceTheme {
        PackingTasksScreen(modifier = Modifier.fillMaxSize(), scannedValue = "QR12345")
    }
}