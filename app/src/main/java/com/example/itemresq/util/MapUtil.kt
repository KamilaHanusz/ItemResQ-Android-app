package com.example.itemresq.util

import android.content.Context
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.example.itemresq.model.ReportModel

object MapUtil {

    fun populateMapWithMarkersAndSetCamera(
        mMap: GoogleMap,
        boundsBuilder: LatLngBounds.Builder,
        reports: List<ReportModel>,
        padding: Int
    ) {
        for (report in reports) {
            val marker = MarkerOptions().position(LatLng(report.latitude, report.longitude))
            if (report.type == "Znalezione") {
                marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            }
            mMap.addMarker(marker)
            boundsBuilder.include(LatLng(report.latitude, report.longitude))
        }
        setCamera(mMap, boundsBuilder, padding)
    }

    fun setCamera(mMap: GoogleMap, boundsBuilder: LatLngBounds.Builder, padding: Int) {
        val bounds = boundsBuilder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
}
