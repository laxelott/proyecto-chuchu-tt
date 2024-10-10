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
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.google.android.gms.maps.model.Polyline
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
    private val polylines: MutableList<Polyline> = mutableListOf()
    private var isLocationUpdatesActive = false
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                currentLocation = LatLng(location.latitude, location.longitude)
            }
        }
    }

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

    private fun fetchBusStops(service: ApiService) {
        ApiHelper().getDataFromDB(
            serviceCall = { service.getBusStopsInfo(60001) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val busStopsInfo = response.body()
                if (busStopsInfo != null) {
                    Log.d("Transport response", "Datos de transporte: $busStopsInfo")
                    busStops = busStopsInfo
                    runOnUiThread {
                        val distance = calculateDistance(
                            LatLng(currentLocation.latitude,currentLocation.longitude),
                            LatLng(busStops[0].latitude, busStops[0].longitude)
                        )
                        if (distance <= 10f
                        ) {
                            setupMapMarkersAndRoutes()
                            CoroutineScope(Dispatchers.IO).launch {
                                startTravel()
                            }
                        } else {
                            showAlertDialog(this@MapaActivity)
                        }
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

    private fun createRoutes() {
        val origin = busStops.first()
        val waypoints = gettingWaypointsFromDestinations(busStops)
        Log.e("Waypoints", "Lista: ${waypoints}")
        getRoute(origin, origin, waypoints)

        for ((i, busStop) in busStops.withIndex()) {
            createMarker(busStop)
        }
    }

    private fun gettingWaypointsFromDestinations(busStopsOD: List<BusStop>): String {
        val waypointsBuilder = StringBuilder()

        for (busStop in busStopsOD) {
            if (busStop.waypoints != null && busStop != busStopsOD[0]) {
                if (waypointsBuilder.isNotEmpty()) {
                    waypointsBuilder.append("|")
                }
                waypointsBuilder.append(busStop.waypoints)
                Log.e(
                    "Waypoints",
                    "Waypoints agregado de ${busStop.name} con valor ${busStop.waypoints}"
                )
            } else {
                if (waypointsBuilder.isNotEmpty()) {
                    waypointsBuilder.append("|")
                }
                waypointsBuilder.append(latLangToStr(LatLng(busStop.latitude, busStop.longitude)))
                Log.e("Waypoints", "Estacion agregada como waypoint: ${busStop.name}")
            }
        }

        return waypointsBuilder.toString()
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

    private fun getRoute(origin: BusStop, destination: BusStop, waypoints: String = "") {
        val apiKey = getString(R.string.api_key)

        val initialWaypoints = mutableListOf<String>()

        waypoints.let {
            initialWaypoints.addAll(it.split("|"))
        }

        val waypointChunks = initialWaypoints.chunked(25)

        var previousStop: BusStop = origin

        waypointChunks.forEachIndexed { index, chunk ->
            val waypointsStr = chunk.joinToString("|")

            val currentDestination: BusStop = if (index == waypointChunks.size - 1) {
                destination
            } else {
                val lastWaypoint = chunk.last().split(",")
                BusStop(
                    0,
                    "Waypoint ${index}",
                    lastWaypoint[0].toDouble(),
                    lastWaypoint[1].toDouble()
                )
            }

            val originLocation = latLangToStr(LatLng(previousStop.latitude, previousStop.longitude))

            val currentDestinationLocation =
                latLangToStr(LatLng(currentDestination.latitude, currentDestination.longitude))

            val call = directionsAPI.getDirections(
                originLocation,
                currentDestinationLocation,
                apiKey,
                "driving",
                waypoints = waypointsStr
            )

            call.enqueue(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.isSuccessful) {
                        val routes = response.body()?.routes
                        if (!routes.isNullOrEmpty()) {
                            val polyline = routes[0].overview_polyline.points
                            polyline.let {
                                val decodedPath = PolyUtil.decode(it)
                                val newPolyline = mMap.addPolyline(
                                    PolylineOptions()
                                        .addAll(decodedPath)
                                        .color(android.graphics.Color.BLUE)
                                        .width(10f)
                                        .geodesic(true)
                                )
                                polylines.add(newPolyline)
                            }
                        } else {
                            Log.e("DirectionsError", "No routes found in response")
                        }
                    } else {
                        Log.e(
                            "DirectionsError",
                            "Response error: ${response.code()} - ${response.message()} - Body: ${
                                response.errorBody()?.string()
                            }"
                        )
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e("DirectionsError", "Failed to get route: ${t.message}")
                }
            })

            previousStop = currentDestination
        }
    }


    private suspend fun startTravel() {
        showTimeToDestination(busStops)
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        background.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        background.visibility = View.GONE
    }

    private suspend fun showTimeToDestination(busStops: List<BusStop>) {
        withContext(Dispatchers.Main) {
            val travelTimes = mutableMapOf<BusStop, Pair<String, String>>()
            var currentStopIndex = 0
            val destination = busStops.first()
            var alertFinalDestinationShown = false

            while (currentStopIndex < busStops.size - 1) {
                val busStopDestination = busStops[currentStopIndex + 1]
                val busStopOrigin = busStops[currentStopIndex]

                if (destination == busStopDestination) {
                    alertUserForFinalDestination()
                    alertFinalDestinationShown = true
                }

                val (distance, duration) = travelTimes[busStopDestination] ?: run {
                    val (dist, dur) = getDistanceAndTime(busStopOrigin, busStopDestination).await()
                    travelTimes[busStopDestination] = Pair(dist, dur)
                    Pair(dist, dur)
                }

                var hasArrived = false
                while (!hasArrived) {
                    delay(5000)
                    updateCurrentLocation()
                    val currentDistance = calculateDistance(
                        LatLng(
                            currentLocation.latitude,
                            currentLocation.longitude
                        ), LatLng(busStopDestination.latitude, busStopDestination.longitude)
                    )

                    hasArrived = updateTravelInfo(busStopDestination, currentDistance, duration)

                    if (alertFinalDestinationShown && hasArrived) {
                        showBusStopAlert()
                        endTravel()
                    }
                    if (hasArrived) {
                        Log.e("Directions", "Llegaste a la parada: ${busStopDestination.name}")
                        currentStopIndex++
                    }

                }
            }

        }
    }

    private fun endTravel() {
        val container = findViewById<LinearLayout>(R.id.traveling_container)
        container.visibility = View.GONE

        val cameraPosition = CameraPosition.Builder()
            .target(currentLocation)
            .zoom(16f)
            .bearing(0f)
            .tilt(0f)
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        stopLocationUpdates()

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Llegaste a tu destino!")
            .setMessage("Haz llegado a tu destino, gracias por viajar con Nintrip :)")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun showBusStopAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Llegaste a tu destino!")
            .setMessage("Haz llegado a tu destino, gracias por viajar con Nintrip :)")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun alertUserForFinalDestination() {
        // Hacer vibrar el dispositivo
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(
            VibrationEffect.createOneShot(
                1000,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )

        // Reproducir sonido de alerta
//        val mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound) // Asegúrate de tener el archivo en res/raw
//        mediaPlayer.start()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Última estación!")
            .setMessage("La siguiente estación es tu destino.")
            .setPositiveButton("Okay") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun updateCurrentLocation() {
        if (isLocationPermissionGranted()) {
            val locationRequest = createLocationRequest()
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Log.e("LocationError", "Permisos de ubicación no concedidos.")
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) return

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            results
        )
        return results[0]
    }

    private fun updateTravelInfo(
        busStopDestination: BusStop,
        currentDistance: Float,
        duration: String
    ): Boolean {
        val title = findViewById<TextView>(R.id.traveling_container_title)
        val showStop = findViewById<TextView>(R.id.traveling)
        val timer = findViewById<TextView>(R.id.traveling_container_timer)

        return if (currentDistance < 10f) {
            showStop.text = "Estás en"
            title.text = "${busStopDestination.name}"
            true
        } else {
            showStop.text = "Siguiente estación"
            title.text = "${busStopDestination.name}"
            timer.text = duration
            false
        }
    }



    private fun latLangToStr(latLng: LatLng) = "${latLng.latitude},${latLng.longitude}"

    private fun getDistanceAndTime(
        origin: BusStop,
        destination: BusStop
    ): Deferred<Pair<String, String>> {
        val apiKey = getString(R.string.api_key)
        val waypointsStr = destination.waypoints ?: ""
        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        val deferredResult = CompletableDeferred<Pair<String, String>>()

        val call = directionsAPI.getDirections(
            originLocation,
            destinationLocation,
            apiKey,
            "driving",
            waypoints = waypointsStr
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    val leg = directionsResponse?.routes?.get(0)?.legs?.get(0)

                    val distance = leg?.distance?.text ?: "Unknown distance"
                    val duration = leg?.duration?.text ?: "Unknown duration"

                    deferredResult.complete(Pair(distance, duration))
                } else {
                    deferredResult.completeExceptionally(Exception("Error: ${response.code()} - ${response.message()}"))
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Failed to get route: ${t.message}")
                deferredResult.completeExceptionally(t)
            }
        })

        return deferredResult
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
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()
                    val legs = directionsResponse?.routes?.get(0)?.legs

                    if (legs != null && legs.isNotEmpty()) {
                        val steps = legs[0].steps
                        displayDirections(steps)
                    } else {
                        Log.e("DirectionsError", "No legs found in response")
                    }
                } else {
                    Log.e(
                        "DirectionsError",
                        "Response error: ${response.code()} - ${response.message()}"
                    )
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
            val distance = step.distance.text
            val htmlInstruction = step.html_instructions
            val instruction = Html.fromHtml(htmlInstruction, Html.FROM_HTML_MODE_LEGACY).toString()
            instructionsList.append("$instruction - $distance\n")
        }
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
                    //updateCameraPosition(location)
                    // Check if the current location is within 5 meters of busStops[0]
                    if (busStops.isNotEmpty()) {
                        val busStopLocation = Location("Bus Stop 0").apply {
                            latitude = busStops[0].latitude
                            longitude = busStops[0].longitude
                        }

                        if (isWithinDistance(busStopLocation, 5f)) {
                            showAlertDialog(this@MapaActivity)
                        }
                    }
                }
            }
        }
        // Start location updates
        if (isLocationPermissionGranted()) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            requestLocationPermission()
        }
    }

    private fun updateCameraPosition(location: Location) {
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .zoom(19.4f)
            .bearing(location.bearing)
            .tilt(45f)
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
                dialog.dismiss()
            }
            create().show()
        }
    }

    private fun startSendingDataPeriodically() {
        coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    sendLatitudeLongitude(
                        currentLocation.latitude,
                        currentLocation.longitude
                    )
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
                Log.e(
                    "API Error",
                    "Error al enviar datos: ${response.code()} ${response.message()}"
                )
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
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION
        )
    }

}
