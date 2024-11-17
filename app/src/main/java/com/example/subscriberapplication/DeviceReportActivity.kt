package com.example.subscriberapplication

import android.app.DatePickerDialog
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Date
import java.util.Locale

class DeviceReportActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var btnStartDateTime: Button
    private lateinit var btnEndDateTime: Button
    private lateinit var tvMinSpeed: TextView
    private lateinit var tvMaxSpeed: TextView
    private lateinit var tvAverageSpeed: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var startDateTime: Long = 0L
    private var endDateTime: Long = 0L
    private var studentID: String? = null
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_report)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deviceMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /* RETRIEVE THE PRESENT DB */
        databaseHelper = DatabaseHelper.getInstance(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnStartDateTime = findViewById(R.id.btnStartDateTime)
        btnEndDateTime = findViewById(R.id.btnEndDateTime)
        tvMinSpeed = findViewById(R.id.tvMinSpeed)
        tvMaxSpeed = findViewById(R.id.tvMaxSpeed)
        tvAverageSpeed = findViewById(R.id.tvAverageSpeed)

        studentID = intent.getStringExtra("studentID")
        val summaryTitleTextView = findViewById<TextView>(R.id.tvSummaryTitle)
        summaryTitleTextView.text = "Summary of $studentID"

        btnStartDateTime.setOnClickListener {
            showDatePickerDialog { timestamp ->
                startDateTime = timestamp
                val date = Date(timestamp)
                var dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(date)
                findViewById<TextView>(R.id.tvStartDate).text = formattedDate
                if (startDateTime != 0L && endDateTime != 0L) {
                    getTimePeriodInformation()
                }
            }
        }

        btnEndDateTime.setOnClickListener {
        showDatePickerDialog { timestamp ->
                endDateTime = timestamp
                val date = Date(timestamp)
                var dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(date)
                findViewById<TextView>(R.id.tvEndDate).text = formattedDate
                if (startDateTime != 0L && endDateTime != 0L) {
                    getTimePeriodInformation()
                }
            }
        }
    }

    private fun showDatePickerDialog(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected(calendar.timeInMillis)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        datePickerDialog.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setBuildingsEnabled(false) // to avoid 3D buildings overlaying path when zoomed in
        val countryCenter = LatLng(10.483162, -61.263089)
        val zoomLevel = 10.0f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(countryCenter, zoomLevel))
    }

    /* METHOD TO RETRIEVE LOCATION INFO FROM DB ONCE START AND END DATES ARE INPUT */
    private fun getTimePeriodInformation() {
        val latLngPoints = databaseHelper.getLocationsForDeviceByDate(studentID!!, startDateTime, endDateTime)
        drawPolyline(latLngPoints)
        val speedRange = databaseHelper.getSpeedRangeForDeviceByDate(studentID!!, startDateTime, endDateTime)
        if (speedRange != null) {
            val (minSpeed, maxSpeed) = speedRange
            tvMinSpeed.text = getString(R.string.min_speed_label, minSpeed)
            tvMaxSpeed.text = getString(R.string.max_speed_label, maxSpeed)
            val averageSpeed = (minSpeed + maxSpeed) / 2
            tvAverageSpeed.text = getString(R.string.average_speed_label, averageSpeed)
        }
    }

    private fun drawPolyline(latLngPoints : List<LatLng>) {
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
}