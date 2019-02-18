package com.ptrprograms.androidmaps

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener {

    private lateinit var mMap: GoogleMap

    private val LOCATION_PERMISSION = 42

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var compassEnabled = false
    private var tiltEnabled = false
    private var indoorLevelPickerEnabled = false
    private var scrollEnabled = false
    private var rotateEnabled = false
    private var zoomEnabled = false
    private var buildingsEnabled = false
    private var boundariesEnabled = false
    private var currentLocation: Location? = null

    private val DENVER = LatLng(39.7392, -104.9903)
    private val FRESNO = LatLng(36.7378, -119.7871)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            initMap()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION)
        }
    }

    private fun initMap() {
        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                updateMapLocation(location)
            }

        initLocationTracking()

    }

    private fun initLocationTracking() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    updateMapLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            LocationRequest(),
            locationCallback,
            null)
    }

    override fun onResume() {
        super.onResume()
        if( ::mMap.isInitialized ) {
            initLocationTracking()
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateMapLocation(location: Location?) {
        currentLocation = location
        mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(
            location?.latitude ?: 0.0,
            location?.longitude ?: 0.0)))

        updateUiSettings()
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15.0f))
    }

    private fun updateUiSettings() {
        mMap.isBuildingsEnabled = buildingsEnabled
        mMap.uiSettings.isTiltGesturesEnabled = tiltEnabled
        mMap.uiSettings.isCompassEnabled = compassEnabled
        mMap.uiSettings.isIndoorLevelPickerEnabled = indoorLevelPickerEnabled
        mMap.uiSettings.isRotateGesturesEnabled = rotateEnabled
        mMap.uiSettings.isScrollGesturesEnabled = scrollEnabled
        mMap.uiSettings.isZoomControlsEnabled = zoomEnabled
        mMap.uiSettings.isZoomGesturesEnabled = zoomEnabled

        if( boundariesEnabled && currentLocation != null ) {
            val bounds = LatLngBounds(
                LatLng(currentLocation!!.latitude - 0.001, currentLocation!!.longitude - 0.001),
                LatLng(currentLocation!!.latitude + 0.001, currentLocation!!.longitude + 0.001))
            mMap.setLatLngBoundsForCameraTarget(bounds)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION) {
            if (permissions.size == 1 &&
                permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                initMap()
            }
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "my location button click", Toast.LENGTH_LONG).show()
        return false
    }

    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(this, "my location click", Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.map_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if( item?.isCheckable ?: false ) {
            item!!.isChecked = !item.isChecked
        }

        when( item?.itemId ) {
            R.id.compass -> {
                compassEnabled = item.isChecked
            }
            R.id.tilt -> {
                tiltEnabled = item.isChecked
            }
            R.id.indoor -> {
                indoorLevelPickerEnabled = item.isChecked
            }
            R.id.scroll -> {
                scrollEnabled = item.isChecked
            }
            R.id.rotate -> {
                rotateEnabled = item.isChecked
            }
            R.id.zoom -> {
                zoomEnabled = item.isChecked
            }
            R.id.buildings -> {
                buildingsEnabled = item.isChecked
            }
            R.id.boundaries -> {
                boundariesEnabled = item.isChecked
            }
        }

        when( item?.itemId ) {
            R.id.animate -> {
                val cameraPositionFresno = CameraPosition.Builder()
                    .bearing(0f)
                    .zoom(15f)
                    .target(FRESNO)
                    .build()

                val cameraPositionDenver = CameraPosition.Builder()
                    .bearing(0f)
                    .zoom(15f)
                    .target(DENVER)
                    .build()

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPositionFresno), 3000, object: GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        Thread.sleep(3000)
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPositionDenver), 3000, null)
                    }

                    override fun onCancel() { }
                })

            }
            else -> {
                updateUiSettings()
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
