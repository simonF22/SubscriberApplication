package com.example.subscriberapplication

import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import org.json.JSONObject
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var client: Mqtt5AsyncClient? = null
    val pointsList = mutableListOf<CustomMarkerPoints>()
    private lateinit var databaseHelper: DatabaseHelper
    private val devicesMap = mutableMapOf<String, Device>()
    private var deviceAdapter:DeviceAdapter? = null
    private val devices:MutableList<Device> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        databaseHelper = DatabaseHelper(this, null)
        databaseHelper.clearDatabase()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        deviceAdapter = DeviceAdapter()
        val deviceLayout: RecyclerView = findViewById(R.id.rvDevices)
        deviceLayout.adapter = deviceAdapter
        deviceLayout.layoutManager = LinearLayoutManager(this)

        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816028524.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toAsync()

        try {
            client?.connect()
            Toast.makeText(this,"Successfully connected to broker", Toast.LENGTH_SHORT).show()
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
        }
        try {
            client!!.subscribeWith()
                .topicFilter("the/location")
                .callback { publish ->
                    val payload = String(publish.payloadAsBytes, Charsets.UTF_8)
                    Log.d("MQTT", "Received message: $payload")
                    runOnUiThread {
                        handleIncomingData(payload)
                        drawPolyline()

                        deviceAdapter?.updateList(devices)
                    }
                }
                .send()

            Toast.makeText(this,"Successfully subscribed to topic", Toast.LENGTH_SHORT).show()
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when subscribing to topic", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleIncomingData(payload: String) {
        try {
            val json = JSONObject(payload)
            val studentID = json.getString("studentID")
            val latitude = json.getDouble("latitude")
            val longitude = json.getDouble("longitude")
            val speed = json.getDouble("speed")
            val latLng = LatLng(latitude, longitude)
            val dateTime = json.getInt("timestamp")
            databaseHelper.addData(studentID, latitude, longitude, speed, dateTime)
            //val newCustomPoint = CustomMarkerPoints(pointsList.size + 1, latLng)
            //pointsList.add(newCustomPoint) )
            if (devicesMap.containsKey(studentID)) {
                //updateDeviceSpeed(devicesMap[studentID]!!, speed)
                val device = devicesMap[studentID]
                val speedRange = databaseHelper.getSpeedRangeForDevice(studentID)
                if (speedRange != null) {
                    val (minSpeed, maxSpeed) = speedRange
                    device!!.minSpeed = minSpeed
                    device.maxSpeed = maxSpeed
                }

            } else {
                val newDevice = Device(studentID, speed, speed)
                devices.add(newDevice)
                devicesMap.put(studentID,newDevice)
            }
        } catch (e:Exception) {
            Log.e("IncomingData", "Failed to parse data")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val countryCenter = LatLng(10.483162, -61.263089)
        val zoomLevel = 10.0f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(countryCenter, zoomLevel))
    }

    private fun drawPolyline() {
        val latLngPoints = databaseHelper.getAllLocations()
        //val latLngPoints = pointsList.map { it.point }
        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(Color.BLUE)
            .width(5f)
            .geodesic(true)

        mMap.addPolyline(polylineOptions)

        val bounds = LatLngBounds.builder()
        latLngPoints.forEach { bounds.include(it) }
        //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    private fun updateDeviceSpeed(device: Device, newSpeed: Double) {
        device.minSpeed = minOf(device.minSpeed, newSpeed)
        device.maxSpeed = maxOf(device.maxSpeed, newSpeed)
    }
    private fun convertTimeToReadableFormat(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Define your desired format
        val date = Date(timestamp) // Convert the timestamp to a Date object
        return dateFormat.format(date) // Return the formatted date as a string
    }
}