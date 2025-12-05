package com.parrot.hellodrone

import android.graphics.Bitmap
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModel
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.stream.GsdkStreamView

class VideoStreamViewModel : ViewModel() {
    lateinit var capturePhotoButton: Button

    // Drone:
    /** Reference to the current drone stream server Peripheral. */
    var streamServerRef: Ref<StreamServer>? = null
    /** Reference to the current drone live stream. */
    var liveStreamRef: Ref<CameraLive>? = null
    /** Current drone live stream. */
    var liveStream: CameraLive? = null

    // User Interface Stream:
    /** Video stream view. */
    lateinit var streamView: GsdkStreamView
    // private lateinit var notConnectedLayout: RelativeLayout
    lateinit var messageTextView: TextView
    lateinit var noStreamIcon: ImageView
    lateinit var connectedDroneIcon: ImageView
    lateinit var notConnectedDroneIcon: ImageView

    //Capture photo & video :
    lateinit var dataParamButton : ImageButton
    lateinit var EmergencyButton : ImageButton

    // Monitoring SD Card Media
    var mediaItemList: MediaItemList = MediaItemList()
    var thumbnailObserverRef: Ref.Observer<Bitmap>? = null
    var mediaList: MutableList<MediaItem>? = null
    var mediaListCount: Int = 0

}