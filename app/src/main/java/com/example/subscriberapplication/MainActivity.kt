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

    private lateinit var databaseHelper: DatabaseHelper
    private var client: Mqtt5AsyncClient? = null
    private lateinit var mMap: GoogleMap
    private var deviceAdapter:DeviceAdapter? = null

    /* this list will store an object that represents a given device sending location info and will contain the min and max speed
    - the min and max speed will be queried from the db
     */
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

        deviceAdapter = DeviceAdapter(this)
        val deviceLayout: RecyclerView = findViewById(R.id.rvDevices)
        deviceLayout.adapter = deviceAdapter
        deviceLayout.layoutManager = LinearLayoutManager(this)

        /* SET UP BROKER */
        client = Mqtt5Client.builder()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker-816028524.sundaebytestt.com")
            .serverPort(1883)
            .build()
            .toAsync()

        /* CONNECT TO BROKER */
        try {
            client?.connect()
            Toast.makeText(this,"Successfully connected to broker", Toast.LENGTH_SHORT).show()
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when connecting to broker", Toast.LENGTH_SHORT).show()
        }

        /* LISTEN TO MESSAGES FROM BROKER */
        try {
            client!!.subscribeWith()
                .topicFilter("the/location")
                .callback { publish ->
                    val payload = String(publish.payloadAsBytes, Charsets.UTF_8)
                    Log.d("MQTT", "Received message: $payload")
                    runOnUiThread {
                        handleIncomingData(payload)
                        drawPolyline()
                    }
                }
                .send()
            Toast.makeText(this,"Successfully subscribed to topic", Toast.LENGTH_SHORT).show()
        } catch (e:Exception){
            Toast.makeText(this,"An error occurred when subscribing to topic", Toast.LENGTH_SHORT).show()
        }
    }

    /* PARSE INCOMING LOCATION INFORMATION AND ADD IT TO DATABASE */
    private fun handleIncomingData(payload: String) {
        try {
            val json = JSONObject(payload)
            val studentID = json.getString("studentID")
            val latitude = json.getDouble("latitude")
            val longitude = json.getDouble("longitude")
            val speed = json.getDouble("speed")
            val dateTime = json.getLong("timestamp")

            databaseHelper.addData(studentID, latitude, longitude, speed, dateTime)

            /* update min and max speed for a device if it already exists */
            if (isStudentIDExists(studentID)) {
                val device = getDeviceByStudentID(studentID)
                val speedRange = databaseHelper.getSpeedRangeForDevice(studentID)
                if (speedRange != null) {
                    val (minSpeed, maxSpeed) = speedRange
                    device!!.minSpeed = minSpeed
                    device.maxSpeed = maxSpeed
                }
                deviceAdapter?.updateList(devices)
            }
            /* create a new device object if it does not exist */
            else {
                val newDevice = Device(studentID, speed, speed)
                devices.add(newDevice)
                deviceAdapter?.updateList(devices)
            }

        } catch (e:Exception) {
            Log.e("IncomingData", "Failed to parse data")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setBuildingsEnabled(false) // to avoid 3D buildings overlaying path when zoomed in
        val countryCenter = LatLng(10.483162, -61.263089)
        val zoomLevel = 10.0f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(countryCenter, zoomLevel))
    }

    private fun drawPolyline() {
        val latLngPoints = databaseHelper.getAllLocations()
        val polylineOptions = PolylineOptions()
            .addAll(latLngPoints)
            .color(Color.RED)
            .width(20f)
            .geodesic(true)

        mMap.addPolyline(polylineOptions)

        val bounds = LatLngBounds.builder()
        latLngPoints.forEach { bounds.include(it) }
        //mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    /* HELPER METHOD TO GET DEVICE OBJECT BASED ON ITS STUDENT ID */
    fun getDeviceByStudentID(studentID: String): Device? {
        return devices.find { it.studentID == studentID }
    }

    /* HELPER METHOD TO CHECK IF A DEVICE WITH STUDENT ID EXISTS */
    fun isStudentIDExists(studentID: String): Boolean {
        return devices.any { it.studentID == studentID }
    }
}