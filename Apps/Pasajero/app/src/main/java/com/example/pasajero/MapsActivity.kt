package com.example.pasajero

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.example.pasajeropackage.MarkerTag
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
    private var driverMarkers = mutableMapOf<DriverLocation, Marker>()
    private var incidentsMarkers = mutableMapOf<Incident, Marker>()
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

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0, 0.0)
    private lateinit var progressBar: ProgressBar
    private lateinit var background: LinearLayout
    private var gettingDriversJob: Job? = null
    private var gettingIncidentsJob: Job? = null
    private var gettingInfoJob: Job? = null
    private var isMarkerSelected = false
    private var infoWindowMode: InfoMode = InfoMode.INACTIVE;
    private var busStopsODGlobal: List<BusStop>? = null
    private var alertShown = false
    private var hasDrawnRoutes = false
    private var alertErrorFlag = false
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private val REQUEST_CODE_LOCATION = 1002
    private var isNotificationPermissionRequested = false  // Control para manejar permisos de notificación
    private var isLocationPermissionRequested = false  // Control para manejar permisos de ubicación

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Iniciar el flujo de solicitud de permisos
        requestPermissionsFlow()
        btnLocation = findViewById(R.id.location_button)
        btnCancelTravel = findViewById(R.id.btn_cancel_travel)
        // Get all the busStops from the line selected
        routeID = intent.getIntExtra("routeID", 0)
        Log.d("Transport response", "Datos de transporte: $routeID")
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
        if (!::coroutineScope.isInitialized) {
            coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        }

        gettingIncidentsJob?.cancel()
        gettingIncidentsJob = coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    getIncidentsList(this@MapsActivity, routeID)
                }
                delay(1000)
            }
        }
    }

    private fun getBitmapFromDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private suspend fun getIncidentsList(context: Context, routeID: Int) {
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.getIncidentsList(routeID) },
            processResponse = { response ->
                val incidentsList = response.body()
                var newMarkers = mutableMapOf<Incident, Marker>()
                incidentsList?.let {
                    for (incident in it) {
                        val existingMarker = incidentsMarkers.keys.any { existingIncident ->
                            existingIncident.lat == incident.lat && existingIncident.lon == incident.lon
                        }
                        if (existingMarker) {
                            if (incidentsMarkers.containsKey(incident)) {
                                newMarkers[incident] = incidentsMarkers[incident]!!
                            }
                            incidentsMarkers.remove(incident)
                        } else {
                            Log.d("API Response", "Datos recibidos correctamente")
                            Log.d("API Response", "Creando marker para: ${incident.name}")
                            Log.d(
                                "API Response",
                                "Latitude: ${incident.lat} Longitude: ${incident.lon}"
                            )
                            val location = LatLng(incident.lat, incident.lon)

                            val bitmap = getBitmapFromDrawable(context, R.drawable.traffic_sign)
                            if (bitmap == null) {
                                Log.e(
                                    "Bitmap Error",
                                    "El bitmap es nulo, no se puede crear el marcador."
                                )
                            } else {
                                Log.d("Bitmap Status", "Bitmap creado correctamente.")
                                val resizedBitmap =
                                    Bitmap.createScaledBitmap(bitmap, 200, 200, false)
                                val markerIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

                                // Agregar el marcador al mapa en el hilo principal
                                val marker = mMap.addMarker(
                                    MarkerOptions()
                                        .position(location)
                                        .title(incident.name)
                                        .icon(markerIcon)
                                )

                                marker?.tag = MarkerTag(type = "incident", mode = infoWindowMode)

                                if (marker != null) {
                                    Log.d(
                                        "Marker Creation",
                                        "Marcador creado exitosamente: ${incident.name}"
                                    )
                                    newMarkers[incident] = marker
                                } else {
                                    Log.e(
                                        "Marker Creation Error",
                                        "No se pudo crear el marcador para: ${incident.name}"
                                    )
                                }
                            }
                        }
                    }
                    clearMarkers("incident")
                    incidentsMarkers = newMarkers
                } ?: Log.e("API Error", "La lista de incidentes es nula")
            }
        )
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        val intent = Intent(this, MyService::class.java)
        stopService(intent)
        stopJobs()
    }

    private fun getDriversLocations() {
        if (!::coroutineScope.isInitialized) {
            coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        }

        gettingDriversJob?.cancel()
        gettingDriversJob = coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    getDriverLocation()
                }
                delay(1000)
            }
        }
    }


    private fun stopJobs() {
        gettingDriversJob?.cancel()
        gettingDriversJob = null
        gettingIncidentsJob?.cancel()
        gettingIncidentsJob = null
        endInfoJob()
    }

    private fun animateMarker(marker: Marker, newPosition: LatLng, newRotation: Float) {
        val startPosition = marker.position
        val endPosition = newPosition
        val duration: Long = 1000 // Duration in milliseconds

        val startLat = startPosition.latitude
        val startLng = startPosition.longitude
        val endLat = endPosition.latitude
        val endLng = endPosition.longitude

        // Start and end rotation angles
        val startRotation = marker.rotation
        val endRotation = newRotation
        var deltaRotation = endRotation - startRotation

        // Adjust for the shortest rotation direction
        if (deltaRotation > 180) {
            deltaRotation -= 360
        } else if (deltaRotation < -180) {
            deltaRotation += 360
        }

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float

            // Animate position
            val newLat = startLat + (endLat - startLat) * fraction
            val newLng = startLng + (endLng - startLng) * fraction
            marker.position = LatLng(newLat, newLng)

            // Animate rotation to take the shortest path
            val newRotationValue = startRotation + deltaRotation * fraction
            marker.rotation = newRotationValue
        }
        valueAnimator.duration = duration
        valueAnimator.start()
    }


    private suspend fun getDriverLocation() {
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.getDriversLocations(routeID) },
            processResponse = { response ->
                val driversList = response.body()

                val currentDriverIdentifiers =
                    driversList?.map { it.identifier }?.toSet() ?: emptySet()

                val driversToRemove = driverMarkers.keys.filter { driver ->
                    !currentDriverIdentifiers.contains(driver.identifier)
                }

                for (driver in driversToRemove) {
                    driverMarkers[driver]?.remove()
                    driverMarkers.remove(driver)
                }

                driversList?.forEach { driver ->
                    val existingMarker = driverMarkers[driver]
                    if (existingMarker != null) {

                        val newLocation = LatLng(driver.lat, driver.lon)
                        animateMarker(existingMarker, newLocation, driver.direction)
                    } else {

                        val location = LatLng(driver.lat, driver.lon)
                        val markerIcon = getMarkerIconFromDrawable(R.drawable.driverlocation)
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(location)
                                .title(driver.identifier)
                                .icon(markerIcon)
                                .anchor(0.0f, 0.8f)
                                .rotation(driver.direction)
                                .flat(true)
                        )

                        marker?.tag = MarkerTag(type = "driverLocation", mode = infoWindowMode)

                        marker?.let {
                            driverMarkers[driver] = it
                        } ?: Log.e(
                            "Marker Creation Error",
                            "No se pudo crear el marcador para: ${driver.identifier}"
                        )
                    }
                }
            }
        )
    }


    private fun getMarkerIconFromDrawable(drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableId)
            ?: throw IllegalArgumentException("Drawable not found: $drawableId")
        val bitmap = Bitmap.createBitmap(230, 230, Bitmap.Config.ARGB_8888)
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
        marker?.tag = MarkerTag(type = "busStop", mode = infoWindowMode)
        if (marker != null) {
            busStopMarkers[busStop] = marker
        }
    }
    // Add a marker with color
    private fun createMarker(busStop: BusStop, markerColor: Int) {
        val location = LatLng(busStop.latitude, busStop.longitude)
        val originalBitmap = BitmapFactory.decodeResource(this.resources, R.mipmap.ic_bus_station)
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 100, 100, false)
        val outlineWidth = 20f

        // Create a new bitmap with extra space for the outline
        val outlineBitmap = Bitmap.createBitmap(
            resizedBitmap.width + outlineWidth.toInt() * 2,
            resizedBitmap.height + outlineWidth.toInt() * 2,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(outlineBitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = markerColor
            style = Paint.Style.STROKE
            strokeWidth = outlineWidth
        }

        // Draw the outline (circle or rectangle depending on your needs)
        val rect = RectF(
            outlineWidth / 2,
            outlineWidth / 2,
            canvas.width - outlineWidth / 2,
            canvas.height - outlineWidth / 2
        )
        canvas.drawOval(rect, paint)

        // Draw the original bitmap centered inside the outline
        canvas.drawBitmap(resizedBitmap, outlineWidth, outlineWidth, null)

        // Create a marker icon from the modified bitmap
        val markerIcon = BitmapDescriptorFactory.fromBitmap(outlineBitmap)
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(busStop.name)
                .icon(markerIcon)
        )

        // Attach metadata to the marker and store it in your map
        marker?.tag = MarkerTag(type = "busStop", mode = infoWindowMode)
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
        updateCurrentLocation()
        setupLocationButton()
        searchBarForBusStops()
        setupInfoWindowAdapter()
        lifecycleScope.launch {
            setupMarkerClickListeners()
        }

        mMap.setOnMapLoadedCallback {
            // Ejecuta las llamadas a la API después de que el mapa esté completamente cargado
            lifecycleScope.launch {
                fetchDataInBackground()  // Llama a la función que ejecuta las llamadas a la API
            }
        }
    }

    private suspend fun fetchDataInBackground() = withContext(Dispatchers.IO) {
        // Inicializa tu servicio y realiza las llamadas a la API en segundo plano
        val service = ApiHelper().prepareApi()
        fetchBusStops(service)
        getDriversLocations()
        startGettingIncidents()
    }


    private fun setupMap() {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.custom_map_style))
        with(mMap.uiSettings) {
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
            isZoomControlsEnabled = false
            isCompassEnabled = false
            mMap.isBuildingsEnabled = false
            mMap.uiSettings.isTiltGesturesEnabled = false
        }
    }

    private fun setupLocationButton() {
        btnLocation.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
        }
    }

    private fun searchBarForBusStops() {
        btnLocation = findViewById(R.id.location_button)
        stationsButton = findViewById(R.id.showStations)

        stationsButton.setOnClickListener {
            if (binding.rvEstaciones.isVisible) binding.rvEstaciones.visibility =
                View.GONE else binding.rvEstaciones.visibility = View.VISIBLE
        }

        binding.searchBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
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

    private suspend fun getInfoJob(busStops: List<BusStop>, driverIdentifier: String) {
        if (!::coroutineScope.isInitialized) {
            coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        }

        // replace fragment according to mode
        val fragment: Fragment = when (infoWindowMode) {
            InfoMode.INACTIVE -> {
                Log.d("Debug", "Targeted")
                InfoFragment()
            }

            InfoMode.TARGETED -> {
                Log.d("Debug", "Targeted")
                InfoFragment()
            }

            InfoMode.WAITING -> {
                clearOldRoutes()
                Log.d("Debug", "Waiting")
                StartTravelFragment()
            }

            InfoMode.ON_ROUTE -> {
                Log.d("Debug", "On route")
                TravelFragment()
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.info_fragment_container, fragment)
            .commit()
        findViewById<FrameLayout>(R.id.info_fragment_container).visibility = View.VISIBLE

        // Cancel any existing job before starting a new one.
        gettingInfoJob?.cancel()
        gettingInfoJob = coroutineScope.launch {
            while (true) {
                if (infoWindowMode == InfoMode.INACTIVE) {
                    findViewById<FrameLayout>(R.id.info_fragment_container).visibility = View.GONE
                    break
                } else {
                    getInfo(busStops, driverIdentifier)
                    delay(1000)
                }
            }
        }
    }

    private suspend fun getInfo(busStops: List<BusStop>, driverIdentifier: String) {

        val service = withContext(Dispatchers.IO) {
            ApiHelper().prepareApi()
        }

        if (infoWindowMode == InfoMode.INACTIVE) {
            throw Exception("Invalid InfoMode REQUEST")
        }
        busStopsODGlobal = busStops
        var serviceCall: suspend () -> Response<List<InfoResponse>>
        serviceCall = when (infoWindowMode) {
            InfoMode.INACTIVE -> {
                { -> service.getWaitTime(routeID, busStops[0].id) }
            }

            InfoMode.TARGETED -> {
                { ->
                    service.getWaitTimeForVehicle(
                        routeID,
                        findClosestBusStop(currentLocation).id,
                        driverIdentifier
                    )
                }
            }

            InfoMode.WAITING -> {
                { -> service.getWaitTime(routeID, busStops[0].id) }
            }

            InfoMode.ON_ROUTE -> {
                { ->
                    service.getWaitTimeForVehicle(
                        routeID,
                        busStops.last().id,
                        driverIdentifier
                    )
                }
            }
        }

        ApiHelper().getDataFromDB(
            serviceCall = serviceCall,
            processResponse = { response ->
                val waitTime = response.body()
                Log.d("Info", findClosestBusStop(currentLocation).name)
                Log.d("Info", "$waitTime")

                if (waitTime == null) {
                    Log.d("Info", "Error en la respuesta para la informacion del conductor")
                    return@getDataFromDB
                }

                Log.d("Info", "Informacion del conductor recibida correctamente")
                val info = waitTime[0]
                if (info.error == 1) {
                    if (!alertErrorFlag) {
                        // Show error dialog on the Main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            AlertDialog.Builder(this@MapsActivity)
                                .setTitle("¡Error!")
                                .setMessage("IDs inválidas")
                                .setNegativeButton("Cerrar", null)
                                .show()
                        }
                        alertErrorFlag = true
                        return@getDataFromDB
                    }
                }

                if (info.error == 3) {
                    if (!alertErrorFlag) {
                        // Show error dialog on the Main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            AlertDialog.Builder(this@MapsActivity)
                                .setTitle("¡Espera!")
                                .setMessage("No hay vehículos en ruta")
                                .setNegativeButton("Cerrar", null)
                                .show()
                        }
                        alertErrorFlag = true
                        return@getDataFromDB
                    }
                }

                Log.d("Info", "Informacion: $info")
                Log.d("Transport response", "Tiempos de conductor")
                // Update the UI fragment on the Main thread
                CoroutineScope(Dispatchers.Main).launch {
                    Log.d("debug", progressBar.toString())
                    hideProgressBar(progressBar)

                    if (infoWindowMode == InfoMode.INACTIVE) {
                        return@launch
                    }

                    var infoFragment: Fragment? = null
                    when (infoWindowMode) {
                        InfoMode.INACTIVE -> {
                            infoFragment =
                                supportFragmentManager.findFragmentById(R.id.info_fragment_container)
                        }

                        InfoMode.TARGETED -> {
                            infoFragment =
                                supportFragmentManager.findFragmentById(R.id.info_fragment_container)
                        }

                        InfoMode.WAITING -> {
                            infoFragment = supportFragmentManager.findFragmentById(R.id.info_fragment_container)

                            if (!hasDrawnRoutes) {
                                drawRoutesAndShowTimes()
                                hasDrawnRoutes = true
                            }

                            if (infoFragment is StartTravelFragment) {
                                val startTravelFragment = infoFragment
                                val distanceCheck = calculateDistance(
                                    LatLng(currentLocation.latitude, currentLocation.longitude), LatLng(
                                        busStops[0].latitude, busStops[0].longitude))
                                startTravelFragment.setBusStop(busStops[0])
                                if (info.arrived == 1 && distanceCheck < 20f) {
                                    startTravelFragment.enableStartButton()
                                } else {
                                    startTravelFragment.disableStartButton()
                                }
                            }
                        }


                        InfoMode.ON_ROUTE -> {
                            infoFragment =
                                supportFragmentManager.findFragmentById(R.id.info_fragment_container)
                            showDropAndDestination(busStops, info)
                        }
                    }
                    Log.d("Info", "infoFragment type: ${infoFragment!!::class.java}")
                    if (infoFragment is InfoFragmentInterface) {
                        infoFragment.updateData(info) // Call updateData on the InfoFragment
                    }
                }
            }
        )
    }

    private fun showDropAndDestination(busStops: List<BusStop>, response: InfoResponse) {
        Log.d("Info", "bus stops on travel: ${busStops.size}")
        // Prepararse para bajar
        if (response.nextName == busStops.last().name && !alertShown) {
            alertUserForFinalDestination()
            alertShown = true
        } else if (response.arrived == 1 && alertShown) { // Bajaaan
            arrivalAlert()
            showNotification(this)
            endTravel() // Stop API call here
            alertShown = false
        }
    }

    fun showNotification(context: Context) {
        // Crear el canal de notificación (si aún no existe)
        createNotificationChannel(context)

        // Verificar si la versión es Android 13 o superior antes de solicitar el permiso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Verificar si el permiso de notificación está concedido
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Crear y mostrar la notificación
                createAndShowNotification(context)
            } else {
                // Solicitar el permiso si es necesario
                if (context is Activity) {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        } else {
            // En versiones anteriores a Android 13 no se necesita permiso de notificación
            createAndShowNotification(context)
        }
    }

    // Función para crear y mostrar la notificación
    private fun createAndShowNotification(context: Context) {
        val channelId = "mi_canal_id"
        val notificationId = 1

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.bus_logo)
            .setContentTitle("Nintrip")
            .setContentText("Prepárate para bajar")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            // Envolver en try-catch para manejar posibles SecurityException
            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            Log.e("NotificationError", "No se pudo mostrar la notificación: permiso no otorgado.")
        }
    }

    // Función para crear el canal de notificación
    private fun createNotificationChannel(context: Context) {
        val channelId = "mi_canal_id"
        val channelName = "Canal de Notificaciones"
        val channelDescription = "Descripción del canal de notificaciones"

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, channelName, importance).apply {
            description = channelDescription
        }
        // Registrar el canal en el sistema
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }




    @SuppressLint("PotentialBehaviorOverride")
    private suspend fun setupMarkerClickListeners() {
        mMap.setOnMarkerClickListener { marker ->
            (marker.tag as MarkerTag).mode = infoWindowMode
            Log.d("Debug", "Marker: ${marker.tag}, $infoWindowMode")
            if ((marker.tag as MarkerTag).type == "driverLocation" && (marker.tag as MarkerTag).mode == InfoMode.INACTIVE) {
                Log.d("Debug", "fragment for driver")
                isMarkerSelected = true
                CoroutineScope(Dispatchers.Main).launch {
                    val driverIdentifier = withContext(Dispatchers.Main) {
                        marker.title ?: "Unknown Title"
                    }

                    infoWindowMode = InfoMode.TARGETED
                    getInfoJob(busStops, driverIdentifier)
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 18f))
            marker.showInfoWindow()
            true
        }
        mMap.setOnInfoWindowClickListener { marker ->
            if (marker.tag.toString().indexOf("busStop") > -1 && (infoWindowMode == InfoMode.INACTIVE || infoWindowMode == InfoMode.TARGETED)) {
                CoroutineScope(Dispatchers.IO).launch {
                    findBusStopForMarker(marker)?.let {
                        goToBusStop(marker, busStops, progressBar)
                    }
                }
            }
        }


        mMap.setOnMapClickListener {
            if (isMarkerSelected && infoWindowMode == InfoMode.TARGETED) {
                val fragment =
                    supportFragmentManager.findFragmentById(R.id.info_fragment_container) as? InfoFragment
                fragment?.dismissFragment()
                endInfoJob()
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

                val closestBusStop = findClosestBusStop(currentLatLng)
                val distanceToMarker = calculateDistance(LatLng(closestBusStop.latitude, closestBusStop.longitude), markerPosition)

                if (distanceToMarker <= thresholdDistance) {
                    AlertDialog.Builder(this@MapsActivity)
                        .setTitle("¡Espera!")
                        .setMessage("${marker.title} es tu estación más cercana")
                        .setNegativeButton("Okay", null)
                        .show()

                    hideProgressBar(progressBar)
                    return@withContext
                }

                Log.d(
                    "Info",
                    "estacion mas cercana ${closestBusStop.name}, ${closestBusStop.id}"
                )
                val searchBar = findViewById<LinearLayout>(R.id.searchContainer)
                searchBar.visibility = View.GONE

                busStopsToBeVisited(closestBusStop, marker, busStops)


            } catch (e: Exception) {
                Log.e("DirectionsError", "Error: ${e.message}")
            } finally {
                hideProgressBar(progressBar)
            }
        }
    }

    private fun findClosestBusStop(currentLatLng: LatLng): BusStop {
        var closestBusStop: BusStop = busStops[0]
        var minDistance = calculateDistance(
            currentLatLng,
            LatLng(closestBusStop.latitude, closestBusStop.longitude)
        )
        for (busStop in busStops) {
            val distanceToStop = calculateDistance(
                currentLatLng,
                LatLng(busStop.latitude, busStop.longitude)
            )
            if (distanceToStop < minDistance) {
                minDistance = distanceToStop
                closestBusStop = busStop
            }
        }
        return closestBusStop;
    }


    private suspend fun showProgressBar(progressBar: ProgressBar) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            background.visibility = View.VISIBLE
        }
    }


    private suspend fun busStopsToBeVisited(
        busStopOrigin: BusStop,
        marker: Marker,
        busStops: List<BusStop>
    ) {
        Log.d("Debug", "Lista anterior: $busStopMarkers")
        clearMarkers("busStop")
        val busStopsOD = getBusStopsToTravel(busStopOrigin, marker, busStops)
        Log.d("Debug", "ESTACIONES: $busStopsOD")
        createNewMarkers(busStopsOD)
        Log.d("Debug", "Lista actualizada: $busStopMarkers")
        marker.hideInfoWindow()
        CoroutineScope(Dispatchers.Main).launch {
            infoWindowMode = InfoMode.WAITING
            getInfoJob(busStopsOD, "")
        }
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
                Log.d("Waypoints", "${busStops[i].id}")
                Log.d("Waypoints", "$busStopLatLng")
                Log.d("Waypoints", "${marker.position}")
                Log.d("Waypoints", "--------------------")

            }
            busStopsOD
        }
    }

    private fun clearMarkers(tipo: String) {
        when (tipo) {
            "busStop" -> {
                busStopMarkers.forEach { (_, marker) ->
                    marker.remove()
                }
            }

            "incident" -> {
                for ((incident, marker) in incidentsMarkers) {
                    marker.remove()
                }
            }

            "drivers" -> {
                for ((driver, marker) in driverMarkers) {
                    marker.remove()
                }
                driverMarkers.clear()
            }
        }
    }

    private fun createNewMarkers(busStops: List<BusStop>) {
        // Create first and last stops with custom colors
        createMarker(busStops.first(), Color.GREEN)
        createMarker(busStops.last(), Color.RED)

        // Remove first and last stops
        busStops.withIndex().filter { it.index != 0 && it.index != busStops.lastIndex }
        for ((i, busStop) in busStops.subList(1, busStops.size - 1).withIndex()) {
            Log.d("Debug", "Creando marker para: ${busStop.name}")
            createMarker(busStop)
        }

    }

    fun onStartTravelClicked(driverIdentifier: String) {
        CoroutineScope(Dispatchers.Main).launch {
            infoWindowMode = InfoMode.ON_ROUTE
            busStopsODGlobal?.let { getInfoJob(it, driverIdentifier) }
        }
    }

    private suspend fun drawRoutesAndShowTimes() {
        withContext(Dispatchers.Main) {
            hideProgressBar(progressBar)
            hideSearchBarAndShowDialog()

            require(busStopsODGlobal is List<BusStop>) { "Bus stop list is null!" }

            val origin = busStopsODGlobal!!.first()
            val destination = busStopsODGlobal!!.last()
            val waypoints = gettingWaypointsFromDestinations(busStopsODGlobal!!)

            Log.e("Waypoints", "Lista: $waypoints")
            getRoute(origin, destination, waypoints)

        }
    }

    private fun endTravel() {
        val searchBar = findViewById<LinearLayout>(R.id.searchContainer)
        searchBar.visibility = View.VISIBLE

        val cameraPosition = CameraPosition.Builder()
            .target(currentLocation)
            .zoom(16f)
            .bearing(0f)
            .tilt(0f)
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        clearOldRoutes()
        clearMarkers("busStop")
        createRoutes()
        endInfoJob()
        hasDrawnRoutes = false

        findViewById<FrameLayout>(R.id.info_fragment_container).visibility = View.GONE
        btnCancelTravel.visibility = View.GONE
    }

    private fun arrivalAlert() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¡Llegaste a tu destino!")
            .setMessage("Has llegado a tu destino, gracias por viajar con Nintrip :)")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                alertShown = false
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
        btnCancelTravel.visibility = View.VISIBLE
        btnCancelTravel.setOnClickListener {
            showAlertDialog()
        }
    }

    private fun showAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Finalizar viaje")
            .setMessage("¿Quieres finalizar tu viaje?")
            .setPositiveButton("Si") { dialog, _ ->
                endTravel()
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

    private suspend fun hideProgressBar(progressBar: ProgressBar) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            background.visibility = View.GONE
        }
    }

    // Iniciar el servicio en primer plano
    private fun startMyService() {
        val serviceIntent = Intent(this, MyService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }


    /*
    *
    * PERMISOS
    *
    * */
    // Flujo de permisos: Primero verificamos y pedimos el permiso de notificación
    private fun requestPermissionsFlow() {
        if (checkNotificationPermission()) {
            // Si ya tiene permiso de notificación, pasamos al permiso de ubicación
            checkAndRequestLocationPermission()
        } else {
            // Si no tiene permiso de notificación, pedimos el permiso
            requestNotificationPermission()
        }
    }

    // Verifica si el permiso de notificación está concedido
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No es necesario en versiones anteriores
        }
    }

    // Solicita el permiso de notificación
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isNotificationPermissionRequested = true
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Si no es necesario el permiso explícito, pasamos al permiso de ubicación
            showNotification()
            checkAndRequestLocationPermission()
        }
    }

    // Verifica si el permiso de ubicación está concedido
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Solicita el permiso de ubicación
    private fun requestLocationPermission() {
        isLocationPermissionRequested = true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION
        )
    }

    // Verifica si el permiso de ubicación está concedido y lo solicita si no lo está
    private fun checkAndRequestLocationPermission() {
        if (isLocationPermissionGranted()) {
            enableLocation()
        } else {
            if (isLocationPermissionRequested) {
                // Solo muestra el Toast si ya se ha solicitado anteriormente y el permiso aún está denegado
                Toast.makeText(this, "Permiso de localización denegado", Toast.LENGTH_SHORT).show()
                // Luego, pedimos al usuario que habilite el permiso de ubicación
                showLocationPermissionDeniedDialog()
            } else {
                // Solicita el permiso de ubicación si no se ha solicitado antes
                requestLocationPermission()
            }
        }
    }

    // Activa la ubicación en el mapa si el permiso está concedido
    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true
            getLastLocation()
        }
    }

    // Muestra una notificación simple si el permiso está concedido
    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "mi_canal_id"
            val channelName = "Notificaciones de mi aplicación"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, "mi_canal_id")
            .setSmallIcon(R.drawable.bus_logo)
            .setContentTitle("Título de la notificación")
            .setContentText("Contenido de la notificación")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(1, notification)
    }

    // Muestra un diálogo si el permiso de notificación ha sido denegado
    private fun showNotificationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Notificación Requerido")
            .setMessage("Para recibir notificaciones, habilita el permiso en los ajustes de la aplicación.")
            .setPositiveButton("Ir a ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /*
     * Funciones de ubicación
     */

    // Obtiene la última ubicación del usuario si el permiso está concedido
    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                currentLocation = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
            }
        }
    }

    // Muestra un diálogo si el permiso de ubicación ha sido denegado
    private fun showLocationPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permiso de Ubicación Requerido")
            .setMessage("Para usar la ubicación, habilita el permiso en los ajustes de la aplicación.")
            .setPositiveButton("Ir a ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Manejador del resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El usuario concedió el permiso de notificación
                    showNotification()
                    checkAndRequestLocationPermission()
                } else {
                    // Si no concedió el permiso, mostramos el diálogo para que lo habilite manualmente
                    showNotificationPermissionDeniedDialog()
                }
            }
            REQUEST_CODE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El permiso de ubicación fue concedido, activa la ubicación en el mapa
                    enableLocation()
                } else {
                    // Si no concedió el permiso, mostramos un mensaje y el diálogo de ajustes
                    showLocationPermissionDeniedDialog()
                }
            }
        }
    }



    override fun onResume() {
        super.onResume()
        // Verificar nuevamente los permisos en caso de que hayan sido habilitados desde los ajustes
        if (isLocationPermissionGranted()) {
            // Si los permisos fueron otorgados, iniciar el servicio en primer plano
            enableLocation()
            startMyService()
        }
    }



    //Metodo para comprobar que los permisos siguen activos despues de que el usuario dejo la aplicacion en background

    private fun endInfoJob() {
        gettingInfoJob?.cancel()
        gettingInfoJob = null
        isMarkerSelected = false
        infoWindowMode = InfoMode.INACTIVE
        findViewById<FrameLayout>(R.id.info_fragment_container).visibility = View.GONE
    }


}