# Kotlin-LlamaCpp

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

Run GGUF models on your android app with ease!

This is a Android binding for [llama.cpp](https://github.com/ggerganov/llama.cpp) written in Kotlin, designed for native Android applications. This project is inspired (forked) by [cui-llama.rn](https://github.com/Vali-98/cui-llama.rn) and [llama.cpp](https://github.com/ggerganov/llama.cpp): Inference of [LLaMA](https://arxiv.org/abs/2302.13971) model in pure C/C++but specifically tailored for Android development in Kotlin.

This is a very early alpha version and API may change in the future.

[![ko-fi](https://www.ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/P5P6149YRQ)

## Features

- Helper class to handle initialization and context handling
- Native Kotlin bindings for llama.cpp
- Support for stopping prompt processing between batches
- Vocabulary-only mode for tokenizer functionality
- Synchronous tokenizer functions
- Context Shift support (from [kobold.cpp](https://github.com/LostRuins/koboldcpp))
- XTC sampling implementation
- Progress callback support
- CPU feature detection (i8mm and dotprod flags)
- Seamless integration with Android development workflow


## Installation

Add the following to your project's `build.gradle`:

```gradle
dependencies {
    implementation 'io.github.ljcamargo:llamacpp-kotlin:0.1.0'
}
```

## Model Requirements

You'll need a GGUF model file to use this library. You can:

- Download pre-converted GGUF models from [HuggingFace](https://huggingface.co/search/full-text?q=GGUF&type=model)
- Convert your own models following the [llama.cpp quantization guide](https://github.com/ggerganov/llama.cpp#prepare-and-quantize)

## Usage

Check this example ViewModel using LlamaHelper class for basic usage

```kotlin
class MainViewModel: ViewModel() {

    private val viewModelJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + viewModelJob)
    private val llamaHelper by lazy { LlamaHelper(scope) }
    
    val text = MutableStateFlow("")

    // load model into memory
    suspend fun loadModel() {
        llamaHelper.load(
            path = "/sdcard/Download/llama.ggmlv3.q4_0.bin",
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
```

You can also use LlamaContext.kt directly to handle several contexts or other complex features

## Performance Considerations

- The library currently supports arm64-v8a and x86_64 platforms
- 64-bit platforms are recommended for better memory allocation
- CPU feature detection helps optimize performance based on device capabilities
- Batch processing can be interrupted, which is crucial for mobile devices with limited processing power

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT

## Acknowledgments

This project builds upon the work of several excellent projects:
- [llama.cpp](https://github.com/ggerganov/llama.cpp) by Georgi Gerganov
- [cui-llama.rn](https://github.com/Vali-98/cui-llama.rn)
- [llama.rn](https://github.com/mybigday/llama.rn)
