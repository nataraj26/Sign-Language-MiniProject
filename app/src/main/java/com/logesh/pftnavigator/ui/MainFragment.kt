package com.logesh.pftnavigator.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController // <--- NEW IMPORT
import com.bumptech.glide.Glide
import com.logesh.pftnavigator.R
import com.logesh.pftnavigator.databinding.FragmentMainBinding
import kotlinx.coroutines.*
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import java.io.*
import java.nio.ByteBuffer
import java.util.*

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // Vosk Variables
    private var model: Model? = null
    private var isVoskLoading = false
    private val handler = Handler(Looper.getMainLooper())

    // Animation Variables
    private var animationString = ""
    private var charIndex = 0


    // --------------------- 1. GOOGLE REAL-TIME MIC ---------------------
    private val googleMicLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK && r.data != null) {
                val result = r.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!result.isNullOrEmpty()) {
                    binding.editTextSentence.setText(result[0])
                    translateSentence()
                }
            }
        }

    // --------------------- 2. AUDIO FILE PICKER ---------------------
    private val audioPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == Activity.RESULT_OK && r.data != null) {
                val uri = r.data!!.data!!
                Toast.makeText(requireContext(), "File selected. Processing...", Toast.LENGTH_SHORT).show()

                lifecycleScope.launch(Dispatchers.IO) {
                    // Ensure model is loaded before processing
                    if (model == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Initializing AI...", Toast.LENGTH_SHORT).show()
                        }
                        loadModelSynchronously()
                    }

                    if (model != null) {
                        processPickedAudio(uri)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to load AI Model", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupButtons()
        // Attempt background load, but don't block UI
        lifecycleScope.launch(Dispatchers.IO) { loadModelSynchronously() }
    }

    private fun setupButtons() {
        // --- BACK BUTTON (Navigate to Language Selection) ---
        binding.backButton.setOnClickListener {
            try {
                // This works if you have defined an ID "languageSelectionFragment" in your nav_graph.xml
                findNavController().navigate(R.id.languageSelectionFragment)
            } catch (e: Exception) {
                // If the ID isn't found, or navigation isn't set up, just go "Back" normally
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.buttonMic.setOnClickListener { startBuiltInMic() }

        binding.importAudioButton.setOnClickListener {
            if (isVoskLoading) {
                Toast.makeText(requireContext(), "AI is loading, please wait...", Toast.LENGTH_SHORT).show()
            } else {
                openAudioPicker()
            }
        }

        binding.translateButton.setOnClickListener { translateSentence() }
        binding.reloadButton.setOnClickListener { resetIdle() }
        binding.exitButton.setOnClickListener { requireActivity().finish() }
    }

    // ================== PERMISSIONS & PICKERS ==================

    private fun startBuiltInMic() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 101)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        try {
            googleMicLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Mic not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAudioPicker() {
        val permission = if (Build.VERSION.SDK_INT >= 33)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Please allow storage access", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(permission), 102)
            return
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        audioPicker.launch(intent)
    }

    // ================== MANUAL MODEL LOADING ==================
    private fun loadModelSynchronously() {
        if (model != null) return
        isVoskLoading = true

        val modelPath = File(requireContext().filesDir, "vosk-model/vosk-model-small-en-us")

        if (!modelPath.exists()) {
            Log.d("Vosk", "Model not found on disk, unpacking...")
            StorageService.unpack(requireContext(), "vosk-model-small-en-us", "vosk-model",
                { m ->
                    model = m
                    isVoskLoading = false
                    handler.post { Toast.makeText(requireContext(), "AI Ready!", Toast.LENGTH_SHORT).show() }
                },
                { e ->
                    Log.e("Vosk", "Unpack Error", e)
                    isVoskLoading = false
                }
            )
        } else {
            Log.d("Vosk", "Model found, loading directly...")
            try {
                model = Model(modelPath.absolutePath)
                isVoskLoading = false
                handler.post { Toast.makeText(requireContext(), "AI Loaded from Cache", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                Log.e("Vosk", "Direct Load Error", e)
            }
        }
    }

    // ================== AUDIO PROCESSING LOGIC ==================

    private suspend fun processPickedAudio(uri: Uri) {
        withContext(Dispatchers.Main) { binding.editTextSentence.hint = "Reading File..." }

        val tempFile = saveUriToTemp(uri)
        if (tempFile == null) {
            withContext(Dispatchers.Main) { binding.editTextSentence.setText("Error: Read Failed") }
            return
        }

        val wavFile = File(requireContext().cacheDir, "converted.wav")
        withContext(Dispatchers.Main) { binding.editTextSentence.hint = "Converting..." }

        val (success, resultMsg) = convertAudioToWav(File(tempFile), wavFile)

        if (success) {
            withContext(Dispatchers.Main) { binding.editTextSentence.hint = "Recognizing..." }
            recognizeFileWithVosk(wavFile, resultMsg.toInt())
        } else {
            withContext(Dispatchers.Main) {
                binding.editTextSentence.setText("Convert Error: $resultMsg")
            }
        }
    }

    private fun saveUriToTemp(uri: Uri): String? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val temp = File(requireContext().cacheDir, "input_audio.mp3")
            val out = FileOutputStream(temp)
            input.copyTo(out)
            out.close()
            input.close()
            temp.absolutePath
        } catch (e: Exception) { null }
    }

    private fun convertAudioToWav(inputFile: File, outFile: File): Pair<Boolean, String> {
        return try {
            val extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var trackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    extractor.selectTrack(i)
                    break
                }
            }

            if (trackIndex == -1) return Pair(false, "No Audio Track")

            val format = extractor.getTrackFormat(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val output = ByteArrayOutputStream()

            // Large buffer for safety (1MB)
            val temp = ByteArray(1024 * 1024)

            var outputDone = false
            var inputDone = false
            var loopCount = 0
            val MAX_LOOPS = 500000

            while (!outputDone && loopCount < MAX_LOOPS) {
                loopCount++
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(5000)
                    if (inIndex >= 0) {
                        val buf = codec.getInputBuffer(inIndex)!!
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 5000)
                if (outIndex >= 0) {
                    val outBuf = codec.getOutputBuffer(outIndex)!!
                    val chunkLen = bufferInfo.size.coerceAtMost(temp.size)
                    outBuf.get(temp, 0, chunkLen)
                    output.write(temp, 0, chunkLen)
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            writeWav(outFile, output.toByteArray(), sampleRate, channels)
            Pair(true, sampleRate.toString())
        } catch (e: Exception) {
            Log.e("Convert", "Failed: ${e.message}")
            Pair(false, e.message ?: "Unknown Codec Error")
        }
    }

    private fun writeWav(file: File, pcm: ByteArray, sampleRate: Int, channels: Int) {
        val out = FileOutputStream(file)
        val totalDataLen = pcm.size + 36
        val byteRate = sampleRate * channels * 2
        fun intToBytes(v: Int) = byteArrayOf((v and 0xff).toByte(), ((v shr 8) and 0xff).toByte(), ((v shr 16) and 0xff).toByte(), ((v shr 24) and 0xff).toByte())
        out.write("RIFF".toByteArray())
        out.write(intToBytes(totalDataLen))
        out.write("WAVEfmt ".toByteArray())
        out.write(byteArrayOf(16, 0, 0, 0)); out.write(byteArrayOf(1, 0))
        out.write(byteArrayOf(channels.toByte(), 0))
        out.write(intToBytes(sampleRate))
        out.write(intToBytes(byteRate))
        out.write(byteArrayOf((channels * 2).toByte(), 0))
        out.write(byteArrayOf(16, 0))
        out.write("data".toByteArray())
        out.write(intToBytes(pcm.size))
        out.write(pcm)
        out.close()
    }

    private suspend fun recognizeFileWithVosk(wav: File, sampleRate: Int) {
        try {
            val rec = Recognizer(model!!, sampleRate.toFloat())
            val fis = FileInputStream(wav)
            val buffer = ByteArray(4096)
            var read: Int

            while (fis.read(buffer).also { read = it } > 0) {
                rec.acceptWaveForm(buffer, read)
            }
            fis.close()

            val resultJson = rec.finalResult
            val text = resultJson.substringAfter("\"text\" : \"").substringBefore("\"")

            withContext(Dispatchers.Main) {
                if (text.isNotEmpty()) {
                    binding.editTextSentence.setText(text)
                    translateSentence()
                } else {
                    binding.editTextSentence.setText("No speech detected.")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.editTextSentence.setText("AI Error: ${e.message}")
            }
        }
    }

    // ================== ANIMATION ==================
    private fun translateSentence() {
        handler.removeCallbacksAndMessages(null)
        val txt = binding.editTextSentence.text.toString().trim().lowercase(Locale.ROOT)
        val rawId = when (txt) {
            "hello" -> R.raw.hello
            "good" -> R.raw.good
            "you" -> R.raw.you
            "morning" -> R.raw.morning
            else -> 0
        }
        if (rawId != 0) {
            Glide.with(this).asGif().load(rawId).into(binding.imageViewSign)
            return
        }
        animationString = txt.replace(Regex("[^a-z0-9]"), "")
        charIndex = 0
        showNextChar()
    }

    private fun showNextChar() {
        if (charIndex >= animationString.length) {
            handler.postDelayed({ resetIdle() }, 1200)
            return
        }
        if (!isAdded) return
        val ch = animationString[charIndex]
        val drawableId = when (ch) {
            'a' -> R.drawable.a; 'b' -> R.drawable.b; 'c' -> R.drawable.c; 'd' -> R.drawable.d; 'e' -> R.drawable.e
            'f' -> R.drawable.f; 'g' -> R.drawable.g; 'h' -> R.drawable.h; 'i' -> R.drawable.i; 'j' -> R.drawable.j
            'k' -> R.drawable.k; 'l' -> R.drawable.l; 'm' -> R.drawable.m; 'n' -> R.drawable.n; 'o' -> R.drawable.o
            'p' -> R.drawable.p; 'q' -> R.drawable.q; 'r' -> R.drawable.r; 's' -> R.drawable.s; 't' -> R.drawable.t
            'u' -> R.drawable.u; 'v' -> R.drawable.v; 'w' -> R.drawable.w; 'x' -> R.drawable.x; 'y' -> R.drawable.y
            'z' -> R.drawable.z; '0' -> R.drawable.image_0; '1' -> R.drawable.image_1; '2' -> R.drawable.image_2
            '3' -> R.drawable.image_3; '4' -> R.drawable.image_4; '5' -> R.drawable.image_5; '6' -> R.drawable.image_6
            '7' -> R.drawable.image_7; '8' -> R.drawable.image_8; '9' -> R.drawable.image_9; else -> R.drawable.space
        }
        Glide.with(this).load(drawableId).into(binding.imageViewSign)
        charIndex++
        handler.postDelayed({ showNextChar() }, 300)
    }

    private fun resetIdle() {
        if (_binding == null || !isAdded) return
        handler.removeCallbacksAndMessages(null)
        charIndex = 0
        animationString = ""
        binding.editTextSentence.setText("")
        binding.editTextSentence.hint = "Type or mic..."
        binding.editTextSentence.clearFocus()
        Glide.with(this).load(R.drawable.space).into(binding.imageViewSign)

        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)

        Toast.makeText(requireContext(), "Reset", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}