package com.andresfila.magicball

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MagicBallApp()
            }
        }
    }
}

@Composable
fun MagicBallApp() {
    val answers = listOf(
        "It is certain",
        "⭐ Without a doubt",
        "💫 Yes, definitely",
        "Most likely",
        "🌠 Signs point to yes",
        "🌙 Ask again later",
        "Cannot predict now",
        "🌌 The universe says YES!",
        "🪄 Trust your instincts",
        "Better not tell you now",
        "🌑 Don't count on it",
        "👻 My sources say no",
        "Very doubtful",
        "Outlook not so good",
        "🔮 The future is right!",
        "💫 Very likely!"
    )

    var question by remember { mutableStateOf("") }
    var previousQuestion by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("8") }
    var showQuestion by remember { mutableStateOf(false) }
    var sameQuestion by remember { mutableStateOf(false) }
    var questionLabel by remember { mutableStateOf("Your question") }

    val rotation = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }

    val coroutineScope = rememberCoroutineScope()

    // Answer reveal animation
    val answerAlpha = remember { Animatable(1f) }
    val answerScale = remember { Animatable(1f) }
    val answerGlow = remember { Animatable(0f) }

    var isShaking by remember { mutableStateOf(false) }

    val background = Color(0xFF101427)
    val blue = Color(0xFF536DFE)

    // Initialize sound effects
    val context = LocalContext.current
    val soundManager = remember(context) {
        MagicBallSoundManager(context)
    }

    //vibration variable
    val haptics = remember(context) {
        MagicBallHaptics(context)
    }

    val preferences = remember(context) {
        context.getSharedPreferences(
            "magicball_settings",
            Context.MODE_PRIVATE
        )
    }

    var soundEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("sound_enabled", true)
        )
    }

    var vibrationEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("vibration_enabled", true)
        )
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    // Release audio resources when the screen is removed
    DisposableEffect(soundManager) {
        onDispose {
            soundManager.release()
        }
    }

    //release vibration when the screen is removed
    DisposableEffect(haptics) {
        onDispose {
            haptics.stop()
        }
    }

    val generateAnswer = {
        if (question.isNotBlank()) {
            if ( question != previousQuestion ) {
                answer = answers.random()
                sameQuestion = false;
                previousQuestion = question
                questionLabel = "Your question"
            }
            else {
                sameQuestion = true
                questionLabel = "Ask a different question"
                answer = "The events haven't change since last time you asked this"
            }
        }
    }

    val animateAndAnswer: () -> Unit = {
        if (question.isNotBlank() && !isShaking) {

            isShaking = true

            coroutineScope.launch {
                try {

                    // Hide the previous answer before shaking
                    answerAlpha.snapTo(0f)
                    answerScale.snapTo(0.65f)
                    answerGlow.snapTo(0f)

                    if (soundEnabled) {
                        soundManager.playShake()
                    }

                    if (vibrationEnabled) {
                        haptics.shake()
                    }
                    // Move left
                    offsetX.animateTo(-35f, tween(120))
                    rotation.snapTo(-12f)

                    // Move right
                    offsetX.animateTo(35f, tween(120))
                    rotation.snapTo(12f)

                    // Move left again
                    offsetX.animateTo(-30f, tween(120))
                    rotation.snapTo(-10f)

                    // Move right again
                    offsetX.animateTo(30f, tween(120))
                    rotation.snapTo(10f)

                    // Smaller movements
                    offsetX.animateTo(-20f, tween(120))
                    rotation.snapTo(-6f)

                    offsetX.animateTo(20f, tween(120))
                    rotation.snapTo(6f)

                    // Return to center
                    offsetX.animateTo(0f, tween(280))
                    rotation.snapTo(0f)

                    // generate the answer
                    generateAnswer()
                    // Play answer sound
                    if (soundEnabled) {
                        soundManager.playAnswer()
                    }

                    if (vibrationEnabled) {
                        haptics.answer()
                    }

                    // Briefly illuminate the answer, then fade the glow away
                    launch {
                        answerGlow.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 250)
                        )

                        answerGlow.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 650)
                        )
                    }

                    // Reveal the answer with fade and scale animations
                    launch {
                        answerAlpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 650)
                        )
                    }

                    answerScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 650)
                    )

                } finally {
                    isShaking = false
                }
            }
        }
    }

    ShakeDetector(
        enabled = question.isNotBlank(),
        onShake = animateAndAnswer
    )

    // ====================================
    // NEW: Display Settings screen
    // ====================================

    if (showSettings) {
        MagicBallSettingsScreen(
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,

            onSoundChanged = { enabled ->
                soundEnabled = enabled

                preferences.edit()
                    .putBoolean("sound_enabled", enabled)
                    .apply()
            },

            onVibrationChanged = { enabled ->
                vibrationEnabled = enabled

                preferences.edit()
                    .putBoolean("vibration_enabled", enabled)
                    .apply()

                if (!enabled) {
                    haptics.stop()
                }
            },

            onBack = {
                showSettings = false
            }
        )

        return
    }

    // ====================================
    // EXISTING MAIN UI
    // ====================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MAGIC BALL",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = blue
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Ask a question and discover your future!",
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if ( question.isBlank()){
            sameQuestion = false
            questionLabel = "Your question"
            answer = "8"
        }

        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(text = questionLabel) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        /*
        Spacer(modifier = Modifier.height(32.dp))

        if (showQuestion) {
            Text(
                text = previousQuestion,
                color = blue,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(125.dp)
            )
        }
        */

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(280.dp)
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = rotation.value
                }
                .clip(CircleShape)
                .clickable (enabled = question.isNotBlank() && !isShaking ){
                    animateAndAnswer()
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {

                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // 1. Glossy black sphere with spherical shading
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF687386),
                            Color(0xFF30394B),
                            Color(0xFF111724),
                            Color(0xFF030407),
                            Color.Black
                        ),
                        center = Offset(
                            size.width * 0.30f,
                            size.height * 0.23f
                        ),
                        radius = size.minDimension * 0.85f
                    ),
                    radius = radius,
                    center = center
                )

                
                // 2. Curved glossy reflection on the sphere
                val reflection = Path().apply {
                    moveTo(
                        size.width * 0.15f,
                        size.height * 0.40f
                    )

                    cubicTo(
                        size.width * 0.12f,
                        size.height * 0.17f,
                        size.width * 0.33f,
                        size.height * 0.06f,
                        size.width * 0.53f,
                        size.height * 0.10f
                    )

                    cubicTo(
                        size.width * 0.40f,
                        size.height * 0.13f,
                        size.width * 0.23f,
                        size.height * 0.24f,
                        size.width * 0.15f,
                        size.height * 0.40f
                    )

                    close()
                }

                drawPath(
                    path = reflection,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        startY = size.height * 0.08f,
                        endY = size.height * 0.42f
                    )
                )


                // Soft blue glow around the answer window
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.78f to Color.Transparent,
                            0.88f to Color(0xFF3268C8).copy(alpha = 0.20f),
                            0.94f to Color(0xFF3268C8).copy(alpha = 0.10f),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.44f
                    ),
                    radius = size.minDimension * 0.44f,
                    center = center
                )


                // 3. Outer rim of the answer window
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38445D),
                            Color(0xFF101827),
                            Color(0xFF030509)
                        ),
                        center = center,
                        radius = size.minDimension * 0.38f
                    ),
                    radius = size.minDimension * 0.37f,
                    center = center
                )

                // Thin illuminated ring around the inner window
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF7198D5).copy(alpha = 0.65f),
                            Color(0xFF263F72).copy(alpha = 0.35f),
                            Color(0xFF050914)
                        ),
                        start = Offset(
                            size.width * 0.25f,
                            size.height * 0.25f
                        ),
                        end = Offset(
                            size.width * 0.75f,
                            size.height * 0.75f
                        )
                    ),
                    radius = size.minDimension * 0.345f,
                    center = center
                )

                // 4. Dark blue inner window
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF243C73),
                            Color(0xFF101C3B),
                            Color(0xFF040817)
                        ),
                        center = center,
                        radius = size.minDimension * 0.34f
                    ),
                    radius = size.minDimension * 0.33f,
                    center = center
                )

                // 5. Blue triangular answer window
                val triangle = Path().apply {
                    moveTo(size.width / 2f, size.height * 0.25f)
                    lineTo(size.width * 0.78f, size.height * 0.70f)
                    lineTo(size.width * 0.22f, size.height * 0.70f)
                    close()
                }

                // Soft blue illumination behind the triangle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF438BFF).copy(alpha = 0.30f),
                            Color(0xFF2452C8).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.31f
                    ),
                    center = center,
                    radius = size.minDimension * 0.31f
                )

                drawPath(
                    path = triangle,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9BD4FF),
                            Color(0xFF3978F5),
                            Color(0xFF142D91)
                        ),
                        startY = size.height * 0.25f,
                        endY = size.height * 0.70f
                    )
                )
            }

            Text(
                text = answer,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (answer == "8") 48.sp else 15.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF66CCFF).copy(
                            alpha = answerGlow.value
                        ),
                        offset = Offset.Zero,
                        blurRadius = 18f * answerGlow.value
                    )
                ),
                modifier = Modifier
                    .width(125.dp)
                    .graphicsLayer {
                        alpha = answerAlpha.value
                        scaleX = answerScale.value
                        scaleY = answerScale.value
                    }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                animateAndAnswer()
            },
            enabled = question.isNotBlank() && !isShaking
        ) {
            Text("Ask the Magic Ball")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
                onClick = {
                    showSettings = true
                }
        ) {
                Text("⚙ Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "For entertainment purposes only",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}


@Composable
fun MagicBallSettingsScreen(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundChanged: (Boolean) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                text = "← Back to MagicBall",
                fontSize = 18.sp
            )
        }

        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sound effects")

            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundChanged
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Vibration")

            Switch(
                checked = vibrationEnabled,
                onCheckedChange = onVibrationChanged
            )
        }
    }
}

