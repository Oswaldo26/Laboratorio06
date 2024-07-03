package com.ebookfrenzy.laboratorio06
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

import com.ebookfrenzy.laboratorio06.ui.theme.Laboratorio06Theme

import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var rotationMatrix = FloatArray(9)
    private var orientationAngles = FloatArray(3)

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationAngleFlow = MutableStateFlow(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        setContent {
            Laboratorio06Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var isFixed by remember { mutableStateOf(false) }
                    var fixedAngle by remember { mutableStateOf(0f) }
                    val rotationAngle by rotationAngleFlow.collectAsState()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        PolygonCanvas(if (isFixed) fixedAngle else rotationAngle, Modifier.weight(1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            if (!isFixed) {
                                fixedAngle = rotationAngle
                            }
                            isFixed = !isFixed
                        }) {
                            Text(text = if (isFixed) "Desfijar" else "Fijar")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val newRotationAngle = orientationAngles[0] * (180 / Math.PI).toFloat()
        rotationAngleFlow.value = newRotationAngle
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    @Composable
    fun PolygonCanvas(rotationAngle: Float, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier.fillMaxSize()) {
            drawPolygon(rotationAngle)
        }
    }

    private fun DrawScope.drawPolygon(rotationAngle: Float) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val triangleSize = 300f
        val halfBase = (triangleSize * Math.sqrt(3.0) / 2).toFloat()

        val path = Path().apply {
            moveTo(centerX, centerY - triangleSize)
            lineTo(centerX - halfBase, centerY + triangleSize / 2)
            lineTo(centerX + halfBase, centerY + triangleSize / 2)
            close()
        }

        val redPath = Path().apply {
            moveTo(centerX, centerY - triangleSize)
            lineTo(centerX - (triangleSize / 3), centerY - (triangleSize / 2))
            lineTo(centerX + (triangleSize / 3), centerY - (triangleSize / 2))
            close()
        }

        rotate(-rotationAngle, Offset(centerX, centerY)) {
            drawPath(path, Color.Blue)
            drawPath(redPath, Color.Red)
        }
    }
}
