package com.example.mapa

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mapa.databinding.ActivityMapaTransporteBinding
import com.example.mapa.interfaces.ApiService
import com.example.mapa.interfaces.CustomInfoWindowAdapter
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MapaActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaTransporteBinding
    private lateinit var btnLocation: ImageView
    private lateinit var btnEndTravel: ImageView
    private lateinit var btnReportIncident: ImageView
    private var busStops: List<BusStop> = listOf()
    private var busStopMarkers: MutableMap<BusStop, Marker> = mutableMapOf()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var progressBar: ProgressBar
    private lateinit var background: LinearLayout
    private val polylines: MutableList<Polyline> = mutableListOf()
    private var isLocationUpdatesActive = false
    private var speed:Float = 0.0F
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                // Actualizar la ubicación actual
                currentLocation = LatLng(location.latitude, location.longitude)

                // Actualizar la velocidad
                speed = if (location.hasSpeed()) {
                    val speedInMetersPerSecond = location.speed
                    speedInMetersPerSecond
                } else {
                    0.0F
                }

                // Actualizar la posición de la cámara
                currentLocation.let { currentLatLng ->
                    updateCameraPosition()
                }
            }
        }
    }

    private var zoomLevel: Float = 19.4f


    private val REQUEST_CODE_LOCATION = 1002

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0, 0.0)
    private var token: String = ""
    private var routeID: Int = 0
    private var vehicleIdentifier: String = ""
    private var sendingDataJob: Job? = null
    private var gettingIncidentsJob: Job? = null
    private var gettingInfoJob: Job? = null
    private var incidentsMarkers = mutableMapOf<Incident, Marker>()
    private var alertErrorFlag = false
    private var busStopCounter: Int = 0
    private var checkRange = false
    private var isLocationPermissionRequested = false  // Control para manejar permisos de ubicación
    private var hasFetchedData = false
    private var isFetchingData = false
    private var hasFetchedBusStops = false


    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var azimuth: Float = 0.0f
    private var lastAzimuth: Float = 0.0f



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaTransporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkRange = intent.getBooleanExtra("checkRange", false)
        Log.d("UsingVehicle", "Using vehicle: $checkRange")

        // Construct a FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        requestPermissionsFlow{}
        btnLocation = findViewById(R.id.location_button)
        btnReportIncident = findViewById(R.id.btn_report_incident)
        btnEndTravel = findViewById(R.id.traveling_container_button)
        coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        progressBar = findViewById(R.id.progress_bar)
        background = findViewById(R.id.ownBackground)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)



        // Initialize directions API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsAPI = retrofit.create(GoogleDirectionsApi::class.java)

        // Get token and idRoute
        token = intent.getStringExtra("token").toString()
        routeID = intent.getIntExtra("idRoute", 0)
        vehicleIdentifier = intent.getStringExtra("vehicleIdentifier").toString()
    }

    override fun onBackPressed() {
        // No hacer nada aquí bloquea el botón "Atrás"
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            fetchDataInBackground()
            //checkVehicle(token, vehicleIdentifier)
        }
        // Registra el listener del sensor
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val newAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val normalizedAzimuth = (newAzimuth + 360) % 360

            if (Math.abs(normalizedAzimuth - lastAzimuth) > 2) { // Cambia solo si supera los 2 grados
                lastAzimuth = normalizedAzimuth
                azimuth = lastAzimuth
                updateCameraPosition()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No es necesario manejar este evento en este caso
    }

    override fun onPause() {
        super.onPause()
        // Detén las actualizaciones del sensor
        sensorManager.unregisterListener(this)
    }

    private fun updateCurrentLocation() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000 // Cada segundo
            fastestInterval = 500
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


    private fun fetchBusStops(service: ApiService) {
        if (hasFetchedBusStops) return // Evitar múltiples ejecuciones
        hasFetchedBusStops = true

        ApiHelper().getDataFromDB(
            serviceCall = { service.getBusStopsInfo(routeID) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val busStopsInfo = response.body()
                if (busStopsInfo != null) {
                    Log.d("Transport response", "Datos de transporte: $busStopsInfo")
                    busStops = busStopsInfo
                    runOnUiThread {
                        val distance = calculateDistance(
                            LatLng(currentLocation.latitude, currentLocation.longitude),
                            LatLng(busStops[0].latitude, busStops[0].longitude)
                        )
                        Log.d("Distancia", "$distance, $checkRange")

                        if (checkRange && distance >= 30f) {
                            showAlertDialog(this@MapaActivity)
                        } else {
                            setupMapMarkersAndRoutes()
                            CoroutineScope(Dispatchers.IO).launch {
                                useVehicle(token, vehicleIdentifier)
                            }
                        }
                    }
                }
            }
        )
    }

    private suspend fun checkVehicle(token: String, vehicleIdentifier: String) {
        val service = ApiHelper().prepareApi()
        val tokenRequest = TokenRequest(token)
        ApiHelper().getDataFromDB(
            serviceCall = {
                service.checkVehicle(
                    vehicleIdentifier,
                    tokenRequest
                )
            }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val responseBody = response.body()
                if (responseBody != null) {
                    when (responseBody.error) {
                        0 -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                startTravel()
                            }
                        }

                        1 -> {
                            finish()
                        }
                    }
                }
            }
        )
    }


    private suspend fun useVehicle(token: String, vehicleIdentifier: String) {
        val service = ApiHelper().prepareApi()
        val tokenRequest = TokenRequest(token)
        Log.d("Debug", "enviando vehiculo a utilizar: $vehicleIdentifier")
        ApiHelper().getDataFromDB(
            serviceCall = {
                service.postUseVehicle(
                    vehicleIdentifier,
                    tokenRequest
                )
            }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val responseBody = response.body()
                Log.d("Response", "$responseBody")
                if (responseBody != null) {
                    when (responseBody.error) {
                        0 -> {
                            CoroutineScope(Dispatchers.Main).launch {
                                val response = ApiHelper().prepareApi().startTrip(tokenRequest, routeID)
                                if (response.isSuccessful) {
                                    Log.d("API Respose", "$response")
                                    Log.d("API Response", "$tokenRequest, $routeID")
                                    Log.d("API Response", "Listo para iniciar viaje")
                                    startTravel()
                                } else {
                                    Log.e(
                                        "API Error",
                                        "Error al enviar ubicacion: ${response.code()} ${response.message()}"
                                    )
                                }
                            }
                        }

                        2 -> {
                            newAlertDialog(
                                "¡Alerta!",
                                "Este vehículo ya está en uso"
                            )
                        }
                    }
                }
            }
        )
    }

    private fun newAlertDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupMapMarkersAndRoutes() {
        if (!::mMap.isInitialized) return
        // Creating the markers and routes
        createRoutes()
    }

    private fun createRoutes() {
        val origin = busStops.first()
        val waypoints = gettingWaypointsFromDestinations(busStops)
        getRoute(origin, origin, waypoints)

        for (busStop in busStops) {
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
                    "Waypoint $index",
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
            startSendingDataPeriodically()
            startGettingIncidents()
            showProgressBar()
            Log.d("Debug", "Identificador de vehiculo: $vehicleIdentifier")
            getInfoJob(busStops, vehicleIdentifier)
        }
    }

    private suspend fun getInfoJob(busStops: List<BusStop>, driverIdentifier: String) {
        if (!::coroutineScope.isInitialized) {
            coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        }
        val fragment: Fragment = InfoFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.info_fragment_container, fragment)
            .commit()
        findViewById<FrameLayout>(R.id.info_fragment_container).visibility = View.VISIBLE
        Log.d("Debug", "En get info job")
        // Cancel any existing job before starting a new one.
        gettingInfoJob?.cancel()
        gettingInfoJob = coroutineScope.launch {
            while (true) {
                getInfo(busStops, driverIdentifier)
                delay(1000)
            }
        }
    }

    private suspend fun getInfo(busStops: List<BusStop>, driverIdentifier: String) {

        val service = withContext(Dispatchers.IO) {
            ApiHelper().prepareApi()
        }
        Log.d("Debug", "Solicitud de los tiempos del vehiculo")
        Log.d("Debug", "Datos: $routeID, ${busStops[busStopCounter].id}, $vehicleIdentifier")
        val tokenRequest = TokenRequest(token)
        ApiHelper().getDataFromDB(
            serviceCall = {
                service.getWaitTimeForVehicle(tokenRequest)
            },
            processResponse = { response ->
                val waitTime = response.body()
                if (waitTime == null) {
                    Log.d("Info", "Error en la respuesta para la informacion del conductor")
                    return@getDataFromDB
                }

                Log.d("Info", "Informacion del conductor recibida correctamente")
                if (waitTime.error == 1) {
                    if (!alertErrorFlag) {
                        // Show error dialog on the Main thread
                        CoroutineScope(Dispatchers.Main).launch {
                            AlertDialog.Builder(this@MapaActivity)
                                .setTitle("¡Error!")
                                .setMessage("IDs inválidas")
                                .setNegativeButton("Cerrar", null)
                                .show()
                        }
                        alertErrorFlag = true
                        return@getDataFromDB
                    }
                }
                Log.d("Info", "Informacion: $waitTime")
                Log.d("Transport response", "Tiempos de conductor")
                // Update the UI fragment on the Main thread
                CoroutineScope(Dispatchers.Main).launch {
                    Log.d("debug", "Contador: $busStopCounter")
                    hideProgressBar()

                    var infoFragment: Fragment? =
                        supportFragmentManager.findFragmentById(R.id.info_fragment_container)
                    if (infoFragment is InfoFragment) {
                        val startTravelFragment = infoFragment
                        startTravelFragment.updateData(waitTime)
                        if (waitTime.inTerminal == 1) {
                            endTravel()
                        }
                    }
                }
            }
        )
    }


    private fun leaveVehicle() {
        val service = ApiHelper().prepareApi()
        val tokenRequest = TokenRequest(token)
        ApiHelper().getDataFromDB(
            serviceCall = { service.leaveVehicle(tokenRequest) },
            processResponse = { response ->
                val responseBody = response.body()
                Log.d("Response", "$responseBody")
                if (responseBody != null) {
                    when (responseBody.error) {
                        0 -> {
                            finish()
                        }

                        1 -> {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("¡Error!")
                                .setMessage("Consulta al administrador")
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    finish()
                                }
                                .setCancelable(false)
                            builder.create().show()
                        }
                    }
                }
            }
        )
    }


    private fun endTravel() {
        sendingDataJob?.cancel()
        gettingIncidentsJob?.cancel()
        gettingInfoJob?.cancel()
        endTrip()
        val sharedPreferences = getSharedPreferences("vehicleIsSelected", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

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
        builder.setTitle("¡Terminaste la ruta!")
            .setMessage("Haz llegado a la ruta, gracias por viajar con Nintrip :)")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                leaveVehicle()
            }
            .setCancelable(false)
        builder.create().show()
    }

    private fun endTrip() {
        val service = ApiHelper().prepareApi()
        val tokenRequest = TokenRequest(token)
        ApiHelper().getDataFromDB(
            serviceCall = { service.endTrip(tokenRequest) },
            processResponse = { response ->
                val responseBody = response.body()
                Log.d("Response", "$responseBody")
                if (responseBody != null) {
                    when (responseBody.error) {
                        0 -> {
                        }

                        1 -> {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("¡Error!")
                                .setMessage("Consulta al administrador")
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    finish()
                                }
                                .setCancelable(false)
                            builder.create().show()
                        }
                        2 -> {
                            val builder = AlertDialog.Builder(this)
                            builder.setTitle("¡Error!")
                                .setMessage("Token inválida, consulta al administrador")
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    finish()
                                }
                                .setCancelable(false)
                            builder.create().show()
                        }
                    }
                }
            }
        )

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


    private fun latLangToStr(latLng: LatLng) = "${latLng.latitude},${latLng.longitude}"


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

                    if (!legs.isNullOrEmpty()) {
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

    private fun updateCameraPosition() {
        if (!::mMap.isInitialized) {
            Log.e("MapError", "Map is not initialized yet")
            return
        }

        val cameraPosition = CameraPosition.Builder()
            .target(currentLocation)
            .zoom(zoomLevel)
            .bearing(azimuth)
            .tilt(90f)
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun showAlertDialog(context: Context) {
        if (context is Activity && !context.isFinishing) {
            AlertDialog.Builder(context).apply {
                setTitle("Rango no permitido")
                setMessage("No estas dentro del rango para iniciar recorrido")
                setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    sendingDataJob?.cancel()
                    gettingIncidentsJob?.cancel()
                    gettingInfoJob?.cancel()
                    leaveVehicle()
                    val sharedPreferences = context.getSharedPreferences("vehicleIsSelected", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.clear()

                    // Usar commit() en lugar de apply() para asegurar que los cambios se guarden antes de cerrar la actividad
                    if (editor.commit()) {
                        Log.d("SharedPreferences", "Preferences cleared successfully.")
                    } else {
                        Log.e("SharedPreferences", "Failed to clear preferences.")
                    }
                    if (!context.isFinishing) {
                        context.finish()
                    }
                }
                create().show()
            }
        } else {
            Log.e("AlertDialog", "No se puede mostrar el diálogo: la actividad no está activa.")
        }
    }


    private fun startSendingDataPeriodically() {
        sendingDataJob?.cancel()
        sendingDataJob = coroutineScope.launch {
            while (isActive) {
                withContext(Dispatchers.IO) {
                    try {
                        sendLatitudeLongitude(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                    } catch (e: Exception) {
                        Log.e("LocationUpdate", "Error sending location: ${e.message}")
                    }
                }
                delay(5000)
            }
        }
    }

    private suspend fun sendLatitudeLongitude(latitude: Double, longitude: Double) {
        try {
            val bodyDriver = ApiService.BodyDriver(token, speed)
            Log.d("Debug", "Token para enviar la ubicacion: ")
            val tokenRequest = TokenRequest(token)
            val response =
                ApiHelper().prepareApi().postLatitudeLongitude(latitude, longitude, bodyDriver)
            if (response.isSuccessful) {
                Log.d("API Response", "Ubicacion enviada")
            } else {
                Log.e(
                    "API Error",
                    "Error al enviar ubicacion: ${response.code()} ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.e("API Error", "Excepción: ${e.message}")
        }
    }

    private fun startGettingIncidents() {
        if (!::coroutineScope.isInitialized) {
            coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        }

        gettingIncidentsJob?.cancel()
        gettingIncidentsJob = coroutineScope.launch {
            while (true) {
                withContext(Dispatchers.IO) {
                    getIncidentsList(this@MapaActivity, routeID)
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
                            Log.d("API Response", "Incidencias recibidas")
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
                                marker?.tag = "incident"

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
                    clearMarkers()
                    incidentsMarkers = newMarkers
                } ?: Log.e("API Error", "La lista de incidentes es nula")
            }
        )
    }

    private fun clearMarkers() {
        for ((incident, marker) in incidentsMarkers) {
            marker.remove()
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
        if (!::mMap.isInitialized) {
            mMap = googleMap
            setupMap()

            // Establecer lógica inicial
            requestPermissionsFlow { granted ->
                if (granted) {
                    initializeMapLogic()
                } else {
                    Toast.makeText(this, "Permisos de ubicación son requeridos", Toast.LENGTH_LONG).show()
                }
            }

            // Callback de permisos
            onPermissionGranted = { initializeMapLogic() }
        }
    }

    private fun initializeMapLogic() {
        enableLocation()
        setupLocationButton()
        setUpIncidentButton()
        setUpEndTravelButton()
        setupInfoWindowAdapter()
        setupMarkerClickListeners()

        mMap.setOnMapLoadedCallback {
            if (!hasFetchedData) {
                hasFetchedData = true
                lifecycleScope.launch {
                    fetchDataInBackground()
                }
            }
        }
    }


    private suspend fun fetchDataInBackground() = withContext(Dispatchers.IO) {
        if (isFetchingData) return@withContext
        isFetchingData = true

        try {
            updateCurrentLocation()
            val service = ApiHelper().prepareApi()
            fetchBusStops(service)
        } finally {
            isFetchingData = false // Restablece si es necesario permitir nuevas ejecuciones
        }
    }


    private fun setupMap() {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.custom_map_style))
        with(mMap.uiSettings) {
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
            isZoomControlsEnabled = false
            isCompassEnabled = false
            mMap.isBuildingsEnabled = false
        }
    }

    private fun setupLocationButton() {
        btnLocation.setOnClickListener {
            zoomLevel = if (zoomLevel > 19f) 18f else 19.4f
            updateCameraPosition()
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
            if (marker.tag == "incident") {
                val incident = incidentsMarkers.entries.find { it.value == marker }?.key
                incident?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        removeIncident(it)
                    }
                }
            }
        }
    }

    private fun removeIncident(incident: Incident) {
        val tokenRequest = TokenRequest(token)
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Eliminar incidencia")
                .setMessage("¿Quieres eliminar la incidencia ${incident.name}?")
                .setPositiveButton("Si") { dialog, _ ->
                    val service = ApiHelper().prepareApi()
                    ApiHelper().getDataFromDB(
                        serviceCall = { service.deleteIncident(incident.id, tokenRequest) },
                        processResponse = { response ->
                            val incidentResponse = response.body()
                            runOnUiThread {
                                if (incidentResponse != null) {
                                    Toast.makeText(this, "Incidencia eliminada", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Error al eliminar incidencia",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create().show()
        }
    }


    private fun showReportIncident() {
        val dialogFragment = ReportIncidentDialogFragment(
            routeID,
            currentLocation.latitude,
            currentLocation.longitude,
            token
        )
        dialogFragment.show(supportFragmentManager, "reportIncident")
    }

    private fun showEndTravelWindow() {
        val dialogFragment =
            EndTravelDialogFragment(sendingDataJob, gettingIncidentsJob, gettingInfoJob, token)
        dialogFragment.show(supportFragmentManager, "endTravelDialog")
    }


    /*
    *
    * PERMISOS
    *
    * */
    private fun requestPermissionsFlow(onPermissionResult: (Boolean) -> Unit) {
        if (isLocationPermissionGranted()) {
            enableLocation()
            onPermissionResult(true)
        } else {
            if (isLocationPermissionRequested) {
                Toast.makeText(this, "Permiso de localización denegado", Toast.LENGTH_SHORT).show()
                showLocationPermissionDeniedDialog()
                onPermissionResult(false)
            } else {
                requestLocationPermission()
            }
        }
    }

    // Verifica si el permiso de ubicación está concedido
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Activa la ubicación en el mapa si el permiso está concedido
    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true
            getLastLocation()
        }
    }

    // Obtiene la última ubicación del usuario si el permiso está concedido
    private fun getLastLocation() {
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                lastLocation = location
                currentLocation = LatLng(location.latitude, location.longitude)
                updateCameraPosition()
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

    // Solicita el permiso de ubicación
    private fun requestLocationPermission() {
        isLocationPermissionRequested = true
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION
        )
    }

    // Manejador del resultado de la solicitud de permisos
    private var onPermissionGranted: (() -> Unit)? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocation()
                    onPermissionGranted?.invoke()
                    onPermissionGranted = null
                } else {
                    showLocationPermissionDeniedDialog()
                    onPermissionGranted = null
                }
            }
        }
    }



}
