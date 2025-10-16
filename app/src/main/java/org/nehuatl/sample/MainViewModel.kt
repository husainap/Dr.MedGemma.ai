package org.nehuatl.sample

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nehuatl.llamacpp.LlamaHelper

class MainViewModel : ViewModel() {

    private val llamaHelper by lazy { LlamaHelper(viewModelScope) }
    val text = MutableStateFlow("")

    //private val _isModelLoaded = MutableStateFlow(false)  // private mutable
    //val isModelLoaded: StateFlow<Boolean> = _isModelLoaded  // public read-only
    val isModelLoaded = MutableStateFlow(false)
    val isGenerating = MutableStateFlow(false) //


        /**
     * Loads the model into memory from the given GGUF file path.
     */
    suspend fun loadModel(path: String) {
        Log.i("MainViewModel", "Loading model from path: $path")
        withContext(Dispatchers.IO) {
            llamaHelper.load(
                path = path,   // GGUF model already in filesystem
                contextLength = 2048
            )
            isModelLoaded.emit(true) // ✅ mark loaded
            Log.i("MainViewModel", "Model successfully loaded")

        }
    }

    fun loadModelAsync(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i("MainViewModel", "Loading model on background thread...")
                isModelLoaded.emit(false) // mark as loading

                // Heavy model loading (blocking) happens here
                loadModel(path)

                Log.i("MainViewModel", "Model loaded successfully")
                isModelLoaded.emit(true)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Model load failed", e)
                isModelLoaded.emit(false)
            }
        }
    }

    /**
     * Starts a model prediction with streaming token updates.
     */
    suspend fun submit(prompt: String) {
        Log.i("MainViewModel", "Submitting prompt: $prompt")

        // Mark as generating
        isGenerating.value = true
        // Collector Flow — runs on a separate coroutine
        val collectorFlow = llamaHelper.setCollector()
            .onStart {
                Log.i("MainViewModel", "Prediction started")
                text.emit("") // clear previous output
            }
            .onCompletion {
                Log.i("MainViewModel", "Prediction ended")
                isGenerating.value = false
                llamaHelper.unsetCollector()
            }

        // Launch collector coroutine
        val collectorJob = viewModelScope.launch(Dispatchers.IO) {
            collectorFlow.collect { chunk ->
                Log.i("MainViewModel", "Prediction chunk: $chunk")
                text.value += chunk
            }
        }

        // Launch prediction coroutine (separate, non-blocking)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.i("MainViewModel", "Husain llamaHelper.predict")
                llamaHelper.predict(
                    prompt = prompt,
                    partialCompletion = true,
                    256
                )
            } catch (e: Exception) {
                Log.e("MainViewModel", "Prediction error", e)
                text.value += "\n⚠️ Error: ${e.message}"
            } finally {
                Log.i("MainViewModel", "Husain lfinally block after predit")
                llamaHelper.unsetCollector()
                collectorJob.cancel()
                collectorJob.join()
                Log.i("MainViewModel", "Husain after join")
            }
        }
    }

    /**
     * Abort model load or prediction in progress.
     */
    fun abort() {
        Log.i("MainViewModel", "Aborting prediction...")
        llamaHelper.abort()
    }

    /**
     * Release all model resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        Log.i("MainViewModel", "Releasing llama model resources")
        llamaHelper.abort()
        llamaHelper.release()
    }

    fun clearDiagnosis() {
        viewModelScope.launch {
            text.emit("")  // clears the StateFlow, resetting the diagnosis display
        }
    }
}
