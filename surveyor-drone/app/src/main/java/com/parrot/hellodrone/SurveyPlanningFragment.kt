package com.parrot.hellodrone

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.instrument.Gps
import com.parrot.drone.groundsdk.device.pilotingitf.FlightPlanPilotingItf
import com.parrot.hellodrone.databinding.FragmentSurveyPlanningBinding
import java.io.File

class SurveyPlanningFragment : Fragment() {

    private lateinit var viewModel: SurveyPlanningViewModel
    private lateinit var binding: FragmentSurveyPlanningBinding

    private val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }


    private lateinit var flightPlanPilotingItf: FlightPlanPilotingItf
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        viewModel = ViewModelProvider(this).get(SurveyPlanningViewModel::class.java)
        binding =
            FragmentSurveyPlanningBinding.inflate(inflater, container, false)

        /**Safety landing**/
        sharedViewModel.droneBatteryInfoRef =
            sharedViewModel.drone?.getInstrument(BatteryInfo::class.java) {
                // Called when the battery info instrument is available and when it changes.

                it?.let {
                    // Update drone battery charge level view.
                    if (it.charge < sharedViewModel.safetyLandingLevel) {
                        sharedViewModel.pilotingItfRef?.let { itf ->
                            itf.land()
                        }
                        Log.d("SharedViewModel", "SAFETY LAND: ${it.charge}")
                        Toast.makeText(context, "SAFETY LAND", Toast.LENGTH_LONG).show()
                    } else if (it.charge < (sharedViewModel.safetyLandingLevel + 5)) {
                        Toast.makeText(
                            context,
                            "Low battery : safety landing soon",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        /**Safety landing**/


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // map related buttons, with internet connection
        // val mapView = view.findViewById<MapView>(R.id.map)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        // Initialize the clear map button
        val clearMapButton = view.findViewById<Button>(R.id.clearMapButton)
        clearMapButton.setOnClickListener {
            // Clear the waypoints list
            viewModel.waypoints.clear()

            // Remove all markers from the map
            for (marker in viewModel.markers) {
                marker.remove()
            }
            viewModel.markers.clear()
        }

        // Initialize the select waypoints button
        val selectWaypointsButton = view.findViewById<Button>(R.id.selectWaypointsButton)
        selectWaypointsButton?.setOnClickListener {
            viewModel.isSelectingWaypoints = true

            // Change the button color to red when pressed
            selectWaypointsButton.backgroundTintList = ColorStateList.valueOf(Color.RED)
        }

        // Initialize the clear last waypoint button
        val clearLastWaypointButton = view.findViewById<Button>(R.id.clearLastWaypointButton)
        clearLastWaypointButton?.setOnClickListener {
            if (viewModel.waypoints.isNotEmpty() && viewModel.markers.isNotEmpty()) {
                // Remove the last waypoint from the waypoints list
                viewModel.waypoints.removeAt(viewModel.waypoints.size - 1)

                // Remove the corresponding marker from the map
                viewModel.markers.removeAt(viewModel.markers.size - 1).remove()
            }
        }

        // Initialize the save mission button
        val saveMissionButton = view?.findViewById<Button>(R.id.saveMissionButton)
        saveMissionButton?.setOnClickListener {
            // Create a mission from the waypoints list
            val mission = createMission(viewModel.waypoints)

            // Save the mission
            if (mission != null) {
                viewModel.missions.add(mission)
            } else {
                Toast.makeText(context, "No waypoints selected.", Toast.LENGTH_LONG).show()
            }

            // Reset the waypoints list and the markers list
            viewModel.waypoints.clear()
            for (marker in viewModel.markers) {
                marker.remove()
            }
            viewModel.markers.clear()

            // Reset the select waypoints button
            viewModel.isSelectingWaypoints = false
            selectWaypointsButton.backgroundTintList = ColorStateList.valueOf(Color.LTGRAY)
        }


        // Set the onMapReady callback
        mapFragment.getMapAsync(this::onMapReady)


        val liftoffButton = view?.findViewById<Button>(R.id.liftoffButton)
        liftoffButton?.setOnClickListener {
            sendWaypointsToDrone()
            // flightPlanPilotingItf?.activate(true)
            // checkConditionsAndActivate()
            val unavailabilityReasons = flightPlanPilotingItf?.unavailabilityReasons
            if (unavailabilityReasons != null && unavailabilityReasons.isEmpty()) {
                flightPlanPilotingItf?.activate(true)
            } else {
                println("Cannot activate FlightPlanPilotingItf due to the following reasons: $unavailabilityReasons")
                Toast.makeText(context, "Cannot activate $unavailabilityReasons", Toast.LENGTH_LONG)
                    .show()
            }

        }


//        // Initialize the return home button
//        val returnHomeButton = view.findViewById<Button>(R.id.returnHomeButton)
//        returnHomeButton.setOnClickListener {
//            // Command the drone to return home
//            sharedViewModel?.drone?.let { drone ->
//                val returnHomePilotingItf = drone.getPilotingItf(ReturnHomePilotingItf::class.java)
//                returnHomePilotingItf?.activate()
//            }
//        }

        /** **/
        val returnHomeButton = view.findViewById<Button>(R.id.returnHomeButton)
        returnHomeButton.setOnClickListener {
            sharedViewModel?.drone?.let { drone ->
                val gps = drone.getInstrument(Gps::class.java)
                if (gps?.fixed == true) {
                    val mavlinkFile = File(context?.filesDir, "mission.mavlink")

                    // Write the initial location to the .mavlink file
                    mavlinkFile.printWriter().use { out ->
                        // Write the header
                        out.println("QGC WPL 110")
                        // Write the initial location as the waypoint
                        viewModel.initialLocation?.let {
                            out.println("0\t0\t3\t16\t0\t0\t0\t0\t${it.latitude}\t${it.longitude}\t2\t1")
                        }
                    }

                    // Show a toast message
                    Toast.makeText(context, "Mission created", Toast.LENGTH_SHORT).show()
                    sendWaypointsToDrone()
                    // flightPlanPilotingItf?.activate(true)
                    // checkConditionsAndActivate()
                    val unavailabilityReasons = flightPlanPilotingItf?.unavailabilityReasons
                    if (unavailabilityReasons != null && unavailabilityReasons.isEmpty()) {
                        flightPlanPilotingItf?.activate(true)
                    } else {
                        println("Cannot activate FlightPlanPilotingItf due to the following reasons: $unavailabilityReasons")
                        Toast.makeText(
                            context,
                            "Cannot activate $unavailabilityReasons",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    Toast.makeText(context, "Return home activated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Cannot return home, no GPS fix", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        /** **/


        // Initialize the upload flight plan button
        val uploadFlightPlan = view.findViewById<Button>(R.id.uploadFPButton)
        uploadFlightPlan.setOnClickListener {
            sendWaypointsToDrone()
        }
    }

    var isCameraMoved = false
    fun onMapReady(googleMap: GoogleMap) {
        viewModel.map = googleMap
        // Enable zoom controls
        viewModel.map.uiSettings.isZoomControlsEnabled = true

        // Check if location permissions are granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.map.isMyLocationEnabled = true
            viewModel.map.setOnMyLocationChangeListener { location ->
                if (location != null && !isCameraMoved) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    viewModel.map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            currentLatLng,
                            15f
                        )
                    ) // Adjust the zoom level here
                    isCameraMoved = true
                }
            }
        } else {
            // Request location permissions
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                viewModel.LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        // Set a click listener for the map
        viewModel.map.setOnMapClickListener { latLng ->
            if (viewModel.isSelectingWaypoints) {
                // Add a marker at the clicked location
                val marker = viewModel.map.addMarker(
                    MarkerOptions().position(latLng).icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                )
                if (marker != null) {
                    viewModel.markers.add(marker)
                }
                // Add the location to the waypoints list
                viewModel.waypoints.add(latLng)
            }
        }
    }

    private fun createMission(waypoints: List<LatLng>): File? {
        sharedViewModel.drone?.getInstrument(Gps::class.java)?.lastKnownLocation?.let {
            viewModel.initialLocation = LatLng(it.latitude, it.longitude)
        }
        if (waypoints.isNotEmpty()) {
            // Create a new .mavlink file
            val mavlinkFile = File(context?.filesDir, "mission.mavlink")

            // Write the waypoints to the .mavlink file
            mavlinkFile.printWriter().use { out ->
                // Write the header
                out.println("QGC WPL 110")
                // Write the waypoints
                waypoints.forEachIndexed { index, waypoint ->
                    out.println("$index\t0\t3\t16\t0\t0\t0\t0\t${waypoint.latitude}\t${waypoint.longitude}\t2\t0")
                }
                // Write the initial location as the last waypoint
                viewModel.initialLocation?.let {
                    out.println("${waypoints.size}\t0\t3\t16\t0\t0\t0\t0\t${it.latitude}\t${it.longitude}\t2\t1")
                }
            }

            // Show a toast message
            Toast.makeText(context, "Mission created", Toast.LENGTH_SHORT).show()

            return mavlinkFile
        } else {
            return null
        }
    }

    fun sendWaypointsToDrone() {
        Log.d("YourFragment", "entering sendWaypointsToDrone")
        sharedViewModel?.drone?.let { drone ->
            flightPlanPilotingItf =
                drone.getPilotingItf(FlightPlanPilotingItf::class.java) ?: return@let
        }
        if (sharedViewModel.drone != null) {
            if (viewModel.missions.isNotEmpty()) {
                // Retrieve the last saved mission
                val mission = viewModel.missions.last()
                flightPlanPilotingItf?.uploadFlightPlan(mission)
                Log.d("YourFragment", "FlightPLan uploaded on drone with waypoints function")
                Toast.makeText(context, "FlightPlan uploaded on drone", Toast.LENGTH_SHORT).show()

            } else {
                Log.d("YourFragment", "No mission to upload")
                Toast.makeText(context, "No mission to upload", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Please connect to the drone", Toast.LENGTH_SHORT).show()
        }
    }
}

