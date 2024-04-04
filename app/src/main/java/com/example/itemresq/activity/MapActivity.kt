package com.example.itemresq.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.itemresq.R
import com.example.itemresq.databinding.ActivityMapBinding
import com.example.itemresq.model.ReportModel
import com.example.itemresq.util.MapUtil
import com.example.itemresq.util.UiUtil
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    lateinit var binding : ActivityMapBinding
    private lateinit var mMap : GoogleMap
    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private lateinit var autocompleteFragment : AutocompleteSupportFragment
    private var boundsBuilder = LatLngBounds.Builder()
    private var reportsCollection = FirebaseFirestore.getInstance().collection("reports")
    private var reportsList = mutableListOf<ReportModel>()
    private var latLngToCenter = LatLng(0.0, 0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavBar.menu.findItem(R.id.bottom_menu_map).isChecked = true
        binding.bottomNavBar.menu.findItem(R.id.unchecked_item).isVisible = false

        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(ContextCompat.getColor(this, R.color.application_purple), ContextCompat.getColor(this,
                R.color.gray_for_arrow
            ))
        )

        binding.bottomNavBar.itemIconTintList = colorStateList

        binding.bottomNavBar.setOnItemSelectedListener {menuItem->
            when(menuItem.itemId) {
                R.id.bottom_menu_main ->{
                    startActivity(Intent(this, MainActivity::class.java))
                }
                R.id.bottom_menu_add_report ->{
                    startActivity(Intent(this, AddReportActivity::class.java))
                }
                R.id.bottom_menu_list_reports ->{
                    startActivity(Intent(this, ListAllReportsActivity::class.java))
                }
            }
            false
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        Places.initialize(applicationContext, getString(R.string.google_maps_api_dummy_key))
        autocompleteFragment = supportFragmentManager.findFragmentById(R.id.search_btn) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val position = place.latLng!!
                zoomOnMap(position)
            }
            override fun onError(p0: Status) {
            }
        })

        binding.goBackBtn.setOnClickListener {
            finish()
        }

        binding.backToReportsBtn.setOnClickListener {
            if (reportsList.size == 1) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenter, 18f))
            } else {
                MapUtil.setCamera(mMap, boundsBuilder, 250)
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
//        setUpMap()



        val latitude = intent.getStringExtra("LATITUDE")
        val longitude = intent.getStringExtra("LONGITUDE")
        val startTime = intent.getLongExtra("START_TIME", 0L)

        if (latitude != null && longitude != null) {
            // *************
            reportsCollection
                .whereEqualTo("latitude", latitude.toDouble())
                .whereEqualTo("longitude", longitude.toDouble())
                .get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val report = document.toObject(ReportModel::class.java)
                    reportsList.add(report)
                }
            }
            // *************
            latLngToCenter = LatLng(latitude.toDouble(), longitude.toDouble())
            val marker = MarkerOptions().position(latLngToCenter)
            mMap.addMarker(marker)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngToCenter, 18f))
            val endTime = SystemClock.elapsedRealtime()
            val elapsedTime = endTime - startTime
            //UiUtil.showToast(this, "Czas otwierania mapy: $elapsedTime ms")
            Log.d("LAT", latitude)
            Log.d("LONG", longitude)
            Log.d("CZAS", elapsedTime.toString())
            Log.d("CZAS", reportsList.toString())
        } else {
            setUpMap()
        }




        mMap.setInfoWindowAdapter(this)
        mMap.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        mMap.setOnInfoWindowClickListener { marker ->
            val report = getReportFromMarker(marker)
            report?.let {
                val intent = Intent(this, SingleReportActivity::class.java)
                intent.putExtra("REPORT_ID", it.reportId)
                startActivity(intent)
            }
        }

        mMap.setOnCameraIdleListener {
            updateButtonVisibility()
        }

    }

    private fun setUpMap() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        mMap.isMyLocationEnabled = true
        fetchReports()
    }

    private fun fetchReports() {
        reportsCollection.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val report = document.toObject(ReportModel::class.java)
                reportsList.add(report)
            }

            if (reportsList.size != 0) {
                MapUtil.populateMapWithMarkersAndSetCamera(mMap, boundsBuilder, reportsList, 250)
            }

        } .addOnFailureListener { exception ->
            UiUtil.showToast(this, "Błąd pobierania danych")
        }
    }

    private fun getReportFromMarker(marker: Marker): ReportModel? {
        val clickedMarker = marker.position
        return reportsList.find { it.latitude == clickedMarker.latitude && it.longitude == clickedMarker.longitude }
    }

    override fun getInfoContents(marker: Marker): View? {
        val infoWindow = layoutInflater.inflate(R.layout.report_info_pop_up, null)

        val title = infoWindow.findViewById<TextView>(R.id.infoWindowTitle)
        val category = infoWindow.findViewById<TextView>(R.id.infoWindowCategory)
        val occurrenceDate = infoWindow.findViewById<TextView>(R.id.infoWindowOccurrenceDate)
        val imageView = infoWindow.findViewById<ImageView>(R.id.infoWindowImage)

        val report = getReportFromMarker(marker)
        report?.let {
            title.text = it.title
            category.text = it.category
            val occurrenceDateConverted = it.occurrenceDate?.toDate()
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            occurrenceDate.text = sdf.format(occurrenceDateConverted)

            if (it.imageUrls.isNotEmpty()) {
                val firstImageUrl = report.imageUrls[0]
                Picasso.get().load(firstImageUrl).into(imageView)
            } else {
                Picasso.get().load(R.drawable.icon_image).placeholder(R.drawable.icon_image).into(imageView)
            }
        }
        return infoWindow
    }

    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    private fun zoomOnMap(latLng: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    fun countVisibleMarkers(): Int {
        val bounds = mMap.projection.visibleRegion.latLngBounds
        var visibleMarkersCount = 0

        for (report in reportsList) {
            val markerPosition = LatLng(report.latitude, report.longitude)
            if (bounds.contains(markerPosition)) {
                visibleMarkersCount++
            }
        }
        return visibleMarkersCount
    }


    fun updateButtonVisibility() {
        if (countVisibleMarkers() < reportsList.size) {
            if (reportsList.size == 1) {
                binding.backToReportsBtn.text = "Wróć do zgłoszenia"
            }
            binding.backToReportsBtn.visibility = View.VISIBLE
        } else {
            binding.backToReportsBtn.visibility = View.GONE
        }
    }

}