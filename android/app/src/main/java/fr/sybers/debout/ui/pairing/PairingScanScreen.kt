package fr.sybers.debout.ui.pairing

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.sybers.debout.R
import fr.sybers.debout.domain.PairingParseResult
import fr.sybers.debout.domain.PairingPayload
import java.util.concurrent.Executors

private enum class ScanProblem(@StringRes val message: Int) {
    NotDebout(R.string.scan_not_debout),
    UpdateApp(R.string.scan_update_app),
}

/** [onPaired] receives the raw text of a QR code that [PairingPayload.parse] accepts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScanScreen(onBack: () -> Unit, onPaired: (String) -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    var cameraGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        cameraDenied = !granted
    }
    LaunchedEffect(Unit) {
        if (!cameraGranted) launcher.launch(Manifest.permission.CAMERA)
    }
    LifecycleResumeEffect(Unit) {
        cameraGranted = context.hasCameraPermission()
        onPauseOrDispose {}
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_scan_qr)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.navigate_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        when {
            cameraGranted -> Scanner(onPaired)
            cameraDenied -> CameraDenied(
                modifier = Modifier.padding(padding),
                onAllow = {
                    val canAskAgain = activity != null &&
                        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                    if (canAskAgain) {
                        launcher.launch(Manifest.permission.CAMERA)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun Scanner(onPaired: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    var problem by remember { mutableStateOf<ScanProblem?>(null) }
    var paired by remember { mutableStateOf(false) }

    val onText by rememberUpdatedState { text: String ->
        if (!paired) {
            when (PairingPayload.parse(text)) {
                is PairingParseResult.Valid -> {
                    paired = true
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onPaired(text)
                }
                PairingParseResult.UnsupportedVersion -> problem = ScanProblem.UpdateApp
                PairingParseResult.Invalid -> problem = ScanProblem.NotDebout
            }
        }
    }

    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_ANALYSIS) }
    }
    DisposableEffect(controller, lifecycleOwner) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        controller.setImageAnalysisAnalyzer(analysisExecutor, QrCodeAnalyzer { text -> mainExecutor.execute { onText(text) } })
        controller.bindToLifecycle(lifecycleOwner)
        onDispose {
            controller.unbind()
            controller.clearImageAnalysisAnalyzer()
            analysisExecutor.shutdown()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PreviewView(it).apply { this.controller = controller } },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .size(260.dp)
                .border(3.dp, Color.White, RoundedCornerShape(28.dp)),
        )
        Text(
            stringResource(problem?.message ?: R.string.scan_hint),
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(24.dp)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(16.dp),
        )
    }
}

@Composable
private fun CameraDenied(modifier: Modifier, onAllow: () -> Unit) {
    Surface(modifier.fillMaxSize()) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                stringResource(R.string.scan_camera_rationale),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAllow) { Text(stringResource(R.string.scan_camera_allow)) }
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
