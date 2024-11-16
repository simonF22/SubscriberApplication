package com.example.subscriberapplication

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.android.gms.maps.model.LatLng

class DatabaseHelper(context: Context, factory: SQLiteDatabase.CursorFactory?):
    SQLiteOpenHelper(context, "AppData",factory, 1){

    companion object {
        private var instance: DatabaseHelper? = null

        // Singleton method to get the instance of DatabaseHelper
        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(context.applicationContext, null)
            }
            return instance!!
        }
    }
    override fun onCreate(db: SQLiteDatabase) {
        val createLocationDataTableQuery = ("CREATE TABLE LocationData (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "studentID TEXT," +
                "latitude REAL," +
                "longitude REAL," +
                "speed REAL," +
                "dateTime INTEGER)")
        db.execSQL(createLocationDataTableQuery)
        Log.d("DatabaseHelper", "LocationData table created successfully.")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    fun addData(studentID: String, latitude: Double, longitude: Double, speed: Double, dateTime: Int) {
        val values = ContentValues().apply {
            put("studentID", studentID)
            put("latitude", latitude)
            put("longitude", longitude)
            put("speed", speed)
            put("dateTime", dateTime)
        }
        try {

            val db = this.writableDatabase
            db.insert("LocationData", null, values)
            db.close()
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting data: ${e.message}")
        }
    }

    fun getSpeedRangeForDevice(studentID: String): Pair<Double, Double>? {
        var result: Pair<Double, Double>? = null
        val db = this.readableDatabase
        val cursor = db.rawQuery(" SELECT MIN(speed) AS minSpeed, MAX(speed) AS maxSpeed  FROM LocationData WHERE studentID = ?", arrayOf(studentID))

        if(!(!cursor.moveToFirst() || cursor.count == 0)){
            do {
                val minSpeed = cursor.getDouble(cursor.getColumnIndexOrThrow("minSpeed"))
                val maxSpeed = cursor.getDouble(cursor.getColumnIndexOrThrow("maxSpeed"))
                result = Pair(minSpeed, maxSpeed)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return result
    }

    fun getSpeedRangeForDeviceByDate(studentID: String, startDateTime: Long, endDateTime: Long): Pair<Double, Double>? {
        var result: Pair<Double, Double>? = null
        val db = this.readableDatabase
        val cursor = db.rawQuery(
                "SELECT MIN(speed) AS minSpeed, MAX(speed) AS maxSpeed FROM LocationData WHERE studentID = ? AND dateTime BETWEEN ? AND ?", arrayOf(studentID, startDateTime.toString(), endDateTime.toString()))
        Log.d("App", "app made it here")
        if(!(!cursor.moveToFirst() || cursor.count == 0)){
            do {
                val minSpeed = cursor.getDouble(cursor.getColumnIndexOrThrow("minSpeed"))
                val maxSpeed = cursor.getDouble(cursor.getColumnIndexOrThrow("maxSpeed"))
                result = Pair(minSpeed, maxSpeed)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return result
    }

    fun getAllLocations(): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT latitude, longitude FROM LocationData", null)
        if(!(!cursor.moveToFirst() || cursor.count == 0)){
            do {
                val latitudeIndex = cursor.getColumnIndex("latitude")
                val longitudeIndex = cursor.getColumnIndex("longitude")

                if (latitudeIndex >= 0 && longitudeIndex >= 0){
                    val latitude = cursor.getDouble(latitudeIndex)
                    val longitude = cursor.getDouble(longitudeIndex)
                    points.add(LatLng(latitude,longitude))

                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return points
    }

    fun getLocationsForDeviceByDate(studentID: String, startDateTime : Long, endDateTime : Long): List<LatLng> {
        val points = mutableListOf<LatLng>()
        val db = this.readableDatabase
        val cursor = db.rawQuery(
        "SELECT latitude, longitude, dateTime FROM LocationData WHERE studentID = ? AND dateTime BETWEEN ? AND ?", arrayOf(studentID, startDateTime.toString(), endDateTime.toString()))

        if(!(!cursor.moveToFirst() || cursor.count == 0)){
            do {
                val latitudeIndex = cursor.getColumnIndex("latitude")
                val longitudeIndex = cursor.getColumnIndex("longitude")

                if (latitudeIndex >= 0 && longitudeIndex >= 0){
                    val latitude = cursor.getDouble(latitudeIndex)
                    val longitude = cursor.getDouble(longitudeIndex)
                    points.add(LatLng(latitude,longitude))
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return points
    }

    fun clearDatabase() {
        val db = this.writableDatabase
        try {
            // Option 1: Deleting all rows from the table
            //db.delete("LocationData", null, null)

            // Option 2: Dropping and recreating the table
            db.execSQL("DROP TABLE IF EXISTS LocationData")
            onCreate(db)
            Log.d("DatabaseHelper", "Database reset successfully.")
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error clearing database: ${e.message}")
        } finally {
            db.close()
        }
    }
}