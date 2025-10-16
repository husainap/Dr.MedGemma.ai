package org.nehuatl.llamacpp

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LlamaHelper(val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    private val llama by lazy { LlamaAndroid() }
    private var job: Job?= null
    private var contextId: Int? = null

    private val _resultFlow = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val resultFlow = _resultFlow

    // Load GGUF model
    suspend fun load(path: String, contextLength: Int) = suspendCoroutine { continuation ->
        Log.i("LlamaHelper", "Husain Inside load ")

        job = scope.launch {
            val config =  mapOf(
                "model" to path,
                "n_ctx" to contextLength,
            )
            val map = llama.initContext(config)
            contextId = map?.get("contextId") as? Int ?: throw Exception("Context ID not found")
            continuation.resume(Unit)
        }
    }

    suspend fun setCollector1() = suspendCoroutine { continuation ->
        Log.i("LlamaHelper", "Husain inside setCollector")
        val context = contextId ?: throw Exception("Model was not loaded yet, load it first")
        job = scope.launch {
            continuation.resume(
                llama.setEventCollector(context, this).mapNotNull { (message, token) ->
                    if (message == "token") (token as? String) else null
                }
            )
        }
        Log.i("LlamaHelper", "Husain exit setCollector")
    }
    suspend fun setCollector() =
        llama.setEventCollector(contextId ?: throw Exception("Model was not loaded yet, load it first"), scope)
            .mapNotNull { (message, token) ->
                if (message == "token") token as? String else null
            }

    fun unsetCollector() {
        val context = contextId ?: throw Exception("Model was not loaded yet, load it first")
        llama.unsetEventCollector(context)
    }

    fun predict(prompt: String, partialCompletion: Boolean, maxTokens: Int) {
        val context = contextId ?: throw Exception("Model was not loaded yet, load it first")
        Log.i("LlamaHelper", "calling llama.launchCompletion partialCompletion " + partialCompletion)
        llama.launchCompletion(
            id = context,
            params = mapOf(
                "prompt" to prompt,
                "emit_partial_completion" to partialCompletion,
                "n_predict" to maxTokens, // ✅ limit output tokens
            )
        ).also {
            Log.i("LlamaHelper", "finished launchCompletion $it")
            // 👇 Emit the final model output (if collector is active)
            if( it != null ) {
                val text = it["text"] as String
                if (text != null) {
                    Log.i("LlamaHelper", "About to emit to ModelView")

                   // scope.launch {
                    //    _resultFlow.emit(text)
                   // }
                }
            }
            job?.cancel()
        }
    }

    // Release context and model in memory, call when your viewmodel or activity is destroyed
    fun release() {
        contextId?.let { id ->
            llama.releaseContext(id)
        }
    }

    // Abort model loading or prediction
    fun abort() {
        job?.cancel()
    }
}