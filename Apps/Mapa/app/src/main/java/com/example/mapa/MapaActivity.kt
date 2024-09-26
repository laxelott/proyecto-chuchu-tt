package com.example.mapa

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
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


class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaTransporteBinding
    private lateinit var btnLocation: ImageView
    private var busStops: List<BusStop> = listOf()
    private var busStopMarkers: MutableMap<BusStop, Marker> = mutableMapOf<BusStop, Marker>()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private lateinit var waypoints: List<LatLng>
    private lateinit var coroutineScope: CoroutineScope

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private var currentLocation: LatLng = LatLng(0.0,0.0)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaTransporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // Construct a FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        btnLocation = findViewById(R.id.location_button)
        // Get all the busStops from the line selected
        // api/data/stop/list/idRoute
        //routeID = intent.getIntExtra("routeID",0)
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

        waypoints = listOf(
            LatLng(19.505161688551212, -99.15055914014806),
//            LatLng(19.505189627612836, -99.15048185099594)
        )







        // Add this in onCreate
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    updateCameraPosition(location)
                }
            }
        }

// Start location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

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
                    }
                }
            }
        )
    }

    private fun setupMapMarkersAndRoutes() {
        if (!::mMap.isInitialized) return

        // Creating the markers and routes
        for ((i, busStop) in busStops.withIndex()) {
            createMarker(busStop)
            if (i < busStops.size - 1) {
                // Route between bus stops
                val busStop1 = LatLng(busStops[i].latitude, busStops[i].longitude)
                val busStop2 = LatLng(busStops[i + 1].latitude, busStops[i + 1].longitude)
                getRoute(busStop1, busStop2)
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

    private fun updateLocationOnMap(location: Location) {
        currentLocation = LatLng(location.latitude, location.longitude)

        // Move the camera to follow the user's location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
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

    private fun startSendingDataPeriodically() {
        coroutineScope.launch {
            while (true) {
                // Ejecutar la tarea en un hilo IO
                withContext(Dispatchers.IO) {
                    sendLatitudeLongitude(currentLocation.latitude, currentLocation.longitude) // Reemplaza con tus coordenadas reales
                }

                // Esperar 5 segundos
                delay(5000)
            }
        }
    }
    private suspend fun sendLatitudeLongitude(latitude: Double, longitude: Double) {
        try {
            Log.d("Transport My Location", "$latitude")
            Log.d("Transport My Location", "$longitude")

//            // Format the latitude and longitude to 10 decimal places
//            val formattedLatitude = String.format("%.10f", latitude)
//            val formattedLongitude = String.format("%.10f", longitude)
//
//            // Now convert the formatted strings to floats if needed
//            val latitudeDouble = formattedLatitude.toDouble()
//            val longitudeDouble = formattedLongitude.toDouble()
//
//            Log.d("Transport My Location", "$latitudeDouble")
//            Log.d("Transport My Location", "$longitudeDouble")

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
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.custom_map_style))
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = true

        enableLocation()

        btnLocation.setOnClickListener{
            // Check if currentLocation has been updated from its default value (LatLng(0.0, 0.0))
            if (currentLocation.latitude != 0.0 && currentLocation.longitude != 0.0) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
            } else {
                Toast.makeText(this, "Current location is not available yet.", Toast.LENGTH_SHORT).show()
            }
        }

        // Validate the range of the location with the principal station
        val busBase = Location("Bus Base")
//        busBase.latitude = busStops[0].latitude
//        busBase.longitude = busStops[0].longitude
//        if (!isWithinDistance(busBase , 20f)){
//            Log.e("Direccion", "Dentro del if")
//            showAlertDialog(this)
//        }

        btnLocation.setOnClickListener {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        lastLocation.latitude,
                        lastLocation.longitude
                    ), 19.4f
                )
            )
        }
    }
    // Alert
    fun showAlertDialog(context: Context) {
        // Crear un AlertDialog.Builder
        val builder = AlertDialog.Builder(context)

        // Configurar el título y el mensaje
        builder.setTitle("Rango no permitido")
        builder.setMessage("No estas dentro dentro del rango para iniciar recorrido")

        // Configurar el botón positivo
        builder.setPositiveButton("OK") { dialog, _ ->
            // Acción a realizar cuando el usuario hace clic en el botón "OK"
            if (context is Activity) {
                context.finish()
            }
            dialog.dismiss() // Cierra el diálogo
        }

        // Crear y mostrar el AlertDialog
        val alertDialog = builder.create()
        alertDialog.show()
    }

    // Distance between two points
    fun isWithinDistance(busStop2: Location, minDistanceMeters: Float): Boolean {
        // Calcular la distancia entre los dos puntos
        val distance = lastLocation.distanceTo(busStop2)
        // Verificar si la distancia es menor o igual a la distancia mínima deseada
        return distance <= minDistanceMeters
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

    // Route of two routes
    private fun getRoute(origin: LatLng, destination: LatLng) {
        val apiKey = getString(R.string.api_key)

        val waypointsStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }

        val call = directionsAPI.getDirections(
            "${origin.latitude},${origin.longitude}",
            "${destination.latitude},${destination.longitude}",
            apiKey,
            "transit",
//            waypoints = waypointsStr
        )

        call.enqueue(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.isSuccessful) {
                    val polyline = response.body()?.routes?.get(0)?.overview_polyline?.points
                    polyline?.let {
                        val decodedPath = PolyUtil.decode(it)
                        mMap.addPolyline(PolylineOptions()
                            .addAll(decodedPath)
                            .color(android.graphics.Color.BLUE)
                            .width(10f) // Cambiar el ancho de la ruta
                            .geodesic(true) // Usar una línea geodésica
                        )
                    }
                } else {
                    Log.e("DirectionsError", "Response error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DirectionsError", "Failed to get route: ${t.message}")
            }
        })
    }

    // Metodo para saber si el permiso "FINE LOCATION" esta aceptado o no
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Metodo para habilitar la localizacion al usuario
    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true

            //Go to my current location
            fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                if (location != null) {
                    lastLocation = location
                    currentLocation = LatLng(
                        String.format("%.10f", location.latitude).toDouble(),
                        String.format("%.10f", location.longitude).toDouble()
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
                }
            }

            //Get the location button

        } else {
            requestLocationPermission()
        }
    }

    //Metodo para pedir al usuario el permiso de ver su localizacion
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
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                //Go to my current location
                fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
                    if (location != null) {
                        lastLocation = location
                        currentLocation = LatLng(
                            String.format("%.10f", location.latitude).toDouble(),
                            String.format("%.10f", location.longitude).toDouble()
                        )
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
