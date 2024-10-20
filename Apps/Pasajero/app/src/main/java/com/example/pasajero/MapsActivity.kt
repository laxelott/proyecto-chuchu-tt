package com.example.pasajero

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.strictmode.WebViewMethodCalledOnWrongThreadViolation
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.pasajero.databinding.ActivityMapsBinding
import com.example.pasajero.interfaces.ApiHelper
import com.example.pasajero.interfaces.ApiService
import com.example.pasajero.interfaces.CustomInfoWindowAdapter
import com.example.pasajero.interfaces.DirectionsResponse
import com.example.pasajero.interfaces.GoogleDirectionsApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.*
import retrofit2.awaitResponse
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var btnLocation: ImageView
    private lateinit var btnCancelTravel: ImageView
    private lateinit var adapter: BusStopAdapter
    private lateinit var stationsButton: ImageView
    private var routeID: Int = 0
    private var busStops: List<BusStop> = listOf()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private var busStopMarkers: MutableMap<BusStop, Marker> = mutableMapOf()
    private val driverMarkers = mutableMapOf<String, Marker>()
    private lateinit var coroutineScope: CoroutineScope
    private val polylines: MutableList<Polyline> = mutableListOf()
    private var isLocationUpdatesActive = false
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                currentLocation = LatLng(location.latitude, location.longitude)
            }
        }
    }

    //Variables to ask for the location permission
    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0, 0.0)
    private lateinit var progressBar: ProgressBar
    private lateinit var background: LinearLayout
    private var sendingDataJob: Job? = null
    private var gettingIncidentsJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        btnLocation = findViewById(R.id.location_button)
        btnCancelTravel = findViewById(R.id.btn_cancel_travel)
        // Get all the busStops from the line selected
        routeID = intent.getIntExtra("routeID", 0)
        Log.d("Transport response", "Datos de transporte: $routeID")
        val service = ApiHelper().prepareApi()
        updateCurrentLocation()
        getDriversLocations()
        startGettingIncidents()
        fetchBusStops(service)
        // Configurar CoroutineScope
        coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        progressBar = findViewById(R.id.progress_bar)
        background = findViewById(R.id.ownBackground)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsAPI = retrofit.create(GoogleDirectionsApi::class.java)


    }

    private fun startGettingIncidents() {
        gettingIncidentsJob?.cancel()
        gettingIncidentsJob = coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    getIncidentsList(routeID)
                }
                delay(5000)
            }
        }
    }

    private suspend fun getIncidentsList(routeID: Int) {
        try {
            val response = ApiHelper().prepareApi().getIncidentsList(routeID)
            if (response.isSuccessful) {
                Log.d("API Response", "Datos enviados correctamente")
                // Crear marker
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

    override fun onDestroy() {
        super.onDestroy()
        stopJobs()
    }

    private fun getDriversLocations() {
        if (!::coroutineScope.isInitialized) {
            coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        }

        sendingDataJob?.cancel()
        sendingDataJob = coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    getDriverLocation(busStops)
                }
                delay(5000)
            }
        }
    }


    private fun stopJobs() {
        sendingDataJob?.cancel()
        sendingDataJob = null
        gettingIncidentsJob?.cancel()
        gettingIncidentsJob = null
    }

    private suspend fun getDriverLocation(busStops: List<BusStop>) {
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.getDriversLocations(routeID) },
            processResponse = { response ->
                val locations = response.body()
                Log.d("Response", "$locations")
                if (locations != null) {
                    val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                    for (location in locations) {
                        Log.d("Response", "Creando marker para ${location.identifier}")
                        val newPosition = LatLng(location.lat, location.lon)
                        val existingMarker = driverMarkers[location.identifier]
                        if (existingMarker != null) {
                            existingMarker.position = newPosition
                            val nearestBusStop = findNearestBusStop(currentLatLng, busStops)

//                            if (nearestBusStop != null) {
//                                showInfoWindow(existingMarker, nearestBusStop)
//                            }
                        } else {
                            val markerIcon = getMarkerIconFromDrawable(R.drawable.driverlocation)
                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(newPosition)
                                    .icon(markerIcon)
                            )
                            marker?.let {
                                it.tag = "driverLocation"
                                driverMarkers[location.identifier] = it
                                val nearestBusStop = findNearestBusStop(currentLatLng, busStops)
//                                if (nearestBusStop != null) {
//                                    showInfoWindow(marker, nearestBusStop)
//                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun showInfoWindow(marker: Marker, eta: Double, busStop: BusStop) {
        val etaText = String.format("%.2f minutos", eta)
        Log.d("Response", etaText)
        marker.showInfoWindow()
        marker.title = "A $etaText de ${busStop.name}"
    }


    private fun findNearestBusStop(userLocation: LatLng, busStops: List<BusStop>): BusStop? {
        var nearestBusStop: BusStop? = null
        var minDistance = Double.MAX_VALUE

        for (busStop in busStops) {
            val busStopLocation = LatLng(busStop.latitude, busStop.longitude)
            val distance = calculateDistance(userLocation, busStopLocation)

            if (distance < minDistance) {
                minDistance = distance.toDouble()
                nearestBusStop = busStop // Guardar el BusStop más cercano
            }
        }

        return nearestBusStop // Retornar el BusStop más cercano
    }


    private fun getMarkerIconFromDrawable(drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableId)
            ?: throw IllegalArgumentException("Drawable not found: $drawableId")
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun fetchBusStops(service: ApiService) {
        ApiHelper().getDataFromDB(
            serviceCall = { service.getBusStopsInfo(routeID) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val busStopsInfo = response.body()
                if (busStopsInfo != null) {
                    busStops = busStopsInfo
                    Log.d("Transport response", "BusStops: $busStops")
                    runOnUiThread {
                        setupMapMarkersAndRoutes()
                        setupRecyclerView()
                    }
                }
            }
        )
    }

    private fun setupMapMarkersAndRoutes() {
        if (!::mMap.isInitialized) return
        // Clear old polylines
        clearOldRoutes()
        // Creating the markers and routes
        createRoutes()
    }

    private fun clearOldRoutes() {
        // Remove all polylines from the map
        for (polyline in polylines) {
            polyline.remove()
        }
        // Clear the list
        polylines.clear()
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

    // Add a marker
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
        marker?.tag = "busStop"
        if (marker != null) {
            busStopMarkers[busStop] = marker
        }
    }

    private fun latLangToStr(latLng: LatLng) = "${latLng.latitude},${latLng.longitude}"

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


    private fun setupRecyclerView() {
        binding.rvEstaciones.layoutManager = LinearLayoutManager(this)
        adapter = BusStopAdapter(busStops, mMap, binding, busStopMarkers)
        binding.rvEstaciones.adapter = adapter
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
        searchBarForBusStops()
        setupInfoWindowAdapter()
        setupMarkerClickListeners()
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

    private fun searchBarForBusStops() {
        // Show and hide all the stations
        btnLocation = findViewById(R.id.location_button)
        stationsButton = findViewById(R.id.showStations)

        stationsButton.setOnClickListener {
            if (binding.rvEstaciones.isVisible) binding.rvEstaciones.visibility =
                View.GONE else binding.rvEstaciones.visibility = View.VISIBLE
        }

        binding.searchBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Mostrar el RecyclerView
                binding.rvEstaciones.visibility = View.VISIBLE
            }
        }

        binding.searchBar.setOnClickListener {
            binding.rvEstaciones.visibility = View.VISIBLE
        }

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                binding.rvEstaciones.visibility = View.VISIBLE
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                filtrate(p0.toString())
            }

        })
    }

    private fun filtrate(text: String) {
        val listFiltrate = ArrayList<BusStop>()

        busStops.forEach {
            if (it.name.lowercase().contains(text.lowercase())) {
                listFiltrate.add(it)
            }
        }
        adapter.filtrar(listFiltrate)
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun setupInfoWindowAdapter() {
        val adapter = CustomInfoWindowAdapter(this)
        mMap.setInfoWindowAdapter(adapter)
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun setupMarkerClickListeners() {
        mMap.setOnMarkerClickListener { marker ->
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f))
            marker.showInfoWindow()
            true
        }

        mMap.setOnInfoWindowClickListener { marker ->
            CoroutineScope(Dispatchers.IO).launch {
                findBusStopForMarker(marker)?.let {
                    goToBusStop(marker, busStops, progressBar)
                }
            }
        }
    }

    private suspend fun findBusStopForMarker(marker: Marker): BusStop? {
        return withContext(Dispatchers.Main) {
            busStops.find { busStop ->
                val markerDetails = busStopMarkers[busStop] ?: return@find false
                val markerPosition = markerDetails.position
                markerPosition == marker.position
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun goToBusStop(
        marker: Marker,
        busStops: List<BusStop>,
        progressBar: ProgressBar
    ) {
        val thresholdDistance = 10f
        withContext(Dispatchers.Main) {
            try {
                showProgressBar(progressBar)

                val markerPosition = marker.position
                val currentLatLng = LatLng(currentLocation.latitude, currentLocation.longitude)

                val distanceToMarker = calculateDistance(currentLatLng, markerPosition)

                if (distanceToMarker <= thresholdDistance) {
                    AlertDialog.Builder(this@MapsActivity)
                        .setTitle("¡Espera!")
                        .setMessage("No puede iniciar un viaje a tu estación que está a $distanceToMarker metros de ti. Selecciona otra estación")
                        .setNegativeButton("Okay", null)
                        .show()

                    hideProgressBar(progressBar)
                    return@withContext
                } else {
                    val (busStop, minDistance) = calculateAndShowClosestBusStop(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        busStops
                    ).await()

                    busStop?.let {
                        if (minDistance <= thresholdDistance) {
                            busStopsToBeVisited(it, marker, busStops)
                        } else {
                            showBusStopDialog(it, minDistance.toFloat())
                        }
                    } ?: run {
                        Log.e("DirectionsError", "No se encontraron rutas")
                    }
                }


            } catch (e: Exception) {
                Log.e("DirectionsError", "Error: ${e.message}")
            } finally {
                hideProgressBar(progressBar)
            }
        }
    }


    private suspend fun showProgressBar(progressBar: ProgressBar) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            background.visibility = View.VISIBLE
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun calculateAndShowClosestBusStop(
        currentLat: Double,
        currentLon: Double,
        busStops: List<BusStop>
    ): Deferred<Pair<BusStop?, Int>> = GlobalScope.async {

        val apiKey = getString(R.string.api_key)
        val origin = "$currentLat,$currentLon"
        //val origin = "19.4978416589043,-99.1364507294133" // New York City

        val destinationChunks = busStops.chunked(25) { chunk ->
            chunk.joinToString("|") { "${it.latitude},${it.longitude}" }
        }

        var minDistance = Int.MAX_VALUE
        var closestBusStop: BusStop? = null

        val service = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DistanceMatrixService::class.java)

        for (destinations in destinationChunks) {
            val response = service.getDistanceMatrix(
                origins = origin,
                destinations = destinations,
                apiKey = apiKey,
                mode = "walking"
            )
            if (response.isSuccessful) {
                val distanceMatrix = response.body()
                Log.e("DirectionsError", "$distanceMatrix")
                val distances = distanceMatrix?.rows?.firstOrNull()?.elements

                if (distances != null) {
                    for ((i, element) in distances.withIndex()) {
                        val distance = element.distance.value
                        if (distance < minDistance) {
                            minDistance = distance
                            closestBusStop =
                                busStops[i + destinationChunks.indexOf(destinations) * 25]
                        }
                    }
                }
            } else {
                Log.e("DirectionsError", "Error: ${response.errorBody()?.string()}")
            }
        }

        Pair(closestBusStop, minDistance)
    }

    private fun showBusStopDialog(busStop: BusStop, distance: Float) {
        AlertDialog.Builder(this@MapsActivity)
            .setTitle("Ve a la estación más cercana")
            .setMessage("La estación más cercana es '${busStop.name}' y está a ${distance.toInt()} metros.")
            .setNegativeButton("Okay", null)
            .show()
    }

    private suspend fun busStopsToBeVisited(
        busStopOrigin: BusStop,
        marker: Marker,
        busStops: List<BusStop>
    ) {
        clearMarkers()
        val busStopsOD = getBusStopsToTravel(busStopOrigin, marker, busStops)
        createNewMarkers(busStopsOD)
        drawRoutesAndShowTimes(busStopsOD, marker)
    }

    private fun updateCurrentLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        if (isLocationPermissionGranted()) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Log.e("LocationError", "Permisos de ubicación no concedidos.")
        }
    }

    private suspend fun getBusStopsToTravel(
        busStopOrigin: BusStop,
        marker: Marker,
        busStops: List<BusStop>
    ): MutableList<BusStop> {
        return withContext(Dispatchers.Main) { // Switch to Main thread
            val busStopsOD = mutableListOf<BusStop>()
            var flag = false
            var i = busStops.indexOf(busStopOrigin)

            while (true) {
                val busStopLatLng = LatLng(busStops[i].latitude, busStops[i].longitude)
                if (busStopLatLng == LatLng(busStopOrigin.latitude, busStopOrigin.longitude)) {
                    flag = true
                }
                if (flag) {
                    busStopsOD.add(busStops[i])
                }
                if (flag && busStopLatLng == marker.position) {
                    break
                }
                i = (i + 1) % busStops.size
                Log.d("Waypoints", "$i")
                Log.d("Waypoints", "$busStopLatLng")
                Log.d("Waypoints", "${marker.position}")
                Log.d("Waypoints", "--------------------")

            }
            busStopsOD
        }
    }

    private fun clearMarkers() {
        for ((busStop, marker) in busStopMarkers) {
            marker.remove()
        }
        busStopMarkers.clear()
    }

    private fun createNewMarkers(busStops: List<BusStop>) {
        for (busStop in busStops){
            createMarker(busStop)
        }
    }


    private suspend fun drawRoutesAndShowTimes(busStopsOD: List<BusStop>, marker: Marker) {
        withContext(Dispatchers.Main) {
            marker.hideInfoWindow()
            clearOldRoutes()
            hideProgressBar(progressBar)
            hideSearchBarAndShowDialog()

            val origin = busStopsOD.first()
            val destination = busStopsOD.last()
            val waypoints = gettingWaypointsFromDestinations(busStopsOD)

            Log.e("Waypoints", "Lista: $waypoints")
            getRoute(origin, destination, waypoints)
            val (totalDist, totalTime) = getTotalTime(origin, destination, waypoints).await()

            val travelTimes = mutableMapOf<BusStop, Pair<String, String>>()
            var currentStopIndex = 0
            var alertFinalDestinationShown = false

            while (currentStopIndex < busStopsOD.size - 1) {
                val busStopDestination = busStopsOD[currentStopIndex + 1]
                val busStopOrigin = busStopsOD[currentStopIndex]

                if (destination == busStopDestination) {
                    alertUserForFinalDestination()
                    alertFinalDestinationShown = true
                }

                // Obtener distancia y duración
                val (distance, duration) = travelTimes[busStopDestination] ?: run {
                    val (dist, dur) = getDistanceAndTime(busStopOrigin, busStopDestination).await()
                    travelTimes[busStopDestination] = Pair(dist, dur)
                    Pair(dist, dur)
                }

                var hasArrived = false
                while (!hasArrived) {
                    delay(5000)
                    val currentDistance = calculateDistance(
                        LatLng(
                            currentLocation.latitude,
                            currentLocation.longitude
                        ), LatLng(busStopDestination.latitude, busStopDestination.longitude)
                    )

                    hasArrived =
                        updateTravelInfo(busStopDestination, currentDistance, duration, totalTime)

                    if (alertFinalDestinationShown && hasArrived) {
                        showAlertDialog()
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
        val searchBar = findViewById<LinearLayout>(R.id.searchContainer)
        container.visibility = View.GONE
        searchBar.visibility = View.VISIBLE

        val cameraPosition = CameraPosition.Builder()
            .target(currentLocation)
            .zoom(16f)
            .bearing(0f)
            .tilt(0f)
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        clearOldRoutes()
        createRoutes()
        stopLocationUpdates()
        btnCancelTravel.visibility = View.GONE
    }

    private fun showAlertDialog() {
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
        ) // Vibrar por 1 segundo

        // Reproducir sonido de alerta
//        val mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound) // Asegúrate de tener el archivo en res/raw
//        mediaPlayer.start()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Preparate para bajar!")
            .setMessage("La siguiente estación es tu destino.")
            .setPositiveButton("Okay") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
        builder.create().show()
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

    private fun updateTravelInfo(
        busStopDestination: BusStop,
        currentDistance: Float,
        duration: String,
        totalTime: String
    ): Boolean {
        val title = findViewById<TextView>(R.id.traveling_container_title)
        val showStop = findViewById<TextView>(R.id.traveling)
        val timer = findViewById<TextView>(R.id.traveling_container_timer)
        val totalTimer = findViewById<TextView>(R.id.traveling_container_total_time)

        return if (currentDistance < 10f) {
            showStop.text = "Estás en"
            title.text = "${busStopDestination.name}"
            totalTimer.text = totalTime
            true
        } else {
            showStop.text = "Siguiente estación"
            title.text = "${busStopDestination.name}"
            timer.text = duration
            totalTimer.text = totalTime
            false
        }
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

    private fun hideSearchBarAndShowDialog() {
        val container = findViewById<LinearLayout>(R.id.traveling_container)
        val searchBar = findViewById<LinearLayout>(R.id.searchContainer)
        container.visibility = View.VISIBLE
        searchBar.visibility = View.GONE
        btnCancelTravel.visibility = View.VISIBLE

        btnCancelTravel.setOnClickListener {
            showAlertDialog(container, searchBar)
        }
    }

    private fun showAlertDialog(container: LinearLayout, searchBar: LinearLayout) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Finalizar viaje")
            .setMessage("¿Quieres finalizar tu viaje?")
            .setPositiveButton("Si") { dialog, _ ->
                container.visibility = View.GONE
                searchBar.visibility = View.VISIBLE

                val cameraPosition = CameraPosition.Builder()
                    .target(currentLocation)
                    .zoom(16f)
                    .bearing(0f)
                    .tilt(0f)
                    .build()

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                clearOldRoutes()
                createRoutes()
                stopLocationUpdates()
                btnCancelTravel.visibility = View.GONE
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) return

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
    }

    private fun getTotalTime(
        origin: BusStop,
        destination: BusStop,
        waypoints: String
    ): Deferred<Pair<String, String>> {
        val apiKey = getString(R.string.api_key)
        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        Log.d("Waypoints", "Ubicacion origen: $origin")
        Log.d("Waypoints", "Ubicacion destino: $destination")

        val deferredResult = CompletableDeferred<Pair<String, String>>()

        val call = directionsAPI.getDirections(
            origin = originLocation,
            destination = destinationLocation,
            apiKey = apiKey,
            mode = "driving",
            waypoints = waypoints
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val directionsResponse = response.body()

                    if (directionsResponse?.routes.isNullOrEmpty()) {
                        deferredResult.complete(Pair("Unknown distance", "Unknown duration"))
                        return
                    }

                    val leg = directionsResponse?.routes?.get(0)?.legs?.get(0)

                    // Extract distance and duration
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

    private fun getDistanceAndTime(
        origin: BusStop,
        destination: BusStop
    ): Deferred<Pair<String, String>> {
        val apiKey = getString(R.string.api_key)
        val waypoints = destination.waypoints
        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        val deferredResult = CompletableDeferred<Pair<String, String>>()

        val call = directionsAPI.getDirections(
            originLocation,
            destinationLocation,
            apiKey,
            "driving",
            waypoints = waypoints
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

    private suspend fun hideProgressBar(progressBar: ProgressBar) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            background.visibility = View.GONE
        }
    }


    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true

            fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    lastLocation = location
                    currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(
                this,
                "Habilita los permisos de localización en ajustes",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }
    }

    //metodo para capturar la respuesta de que el usuario acepto los permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                //Go to my current location
                fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                    if (location != null) {
                        lastLocation = location
                        currentLocation = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    "Ve a ajustes para aceptar los permisos de localizacion",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
    }

    //Metodo para comprobar que los permisos siguen activos despues de que el usuario dejo la aplicacion en background
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::mMap.isInitialized) return
        if (!isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = false
            Toast.makeText(
                this,
                "Ve a ajustes para aceptar los permisos de localizacion",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


}