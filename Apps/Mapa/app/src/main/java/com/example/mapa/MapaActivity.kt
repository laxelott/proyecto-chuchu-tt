package com.example.mapa

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapa.databinding.ActivityMapaTransporteBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions


class MapaActivity : AppCompatActivity(), OnMapReadyCallback, OnMyLocationButtonClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaTransporteBinding
    companion object{
        const val REQUEST_CODE_LOCATION = 0
    }
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private var busStops: ArrayList<TransportInformationActivity.BusStop>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaTransporteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        /**
         * Getting all the bus stations of the line selected
         */
        busStops = intent.getSerializableExtra("busStops") as ArrayList<TransportInformationActivity.BusStop>?

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
        mMap.uiSettings.isZoomControlsEnabled = true

        enableLocation()

        //Creating the markers
        if (busStops != null) {
            for ((i, busStop) in busStops!!.withIndex())
            {
                createMarker(busStop.latitude, busStop.longitude, busStop.name)
            }
        }
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
                    val currentLatLong = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 12f))
                }
            }

            //Get the location button

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
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                mMap.isMyLocationEnabled = true
                //Go to my current location
                fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) {
                        location ->
                    if (location != null){
                        lastLocation = location
                        val currentLatLong = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 16f))
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
        if(!::mMap.isInitialized) return
        if (!isLocationPermissionGranted()){
            mMap.isMyLocationEnabled = false
            Toast.makeText(this, "Ve a ajustes para aceptar los permisos de localizacion", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }




}