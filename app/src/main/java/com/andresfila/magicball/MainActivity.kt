package com.andresfila.magicball

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        "Without a doubt",
        "Yes, definitely",
        "Most likely",
        "Signs point to yes",
        "Ask again later",
        "Cannot predict now",
        "Better not tell you now",
        "Don't count on it",
        "My sources say no",
        "Very doubtful",
        "Outlook not so good"
    )

    var question by remember { mutableStateOf("") }
    var previousQuestion by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("8") }
    var showQuestion by remember { mutableStateOf(false) }
    var sameQuestion by remember { mutableStateOf(false) }
    var questionLabel by remember { mutableStateOf("Your question") }

    val background = Color(0xFF101427)
    val blue = Color(0xFF536DFE)

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
            }
        }
    }

    ShakeDetector(
        enabled = question.isNotBlank(),
        onShake = generateAnswer
    )

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
                .clip(CircleShape)
                .clickable (enabled = question.isNotBlank()){
                    generateAnswer()
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Black,
                    radius = size.minDimension / 2
                )

                drawCircle(
                    color = Color(0xFF202845),
                    radius = size.minDimension * 0.35f
                )

                val triangle = Path().apply {
                    moveTo(size.width / 2, size.height * 0.25f)
                    lineTo(size.width * 0.78f, size.height * 0.7f)
                    lineTo(size.width * 0.22f, size.height * 0.7f)
                    close()
                }

                drawPath(
                    path = triangle,
                    color = blue
                )
            }

            Text(
                text = answer,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (answer == "8") 48.sp else 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(125.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                generateAnswer()
            },
            enabled = question.isNotBlank()
        ) {
            Text("Ask the Magic Ball")
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
