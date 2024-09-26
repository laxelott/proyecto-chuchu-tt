package com.example.pasajero

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
    private lateinit var adapter: BusStopAdapter
    private lateinit var stationsButton: ImageView
    private var routeID: Int = 0
    private var busStops: List<BusStop> = listOf()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private lateinit var waypoints: List<LatLng>
    private var busStopMarkers: MutableMap<BusStop, Marker> = mutableMapOf<BusStop, Marker>()
    private lateinit var coroutineScope: CoroutineScope
    private val polylines: MutableList<Polyline> = mutableListOf()
    var ConductorLocationList: List<ConductorLocation> = listOf()

    //Variables to ask for the location permission
    companion object{
        const val REQUEST_CODE_LOCATION = 0
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0,0.0)

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
        // api/data/stop/list/idRoute
        routeID = intent.getIntExtra("routeID",0)
        val service = ApiHelper().prepareApi()
        fetchBusStops(service)
        // Configurar CoroutineScope
        coroutineScope = CoroutineScope(Dispatchers.Main + Job())

        // Llamar a la función para enviar datos periódicamente
        startSendingDataPeriodically()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsAPI = retrofit.create(GoogleDirectionsApi::class.java)
        waypoints = listOf()
    }

    private fun fetchBusStops(service: ApiService){
        ApiHelper().getDataFromDB(
            serviceCall = { service.getBusStopsInfo(routeID) }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val busStopsInfo = response.body()
                if (busStopsInfo != null) {
                    Log.d("Transport response", "Datos de transporte: $busStopsInfo")
                    busStops = busStopsInfo
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
        var idNext:Int = 0
        var busStopOrigin: LatLng = LatLng(19.497761748259805,-99.13647282618467)
        var busStopDestination: LatLng = LatLng(19.495378901114236,-99.13618018883543)
        // Creating the markers and routes
        for ((i, busStop) in busStops.withIndex()) {
            createMarker(busStop)
            waypoints = waypoints + LatLng(busStop.latitude, busStop.longitude)
            //val currentIdNext = busStop.idNext
//            if (currentIdNext > idNext) {
//                idNext = currentIdNext
//                busStopDestination = LatLng(busStops[i].latitude, busStops[i].longitude)
//            }
//            else { busStopOrigin = LatLng(busStops[i].latitude, busStops[i].longitude) }

            if (i < busStops.size - 1) {
                // Route between bus stops
                val busStop1 = LatLng(busStops[i].latitude, busStops[i].longitude)
                val busStop2 = LatLng(busStops[i + 1].latitude, busStops[i + 1].longitude)

                getRoute(busStop1, busStop2)
            }
            //getRoute(busStop, busStopDestination)
        }
    }

    private fun clearOldRoutes() {
        // Remove all polylines from the map
        for (polyline in polylines) {
            polyline.remove()
        }
        // Clear the list
        polylines.clear()
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

    // Route of two routes
    private fun getRoute(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.api_key)

        val waypointsStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }

        Log.e("Transport info", "Origin: $origin")
        Log.e("Transport info", "Destination: $destination")


        val call = directionsAPI.getDirections(
            "${origin.latitude},${origin.longitude}",
            "${destination.latitude},${destination.longitude}",
            apiKey,
            "driving",
            //waypoints = waypointsStr
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val routes = response.body()?.routes
                    if (!routes.isNullOrEmpty()) {
                        val polyline = routes[0].overview_polyline.points
                        // Add logging here to check the polyline
                        Log.d("Polyline", "Polyline points: $polyline")

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

    private fun startSendingDataPeriodically() {
        coroutineScope.launch {
            while (true) {
                // Ejecutar la tarea en un hilo IO
                withContext(Dispatchers.IO) {
                    getLatitudeLongitude()
                }
                // Esperar 1 segundos
                delay(1000)
            }
        }
    }
    private suspend fun getLatitudeLongitude() {
        val service = ApiHelper().prepareApi()
        ApiHelper().getDataFromDB(
            serviceCall = { service.getLatitudeLongitude() }, // Pasamos la función que hace la solicitud
            processResponse = { response ->
                val ConductorLocationResponse = response.body()
                if (ConductorLocationResponse != null) {
                    Log.d("Transport response", "Ubicacion de conductores: $ConductorLocationResponse")
                    ConductorLocationList = ConductorLocationResponse
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancelar las coroutines cuando la actividad se destruye
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
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.custom_map_style))
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = true

        enableLocation()
        btnLocation.setOnClickListener{
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
        }
        // Control the search
        findLocations()


        // Set custom InfoWindow adapter
        val adapter = CustomInfoWindowAdapter(this, currentLocation, busStops)
        mMap.setInfoWindowAdapter(adapter)

        // Handle marker clicks
        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow() // Show the InfoWindow
            true
        }

        // Handle InfoWindow clicks
        mMap.setOnInfoWindowClickListener { marker ->
            // Trigger action when user clicks the InfoWindow (e.g., find nearest bus stop)
            val busStop = findBusStopForMarker(marker)
            busStop?.let {
                findNearestBusStop(marker, busStops) // Call your method to find nearest bus stop
            }
        }
    }

    private fun findLocations(){
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
        val listaFiltrada = ArrayList<BusStop>()

        busStops.forEach {
            if (it.name.lowercase().contains(text.lowercase())) {
                listaFiltrada.add(it)
            }
        }

        adapter.filtrar(listaFiltrada)
    }

    // Convert location to string
    private fun latLangToStr(latLng: LatLng) = "${latLng.latitude},${latLng.longitude}"
    // Get Distance and Time
    private fun getDistanceAndTime(busStop1: BusStop, busStop2: BusStop) {
        val origin = latLangToStr(LatLng(busStop1.latitude, busStop1.longitude))
        val destination = latLangToStr(LatLng(busStop2.latitude, busStop2.longitude))

        val api = this.getString(R.string.api_key)
        val call = directionsAPI.getDirections(origin, destination, api, "driving")
        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                if (response.isSuccessful) {
                    val routes = response.body()?.routes
                    if (routes.isNullOrEmpty()) {
                        Log.e("DirectionsError", "No se encontraron rutas")
                    }
                    else {
                        val leg = routes[0].legs[0]
                        val distance = leg.distance.text
                        val duration = leg.duration.text
                        Log.d("DirectionsInfo", "Distancia: $distance, Tiempo estimado: $duration")
                    }
                }
                else {
                    // Mostrar el código de error y mensaje en caso de respuesta no exitosa
                    Log.e("DirectionsError", "Respuesta fallida: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                println("Error: ${t.message}")
                Log.d("DirectionsInfo", "Not working")
            }
        })

    }





    // Helper function to find bus stop associated with the marker
    private fun findBusStopForMarker(marker: Marker): BusStop? {
        return busStops.find { busStop ->
            busStopMarkers[busStop]?.position == marker.position
        }
    }
    private fun findNearestBusStop(marker: Marker, BusStops: List<BusStop>) {
        var nearestBusStop: BusStop? = null
        var minDistance = Float.MAX_VALUE

        // Loop through all BusStops to find the nearest
        for (busStop in busStops) {
            val busStopLocation = Location("BusStop").apply {
                latitude = busStop.latitude
                longitude = busStop.longitude
            }

            val distance = busStopLocation.distanceTo(Location("CurrentLocation").apply {
                latitude = currentLocation.latitude
                longitude = currentLocation.longitude
            })

            if (distance < minDistance) {
                minDistance = distance
                nearestBusStop = busStop
            }
        }

        // Check if a bus stop was found and handle the distance check
        nearestBusStop?.let { busStopOrigin ->
            // Define a threshold distance, say 50 meters, to determine if user is "at" the bus stop
            val thresholdDistance = 50f  // Adjust threshold as needed

            //fetchAndDisplayDirections(LatLng(busStop.latitude,busStop.longitude))
            var busStopsOD: List<BusStop> = listOf()
            var flag: Boolean = false
            for ((i, busStop) in BusStops.withIndex()){
                Log.d("Transport info", "Paso 1")
                if (LatLng(busStop.latitude,busStop.longitude) == LatLng(busStopOrigin.latitude,busStopOrigin.longitude))
                {
                    Log.d("Transport info", "Paso 2")
                    flag = true
                }
                if (flag)
                {
                    Log.d("Transport info", "Paso 3")
                    busStopsOD += busStop
                }
                if (LatLng(busStop.latitude,busStop.longitude) == marker.position){
                    Log.d("Transport info", "Paso 4")
                    flag = false
                }
            }

            for ((i, busStopDestination) in busStopsOD.withIndex()){
                Log.d("Transport info", "Paso 5")
                if (i < busStopsOD.size - 1) {
                    Log.d("Transport info", "Paso 6")
                    // Route between bus stops
                    showRouteToDestination(busStopsOD[i], busStopsOD[i + 1])
                }
            }


//            if (minDistance > thresholdDistance) {
//                // User is far from the bus stop, prompt them to navigate to the bus stop first
//                showBusStopDialog(busStop, minDistance)
//            } else {
//                // User is close enough to the bus stop, show the route to the final destination
//                showRouteToDestination(busStop, marker)
//            }
        } ?: Toast.makeText(this, "No Bus Stops available", Toast.LENGTH_SHORT).show()
    }

    private fun showBusStopDialog(busStop: BusStop, distance: Float) {
        AlertDialog.Builder(this)
            .setTitle("Ve a la estación más cercana")
            .setMessage("La estación más cercana es '${busStop.name}', que está a ${distance.toInt()} metros de distancia. \nPor favor dirigete allá primero.")
            .setNegativeButton("Okay", null)
            .show()
    }

    private fun fetchAndDisplayDirections(destination: LatLng) {
        val origin = currentLocation // User's current location

        val apiKey = getString(R.string.api_key)
        val call = directionsAPI.getDirections(
            "${origin.latitude},${origin.longitude}",
            "${destination.latitude},${destination.longitude}",
            apiKey,
            "driving"
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val routes = response.body()?.routes
                    if (!routes.isNullOrEmpty()) {
                        val leg = routes[0].legs[0]
                        val steps = leg.steps // Get the detailed steps

                        // Now you can parse the steps and show them in a dialog or new screen
                        val instructions = steps.map { it.html_instructions } // List of HTML instructions
                        showDirectionsDialog(instructions)
                    } else {
                        Toast.makeText(this@MapsActivity, "No route found.", Toast.LENGTH_SHORT).show()
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

    private fun showDirectionsDialog(instructions: List<String>) {
        val dialogBuilder = android.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle("Directions")

        // Convert HTML instructions to plain text
        val plainTextInstructions = instructions.map { android.text.Html.fromHtml(it).toString() }

        // Create a simple ListView to display the instructions
        val listView = ListView(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, plainTextInstructions)
        listView.adapter = adapter

        dialogBuilder.setView(listView)
        dialogBuilder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    private fun showRouteToDestination(busStopOrigin: BusStop, busStopDestination: BusStop) {
        // Clear old routes
        clearOldRoutes()

        // Now, get the route from the bus stop to the destination
        getRoute(LatLng(busStopOrigin.latitude, busStopOrigin.longitude), LatLng(busStopDestination.latitude, busStopDestination.longitude))
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