package com.parrot.hellodrone

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import java.io.File

class SurveyPlanningViewModel : ViewModel() {
    lateinit var map: GoogleMap
    val waypoints = mutableListOf<LatLng>()
    val LOCATION_PERMISSION_REQUEST_CODE = 1
    var isSelectingWaypoints = false
    val markers = mutableListOf<Marker>()
    val missions = mutableListOf<File>()
    var initialLocation: LatLng? = null
}