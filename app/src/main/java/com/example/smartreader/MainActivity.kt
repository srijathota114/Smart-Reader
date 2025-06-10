package com.example.smartreader

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.mlkit.vision.common.InputImage
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.view.accessibility.AccessibilityEvent
import android.view.GestureDetector

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var textRecognitionManager: TextRecognitionManager
    private lateinit var translationManager: TranslationManager
    private var currentLanguage = "English"
    
    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    private val requestCodePermissions = 10
    private var lastTapTime = 0L
    private var selectedLanguage = Locale.ENGLISH // Default language

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        viewFinder = findViewById(R.id.viewFinder)
        
        // Initialize managers
        textToSpeech = TextToSpeech(this, this)
        voiceCommandManager = VoiceCommandManager(this)
        textRecognitionManager = TextRecognitionManager()
        cameraExecutor = Executors.newSingleThreadExecutor()
        translationManager = TranslationManager(this)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
            speakFeedback("Camera permission is required for this app to work")
        }

        // Set up gesture detection first
        setupGestureDetection()
        
        // Then set up double tap on the preview
        viewFinder.setOnTouchListener(null) // Clear any existing listeners
        setupAccessibleDoubleTap(viewFinder)
        
        setupAccessibleButtons()
        
        val stopButton: Button = findViewById(R.id.stopButton)
        stopButton.setOnClickListener {
            stopReading()
        }
    }

    private fun setupAccessibleDoubleTap(view: View) {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                captureAndAnalyzeImage()
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                speakFeedback("Double tap to capture and read text")
                return true
            }
        })

        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Add accessibility description
        view.contentDescription = "Double tap anywhere on screen to capture image"
        announceForAccessibility("Welcome to Smart Reader. Double tap anywhere on the screen to capture and read text.")
    }

    private fun announceForAccessibility(text: String) {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.announceForAccessibility(text)
    }

    private fun setupAccessibleButtons() {
        val captureButton: Button = findViewById(R.id.captureButton)
        val voiceCommandButton: Button = findViewById(R.id.voiceCommandButton)
        val languageButton: Button = findViewById(R.id.languageButton)

        // Improve button accessibility with better descriptions
        setupAccessibleButton(
            captureButton, 
            "Capture Image Button. Double tap to scan and read text",
            "Double tap to scan and read text from camera"
        ) {
            captureAndAnalyzeImage()
        }

        setupAccessibleButton(
            voiceCommandButton, 
            "Voice Command Button. Double tap and hold to give voice commands",
            "Available commands: Read, Stop"
        )
        voiceCommandButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    speakFeedback("Listening for commands. Say Read to scan text, or Stop to stop reading")
                    startVoiceRecognition()
                    view.performClick()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    speakFeedback("Stopped listening")
                    stopVoiceRecognition()
                    true
                }
                else -> false
            }
        }

        setupAccessibleButton(
            languageButton, 
            "Language Selection Button. Double tap to change language",
            "Current language is $currentLanguage. Double tap to change"
        ) {
            showLanguageSelection()
        }

        // Add gesture detection for the entire screen
        setupGestureDetection()
    }

    private fun setupAccessibleButton(
        button: Button, 
        description: String, 
        hint: String,
        onClick: (() -> Unit)? = null
    ) {
        ViewCompat.setAccessibilityDelegate(button, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.isHeading = true
                info.tooltipText = hint
                info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    AccessibilityNodeInfoCompat.ACTION_CLICK,
                    description
                ))
            }
        })
        button.contentDescription = description
        onClick?.let { button.setOnClickListener { it() } }
    }

    private fun setupGestureDetection() {
        val rootView = findViewById<View>(android.R.id.content)
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val diffY = e2.y - (e1?.y ?: 0f)
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY < 0) { // Swipe up
                        speakFeedback("Current language is $currentLanguage")
                    } else { // Swipe down
                        if (textToSpeech.isSpeaking) {
                            stopReading()
                            speakFeedback("Stopped reading")
                        }
                    }
                    return true
                }
                return false
            }
        })

        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun showLanguageSelection() {
        val languages = translationManager.getAvailableLanguages().toTypedArray()
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setItems(languages) { _, which ->
                currentLanguage = languages[which]
                val locale = when (currentLanguage) {
                    "Telugu" -> Locale("te")
                    else -> Locale(translationManager.getLanguageCode(currentLanguage))
                }
                textToSpeech.language = locale
                speakFeedback("Language changed to $currentLanguage")
            }
            .create()

        dialog.setOnShowListener {
            // Make dialog more accessible
            dialog.window?.decorView?.let { decorView ->
                ViewCompat.setAccessibilityDelegate(decorView, object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.isHeading = true
                        info.tooltipText = "Select a language from the list"
                    }
                })
            }
        }
        
        dialog.show()
        speakFeedback("Select a language from: ${languages.joinToString(", ")}")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider)
            
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                speakFeedback("Camera ready. Double tap anywhere to capture image")
            } catch (e: Exception) {
                speakFeedback("Camera initialization failed. Please restart the app")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndAnalyzeImage() {
        if (!allPermissionsGranted()) {
            speakFeedback("Camera permission is required. Please grant permission.")
            requestPermissions()
            return
        }

        speakFeedback("Capturing image")
        
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = viewFinder.bitmap
                    if (bitmap != null) {
                        processImage(bitmap)
                    } else {
                        speakFeedback("Failed to capture image. Please try again.")
                    }
                    image.close()
                }

                override fun onError(exc: ImageCaptureException) {
                    speakFeedback("Failed to capture image. Please try again.")
                }
            }
        )
    }

    private fun processImage(bitmap: Bitmap) {
        speakFeedback("Processing image, please wait")
        textRecognitionManager.recognizeText(
            bitmap,
            onSuccess = { text ->
                if (text.isNotEmpty()) {
                    if (currentLanguage != "English") {
                        speakFeedback("Translating text to $currentLanguage")
                        translationManager.translateText(
                            text,
                            translationManager.getLanguageCode(currentLanguage),
                            onResult = { translatedText ->
                                speakFeedback(translatedText)
                            },
                            onError = {
                                speakFeedback("Translation failed. Please try again")
                            }
                        )
                    } else {
                        speakFeedback(text)
                    }
                } else {
                    speakFeedback("No text found in image. Please try again")
                }
            },
            onError = {
                speakFeedback("Failed to recognize text. Please try again")
            }
        )
    }

    private fun startVoiceRecognition() {
        voiceCommandManager.startListening { command ->
            // Handle the voice command
            when {
                command.contains("read", ignoreCase = true) -> {
                    // Start reading
                    startReading()
                }
                command.contains("stop", ignoreCase = true) -> {
                    // Stop reading
                    stopReading()
                }
                // Add other commands as needed
            }
        }
    }

    private fun stopVoiceRecognition() {
        voiceCommandManager.stopListening()
    }

    private fun startReading() {
        captureAndAnalyzeImage()
    }

    private fun stopReading() {
        textToSpeech.stop()
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, requiredPermissions, requestCodePermissions
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePermissions) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                speakFeedback("Permissions not granted by the user")
                finish()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(selectedLanguage)
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                speakFeedback("Text to speech initialization failed")
            } else {
                speakFeedback("Welcome to Smart Reader. Double tap anywhere on the screen to capture and read text.")
            }
        }
    }

    private fun speakFeedback(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        announceForAccessibility(text) // Also announce for screen readers
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textToSpeech.stop()
        textToSpeech.shutdown()
        voiceCommandManager.stopListening()
        translationManager.cleanup()
    }
} 