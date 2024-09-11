package com.example.mapa

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapa.databinding.ActivityMapaTransporteBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaTransporteBinding
    private lateinit var btnLocation: ImageView
    private var busStops = ArrayList<BusStop>()
    private lateinit var directionsAPI: GoogleDirectionsApi
    private lateinit var waypoints: List<LatLng>
    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaTransporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        lastLocation = Location("User location")
        /**
         * Getting all the bus stations of the line selected
         */
        busStops = intent.getSerializableExtra("busStops") as ArrayList<BusStop>

        btnLocation = findViewById(R.id.location_button)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        directionsAPI = retrofit.create(GoogleDirectionsApi::class.java)

        waypoints = listOf(
            LatLng(19.505161688551212, -99.15055914014806),
//            LatLng(19.505189627612836, -99.15048185099594)
        )

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

        // Validate the range of the location with the principal station
        val busBase = Location("Bus Base")
        busBase.latitude = busStops[0].latitude
        busBase.longitude = busStops[0].longitude
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
                    ), 16f
                )
            )
        }

        //Creating the markers
        for ((i, busStop) in busStops.withIndex()) {
            createMarker(busStop.latitude, busStop.longitude, busStop.name)
            if (i < busStops.size - 1)
            {
                // Distancia y tiempo entre 2 puntos
//                getDistanceAndTime(busStops[i], busStops[i+1])
                // Dibujo de ruta
                val busStop1 = LatLng(busStops[i].latitude, busStops[i].longitude)
                val busStop2 = LatLng(busStops[i+1].latitude, busStops[i+1].longitude)
                getRoute(busStop1, busStop2)
            }
            else {
                // Distancia y tiempo entre 2 puntos
//                getDistanceAndTime(busStops[i], busStops[0])
                // Dibujo de ruta
                val busStop1 = LatLng(busStops[i].latitude, busStops[i].longitude)
                val busStop2 = LatLng(busStops[0].latitude, busStops[0].longitude)
                getRoute(busStop1, busStop2)
            }
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
            "driving",
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

    // Add a marker
    private fun createMarker(latitude: Double, longitude: Double, name: String) {
        val location = LatLng(latitude, longitude)
        val bitmap = BitmapFactory.decodeResource(this.resources, R.mipmap.ic_bus_station)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        val markerIcon = BitmapDescriptorFactory.fromBitmap(resizedBitmap)
        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(name)
                .icon(markerIcon)
        )
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
                    val currentLatLong = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 16f))
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
                        val currentLatLong = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 16f))
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
