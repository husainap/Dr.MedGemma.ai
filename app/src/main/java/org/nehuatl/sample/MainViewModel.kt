package org.nehuatl.sample

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File

class MainViewModel: ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + viewModelJob)
    private val llamaHelper by lazy { LlamaHelper(scope) }

    val text = MutableStateFlow("")

    // load model into memory
    suspend fun loadModel(path: String) {
        llamaHelper.load(
            path = path, // GGUF model already in filesystem
            contextLength = 2048,
        )
    }

    // model should be loaded before submitting or an exception will be thrown
    suspend fun submit(prompt: String) {
        // collector must be called before predict
        llamaHelper.setCollector()
            .onStart {
                Log.i("MainViewModel", "prediction started")
                // prediction started, prepare your UI
                // the first token will arrive after some seconds of warmup
                text.emit("")
            }
            .onCompletion {
                Log.i("MainViewModel", "prediction ended")
                // onCompletion will be triggered when finished or aborted
                llamaHelper.unsetCollector() // unset collector
            }
            .collect { chunk ->
                Log.i("MainViewModel", "prediction $chunk")
                // collect chunks of text as they arrive
                // you can, for example, emit to a StateFlow to observe it in your UI
                text.value += chunk
            }
        llamaHelper.predict(
            prompt = prompt,
            partialCompletion = true
        )
    }

    // you can abort the model load or prediction in progress
    fun abort() {
        Log.i("MainViewModel", "prediction ended")
        llamaHelper.abort()
    }

    // don't forget to release resources when your viewmodel is destroyed
    override fun onCleared() {
        super.onCleared()
        llamaHelper.abort()
        llamaHelper.release()
    }
}