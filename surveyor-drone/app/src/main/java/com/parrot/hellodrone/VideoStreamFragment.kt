package com.parrot.hellodrone

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.peripheral.MainCamera
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.camera.Camera
import com.parrot.drone.groundsdk.device.peripheral.camera.CameraPhoto
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.stream.GsdkStreamView

/**
 * A simple [Fragment] subclass.
 * Use the [VideoStreamFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoStreamFragment : Fragment() {
    private lateinit var viewModel: VideoStreamViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_video_stream, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewModel = ViewModelProvider(this).get(VideoStreamViewModel::class.java)
        viewModel.thumbnailObserverRef = ThumbnailObserver() //initialize the thumbnail observer

        // Get user interface instances.
        viewModel.streamView = view.findViewById(R.id.stream_view)
        viewModel.messageTextView = view.findViewById(R.id.messageTextView)
        viewModel.noStreamIcon = view.findViewById(R.id.noStream)
        viewModel.connectedDroneIcon = view.findViewById(R.id.droneConnected)
        viewModel.notConnectedDroneIcon = view.findViewById(R.id.droneNotConnected)

        viewModel.dataParamButton = view.findViewById(R.id.dataParam)
        viewModel.dataParamButton.setOnClickListener{
            Navigation.findNavController(view as View)
                .navigate(R.id.action_videoStreamFragment_to_dataParam)
        }

        viewModel.EmergencyButton = view.findViewById(R.id.emergencyLanding)
        viewModel.EmergencyButton.setOnClickListener{
            sharedViewModel.pilotingItfRef?.let { itf ->
                itf.land()
            }
            Log.d("SharedViewModel", "EMERGENCY LANDIND")
            Toast.makeText(context,"EMERGENCY LANDIND", Toast.LENGTH_LONG).show()

        }

        setCameraMode(Camera.Mode.PHOTO)

        viewModel.capturePhotoButton = view.findViewById(R.id.capturePhotoButton)
        viewModel.capturePhotoButton.setOnClickListener{
            setPhotoMode(CameraPhoto.Mode.SINGLE)
            toggleStartStopPhotoCapture()
            Toast.makeText(context,"Capture photo", Toast.LENGTH_SHORT).show()
        }

        val captureVideoButton: ToggleButton = view.findViewById(R.id.captureVideoButton)
        val toggleButtonListener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                // ToggleButton is in the "On" state
                // Perform actions for the "On" state
                Log.d("Datacollect", "check")
                Toast.makeText(context,"Start video", Toast.LENGTH_SHORT).show()
                toggleStartStopRecord()
                setCameraMode(Camera.Mode.RECORDING)
            } else {
                // ToggleButton is in the "Off" state
                // Perform actions for the "Off" state
                Toast.makeText(context,"Stop video", Toast.LENGTH_SHORT).show()
                Log.d("Datacollect", "not check")
                toggleStartStopRecord()
                setCameraMode(Camera.Mode.PHOTO)
            }
        }
        captureVideoButton.setOnCheckedChangeListener(toggleButtonListener)

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
        Log.d("videophoto", "drone observer")

        sharedViewModel.droneState.observe(viewLifecycleOwner) { droneState ->
            Log.d("videophoto", droneState)
            if (droneState == DeviceState.ConnectionState.DISCONNECTED.toString()) {
                Log.d("videophoto", "vuDISCONNECTED")
            }
            else if(droneState == DeviceState.ConnectionState.CONNECTED.toString()){
                Log.d("videophoto", "COnnected but missing")
            }
            else{
                Log.d("photovideo", "autre")
            }
        }
    }


    fun setCameraMode(mode: Camera.Mode) {
        sharedViewModel.drone?.getPeripheral(MainCamera::class.java)?.run {
            // set setting value
            mode().value = mode
        }
    }

    fun setPhotoMode(mode: CameraPhoto.Mode) {
        sharedViewModel.drone?.getPeripheral(MainCamera::class.java)?.run {
            // set setting value
            photo().setMode(mode)
        }
    }
    fun toggleStartStopPhotoCapture() {
        sharedViewModel.drone?.getPeripheral(MainCamera::class.java)?.run {
            when {
                // photo capture can be started
                canStartPhotoCapture() -> startPhotoCapture()

                // photo capture can be stopped
                canStopPhotoCapture() -> stopPhotoCapture()

                // photo capture can't be started or stopped
                else -> println("Photo capture can't be started or stopped")
            }
        }
    }

    /** Starts or stops video recording. */
    fun toggleStartStopRecord() {
        sharedViewModel.drone?.getPeripheral(MainCamera::class.java)?.run {
            Log.d("videophoto", "toggleStartStopRecord")

            when {

                // recording can be started
                canStartRecording() -> startRecording()

                // recording can be stopped
                canStopRecording() -> stopRecording()

                // recording can't be started or stopped
                else -> println("Video recording can't be started or stopped")
            }
        }
    }


    // TODO : function to reset the stream if certain conditions are met (I.E drone changes or
    fun resetStream () {
        // if ()
        viewModel.streamView.setStream(null)
    }

    override fun onStart() {
        super.onStart()
        sharedViewModel.droneConnection(activity as MainActivity)
        monitorMediaStoreState()
        (activity as MainActivity).setBottomNavigationVisibility(View.VISIBLE)
    }

    override fun onResume() {
        super.onResume()

        displayPhoto()
        Log.d("YourFragment", "ON RESUME")

        sharedViewModel.droneConnectionStatus.observe(this, Observer { connectionState ->
            when (connectionState) {

                DeviceState.ConnectionState.CONNECTING, DeviceState.ConnectionState.CONNECTED -> {
                    Log.d("YourFragment", "Drone Connection State on: ${sharedViewModel.drone?.state?.getConnectionState()}")
                    // Start video stream.
                    startVideoStream()
                    //monitorMediaStoreState()
                    showStream()
                }
                DeviceState.ConnectionState.DISCONNECTED -> {
                    Log.d("YourFragment", "Drone Connection State off: ${sharedViewModel.drone?.state?.getConnectionState()}")
                }
                else -> {
                    // Stop video stream and show error message.
                    stopVideoStream()
                    showNotConnectedMessage()
                }
            }
        })

        sharedViewModel.checkBatteryLevelAndLand()
    }

    private fun showNotConnectedMessage() {
        viewModel.streamView.visibility = View.GONE
        viewModel.messageTextView.text = getString(R.string.not_connected)
        viewModel.noStreamIcon.visibility = View.VISIBLE
        viewModel.connectedDroneIcon.visibility= View.GONE
        viewModel.notConnectedDroneIcon.visibility= View.VISIBLE
    }

    private fun showStream() {
        viewModel.streamView.visibility = View.VISIBLE
        viewModel.messageTextView.text = getString(R.string.connected)
        viewModel.noStreamIcon.visibility = View.GONE
        viewModel.connectedDroneIcon.visibility= View.VISIBLE
        viewModel.notConnectedDroneIcon.visibility= View.GONE
    }

    override fun onStop() {
        super.onStop()
        viewModel.liveStreamRef?.close()
        viewModel.liveStreamRef = null

        viewModel.streamServerRef?.close()
        viewModel.streamServerRef = null

        sharedViewModel.mediaStoreRef?.close()
        sharedViewModel.mediaStoreRef = null


        viewModel.liveStream = null
    }

    /**
     * Starts the video stream.
     */
    private fun startVideoStream() {
        Log.d("YourFragment", "Start video stream")

        // Monitor the stream server.
        viewModel.streamServerRef = sharedViewModel.drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            // Called when the stream server is available and when it changes.

            if (streamServer != null) {
                // Enable Streaming
                if(!streamServer.streamingEnabled()) {
                    streamServer.enableStreaming(true)
                }

                // Monitor the live stream.
                if (viewModel.liveStreamRef == null) {
                    viewModel.liveStreamRef = streamServer.live { liveStream ->
                        // Called when the live stream is available and when it changes.

                        if (liveStream != null) {
                            if (this.viewModel.liveStream == null) {
                                // It is a new live stream.

                                // Set the live stream as the stream
                                // to be render by the stream view.
                                viewModel.streamView.setStream(liveStream)
                            }

                            // Play the live stream.
                            if (liveStream.playState() != CameraLive.PlayState.PLAYING) {
                                liveStream.play()
                            }
                        } else {
                            // Stop rendering the stream
                            Log.d("YourFragment", " null livestream")

                            viewModel.streamView.setStream(null)
                        }
                        // Keep the live stream to know if it is a new one or not.
                        this.viewModel.liveStream = liveStream
                    }
                }
            } else {
                // Stop monitoring the live stream
                viewModel.liveStreamRef?.close()
                viewModel.liveStreamRef = null
                // Stop rendering the stream
                viewModel.streamView.setStream(null)
                Log.d("YourFragment", "Start video stream : null serveur")

            }
        }
    }

    private fun stopVideoStream() {
        Log.d("YourFragment", "Stop video stream")

        viewModel.liveStreamRef?.close()
        viewModel.liveStreamRef = null
        viewModel.thumbnailObserverRef = null
        viewModel.streamServerRef?.close()
        viewModel.streamServerRef = null

        viewModel.liveStream = null
    }

    private fun monitorMediaStoreState() {
        //Get an instance of the drone's MediaStore and use it to constantly monitor the media content on the SD card7
        Log.d("videophoto", "monitorMediaStoreState()")

        if (sharedViewModel.mediaStoreRef != null ) {
            // The Ref is still active and available
            Log.d("videophoto", "ref ok()")
        } else {
            Log.d("videophoto", "ref not ok()")
            // Handle accordingly
        }


        sharedViewModel.mediaStoreRef = sharedViewModel.drone?.getPeripheral(MediaStore::class.java) { ms ->
            Log.d("videophoto", "monitorMediaStoreState()1")

            if (ms != null) {
                //Send the media info to the sharedViewModel so that the HomeFragment can observe it and update its UI
                sharedViewModel.indexingState.postValue(ms.indexingState.toString())
                sharedViewModel.photoMediaCount.postValue(ms.photoMediaCount.toString())
                sharedViewModel.videoMediaCount.postValue(ms.videoMediaCount.toString())
                Log.d("videophoto", "monitorMediaStoreState()1")

                //If the MediaStore instance exists and its media list has not yet been browsed, browse its media list
                if(sharedViewModel.mediaStoreRef != null && !sharedViewModel.observingMediaList){
                    sharedViewModel.observingMediaList = true
                    //sharedViewModel.mediaItemList.value?.mediaList?.close()
                    Log.d("videophoto", "monitorMediaStoreState()2")

                    viewModel.mediaItemList.mediaList =
                        sharedViewModel.mediaStoreRef!!.get()?.browse { mediaList ->
                            Log.d("videophoto", "List of media items refreshed")

                            viewModel.mediaListCount = 0
                            this.viewModel.mediaList = mediaList as MutableList<MediaItem>?

                            //For each item in the media list, obtain its thumbnail image and send it to thumbnail observer
                            if (mediaList != null) {
                                Log.d("videophoto", "monitorMediaStoreState()3")


                                for (media in  mediaList) {
                                    viewModel.thumbnailObserverRef?.let {
                                        sharedViewModel.mediaStoreRef?.get()?.fetchThumbnailOf(media,
                                            it
                                        )
                                        Log.d("videophoto", "monitorMediaStoreState()4")

                                    }
                                }
                            }
                        } as Ref<MutableList<MediaItem>>?
                }
            }
        }
    }

    private inner class ThumbnailObserver: Ref.Observer<Bitmap> {
        override fun onChanged(obj: Bitmap?) {
            //Each time a media item's thumbnail (Bitmap) is sent to this observer...
            if (obj != null) {
                //add the thumbnail to the mediaItemList's bitmap list
                viewModel.mediaItemList.bitmapList.add(obj)
                viewModel.mediaListCount++
            }

            /*
            When all of the thumbnails of the current media list on the Drone's SD card have been
            added to the mediaItemList's bitmap list, send the mediaItemList to the sharedViewModel
            so that the HomeFragment can observe it and update its UI (recycler view).
             */
            if(viewModel.mediaListCount == viewModel.mediaList?.size){
                sharedViewModel.mediaItemList.postValue(viewModel.mediaItemList)
            }
        }
    }

    fun displayPhoto() {
        Log.d("videophoto", "display photo")

        sharedViewModel.mediaStoreRef = sharedViewModel.drone?.getPeripheral(MediaStore::class.java){ mediaStore ->
            // called at every peripheral changes
            Log.d("videophoto", "display Photo")

            Log.d("videophoto", "Photo count : ${mediaStore?.photoMediaCount}")
            val observer = Ref.Observer<MediaStoreWiper> {
                // This block will be called when the wipe operation is completed
                Log.d("videophoto", "Wipe operation completed")
            }
        }
    }
}