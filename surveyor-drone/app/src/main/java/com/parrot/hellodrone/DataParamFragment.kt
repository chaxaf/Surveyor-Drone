package com.parrot.hellodrone

import android.graphics.Color
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.media3.exoplayer.offline.Downloader
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.peripheral.MediaStore
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDeleter
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDestination
import com.parrot.drone.groundsdk.device.peripheral.media.MediaDownloader
import com.parrot.drone.groundsdk.device.peripheral.media.MediaItem
import com.parrot.drone.groundsdk.device.peripheral.media.MediaStoreWiper
import com.parrot.drone.groundsdk.device.peripheral.media.MediaTaskStatus
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.lifecycle.ViewModelProvider
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo

class DataParamFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var viewModel: DataParamViewModel
    private var fileAdapter: FileListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_param, container, false)
        viewModel = ViewModelProvider(this).get(DataParamViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Media Downloading
        viewModel.mediaDownloaderRef = Downloader()
        viewModel.mediaDestinationRef = MediaDestination.appPrivateFiles("PARROT MEDIA")

        // Media Deleting
        viewModel.mediaDeleterRef = Deleter()
        viewModel.mediaStoreWiperRef = Wiper()

        // Text Views
        viewModel.droneStateTxt = view.findViewById(R.id.droneStateTxt)
        viewModel.indexingTxt = view.findViewById(R.id.indexStateTxt)
        viewModel.photoMediaCountTxt = view.findViewById(R.id.photoMediaCountTxt)
        viewModel.videoMediaCountTxt = view.findViewById(R.id.videoMediaCountTxt)
        viewModel.downloadTxt = view.findViewById(R.id.downloadTextView)
        viewModel.missingMediaTxt = view.findViewById(R.id.missingMediaTextView)

        // Buttons
        viewModel.downloadMediaBt = view.findViewById(R.id.downloadMediaBt)
        viewModel.deleteMediaBt = view.findViewById(R.id.deleteMediaBt)
        viewModel.viewMediaBt = view.findViewById(R.id.viewMediaBt)
        viewModel.photoMediaCountTxt.text = displayCountPhoto().toString()
        viewModel.videoMediaCountTxt.text = displayCountVideo().toString()

        // Recycler View
        viewModel.fileRecyclerView = view.findViewById(R.id.file_recycler_view)
        viewModel.fileRecyclerView.layoutManager = LinearLayoutManager(context)
        fileAdapter = FileListAdapter()
        viewModel.fileRecyclerView.adapter = fileAdapter

        // Progress Bar
        viewModel.progressBar = view.findViewById(R.id.progressBar)
        viewModel.progressLayout = view.findViewById(R.id.progressLayout)

        viewModel.downloadMediaBt.setOnClickListener {
            if(sharedViewModel.selectedMedia != null){
                Log.d("TAG", "Downloading media item $sharedViewModel.{selectedMedia!!.name}...")
                //downloadMedia(sharedViewModel.selectedMedia?.resources as MutableList<MediaItem.Resource>)
            }
        }

        viewModel.deleteMediaBt.setOnClickListener {
            if(sharedViewModel.selectedMedia != null) {
                Log.d("TAG", "Deleting media item ${sharedViewModel.selectedMedia!!.name}...")
                deleteMedia(sharedViewModel.selectedMedia?.resources as MutableList<MediaItem.Resource>)
            }
        }

        viewModel.viewMediaBt.setOnClickListener {
            //findNavController().navigate(R.id.moveToDetailView)
        }

        setHasOptionsMenu(true)

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
        Log.d("photoviedo", "onviewcreated de data frag")

        // Drone Battery
        sharedViewModel.droneBattery.observe(viewLifecycleOwner){ droneBattery ->
        }

        // Indexing State
        sharedViewModel.indexingState.observe(viewLifecycleOwner){ indexingState ->
            viewModel.indexingTxt.text = indexingState
            Log.d("photoviedo", "change indexingState")

        }
        // Photo Media Count
        sharedViewModel.photoMediaCount.observe(viewLifecycleOwner){ photoMediaCount ->
            Log.d("photoviedo", photoMediaCount)
            viewModel.photoMediaCountTxt.text = photoMediaCount
            Log.d("photoviedo", "change mediacount")

        }
        // Video Media Count
        sharedViewModel.videoMediaCount.observe(viewLifecycleOwner){ videoMediaCount ->
            Log.d("photoviedo", videoMediaCount)
            viewModel.videoMediaCountTxt.text = videoMediaCount
            Log.d("photoviedo", "change videoMediaCount")

        }
        // Refresh Recycler View when SD Card Media Changes
        sharedViewModel.mediaItemList.observe(viewLifecycleOwner){
            fileAdapter = FileListAdapter()
            viewModel.fileRecyclerView.adapter = fileAdapter
        }
        // Media type of currently selected media item
        sharedViewModel.selectedMediaType.observe(viewLifecycleOwner){ mediaType ->
            viewModel.viewMediaBt.isEnabled = mediaType == "VIDEO"
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d("photoviedo", "ON RESUME")

        sharedViewModel.droneConnectionStatus.observe(this, Observer { connectionState ->
            when (connectionState) {

                DeviceState.ConnectionState.CONNECTING, DeviceState.ConnectionState.CONNECTED -> {
                    Log.d("YourFragment", "Drone Connection State on: ${sharedViewModel.drone?.state?.getConnectionState()}")
                    // Start video stream.
                    //monitorMediaStoreState()
                    Log.d("photoviedo", "change CONNECTING")
                    viewModel.downloadMediaBt.isEnabled = true
                    viewModel.deleteMediaBt.isEnabled = true
                    viewModel.viewMediaBt.isEnabled = true
                    viewModel.missingMediaTxt.visibility = View.GONE
                    viewModel.fileRecyclerView.visibility = View.VISIBLE

                }
                DeviceState.ConnectionState.DISCONNECTED -> {
                    Log.d("YourFragment", "Drone Connection State off: ${sharedViewModel.drone?.state?.getConnectionState()}")
                    Log.d("photoviedo", "change DISCONNECTING")
                    viewModel.downloadMediaBt.isEnabled = false
                    viewModel.deleteMediaBt.isEnabled = false
                    viewModel.viewMediaBt.isEnabled = false
                    viewModel.photoMediaCountTxt.text = "n/a"
                    viewModel.videoMediaCountTxt.text = "n/a"
                    viewModel.missingMediaTxt.text = "There is no Drone Connected"
                    viewModel.missingMediaTxt.visibility = View.VISIBLE
                    viewModel.fileRecyclerView.visibility = View.VISIBLE

                }
                else -> {
                    Log.d("photoviedo", "change AUTRE")

                }
            }
        })
    }

    private fun checkMediaMissing(): Boolean {
        return sharedViewModel.photoMediaCount.value == "0" && sharedViewModel.videoMediaCount.value == "0"
    }

//    private fun downloadMedia(resources: MutableList<MediaItem.Resource>){
//        mediaDestinationRef?.let {
//            mediaDownloaderRef?.let { it1 ->
//                sharedViewModel.mediaStoreRef?.get()?.download(resources,
//                    it, it1
//                )
//            }
//        }
//    }

    fun displayCountPhoto(): Int? {
        var count: Int? = null
        sharedViewModel.drone?.getPeripheral(MediaStore::class.java) { mediaStore ->
            // called at every peripheral changes
            count = mediaStore?.photoMediaCount
            Log.d("Datacollect", "Photo count: ${mediaStore?.photoMediaCount}")
        }
        return count
    }

    fun displayCountVideo(): Int? {
        var count: Int? = null
        sharedViewModel.drone?.getPeripheral(MediaStore::class.java) { mediaStore ->
            // called at every peripheral changes
            count = mediaStore?.videoMediaCount
            Log.d("Datacollect", "Photo count: ${mediaStore?.videoMediaCount}")
        }
        return count
    }


    private fun deleteMedia(resources: MutableList<MediaItem.Resource>){
        if(sharedViewModel.selectedMedia != null) {
            Log.d("photovideo", "Deleting media item ${sharedViewModel.selectedMedia!!.name}...")
            deleteMedia(sharedViewModel.selectedMedia?.resources as MutableList<MediaItem.Resource>)
        }
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.download_all_menu_item -> {
//                Log.d(TAG, "Downloading all media items...")
//                downloadAllMedia()
//                true
//            }
//            R.id.delete_all_menu_item -> {
//                Log.d(TAG, "Clearing the SD card...")
//                wipeMedia()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    private fun downloadAllMedia(){
        val resources = mutableListOf<MediaItem.Resource>()
        for (mediaItem in sharedViewModel.mediaItemList.value?.mediaList?.get()!!){
            mediaItem.let { resources.addAll(it.resources) }
        }
        //downloadMedia(resources)
    }

    private fun wipeMedia(){
        viewModel.mediaStoreWiperRef?.let { sharedViewModel.mediaStoreRef?.get()?.wipe(it) }
    }

    inner class Downloader: Ref.Observer<MediaDownloader>{
        override fun onChanged(obj: MediaDownloader?) {
            if (obj != null) {
                viewModel.progressLayout.visibility = View.VISIBLE
                viewModel.progressBar.progress = obj.totalProgress
                viewModel.downloadTxt.text = "Downloading ... ${obj.totalProgress}%"

                if(obj.status == MediaTaskStatus.FILE_PROCESSED) {
                    val file = File(obj.downloadedFile?.path.toString())
                    val fileSize = ((file.length() / 1024).toInt()).toString()

                    Log.d("TAG", "Media File ${obj.downloadedFile?.name} ($fileSize MB) has been downloaded")
                }
                if(obj.status == MediaTaskStatus.COMPLETE) {
                    viewModel.progressBar.progress = 0
                    viewModel.progressLayout.visibility = View.GONE
                }
            }
        }
    }

    inner class Deleter: Ref.Observer<MediaDeleter>{
        override fun onChanged(obj: MediaDeleter?) {
            Log.d("TAG", "Deleter()")
            if (obj != null) {
                if(obj.status == MediaTaskStatus.COMPLETE){
                    Log.d("TAG", "Media item has been deleted")
                }
            }
        }
    }

    inner class Wiper: Ref.Observer<MediaStoreWiper>{
        override fun onChanged(obj: MediaStoreWiper?) {
            if (obj != null) {
                if(obj.status == MediaTaskStatus.COMPLETE){
                    viewModel.fileRecyclerView.visibility = View.GONE
                    viewModel.missingMediaTxt.text =
                        "There is currently no media available on the drone's SD card"
                    viewModel.missingMediaTxt.visibility = View.VISIBLE
                }
                Log.d("TAG","Wiper: ${obj.status}")
            }
        }
    }

    private inner class FileListAdapter() : RecyclerView.Adapter<FileListAdapter.ItemHolder>() {
        var selectedPosition = 0

        override fun getItemCount(): Int {
            return sharedViewModel.mediaItemList.value?.mediaList?.get()?.size ?: 0
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_recycler_view_dialog, parent, false)
            return ItemHolder(view)
        }

        override fun onBindViewHolder(mItemHolder: ItemHolder, index: Int) {
            if (selectedPosition == index){
                if (sharedViewModel.selectedMedia != sharedViewModel.mediaItemList.value?.mediaList?.get()?.get(index)){
                    sharedViewModel.selectedMedia = sharedViewModel.mediaItemList.value?.mediaList?.get()?.get(index)

                    sharedViewModel.selectedMediaType.postValue(sharedViewModel.mediaItemList.value?.mediaList?.get()?.get(index)?.type.toString())
                    Log.d("TAG", "Currently selected media item: ${sharedViewModel.selectedMedia?.name}\n")
                }
                mItemHolder.itemView.setBackgroundColor(resources.getColor(R.color.purplePink))

            }else{
                mItemHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
            }
            val file: MediaItem? = sharedViewModel.mediaItemList.value?.mediaList?.get()?.get(index)
            val fileName = file?.name
            val fileType = file?.type
            val fileSize = file?.resources?.get(0)?.let { Formatter.formatShortFileSize(context, it.size) }
            val videoDuration = file?.resources?.get(0)?.duration?.let {
                TimeUnit.MILLISECONDS.toSeconds(
                    it
                )
            }?.let { DateUtils.formatElapsedTime(it) }
            if(sharedViewModel.mediaItemList.value?.bitmapList?.size!! > index){
                val bitmap = sharedViewModel.mediaItemList.value?.bitmapList?.get(index)
                if (file != null) {
                    mItemHolder.fileImageView.setImageBitmap(bitmap)
                }
            }
            mItemHolder.fileNameTextView.text = "File Name: $fileName"
            mItemHolder.fileTypeTextView.text = "File Type: $fileType"
            mItemHolder.fileSizeTextView.text = "File Size: $fileSize"
            mItemHolder.itemView.tag = index

            if(sharedViewModel.mediaItemList.value?.mediaList?.get()?.get(index)?.type == MediaItem.Type.VIDEO){
                mItemHolder.videoDurationTextView.visibility = View.VISIBLE
                mItemHolder.videoDurationTextView.text = "Video Duration: $videoDuration"
            }
            else{
                mItemHolder.videoDurationTextView.visibility = View.INVISIBLE
            }
        }

        inner class ItemHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView), View.OnClickListener {

            init {
                itemView.setOnClickListener(this)
            }
            var fileNameTextView: TextView = itemView.findViewById(R.id.file_name_text_view)
            var fileTypeTextView: TextView = itemView.findViewById(R.id.file_type_text_view)
            var fileSizeTextView: TextView = itemView.findViewById(R.id.file_size_text_view)
            var videoDurationTextView: TextView = itemView.findViewById(R.id.video_duration_text_view)
            var fileImageView: ImageView = itemView.findViewById(R.id.file_image_view)

            override fun onClick(itemView: View) {
                if (adapterPosition == RecyclerView.NO_POSITION) return

                notifyItemChanged(selectedPosition)
                selectedPosition = adapterPosition
                notifyItemChanged(selectedPosition)
                sharedViewModel.selectedMediaPosition = selectedPosition
            }
        }
    }

    override fun onDestroyView() {
        viewModel.mediaDeleterRef = null
        viewModel.mediaDestinationRef = null
        viewModel.mediaDownloaderRef = null
        viewModel.mediaStoreWiperRef = null

        super.onDestroyView()
    }
}