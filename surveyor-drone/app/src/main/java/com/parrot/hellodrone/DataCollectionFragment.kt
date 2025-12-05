package com.parrot.hellodrone

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.instrument.Altimeter
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.instrument.Gps
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.FirebaseDatabase

class DataCollectionFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var viewModel: DataCollectionViewModel
    private val loginViewModel: LoginViewModel by lazy { ViewModelProvider(requireActivity()).get(LoginViewModel::class.java) }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        viewModel = ViewModelProvider(this).get(DataCollectionViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        val view = inflater.inflate(R.layout.fragment_data_collection, container, false)

        viewModel.droneStateTxt = view.findViewById(R.id.droneStateTxt)
        viewModel.droneBatteryTxt = view.findViewById(R.id.droneBatteryTxt)
        viewModel.gpsTextView = view.findViewById(R.id.gpsTextView)
        viewModel.altitudeTextView = view.findViewById(R.id.altitudeTextView)

        /**Safety landing**/
        sharedViewModel.droneBatteryInfoRef = sharedViewModel.drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                if(it.charge < sharedViewModel.safetyLandingLevel){
                    sharedViewModel.pilotingItfRef?.let { itf ->
                        itf.land()
                    }
                    Log.d("SharedViewModel", "SAFETY LAND: ${it.charge}")
                    Toast.makeText(context,"SAFETY LAND", Toast.LENGTH_LONG).show()
                }else if(it.charge < (sharedViewModel.safetyLandingLevel+5)){
                    Toast.makeText(context,"Low battery : safety landing soon", Toast.LENGTH_LONG).show()
                }
            }
        }
        /**Safety landing**/

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.droneConnectionStatus.observe(viewLifecycleOwner) { connectionState ->
            if (connectionState == DeviceState.ConnectionState.CONNECTED || connectionState == DeviceState.ConnectionState.CONNECTING ) {
                sharedViewModel.drone?.let { drone ->
                    // Start monitoring the drone.
                    //
                 startDroneMonitors()
                }
            }
            else {
                 //viewModel.resetDroneUi()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopDroneMonitors()
    }
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.

        sharedViewModel.droneStateRef?.close()
        sharedViewModel.droneStateRef = null

        sharedViewModel.droneBatteryInfoRef?.close()
        sharedViewModel.droneBatteryInfoRef = null

        sharedViewModel.gpsRef?.close()
        sharedViewModel.gpsRef = null

        sharedViewModel.altitudeRef?.close()
        sharedViewModel.altitudeRef = null
    }

    private fun startDroneMonitors() {
        monitorDroneState()

        monitorDroneBatteryChargeLevel()

        if (sharedViewModel.gpsRef!= null) {
            monitorDroneGps()
        }

        if (sharedViewModel.altitudeRef != null) {
            monitorDroneAltitude()
        }
    }


    private fun monitorDroneBatteryChargeLevel() {
        // Monitor the battery info instrument.
        sharedViewModel.droneBatteryInfoRef = sharedViewModel.drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                viewModel.droneBatteryTxt.text = getString(R.string.percentage, it.charge)
            }
        }
    }

    private fun monitorDroneState() {
        // Monitor current drone state.
        sharedViewModel.droneStateRef = sharedViewModel.drone?.getState {
            // Called at each drone state update.

            it?.let {
                // Update drone connection state view.
                viewModel.droneStateTxt.text = it.connectionState.toString()
            }
        }
    }
    private fun monitorDroneGps() {
        // Get the user's key from LoginViewModel
        val userKey = loginViewModel.getUserKey()

        // Monitor the gps instrument.
        sharedViewModel.gpsRef = sharedViewModel.drone?.getInstrument(Gps::class.java) {
            // Called when the gps instrument is available and when it changes.

            it?.let { gps ->
                // Check if the last known location is not null
                val location = gps.lastKnownLocation
                if (location != null) {
                    // Update drone gps view with coordinates.
                    viewModel.gpsTextView.text = String.format("Latitude: %.2f\nLongitude: %.2f", location.latitude, location.longitude)

                    // Get a reference to the Firebase database
                    val database = FirebaseDatabase.getInstance()

                    // Get a reference to the user's profile in the database
                    val userRef = database.getReference("Profiles").child(userKey)

                    // Get the current timestamp
                    val timestamp = System.currentTimeMillis()

                    // Write the location data to Firebase under the user's profile
                    userRef.child("gps").child(timestamp.toString()).setValue(location)
                }
            }
        }
    }

    private fun monitorDroneAltitude() {
        // Get the user's key from LoginViewModel
        val userKey = loginViewModel.getUserKey()

        // Monitor the altitude instrument.
        sharedViewModel.altitudeRef = sharedViewModel.drone?.getInstrument(Altimeter::class.java) {
            // Called when the altitude instrument is available and when it changes.

            it?.let { altimeter ->
                // Check if the absolute altitude is not null
                altimeter.absoluteAltitude?.let { absoluteAltitude ->
                    // Update drone altitude view.
                    viewModel.altitudeTextView.text = String.format("Altitude: %.2f m", absoluteAltitude)
                    Log.d("SharedViewModel", "altitude: ${absoluteAltitude}")

                    // Get a reference to the Firebase database
                    val database = FirebaseDatabase.getInstance()

                    // Get a reference to the user's profile in the database
                    val userRef = database.getReference("Profiles").child(userKey)

                    // Get the current timestamp
                    val timestamp = System.currentTimeMillis()

                    // Write the altitude data to Firebase under the user's profile
                    userRef.child("altitude").child(timestamp.toString()).setValue(absoluteAltitude)
                } ?: run {
                    // Clear the altitude view if the absolute altitude is null.
                    viewModel.altitudeTextView.text = ""
                }

                // Check if the vertical speed is not null
                altimeter.verticalSpeed?.let { verticalSpeed ->
                    // Update drone speed view.
                    viewModel.speedTextView.text = String.format("Speed: %.2f m/s", verticalSpeed)
                    Log.d("SharedViewModel", "speed: ${verticalSpeed}")
                    // Get a reference to the Firebase database
                    val database = FirebaseDatabase.getInstance()

                    // Get a reference to the user's profile in the database
                    val userRef = database.getReference("Profiles").child(userKey)

                    // Get the current timestamp
                    val timestamp = System.currentTimeMillis()

                    // Write the speed data to Firebase under the user's profile
                    userRef.child("speed").child(timestamp.toString()).setValue(verticalSpeed)
                } ?: run {
                    // Clear the speed view if the vertical speed is null.
                    viewModel.speedTextView.text = ""
                }
            }
        }
    }

}