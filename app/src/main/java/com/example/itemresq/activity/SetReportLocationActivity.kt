package com.example.itemresq.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import com.example.itemresq.R
import com.example.itemresq.databinding.ActivitySetReportLocationBinding
import com.example.itemresq.util.UiUtil
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class SetReportLocationActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    lateinit var binding : ActivitySetReportLocationBinding
    private lateinit var mMap : GoogleMap
    private lateinit var currentLocation : Location
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private lateinit var autocompleteFragment : AutocompleteSupportFragment
    private lateinit var addressToSend : String
    private lateinit var latLngToSend : LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetReportLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Places.initialize(applicationContext, getString(R.string.google_maps_api_dummy_key))
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.search_btn) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                UiUtil.showToast(this@SetReportLocationActivity, "Error in Search")
            }

            override fun onPlaceSelected(place: Place) {
                mMap.clear()
                val address = place.address!!
                val position = place.latLng!!
                val marker = addMarker(position)
                marker.title = address
                addressToSend = address
                latLngToSend = position
                marker.showInfoWindow()
                zoomOnMap(position)
                binding.confirmButton.visibility = View.VISIBLE
            }
        })

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.confirmButton.setOnClickListener {
            val address = addressToSend
            val position = latLngToSend
            val latitude = latLngToSend.latitude
            val longitude = latLngToSend.longitude

            setResult(RESULT_OK, Intent()
                .putExtra("ADDRESS", address)
                .putExtra("LATITUDE", latitude)
                .putExtra("LONGITUDE", longitude)
                .putExtra("POSITION", latLngToSend.toString()))

            finish()
        }
    }

    private fun zoomOnMap(latLng: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun addMarker(position : LatLng) : Marker{
        val marker = mMap.addMarker(MarkerOptions()
            .position(position)
        )
        val geocoder = Geocoder(this)
        val addresses = geocoder.getFromLocation(position.latitude, position.longitude, 1)
        if (addresses != null) {
            if(addresses.isNotEmpty()) {
                val address = addresses[0].getAddressLine(0)
                marker!!.title = address
                addressToSend = address
                latLngToSend = position
            }
        }
        return marker!!
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true

        setUpMap()

        mMap.setOnMapClickListener {
            mMap.clear()
            val marker = addMarker(it)
            marker.showInfoWindow()
            binding.confirmButton.visibility = View.VISIBLE
        }
    }

    private fun setUpMap() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        mMap.isMyLocationEnabled = true
        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if(location != null) {
                currentLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }

    override fun onMarkerClick(p0: Marker) = false
}