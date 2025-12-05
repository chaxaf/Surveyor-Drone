package com.parrot.hellodrone

import android.graphics.Bitmap
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection
import android.widget.Toast
import com.parrot.drone.groundsdk.device.instrument.Altimeter
import com.parrot.drone.groundsdk.device.instrument.Gps


class SharedViewModel : ViewModel() {

    /** Current drone instance. */
    var drone: Drone? = null
    var mediaStoreRef: Ref<MediaStore>? = null
    val droneConnectionStatus = MutableLiveData<DeviceState.ConnectionState>()
    var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    var pilotingItfRef: ManualCopterPilotingItf? = null
    var safetyLandingLevel : Int = 10

    /** for data collection **/

    var droneStateRef: Ref<DeviceState>? = null
    var gpsRef : Ref<Gps>? = null
    var speedRef : Ref<Altimeter>? = null
    var altitudeRef : Ref<Altimeter>? = null

    fun droneConnection(mainActivity: MainActivity) {
        // Check if mainActivity is not null and access groundSdk
        mainActivity?.let {
            val groundSdk = it.getGroundSdkInstance()

            // Monitor the auto connection facility.
            groundSdk.getFacility(AutoConnection::class.java) {
                // Called when the auto connection facility is available and when it changes.
                it?.let {
                    // Start auto connection.
                    if (it.status != AutoConnection.Status.STARTED) {
                        it.start()
                        Log.d("YourFragment", "drone is connected")
                    }
                    if (drone?.uid != it.drone?.uid) {
                        drone = it.drone
                        droneConnectionStatus.value = drone?.state?.getConnectionState()
                        Log.d("SharedViewModel", "Drone Connection State: ${drone?.state?.getConnectionState()}")
                    }
                }
            }
        }
    }

    fun checkBatteryLevelAndLand() {

        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            // Called when the battery info instrument is available and when it changes.

            it?.let {
                // Update drone battery charge level view.
                Log.d("SharedViewModel", "Charge: ${it.charge}")
                if(it.charge < 10){
                    pilotingItfRef?.let { itf ->
                            itf.land()
                    }
                    Log.d("SharedViewModel", "SAFETY LAND: ${it.charge}")

                }
            }
        }
    }

    /** Data **/

    var streamServerRef: Ref<StreamServer>? = null
    var streamServer: StreamServer? = null

    var droneState: MutableLiveData<String> =
    MutableLiveData(DeviceState.ConnectionState.DISCONNECTED.toString())
    var droneBattery: MutableLiveData<String> = MutableLiveData("%")
    var rcState: MutableLiveData<String> =
        MutableLiveData(DeviceState.ConnectionState.DISCONNECTED.toString())
    var rcBattery: MutableLiveData<String> = MutableLiveData("%")

    var indexingState: MutableLiveData<String> = MutableLiveData(MediaStore.IndexingState.UNAVAILABLE.toString())
    var photoMediaCount: MutableLiveData<String> = MutableLiveData("n/a")
    var videoMediaCount: MutableLiveData<String> = MutableLiveData("n/a")

    var observingMediaList = false
    var mediaItemList: MutableLiveData<MediaItemList> = MutableLiveData<MediaItemList>()

    var selectedMedia: MediaItem? = null
    var selectedMediaType:MutableLiveData<String> = MutableLiveData("n/a")
    var selectedMediaPosition: Int = 0

}

data class MediaItemList(
    var mediaList: Ref<MutableList<MediaItem>>? = null, var bitmapList: MutableList<Bitmap> = mutableListOf()
)