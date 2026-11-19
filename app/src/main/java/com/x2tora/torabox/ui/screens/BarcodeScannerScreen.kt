package com.x2tora.torabox.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.x2tora.torabox.domain.WaybillDetector
import com.x2tora.torabox.ui.components.MiuixGlassIcon
import com.x2tora.torabox.ui.components.SquirclePillBadge
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Scan
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BarcodeScannerScreen(
    onBarcodeDetected: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedPreview by remember { mutableStateOf<String?>(null) }
    val deliveredOnce = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(torchEnabled, boundCamera) {
        boundCamera?.cameraControl?.enableTorch(torchEnabled)
    }

    Scaffold(
        containerColor = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                CameraBarcodePreview(
                    lifecycleOwner = lifecycleOwner,
                    onCameraBound = { boundCamera = it },
                    onRawBarcode = { raw ->
                        val parsed = WaybillDetector.parseNotificationText(raw).waybillNumber
                            .ifBlank { WaybillDetector.normalizeWaybill(raw) }
                        if (parsed.length >= 6 && parsed.any(Char::isDigit)) {
                            lastScannedPreview = parsed
                            if (deliveredOnce.compareAndSet(false, true)) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onBarcodeDetected(parsed)
                            }
                        }
                    }
                )

                // Darkened viewfinder mask with crisp rounded cutout
                val accent = MiuixTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    drawRect(Color.Black.copy(alpha = 0.56f))
                    val boxWidth = size.width * 0.78f
                    val boxHeight = (boxWidth * 0.52f).coerceAtMost(size.height * 0.38f)
                    val left = (size.width - boxWidth) / 2f
                    val top = (size.height - boxHeight) / 2f - 30.dp.toPx()
                    val radius = 22.dp.toPx()

                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = CornerRadius(radius, radius),
                        blendMode = BlendMode.Clear
                    )
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(left, top),
                        size = Size(boxWidth, boxHeight),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Top controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "뒤로",
                            tint = Color.White
                        )
                    }

                    SquirclePillBadge(
                        text = if (torchEnabled) "플래시 끄기" else "플래시 켜기",
                        backgroundColor = if (torchEnabled) {
                            MiuixTheme.colorScheme.primary
                        } else Color.Black.copy(alpha = 0.55f),
                        contentColor = Color.White,
                        onClick = { torchEnabled = !torchEnabled }
                    )
                }

                // Bottom guidance pill
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        cornerRadius = 18.dp,
                        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "운송장 바코드 · QR 스캔",
                                style = MiuixTheme.textStyles.headline2.copy(fontWeight = FontWeight.Bold),
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = lastScannedPreview?.let { "감지됨: $it" }
                                    ?: "사각형 프레임 안에 택배 상자의 바코드를 맞춰 주세요",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Permission required card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(max = 420.dp)
                            .fillMaxWidth(),
                        cornerRadius = 22.dp,
                        insideMargin = PaddingValues(22.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MiuixGlassIcon(
                                icon = MiuixIcons.Scan,
                                contentDescription = null,
                                accentColor = MiuixTheme.colorScheme.primary,
                                size = 56.dp,
                                iconSize = 28.dp
                            )
                            Text(
                                text = "카메라 권한이 필요합니다",
                                style = MiuixTheme.textStyles.title3.copy(fontWeight = FontWeight.Bold),
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "택배 상자나 영수증의 바코드·QR 코드를 스캔해 송장번호를 바로 입력하려면 카메라 권한을 허용해 주세요.",
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TextButton(
                                    text = "닫기",
                                    onClick = onBack,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    text = "권한 허용",
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBarcodePreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onCameraBound: (Camera) -> Unit,
    onRawBarcode: (String) -> Unit
) {
    val context = LocalContext.current
    val previewView = remember { PreviewView(context) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { scanner.close() }
            runCatching { analyzerExecutor.shutdown() }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                    ?.rawValue
                                    ?.let(onRawBarcode)
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }
                }

            runCatching {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                onCameraBound(camera)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}
