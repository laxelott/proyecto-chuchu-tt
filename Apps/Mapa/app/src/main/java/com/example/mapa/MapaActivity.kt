package com.example.mapa

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mapa.databinding.ActivityMapaTransporteBinding
import com.example.mapa.interfaces.ApiService
import com.example.pasajero.interfaces.ApiHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaTransporteBinding
    private lateinit var btnLocation: ImageView
    private lateinit var btnEndTravel: ImageView
    private lateinit var btnReportIncident: ImageView
    private var busStops: List<BusStop> = listOf()
    private var busStopMarkers: MutableMap<BusStop, Marker> = mutableMapOf()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private var waypoints: List<LatLng> = listOf()
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var progressBar: ProgressBar
    private lateinit var background: LinearLayout

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0, 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaTransporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Construct a FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        btnLocation = findViewById(R.id.location_button)
        btnReportIncident = findViewById(R.id.btn_report_incident)
        btnEndTravel = findViewById(R.id.traveling_container_button)

        // Fetch bus stops from the selected line
        val service = ApiHelper().prepareApi()
        //showProgressBar()
        fetchBusStops(service)
        coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        progressBar = findViewById(R.id.progress_bar)
        background = findViewById(R.id.ownBackground)

        // Configure CoroutineScope
//        startSendingDataPeriodically()

        // Initialize directions API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsAPI = retrofit.create(GoogleDirectionsApi::class.java)

        // Configure location updates
        setupLocationUpdates()
    }

    private fun fetchBusStops(service: ApiService){
        ApiHelper().getDataFromDB(
            serviceCall = { service.getBusStopsInfo(60001) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val busStopsInfo = response.body()
                if (busStopsInfo != null) {
                    Log.d("Transport response", "Datos de transporte: $busStopsInfo")
                    busStops = busStopsInfo
                    runOnUiThread {
                        setupMapMarkersAndRoutes()
                        //startTravel()
                    }
                }
            }
        )
    }
    private fun setupMapMarkersAndRoutes() {
        if (!::mMap.isInitialized) return
        // Creating the markers and routes
        createRoutes()
    }

    private fun createRoutes(){
        for ((i, busStop) in busStops.withIndex()) {
            createMarker(busStop)
            waypoints = waypoints + LatLng(busStop.latitude, busStop.longitude)
            if (i < busStops.size - 1) {
                // Route between bus stops
                getRoute(busStops[i], busStops[i+1])
            }
            else {
                getRoute(busStops[i], busStops[0])
            }
        }
    }
    private fun createMarker(busStop: BusStop) {
        val location = LatLng(busStop.latitude, busStop.longitude)
        val bitmap = BitmapFactory.decodeResource(this.resources, R.mipmap.ic_bus_station)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        val markerIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(busStop.name)
                .icon(markerIcon)
        )
        marker?.let { busStopMarkers[busStop] = it }
    }
    private fun getRoute(origin: BusStop, destination: BusStop) {
        val apiKey = getString(R.string.api_key)
        val waypointsStr = destination.waypoints ?: ""
        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        val call = directionsAPI.getDirections(
            originLocation,
            destinationLocation,
            apiKey,
            "driving",
            waypoints = waypointsStr,
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val routes = response.body()?.routes
                    if (!routes.isNullOrEmpty()) {
                        val polyline = routes[0].overview_polyline.points
                        polyline.let {
                            val decodedPath = PolyUtil.decode(it)
                            mMap.addPolyline(
                                PolylineOptions()
                                    .addAll(decodedPath)
                                    .color(android.graphics.Color.BLUE)
                                    .width(10f)
                                    .geodesic(true)
                            )
                        }
                    } else {
                        Log.e("DirectionsError", "No routes found in response")
                    }
                } else {
                    Log.e("DirectionsError", "Response error: ${response.code()} - ${response.message()} - Body: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Failed to get route: ${t.message}")
            }
        })
    }

    private fun startTravel() {
        lifecycleScope.launch(Dispatchers.Main) {
            for (i in busStops.indices) {
                Log.d("Travel", "For")
                val busStopOrigin = busStops[i]
                val busStopDestination = if (i < busStops.size - 1) busStops[i + 1] else busStops[0]
                //getDirectionsAndShowOnMap(busStopOrigin, busStopDestination)
                showTimeToDestination(busStopOrigin, busStopDestination)
            }
        }
    }
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        background.visibility = View.VISIBLE
    }
    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        background.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    private suspend fun showTimeToDestination(busStopOrigin: BusStop, busStopDestination: BusStop) {
        val title = findViewById<TextView>(R.id.traveling_container_title)
        val timer = findViewById<TextView>(R.id.traveling_container_timer)
        val textTotalTime = findViewById<TextView>(R.id.traveling_total_time)
        val busStopDestinationL = LatLng(busStopDestination.latitude, busStopDestination.longitude)
        var distanceBetweenStation: Float

        while (true) {
            distanceBetweenStation = withContext(Dispatchers.IO) {
                getDistance(currentLocation, busStopDestinationL).await().toFloat()
            }
            val times = calculateTimes(busStops)
            var totalTime = plusAllTimes(times)
            withContext(Dispatchers.Main) {
                if (distanceBetweenStation < 5f) {
                    title.text = "Estás en la estación: \n${busStopDestination.name}"
                    timer.text = "Tiempo estimado: 0 segundos"
                    textTotalTime.text = "Tiempo total: $totalTime"
                    return@withContext
                } else {
                    val (distance, duration) = getDistanceAndTime(busStopOrigin, busStopDestination).await()
                    title.text = "Siguiente estación: \n${busStopDestination.name}"
                    timer.text = "Tiempo estimado: \n$duration"
                    textTotalTime.text = "Tiempo total: $totalTime"
                }
            }
            hideProgressBar()
            delay(10)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun calculateTimes(busStops: List<BusStop>): List<Pair<BusStop, String>> {
        val timesList = mutableListOf<Pair<BusStop, String>>() // To store the results

        for (i in 0 until busStops.size - 1) { // Loop through the list
            val currentStop = busStops[i]
            val nextStop = busStops[i + 1]

            // Launch a coroutine for each pair
            val time = GlobalScope.async {
                val (distance, duration) = getDistanceAndTime(currentStop, nextStop).await() // Await and destructure the Pair
                Pair(currentStop, duration) // Only keep the duration
            }

            // Collect the result
            timesList.add(time.await()) // Wait for each async and add to the list
            Log.d("Transport times", "From ${currentStop.name} to ${nextStop.name} takes ${time.await().second}")
        }

        return timesList // Return the collected times
    }

    private fun plusAllTimes(times: List<Pair<BusStop, String>>): String {
        var totalSeconds = 0

        // Iterate through each bus stop and its associated duration
        for (pair in times) {
            val durationString = pair.second // The duration as a string
            totalSeconds += convertDurationToSeconds(durationString) // Convert to seconds and sum
        }

        // Convert total seconds to a readable format
        return formatTotalTime(totalSeconds)
    }

    private fun formatTotalTime(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val timeParts = mutableListOf<String>()
        if (hours > 0) timeParts.add("$hours hora${if (hours > 1) "s" else ""}")
        if (minutes > 0) timeParts.add("$minutes minuto${if (minutes > 1) "s" else ""}")
        if (seconds > 0) timeParts.add("$seconds segundo${if (seconds > 1) "s" else ""}")

        return timeParts.joinToString(", ")
    }

    private fun convertDurationToSeconds(duration: String): Int {
        var totalSeconds = 0

        // Regex to extract numbers and their corresponding time units
        val regex = "(\\d+)\\s*(hora|horas|hr|h|minuto|minutos|min|m|segundo|segundos|s)".toRegex()
        val matches = regex.findAll(duration)

        for (match in matches) {
            val value = match.groups[1]?.value?.toIntOrNull() ?: 0
            val unit = match.groups[2]?.value

            totalSeconds += when (unit) {
                "hora", "horas", "hr", "h" -> value * 3600 // Convert hours to seconds
                "minuto", "minutos", "min", "m" -> value * 60 // Convert minutes to seconds
                "segundo", "segundos", "s" -> value // Seconds remain the same
                else -> 0
            }
        }

        return totalSeconds
    }

    private fun getDistance(busStop1: LatLng, busStop2: LatLng): Deferred<Int> = GlobalScope.async {
        // Create a CompletableDeferred to handle the result
        val deferredResult = CompletableDeferred<Int>()

        val origin = latLangToStr(LatLng(busStop1.latitude, busStop1.longitude))
        val destination = latLangToStr(LatLng(busStop2.latitude, busStop2.longitude))

        val api = getString(R.string.api_key)
        val call = directionsAPI.getDirections(origin, destination, api, "walking")

        // Make the API call
        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val routes = response.body()?.routes
                    if (routes.isNullOrEmpty()) {
                        Log.e("DirectionsError", "No se encontraron rutas")
                        deferredResult.completeExceptionally(Exception("No routes found"))
                    } else {
                        val leg = routes[0].legs[0]
                        val distance = leg.distance.value
                        // Complete the deferred with the distance
                        deferredResult.complete(distance)
                    }
                } else {
                    // Handle the unsuccessful response
                    Log.e("DirectionsError", "Respuesta fallida: ${response.code()} - ${response.message()}")
                    deferredResult.completeExceptionally(Exception("Error: ${response.code()} - ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.d("DirectionsInfo", "Not working")
                deferredResult.completeExceptionally(t) // Complete with an exception
            }
        })

        // Await for the result
        deferredResult.await()
    }

    private fun latLangToStr(latLng: LatLng) = "${latLng.latitude},${latLng.longitude}"

    private fun getDistanceAndTime(origin: BusStop, destination: BusStop): Deferred<Pair<String, String>> {
        val apiKey = getString(R.string.api_key)
        val waypointsStr = destination.waypoints ?: ""
        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        // Create a CompletableDeferred to handle the result
        val deferredResult = CompletableDeferred<Pair<String, String>>()

        val call = directionsAPI.getDirections(
            originLocation,
            destinationLocation,
            apiKey,
            "driving",
            waypoints = waypointsStr
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    val leg = directionsResponse?.routes?.get(0)?.legs?.get(0)

                    // Extract distance and duration
                    val distance = leg?.distance?.text ?: "Unknown distance"
                    val duration = leg?.duration?.text ?: "Unknown duration"

                    // Complete the deferred with distance and duration
                    deferredResult.complete(Pair(distance, duration))
                } else {
                    // Complete with an exception if the response is not successful
                    deferredResult.completeExceptionally(Exception("Error: ${response.code()} - ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Failed to get route: ${t.message}")
                deferredResult.completeExceptionally(t) // Complete with an exception
            }
        })

        return deferredResult // Return the deferred object
    }

    private fun getDirectionsAndShowOnMap(origin: BusStop, destination: BusStop) {
        val apiKey = getString(R.string.api_key)
        val waypointsStr = destination.waypoints ?: ""
        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        val language = "es"

        val call = directionsAPI.getDirections(
            originLocation,
            destinationLocation,
            apiKey,
            "driving",
            waypoints = waypointsStr,
            language = language
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    val legs = directionsResponse?.routes?.get(0)?.legs

                    // Check if legs are available
                    if (legs != null && legs.isNotEmpty()) {
                        val steps = legs[0].steps // Get the steps of the first leg
                        displayDirections(steps) // Call a method to display instructions
                    } else {
                        Log.e("DirectionsError", "No legs found in response")
                    }
                } else {
                    Log.e("DirectionsError", "Response error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Failed to get directions: ${t.message}")
            }
        })
    }

    private fun displayDirections(steps: List<Step>) {
        val instructionsList = StringBuilder()

        for (step in steps) {
            // Extract the distance and instruction for each step
            val distance = step.distance.text
            val htmlInstruction = step.html_instructions
            val instruction = Html.fromHtml(htmlInstruction, Html.FROM_HTML_MODE_LEGACY).toString()

            // Append formatted instruction
            instructionsList.append("$instruction - $distance\n")
        }

        // Now show the instructions in a TextView or Dialog
        showInstructionsDialog(instructionsList.toString())
    }

    private fun showInstructionsDialog(instructions: String) {
        val textViewInst = findViewById<TextView>(R.id.instrucctions)
        textViewInst.text = instructions
    }


    private fun setupLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    updateCameraPosition(location)
                    // Check if the current location is within 5 meters of busStops[0]
                    if (busStops.isNotEmpty()) {
                        val busStopLocation = Location("Bus Stop 0").apply {
                            latitude = busStops[0].latitude
                            longitude = busStops[0].longitude
                        }

                        if (isWithinDistance(busStopLocation, 5f)) {
                            // Show alert if within range
                            showAlertDialog(this@MapaActivity)
                        }
                    }
                }
            }
        }
        // Start location updates
        if (isLocationPermissionGranted()) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            requestLocationPermission()
        }
    }
    private fun updateCameraPosition(location: Location) {
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))  // Set the new position
            .zoom(19.4f)  // Set zoom level
            .bearing(location.bearing)  // Rotate the camera to match the user’s bearing
            .tilt(45f)  // Tilt for a semi-3D view
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }
    fun isWithinDistance(busStop2: Location, minDistanceMeters: Float): Boolean {
        val distance = lastLocation.distanceTo(busStop2)
        return distance <= minDistanceMeters
    }

    fun showAlertDialog(context: Context) {
        AlertDialog.Builder(context).apply {
            setTitle("Rango no permitido")
            setMessage("No estas dentro dentro del rango para iniciar recorrido")
            setPositiveButton("OK") { dialog, _ ->
                if (context is Activity) context.finish()
                dialog.dismiss() // Close the dialog
            }
            create().show()
        }
    }

    private fun startSendingDataPeriodically() {
        coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    sendLatitudeLongitude(currentLocation.latitude, currentLocation.longitude) // Replace with your actual coordinates
                }
                delay(5000)
            }
        }
    }

    private suspend fun sendLatitudeLongitude(latitude: Double, longitude: Double) {
        try {
            Log.d("Transport My Location", "$latitude, $longitude")
            val response = ApiHelper().prepareApi().postLatitudeLongitude(latitude, longitude)
            if (response.isSuccessful) {
                Log.d("API Response", "Datos enviados correctamente")
            } else {
                Log.e("API Error", "Error al enviar datos: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("API Error", "Excepción: ${e.message}")
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMap()
        enableLocation()
        setupLocationButton()
        setUpIncidentButton()
        setUpEndTravelButton()
    }
    private fun setupMap() {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.custom_map_style))
        with(mMap.uiSettings) {
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
            isZoomControlsEnabled = false
            isCompassEnabled = false
        }
    }
    private fun setupLocationButton() {
        btnLocation.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
        }
    }
    private fun setUpIncidentButton() {
        btnReportIncident.setOnClickListener {
            showReportIncident()
        }
    }
    private fun setUpEndTravelButton() {
        btnEndTravel.setOnClickListener {
            showEndTravelWindow()
        }
    }

    private fun showReportIncident() {
        // Show a new window or dialog when the travel ends
        val dialogFragment = ReportIncidentDialogFragment()
        dialogFragment.show(supportFragmentManager, "reportIncident")
    }
    private fun showEndTravelWindow() {
        // Show a new window or dialog when the travel ends
        val dialogFragment = EndTravelDialogFragment()
        dialogFragment.show(supportFragmentManager, "endTravelDialog")
    }







    private fun enableLocation() {
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                location?.let {
                    lastLocation = it
                    currentLocation = LatLng(location.latitude, location.longitude)
                    updateCameraPosition(it)
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
    }

}
