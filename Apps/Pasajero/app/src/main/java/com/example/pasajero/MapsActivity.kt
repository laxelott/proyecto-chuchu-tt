package com.example.pasajero

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var btnLocation: ImageView
    private lateinit var adapter: BusStopAdapter
    private lateinit var stationsButton: ImageView
    private var busStops = ArrayList<BusStop>()

    //Variables to ask for the location permission
    companion object{
        const val REQUEST_CODE_LOCATION = 0
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var currentLocation: LatLng

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

        /**
         * Getting all the bus stations of the line selected
         */
        busStops = intent.getSerializableExtra("busStops") as ArrayList<BusStop>
        btnLocation = findViewById(R.id.location_button)

    }

    fun setupRecyclerView(mMap: GoogleMap) {
        binding.rvEstaciones.layoutManager = LinearLayoutManager(this)
        adapter = BusStopAdapter(busStops,mMap,binding)
        binding.rvEstaciones.adapter = adapter
    }

    fun filtrar (texto: String) {
        var listaFiltrada = ArrayList<BusStop>()

        busStops.forEach {
            if (it.name.toLowerCase().contains(texto.lowercase())) {
                listaFiltrada.add(it)
            }
        }

        adapter.filtrar(listaFiltrada)
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
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,R.raw.custom_map_style))
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = true

        enableLocation()

        //Log.d("MapsActivity", currentLocation.toString())

        btnLocation.setOnClickListener{
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
        }

        //Creating the markers
        for ((i, busStop) in busStops.withIndex())
        {
            createMarker(busStop.latitude, busStop.longitude, busStop.name)
        }


        setupRecyclerView(mMap)

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
                filtrar(p0.toString())
            }

        })


    }

    // Add a marker
    private fun createMarker(latitude:Double, longitude:Double, name:String) {
        val location = LatLng(latitude, longitude)
        mMap.addMarker(MarkerOptions()
            .position(location)
            .title(name)
            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_bus_station))
        )
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


}