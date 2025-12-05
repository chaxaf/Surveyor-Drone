package com.parrot.hellodrone

import android.widget.TextView
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.parrot.drone.groundsdk.device.DeviceState

class DataCollectionViewModel  : ViewModel() {


    // User interface
    lateinit var droneStateTxt: TextView
    lateinit var droneBatteryTxt: TextView
    lateinit var gpsTextView : TextView
    lateinit var  speedTextView : TextView
    lateinit var altitudeTextView : TextView

//     fun resetDroneUi() {
//        // Reset drone user interface views.
//        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
//        droneBatteryTxt.text = ""
//        gpsTextView.text = ""
//        speedTextView.text = ""
//        altitudeTextView.text = ""
//    }

}