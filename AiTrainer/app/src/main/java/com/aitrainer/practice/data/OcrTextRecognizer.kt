package com.aitrainer.practice.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrTextRecognizer(context: Context) {

    private val appContext = context.applicationContext

    suspend fun recognize(uri: Uri): Result<String> = runCatching {
        val image = InputImage.fromFilePath(appContext, uri)
        val recognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build(),
        )
        try {
            recognizer.process(image).await().text.trim()
        } finally {
            recognizer.close()
        }
    }
}
