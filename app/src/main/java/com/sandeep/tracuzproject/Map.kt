package com.sandeep.tracuzproject

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rtchagas.pingplacepicker.PingPlacePicker

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class Map : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location // get the current location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdatestate = false

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SEETING = 2
        private const val PLACE_PICKER_REQUEST = 3 // using place picker api
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        supportActionBar?.setTitle(R.string.map_name)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)

                lastLocation = p0!!.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }
        createLocationRequest()
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            showPlacePicker()
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
        map = googleMap

        // Add a marker in Sydney and move the camera
//           val sydney = LatLng(-34.0, 151.0)
//        val myPlace = LatLng(40.73, -73.99) // New york
//        map.addMarker(MarkerOptions().position(myPlace).title("Favourite Dream city")).showInfoWindow()
//        map.moveCamera(CameraUpdateFactory.newLatLngZoom(myPlace, 12.0f))


        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener (this)
        setUpMap()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }
    @TargetApi(Build.VERSION_CODES.P)

    private fun setUpMap(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        //map.mapType = GoogleMap.MAP_TYPE_HYBRID
        map.setMapStyle(MapStyleOptions(Context.EUICC_SERVICE))

        fusedLocationClient.lastLocation.addOnSuccessListener {
                location -> if (location != null){
            lastLocation = location
            val currentlatlong = LatLng(location.latitude, location.longitude)
            placeMarkerOnMap(currentlatlong)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentlatlong, 12.0f))
        }
        }
    }
    private fun placeMarkerOnMap(location: LatLng){
        val markerOptions = MarkerOptions().position(location)
        val titleString = geoLocationAddress(location)

        markerOptions.title(titleString)

        map.addMarker(markerOptions)

        if (map.addMarker(markerOptions).isInfoWindowShown){
            Log.d("map", "shown")
        }else{
            Log.d("map", "not shown")
        }



//        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(
//            BitmapFactory.decodeResource(resources, R.mipmap.ic_user_location)
//        ))
//        map.addMarker(markerOptions)

    }

    // Add address to the current location
    private fun geoLocationAddress(latLng: LatLng): String{
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try{
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (null != addresses && !addresses.isEmpty()){
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex){
                    addressText += if (i == 0){
                        address.getAddressLine(i)
                    }else{
                        "\n" + address.getAddressLine(i)
                    }

                }

            }
        }catch (ex: Exception){
            Log.e("MapActivity", ex.localizedMessage)
        }
        return addressText
    }
    //Update the location
    private fun startLocationUpdate(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return

        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationRequest(){
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addAllLocationRequests(mutableListOf(locationRequest))

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdatestate = true
            startLocationUpdate()
        }
        task.addOnFailureListener{
                e -> if(e is ResolvableApiException){
            Log.d("map", "error Caused in api:${e.message}")
            Toast.makeText(this@Map, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            //ToDO show dialog : setting not satisfied

            try{
                //TODO dialog to show by calling StartResolutionForResult(), and check result is on activity

                e.startResolutionForResult(this@Map, REQUEST_CHECK_SEETING)

            }catch (ex: Exception){
                // Ignore the error
                Log.d("map", "error occured:${e.message}")
            }
        }
        }


    }
    private fun showPlacePicker(){
        val builder = PingPlacePicker.IntentBuilder()

        builder.setAndroidApiKey(getString(R.string.google_maps_key))
            .setMapsApiKey(getString(R.string.google_maps_key))


        // For docs https://github.com/rtchagas/pingplacepicker
        try{
            startActivityForResult(builder.build(this@Map), PLACE_PICKER_REQUEST)
        }catch(ex: GooglePlayServicesRepairableException){
            ex.printStackTrace()
        }catch (ex: GooglePlayServicesNotAvailableException){
            ex.printStackTrace()
        }

//        try{
//            val placeIntent = builder.build(this@Map)
//
//        }catch (ex: Exception){
//            Toast.makeText(this@Map, "Place service not available", Toast.LENGTH_SHORT).show()
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SEETING){
            if (resultCode == Activity.RESULT_OK){
                locationUpdatestate = true
                startLocationUpdate()
            }
        }
        if(requestCode == PLACE_PICKER_REQUEST){
            if(resultCode == Activity.RESULT_OK){
                val place = PingPlacePicker.getPlace(data!!)
                var addressText = place?.name.toString()
                addressText += "\n" + place?.address.toString()

                placeMarkerOnMap(place?.latLng!!)

            }
        }

    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if(!locationUpdatestate){
            startLocationUpdate()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home, menu)
        return super.onCreateOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.back -> {
                val bluetooth_chat = Intent(this@Map, MainActivity::class.java)
                startActivity(bluetooth_chat)
            }
        }
        return super.onOptionsItemSelected(item)
    }

}