@Composable
fun GlossyMagicBall(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF737D91),
                        0.18f to Color(0xFF353E52),
                        0.48f to Color(0xFF151B29),
                        0.78f to Color(0xFF080B12),
                        1.0f to Color.Black
                    ),
                    center = Offset(190f, 140f),
                    radius = 600f
                ),
                shape = CircleShape
            )
    ) {
        content()
    }
}

@Composable
fun ShakeDetector(
    enabled: Boolean,
    onShake: () -> Unit
) {
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)

    DisposableEffect(context, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(
                Context.SENSOR_SERVICE
            ) as SensorManager

            val accelerometer = sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER
            )

            var lastShakeTime = 0L

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val acceleration = sqrt(
                        x * x + y * y + z * z
                    ) / SensorManager.GRAVITY_EARTH

                    val now = SystemClock.elapsedRealtime()

                    if (acceleration > 2.5f &&
                        now - lastShakeTime > 1200L
                    ) {
                        lastShakeTime = now
                        currentOnShake()
                    }
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int
                ) {
                    // No action required
                }
            }

            if (accelerometer != null) {
                sensorManager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

            onDispose {
                sensorManager.unregisterListener(listener)
            }
        }
    }
}

class MagicBallSoundManager(context: Context) {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()
        )
        .build()

    private val loadedSounds = mutableSetOf<Int>()

    private val shakeSound: Int
    private val answerSound: Int

    init {
        soundPool.setOnLoadCompleteListener {
            _, sampleId, status ->

            if (status == 0) {
                synchronized(loadedSounds) {
                    loadedSounds.add(sampleId)
                }
            }
        }

        shakeSound = soundPool.load(context, R.raw.shake, 1)
        answerSound = soundPool.load(context, R.raw.answer, 1)
    }

    fun playShake() {
        play(shakeSound)
    }

    fun playAnswer() {
        play(answerSound)
    }

    private fun play(soundId: Int) {
        val ready = synchronized(loadedSounds) {
            soundId in loadedSounds
        }

        if (ready) {
            soundPool.play(
                soundId,
                1f,  // Left volume
                1f,  // Right volume
                1,   // Priority
                0,   // No looping
                1f   // Normal playback speed
            )
        }
    }

    fun release() {
        soundPool.release()
    }
}


class MagicBallHaptics(context: Context) {

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(
                Context.VIBRATOR_MANAGER_SERVICE
            ) as VibratorManager

            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(
                Context.VIBRATOR_SERVICE
            ) as Vibrator
        }

    fun shake() {
        if (!vibrator.hasVibrator()) return

        val timings = longArrayOf(
            0, 70, 130, 70, 130, 70,
            130, 70, 130, 70, 130, 70
        )

        val amplitudes = intArrayOf(
            0, 130, 0, 130, 0, 130,
            0, 130, 0, 130, 0, 130
        )

        vibrator.vibrate(
            VibrationEffect.createWaveform(
                timings,
                amplitudes,
                -1
            )
        )
    }

    fun answer() {
        if (!vibrator.hasVibrator()) return

        val timings = longArrayOf(
            150,  // Pause before first vibration
            60,   // First vibration
            100,  // Pause between vibrations
            60    // Second vibration
        )

        val amplitudes = intArrayOf(
            0,    // No vibration
            180,  // First pulse
            0,    // No vibration
            180   // Second pulse
        )

        vibrator.vibrate(
            VibrationEffect.createWaveform(
                timings,
                amplitudes,
                -1
            )
        )
    }

    fun stop() {
        vibrator.cancel()
    }
}