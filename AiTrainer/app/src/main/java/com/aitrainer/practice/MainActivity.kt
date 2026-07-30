package com.aitrainer.practice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.aitrainer.practice.ui.AiTrainerApp
import com.aitrainer.practice.ui.AppViewModel
import com.aitrainer.practice.ui.theme.AiTrainerTheme
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private var cameraPhotoUri: Uri? = null

    private val jsonImportLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importQuestionBank(uri)
    }

    private val ocrGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.startOcrFromUri(uri)
    }

    private val ocrCropPickLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) launchCrop(uri)
    }

    private val ocrMultiGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris ->
        if (uris.isNotEmpty()) vm.startOcrFromUris(uris)
    }

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let(vm::startOcrFromUri)
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraPhotoUri?.let(vm::startOcrFromUri)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiTrainerTheme {
                AiTrainerApp(
                    vm = vm,
                    onRequestImport = { jsonImportLauncher.launch("application/json") },
                    onRequestOcrGallery = { ocrGalleryLauncher.launch("image/*") },
                    onRequestOcrGalleryBatch = { launchMultiGalleryPicker() },
                    onRequestOcrGalleryCrop = { ocrCropPickLauncher.launch("image/*") },
                    onRequestOcrCamera = ::requestOcrCamera,
                    onRequestOcrAddImages = { launchMultiGalleryPicker() },
                    onShareOcrExport = ::shareOcrExportJson,
                )
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            if (!vm.navigateBack()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun launchMultiGalleryPicker() {
        ocrMultiGalleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    private fun launchCrop(sourceUri: Uri) {
        cropImageLauncher.launch(
            CropImageContractOptions(
                uri = sourceUri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    fixAspectRatio = false,
                    allowFlipping = true,
                    allowRotation = true,
                ),
            ),
        )
    }

    private fun shareOcrExportJson(json: String) {
        val file = File(cacheDir, "ocr_batch_export_${System.currentTimeMillis()}.json")
        file.writeText(json)
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "OCR 批次导出")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "导出 OCR JSON"))
    }

    private fun requestOcrCamera() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                launchCameraCapture()
            }
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCameraCapture() {
        val photoFile = File(cacheDir, "ocr_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile,
        )
        cameraPhotoUri = uri
        takePictureLauncher.launch(uri)
    }
}
