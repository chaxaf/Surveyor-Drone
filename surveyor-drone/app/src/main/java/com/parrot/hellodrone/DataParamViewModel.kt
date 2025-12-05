package com.parrot.hellodrone

import androidx.lifecycle.ViewModel
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper


class DataParamViewModel : ViewModel() {
    var mediaDestinationRef: MediaDestination? = null

    var mediaDownloaderRef: Ref.Observer<MediaDownloader>? = null

    var mediaStoreWiperRef: Ref.Observer<MediaStoreWiper>? = null

    var mediaDeleterRef: Ref.Observer<MediaDeleter>? = null

    // Text Views:
    lateinit var droneStateTxt: TextView

    lateinit var indexingTxt: TextView
    lateinit var photoMediaCountTxt: TextView
    lateinit var videoMediaCountTxt: TextView

    // Pop-ups
    lateinit var downloadTxt: TextView
    lateinit var missingMediaTxt: TextView

    // Buttons
    lateinit var deleteMediaBt: Button
    lateinit var downloadMediaBt: Button
    lateinit var viewMediaBt: Button

    // Recycler View
    lateinit var fileRecyclerView: RecyclerView


    // Progress Bar
    lateinit var progressBar: ProgressBar
    lateinit var progressLayout: FrameLayout
}

