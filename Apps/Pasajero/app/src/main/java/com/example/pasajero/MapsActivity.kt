package com.example.pasajero

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var btnLocation: ImageView
    private lateinit var adapter: BusStopAdapter
    private lateinit var stationsButton: ImageView
    private var routeID: Int = 0
    private var busStops: List<BusStop> = listOf()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private var waypoints: List<LatLng> = listOf()
    private var busStopMarkers: MutableMap<BusStop, Marker> = mutableMapOf<BusStop, Marker>()
    private lateinit var coroutineScope: CoroutineScope
    private val polylines: MutableList<Polyline> = mutableListOf()
    private var isLocationUpdatesActive = false
    private lateinit var locationCallback: LocationCallback
    var ConductorLocationList: List<ConductorLocation> = listOf()

    //Variables to ask for the location permission
    companion object{
        const val REQUEST_CODE_LOCATION = 0
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0,0.0)
    private lateinit var progressBar: ProgressBar
    private lateinit var background: LinearLayout

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
        // Get all the busStops from the line selected
        routeID = intent.getIntExtra("routeID",0)
        Log.d("Transport response", "Datos de transporte: $routeID")
        val service = ApiHelper().prepareApi()
        fetchBusStops(service)
        // Configurar CoroutineScope
        coroutineScope = CoroutineScope(Dispatchers.Main + Job())
        progressBar = findViewById(R.id.progress_bar)
        background = findViewById(R.id.ownBackground)

        // Llamar a la función para enviar datos periódicamente
        //startSendingDataPeriodically()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsAPI = retrofit.create(GoogleDirectionsApi::class.java)

    }

    private fun fetchBusStops(service: ApiService){
        ApiHelper().getDataFromDB(
            serviceCall = { service.getBusStopsInfo(routeID) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val busStopsInfo = response.body()
                if (busStopsInfo != null) {
                    busStops = busStopsInfo
                    Log.d("Transport response", "BusStops: $busStops")
                    runOnUiThread {
                        setupMapMarkersAndRoutes()
                        setupRecyclerView()  // Ensure RecyclerView is updated after fetching bus stops
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
        if (marker != null) {
            busStopMarkers[busStop] = marker
        }
    }

    private fun latLangToStr(latLng: LatLng) = "${latLng.latitude},${latLng.longitude}"

    private fun getRoute(origin: BusStop, destination: BusStop) {
        val apiKey = getString(R.string.api_key)

        val waypointsStr = destination.waypoints ?: ""

        val originLocation = latLangToStr(LatLng(origin.latitude, origin.longitude))
        val destinationLocation = latLangToStr(LatLng(destination.latitude, destination.longitude))

        val call = directionsAPI.getDirections(
            originLocation,
            destinationLocation, apiKey,
            "driving",
            waypoints = waypointsStr
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
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
                    Log.e("DirectionsError", "Response error: ${response.code()} - ${response.message()} - Body: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Failed to get route: ${t.message}")
            }
        })
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

    private fun searchBarForBusStops(){
        // Show and hide all the stations
        btnLocation = findViewById(R.id.location_button)
        stationsButton = findViewById(R.id.showStations)

        stationsButton.setOnClickListener {
            if (binding.rvEstaciones.isVisible) binding.rvEstaciones.visibility = View.GONE else binding.rvEstaciones.visibility = View.VISIBLE
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

    private fun filtrate (text: String) {
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
        val adapter = CustomInfoWindowAdapter(this, currentLocation, busStops)
        mMap.setInfoWindowAdapter(adapter)
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun setupMarkerClickListeners() {
        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow() // Show the InfoWindow
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
        return withContext(Dispatchers.Main) { // Ensure you're on the Main thread
            busStops.find { busStop ->
                val markerDetails = busStopMarkers[busStop] ?: return@find false
                val markerPosition = markerDetails.position
                markerPosition == marker.position
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun goToBusStop(marker: Marker, busStops: List<BusStop>, progressBar: ProgressBar) {
        var nearestBusStop: BusStop? = null

        showProgressBar(progressBar)

        try {
            val distances = calculateDistances(busStops)
            nearestBusStop = findNearestBusStop(distances)

            nearestBusStop?.let { busStopOrigin ->
                val thresholdDistance = 10f  // Adjust threshold as needed
                val distanceToBusStop = distances.firstOrNull { it.first == busStopOrigin }?.second ?: Float.MAX_VALUE
//                if (distanceToBusStop > thresholdDistance) {
//                    showBusStopDialog(busStopOrigin, distanceToBusStop)
//                } else {
                    handleBusStopOrigin(busStopOrigin, marker, busStops)
//                }
            } ?: run {
                showToast("No hay estaciones disponibles")
            }
        } finally {
            hideProgressBar(progressBar)
        }
    }
    private suspend fun showProgressBar(progressBar: ProgressBar) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            background.visibility = View.VISIBLE
        }
    }
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun calculateDistances(busStops: List<BusStop>): List<Pair<BusStop, Float>> {
        return busStops.map { busStop ->
            GlobalScope.async {
                val busStopLocation = LatLng(busStop.latitude, busStop.longitude)
                val distance = getDistance(currentLocation, busStopLocation).await().toFloat()
                Pair(busStop, distance)
            }
        }.awaitAll()
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

    private fun findNearestBusStop(distances: List<Pair<BusStop, Float>>): BusStop? {
        return distances.minByOrNull { it.second }?.first
    }

    private suspend fun showBusStopDialog(busStop: BusStop, distance: Float) {
        withContext(Dispatchers.Main) {
            AlertDialog.Builder(this@MapsActivity)
                .setTitle("Ve a la estación más cercana")
                .setMessage("La estación más cercana es '${busStop.name}' y esta a ${distance.toInt()}m.")
                .setNegativeButton("Okay", null)
                .show()
        }
    }
    private suspend fun handleBusStopOrigin(busStopOrigin: BusStop, marker: Marker, busStops: List<BusStop>) {
        setupLocationUpdates()

        val busStopsOD = getBusStopsToTravel(busStopOrigin, marker, busStops)
        drawRoutesAndShowTimes(busStopsOD, marker)
    }
    private fun setupLocationUpdates() {
        if (isLocationUpdatesActive) return
        val locationRequest = LocationRequest.create().apply {
            interval = 1000
            fastestInterval = 500
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    updateCameraPosition(LatLng(location.latitude, location.longitude))
                }
            }
        }

        // Start location updates
        if (isLocationPermissionGranted()) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            isLocationUpdatesActive = true // Set the flag to true
        } else {
            requestLocationPermission()
        }
    }
    private fun updateCameraPosition(latLangLocation: LatLng) {
        val location = Location("UserLocation")
        location.latitude = latLangLocation.latitude
        location.longitude = latLangLocation.longitude

        val cameraPosition = CameraPosition.Builder()
            .target(latLangLocation)  // Set the new position
            .zoom(19.4f)  // Set zoom level
            .bearing(location.bearing)  // Rotate the camera to match the user’s bearing
            .tilt(45f)  // Tilt for a semi-3D view
            .build()

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private suspend fun getBusStopsToTravel(busStopOrigin: BusStop, marker: Marker, busStops: List<BusStop>): MutableList<BusStop> {
        return withContext(Dispatchers.Main) { // Switch to Main thread
            val busStopsOD = mutableListOf<BusStop>()
            var flag = false
            var repeatStations = true

            while (repeatStations) {
                for (busStop in busStops) {
                    val busStopLatLng = LatLng(busStop.latitude, busStop.longitude)
                    if (busStopLatLng == LatLng(busStopOrigin.latitude, busStopOrigin.longitude)) {
                        flag = true
                    }
                    if (flag) {
                        busStopsOD.add(busStop)
                    }
                    if (flag && busStopLatLng == marker.position) {
                        repeatStations = false
                        break
                    }
                }
            }
            busStopsOD // Return the list from the main thread
        }
    }

    private suspend fun drawRoutesAndShowTimes(busStopsOD: List<BusStop>, marker: Marker) {
        withContext(Dispatchers.Main) {
            marker.hideInfoWindow()
            clearOldRoutes()
            hideSearchBarAndShowDialog()
            val searchBar = findViewById<LinearLayout>(R.id.searchContainer)
            searchBar.visibility = View.GONE
            for (i in busStopsOD.indices) {
                if (i < busStopsOD.size - 1) {
                    getRoute(busStopsOD[i], busStopsOD[i + 1])
                }
            }
            // Show times for each route between bus stops
            lifecycleScope.launch {
                for (i in busStopsOD.indices) {
                    if (i < busStopsOD.size - 1) {
                        showTimeToDestination(busStopsOD[i], busStopsOD[i + 1])
                    }
                }
            }
        }
    }

    private fun hideSearchBarAndShowDialog(){
        val container = findViewById<LinearLayout>(R.id.traveling_container)
        val searchBar = findViewById<LinearLayout>(R.id.searchContainer)
        val button = findViewById<Button>(R.id.traveling_container_button)

        container.visibility = View.VISIBLE

        button.setOnClickListener {
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
        }
    }

    private fun stopLocationUpdates() {
        if (!isLocationUpdatesActive) return

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        isLocationUpdatesActive = false
    }

    @SuppressLint("SetTextI18n")
    private suspend fun showTimeToDestination(busStopOrigin: BusStop, busStopDestination: BusStop) {
        val title = findViewById<TextView>(R.id.traveling_container_title)
        val timer = findViewById<TextView>(R.id.traveling_container_timer)
        val busStopDestinationL = LatLng(busStopDestination.latitude, busStopDestination.longitude)
        var distanceBetweenStation: Float

        while (true) {
            distanceBetweenStation = withContext(Dispatchers.IO) {
                getDistance(currentLocation, busStopDestinationL).await().toFloat()
            }
            withContext(Dispatchers.Main) {
                if (distanceBetweenStation < 5f) {
                    title.text = "Estás en la estación: \n${busStopDestination.name}"
                    timer.text = "Tiempo estimado: 0 segundos"
                    return@withContext
                } else {
                    val (distance, duration) = getDistanceAndTime(busStopOrigin, busStopDestination).await()
                    title.text = "Siguiente estación: \n${busStopDestination.name}"
                    timer.text = "Tiempo estimado: \n$duration"
                }
            }
            delay(10)
        }
    }

    private suspend fun hideProgressBar(progressBar: ProgressBar) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            background.visibility = View.GONE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

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












    // Metodo para saber si el permiso "FINE LOCATION" esta aceptado o no
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Metodo para habilitar la localizacion al usuario
    private fun enableLocation () {
        if(!::mMap.isInitialized) return
        if(isLocationPermissionGranted()){
            mMap.isMyLocationEnabled = true

            //Go to my current location
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) {
                    location ->
                if (location != null){
                    lastLocation = location
                    currentLocation = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
                }
            }
        }else{
            requestLocationPermission()
        }
    }

    //Metodo para pedir al usuario el permiso de ver su localizacion
    private fun requestLocationPermission () {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
            Toast.makeText(this, "Habilita los permisos de localización en ajustes", Toast.LENGTH_SHORT).show()
        }else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION)
        }
    }

    //metodo para capturar la respuesta de que el usuario acepto los permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                mMap.isMyLocationEnabled = true
                //Go to my current location
                fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) {
                        location ->
                    if (location != null){
                        lastLocation = location
                        currentLocation = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
                    }
                }
            }else{
                Toast.makeText(this, "Ve a ajustes para aceptar los permisos de localizacion", Toast.LENGTH_SHORT).show()
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