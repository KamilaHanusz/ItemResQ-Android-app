package com.example.itemresq.activity

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import com.example.itemresq.R
import com.example.itemresq.databinding.ActivitySingleReportBinding
import com.example.itemresq.model.ReportModel
import com.example.itemresq.util.UiUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class SingleReportActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding : ActivitySingleReportBinding
    private var reportsCollection = FirebaseFirestore.getInstance().collection("reports")
    private lateinit var mMap : GoogleMap
    private var currentImageIndex = 0
    private lateinit var report : ReportModel
    private lateinit var phoneNumber : String
    private lateinit var reportType : String
    private var userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
    private lateinit var reportId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backArrow.setOnClickListener {
            finish()
        }

        binding.previousPicBtn.setOnClickListener {
            navigateToPreviousImage(report)
        }

        binding.nextPicBtn.setOnClickListener {
            navigateToNextImage(report)
        }

        binding.pictureIcon.setOnClickListener {
            if (report.imageUrls.isNotEmpty()) {
                val intent = Intent(this, FullScreenImageActivity::class.java)
                intent.putStringArrayListExtra("IMAGE_URLS", ArrayList(report.imageUrls))
                    .putExtra("CURRENT_IMAGE_INDEX", currentImageIndex)
                startActivity(intent)
            }
        }

        binding.callBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.progressBar.visibility = View.VISIBLE
        binding.rootLayout.visibility = View.GONE

        binding.deleteReportBtn.setOnClickListener {
            deleteReport()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.setAllGesturesEnabled(false)
        mMap.uiSettings.isMyLocationButtonEnabled = false

        reportId = intent.getStringExtra("REPORT_ID").toString()
        val reportRef = reportsCollection.document(reportId!!)

        reportRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                report = documentSnapshot.toObject(ReportModel::class.java)!!
                reportType = report.type

                if (userId == report.reporterId) {
                    binding.deleteReportBtn.visibility = View.VISIBLE
                }

                binding.reportTitle.text = report.title

                if (report.imageUrls?.isNotEmpty() == true) {
                    val firstImageUrl = report.imageUrls[0]
                    Picasso.get().load(firstImageUrl).into(binding.pictureIcon)
                } else {
                    Picasso.get().load(R.drawable.icon_image).placeholder(R.drawable.icon_image).into(binding.pictureIcon)
                }

                updateArrowVisibility()

                binding.reportType.text = reportType
                binding.categoryInfo.text = report.category
                if (report.description == "") {
                    binding.descriptionInfo.text = "Nie podano"
                } else {
                    binding.descriptionInfo.text = report?.description
                }
                val geocoder = Geocoder(this)
                val addresses = geocoder.getFromLocation(report.latitude, report.longitude, 1)
                val address = addresses!![0].getAddressLine(0)

                binding.addressInfo.text = address
                val occurrenceDateConverted = report.occurrenceDate.toDate()
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.occurrenceDateInfo.text = sdf.format(occurrenceDateConverted)

                val reportDateConverted = report.createdTime.toDate()
                binding.reportDateInfo.text = sdf.format(reportDateConverted)

                if (reportType == "Znalezione") {
                    binding.rewardLayout.visibility = View.GONE
                } else {
                    if (report.reward == "") {
                        binding.rewardInfo.text = "Nie podano"
                    } else {
                        binding.rewardInfo.text = report.reward
                    }
                }

                val part1 = report.phoneNumber.substring(0, 3)
                val part2 = report.phoneNumber.substring(3, 6)
                val part3 = report.phoneNumber.substring(6)

                phoneNumber = "+48 $part1 $part2 $part3"
                binding.phoneNumberInfo.text = phoneNumber

                if (report.email == "") {
                    binding.emailInfo.text = "Nie podano"
                } else {
                    binding.emailInfo.text = report.email
                }

                val latitude = report.latitude
                val longitude = report.longitude
                val latLngToSet = LatLng(latitude, longitude)
                setMap(latLngToSet)

                binding.testBtn.setOnClickListener {
                    val startTime = SystemClock.elapsedRealtime()
                    val intent = Intent(this, MapActivity::class.java)
                    intent.putExtra("LATITUDE", report.latitude.toString())
                    intent.putExtra("LONGITUDE", report.longitude.toString())
                    intent.putExtra("START_TIME", startTime)
                    startActivity(intent)
                }

                binding.progressBar.visibility = View.GONE
                binding.rootLayout.visibility = View.VISIBLE

            } else {
                binding.reportTitle.text = "Report not found"
                binding.progressBar.visibility = View.GONE
            }
        } .addOnFailureListener { exception ->
            binding.reportTitle.text = "Error: ${exception.message}"
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun setMap(position : LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 12f))
        val marker = MarkerOptions().position(position)
        if (reportType == "Znalezione") {
            marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        }
        mMap.addMarker(marker)
    }

    private fun navigateToPreviousImage(report: ReportModel) {
        if (currentImageIndex > 0) {
            currentImageIndex--
            loadImage(report.imageUrls[currentImageIndex])
        }
        updateArrowVisibility()
    }

    private fun navigateToNextImage(report: ReportModel) {
        if (currentImageIndex < report.imageUrls.size - 1) {
            currentImageIndex++
            loadImage(report.imageUrls[currentImageIndex])
        }
        updateArrowVisibility()
    }

    private fun loadImage(imageUrl: String) {
        Picasso.get().load(imageUrl).into(binding.pictureIcon)
    }

    private fun updateArrowVisibility() {
        if (currentImageIndex == 0) {
            binding.previousPicBtn.visibility = View.GONE
        } else {
            binding.previousPicBtn.visibility = View.VISIBLE
        }

        if (currentImageIndex == report.imageUrls.size - 1) {
            binding.nextPicBtn.visibility = View.GONE
        } else {
            binding.nextPicBtn.visibility = View.VISIBLE
        }
    }

    private fun deleteReport() {
        val reportId = intent.getStringExtra("REPORT_ID")
        reportsCollection.document(reportId!!)
            .delete()
            .addOnSuccessListener {
                UiUtil.showToast(this, "Zgłoszenie usunięte pomyślnie.")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                UiUtil.showToast(this, "Błąd usuwania zgłoszenia. Spróbuj ponownie później.")
            }
    }

}