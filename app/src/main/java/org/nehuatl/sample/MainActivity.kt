package org.nehuatl.sample

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardOptions

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nehuatl.sample.ui.theme.KotlinLlamaCppTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val viewModel = MainViewModel()

    private fun copyModelFromAssets(context: Context): File {
        val assetManager = context.assets
        val outputFile = File(context.filesDir, "sensor_model.gguf")

        if (!outputFile.exists()) {
            assetManager.open("sensor_model.gguf").use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outputFile
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modelFile = copyModelFromAssets(this)
        enableEdgeToEdge()

        setContent {
            KotlinLlamaCppTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = stringResource(R.string.app_title),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        )
                    }
                ) { innerPadding ->

                    // Launch model loading asynchronously
                    LaunchedEffect(Unit) {
                        Log.v("MainActivity", "file exists: ${modelFile.exists()}")
                        viewModel.loadModel(modelFile.absolutePath)
                        Log.v("MainActivity", "Model loaded successfully")
                    }

                    // Diagnosis form with minimal padding
                    DiagnosisForm(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding) // only system/app bar padding
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DiagnosisForm(
        modifier: Modifier = Modifier,
        viewModel: MainViewModel
    ) {
        val scrollState = rememberScrollState()
        var symptom by remember { mutableStateOf("Fever") }
        var heartRate by remember { mutableStateOf("80") }
        var temperature by remember { mutableStateOf("38") }
        var stressLevel by remember { mutableStateOf("8") }
        var systolic by remember { mutableStateOf("110") }
        var diastolic by remember { mutableStateOf("90") }
        val diagnosis by viewModel.text.collectAsState()
        val isModelLoaded by viewModel.isModelLoaded.collectAsState()
        val isGenerating by viewModel.isGenerating.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            OutlinedTextField(
                value = symptom,
                onValueChange = { symptom = it },
                label = { Text("Symptoms (e.g., Fever, Headache)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Vital info section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🩺 Vital Info",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    val compactFieldModifier = Modifier.defaultMinSize(minHeight = 30.dp)

                    OutlinedTextField(
                        value = heartRate,
                        onValueChange = { heartRate = it },
                        label = { Text("Heart Rate (bpm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().then(compactFieldModifier)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temp (°C)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).then(compactFieldModifier)
                        )
                        OutlinedTextField(
                            value = stressLevel,
                            onValueChange = { stressLevel = it },
                            label = { Text("Stress (1–10)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).then(compactFieldModifier)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = systolic,
                            onValueChange = { systolic = it },
                            label = { Text("BP: Systolic") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).then(compactFieldModifier)
                        )
                        Text("/", modifier = Modifier.padding(horizontal = 2.dp))
                        OutlinedTextField(
                            value = diastolic,
                            onValueChange = { diastolic = it },
                            label = { Text("Diastolic") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).then(compactFieldModifier)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Generate Diagnosis button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val missingField = when {
                            symptom.isBlank() -> "Symptom"
                            heartRate.isBlank() -> "Heart rate"
                            temperature.isBlank() -> "Temperature"
                            stressLevel.isBlank() -> "Stress level"
                            systolic.isBlank() || diastolic.isBlank() -> "Blood pressure"
                            else -> null
                        }
                        if (missingField != null) {
                            snackbarHostState.showSnackbar("Please enter $missingField.")
                            return@launch
                        }
                        if (stressLevel.toIntOrNull() !in 1..10) {
                            snackbarHostState.showSnackbar("Stress level must be 1–10.")
                            return@launch
                        }

                        val inputText = """
                            Symptoms: $symptom
                            Heart Rate: $heartRate bpm
                            Temperature: $temperature °C
                            Stress Level: $stressLevel/10
                            Blood Pressure: $systolic/$diastolic
                            Treatment Plan:
                        """.trimIndent()

                        viewModel.submit(inputText)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isModelLoaded && !isGenerating,
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                when {
                    !isModelLoaded -> Text("Loading model…")
                    isGenerating -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Text("Generating…")
                        }
                    }

                    else -> Text("Generate Diagnosis")
                }
            }

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = diagnosis,
                onValueChange = {},
                label = { Text("Diagnosis Result") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                readOnly = true
            )

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    symptom = ""
                    heartRate = ""
                    temperature = ""
                    stressLevel = ""
                    systolic = ""
                    diastolic = ""
                    viewModel.clearDiagnosis()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
            ) {
                Text("Clear")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DiagnosisFormPreview() {
        KotlinLlamaCppTheme {
            DiagnosisForm(viewModel = MainViewModel())
        }
    }
}
