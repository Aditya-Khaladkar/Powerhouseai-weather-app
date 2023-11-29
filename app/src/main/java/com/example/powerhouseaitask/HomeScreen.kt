package com.example.powerhouseaitask

import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.powerhouseaitask.databinding.ActivityHomeScreenBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import org.json.JSONObject
import java.util.Locale

class HomeScreen : AppCompatActivity() {
    lateinit var binding: ActivityHomeScreenBinding
    lateinit var locationRequest: LocationRequest
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    var lat = ""
    var lng = ""
    private val apiKey = "YOUR_API_KEY"
    private val weatherApiBaseUrl = "https://api.openweathermap.org/data/2.5/weather"

    // save data offline
    private val PREF_NAME = "WeatherPrefs"
    private val KEY_LOCATION = "location"
    private val KEY_TEMPERATURE = "temperature"
    private val KEY_WEATHER_DESCRIPTION = "weather_description"

    // inti adapter
    lateinit var adapter: WeatherCityAdapter

    var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeScreenBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        checkLocationPermissionAndGPS()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Log.d("@debug", "onCreate 2: $lat$lng")

        val sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val cachedLocation = sharedPreferences.getString(KEY_LOCATION, "")
        val cachedTemperature = sharedPreferences.getString(KEY_TEMPERATURE, "")
        val cachedWeatherDescription = sharedPreferences.getString(KEY_WEATHER_DESCRIPTION, "")

        if (cachedLocation!!.isNotEmpty() && cachedTemperature!!.isNotEmpty() && cachedWeatherDescription!!.isNotEmpty()) {
            // Update UI with cached data
            binding.txtCurrentLocation.text = cachedLocation
            binding.txtCurrentTemperature.text = cachedTemperature
            binding.txtSkyCondition.text = cachedWeatherDescription
        }


        // logic for recycler view layout
        binding.fLayoutSeeMore.setOnClickListener {
            counter++
            if (counter % 2 == 0) {
                binding.cityRecyclerView.visibility = View.VISIBLE
            } else {
                binding.cityRecyclerView.visibility = View.GONE
            }
        }

        // inflating recyclerview
        binding.cityRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.cityRecyclerView.setHasFixedSize(true)

        val cityNames = listOf("New York", "Singapore", "Mumbai", "Delhi", "Sydney", "Melbourne")

        // Initialize the adapter with an empty list
        adapter = WeatherCityAdapter(emptyList())
        binding.cityRecyclerView.adapter = adapter

        // Fetch weather data for each city
        for (city in cityNames) {
            fetchWeatherData(city)
        }

    }

    private fun checkLocationPermissionAndGPS() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location Granted", Toast.LENGTH_LONG).show()
            checkGPS()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission Granted", Toast.LENGTH_LONG).show()
                checkGPS()
            } else {
                Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkGPS() {
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 2000

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        builder.setAlwaysShow(true)

        val result = LocationServices.getSettingsClient(
            this.applicationContext
        ).checkLocationSettings(builder.build())

        result.addOnCompleteListener { task ->
            try {
                task.getResult(ApiException::class.java)
                // Location settings are satisfied, proceed to get user location
                getUserLocation()
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        // Location settings are not satisfied, prompt user to turn on location
                        try {
                            // Cast to ResolvableApiException for prompt resolution
                            val resolvable = exception as ResolvableApiException
                            resolvable.startResolutionForResult(this@HomeScreen, 123)
                        } catch (e: IntentSender.SendIntentException) {
                            Log.e("@debug", "Error resolving location settings: $e")
                        }
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        // Location settings are not satisfied and cannot be resolved
                        Log.e("@debug", "Location settings are not satisfied and cannot be resolved")
                        // You may want to show a dialog or take other appropriate action here
                    }
                }
            }
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {

            val location = it.result

            if (location != null) {
                try {

                    val geocoder = Geocoder(this, Locale.getDefault())

                    val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val cityName = address!![0].locality
                    val areaName = address[0].subLocality
                    val addressString = "$areaName, $cityName"
                    binding.txtCurrentLocation.text = addressString

                    lat += location.latitude.toString()
                    lng += location.latitude.toString()

                    makeWeatherApiCall(lat, lng)


                } catch (e: Exception) {

                }
            }
        }
    }

    private fun makeWeatherApiCall(lat: String, lng: String) {
        val url = "$weatherApiBaseUrl?lat=$lat&lon=$lng&appid=$apiKey"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                // Handle the weather API response here
                handleWeatherApiResponse(response)
            },
            { error ->
                // Handle error
                Log.e("@debug", "Weather API Error: $error")
            }
        )

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

    private fun handleWeatherApiResponse(response: JSONObject) {
        // Extract data from the response and update UI accordingly
        val weatherArray = response.getJSONArray("weather")
        val mainObject = response.getJSONObject("main")

        val weatherDescription = weatherArray.getJSONObject(0).getString("description")
        val temperature = mainObject.getDouble("temp")

        // Store data in SharedPreferences
        val sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_LOCATION, binding.txtCurrentLocation.text.toString())
        editor.putString(KEY_TEMPERATURE, "${temperature.toInt() - 273} °C")
        editor.putString(KEY_WEATHER_DESCRIPTION, weatherDescription)
        editor.apply()

        // Update UI with the extracted data
        runOnUiThread {
            binding.txtCurrentTemperature.text = "${temperature.toInt() - 273} °C"
            binding.txtSkyCondition.text = "$weatherDescription"
        }
    }


    // for particulat cities
    private fun fetchWeatherData(cityName: String) {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$cityName&APPID=$apiKey"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val weatherInfo = parseWeatherResponse(response)
                updateWeatherList(weatherInfo)
            },
            { error ->
                Log.e("WeatherApp", "Error fetching weather data for $cityName: $error")
            })

        // Add the request to the RequestQueue
        Volley.newRequestQueue(this).add(request)
    }

    private fun parseWeatherResponse(response: JSONObject): WeatherCityModel {
        val cityName = response.getString("name")
        val main = response.getJSONObject("main")
        val temperature = main.getDouble("temp")
        val weatherArray = response.getJSONArray("weather")
        val weatherObject = weatherArray.getJSONObject(0)
        val weatherDescription = weatherObject.getString("description")

        return WeatherCityModel(cityName, temperature, weatherDescription)
    }

    private fun updateWeatherList(weatherInfo: WeatherCityModel) {
        val updatedList = adapter.list.toMutableList()
        updatedList.add(weatherInfo)
        adapter = WeatherCityAdapter(updatedList)
        binding.cityRecyclerView.adapter = adapter
    }
}