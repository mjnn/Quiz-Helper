package com.aitrainer.practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.aitrainer.practice.ui.AiTrainerApp
import com.aitrainer.practice.ui.AppViewModel
import com.aitrainer.practice.ui.theme.AiTrainerTheme

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.importQuestionBank(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiTrainerTheme {
                AiTrainerApp(
                    vm = vm,
                    onRequestImport = { importLauncher.launch("application/json") },
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
}
