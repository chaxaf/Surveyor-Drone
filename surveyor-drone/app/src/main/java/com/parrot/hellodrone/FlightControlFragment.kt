package com.parrot.hellodrone

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.hellodrone.databinding.FragmentFlightControlBinding
import com.parrot.drone.groundsdk.device.pilotingitf.GuidedPilotingItf


// TODO : disable piloting itf if null, so that it cannot make the app crash
class FlightControlFragment : Fragment() {

    private lateinit var viewModel: FlightControlViewModel
    private lateinit var binding: FragmentFlightControlBinding
    private val sharedViewModel: SharedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding =
            FragmentFlightControlBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(FlightControlViewModel::class.java)


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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the buttons and set onClickListeners
        initializeButtons()
        viewModel.takeOffLandBt = binding.takeOffLandBt
        viewModel.forwardButton = binding.forwardButton

        viewModel.forwardButton = binding.backwardButton
        viewModel.leftButton= binding.leftButton
        viewModel.rightButton = binding.rightButton
        viewModel.upButton = binding.upButton
        viewModel.downButton = binding.downButton
        viewModel.turnLeftButton = binding.turnLeftButton
        viewModel.turnLeftButton = binding.turnRightButton
        viewModel.emergencyLandingButton = binding.emergencyLanding

        binding.emergencyLanding.setOnClickListener {
            sharedViewModel.pilotingItfRef?.let { itf ->
                itf.land()
            }
            Log.d("SharedViewModel", "EMERGENCY LANDIND")
            Toast.makeText(context,"EMERGENCY LANDIND", Toast.LENGTH_LONG).show()
        }


        sharedViewModel.droneConnectionStatus.observe(viewLifecycleOwner) { connectionState ->
            if (connectionState == DeviceState.ConnectionState.CONNECTED || connectionState == DeviceState.ConnectionState.CONNECTING) {
                sharedViewModel.drone?.let { drone ->
                    sharedViewModel.pilotingItfRef = drone.getPilotingItf(ManualCopterPilotingItf::class.java)
                    sharedViewModel.pilotingItfRef?.let { itf ->
                        managePilotingItfState(itf)
                    }
                }
            }
        }
    }




    //    private fun closeMonitors() {
//        pilotingItfRef?.close()
//        pilotingItfRef = null
//    }
    private fun onTakeOffLandClick() {
        // Get the piloting interface from its reference.
        sharedViewModel.pilotingItfRef?.let { itf ->
            // Do the action according to the interface capabilities
            if (itf.canTakeOff()) {
                // Take off
                itf.takeOff()
                viewModel.takeOffLandBt.text = "Land"

            } else if (itf.canLand()) {
                // Land
                itf.land()
                viewModel.takeOffLandBt.text = "Take off"
            }
        }
    }

    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> {
                // Piloting interface is unavailable.
                viewModel.takeOffLandBt.isEnabled = false
                disableMovementButtons()
            }

            Activable.State.IDLE -> {
                // Piloting interface is idle.
                viewModel.takeOffLandBt.isEnabled = false
                Log.d("YourFragment", "piloting interface idle1")
                // Activate the interface.
                itf.activate()
                Log.d("YourFragment", "piloting interface idle2")
            }

            Activable.State.ACTIVE -> {
                // Piloting interface is active.
                Log.d("YourFragment", "piloting interface active")
               // enableMovementButtons()
                binding.takeOffLandBt.setOnClickListener { onTakeOffLandClick() }

                binding.forwardButton.setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.performClick() // Call performClick when the button is pressed
                            moveDrone(1, 0, 0, 0)
                            v.setBackgroundColor(Color.GREEN)
                        }
                        MotionEvent.ACTION_UP -> {
                            moveDrone(0, 0, 0, 0)
                            v.setBackgroundColor(Color.WHITE)
                        }
                    }
                    true
                }
                //binding.forwardButton.setOnClickListener {moveDrone(1, 0, 0, 0)}
                Log.d("YourFragment", "buttons enabled")
                viewModel.forwardButton.isEnabled = true

                when {
                    itf.canTakeOff() -> {
                        // Drone can take off.
                        viewModel.takeOffLandBt.isEnabled = true
                        // forwardButton.isEnabled = true
                        viewModel.takeOffLandBt.text = getString(R.string.take_off)
                        Log.d("YourFragment", "cantakeoff")

                    }

                    itf.canLand() -> {
                        // Drone can land.
                        viewModel.takeOffLandBt.isEnabled = true
                        viewModel.takeOffLandBt.text = getString(R.string.land)
                        Log.d("YourFragment", "canaLand")
                    }

                    else -> // Disable the button.
                        viewModel.takeOffLandBt.isEnabled = false
                }
            }
        }
    }

    private fun moveDrone(pitch: Int, roll: Int, gaz: Int, yaw: Int) {
        sharedViewModel.pilotingItfRef?.let { itf ->
            itf.setPitch(pitch)
            // left/right
            itf.setRoll(roll)
            itf.setVerticalSpeed(gaz)
            itf.setYawRotationSpeed(yaw)

            Log.d("Move", "moveDrone")
            Log.d("Move", "${itf.setRoll(roll)}")
            Log.d("Move", "${itf.setVerticalSpeed(gaz)}")
            Log.d("Move", "${itf.setYawRotationSpeed(yaw)}")
        }
    }

    private fun initializeButtons() {
        binding.takeOffLandBt.setOnClickListener { onTakeOffLandClick() }

        // Declare the flag outside the OnTouchListener
        var isButtonPressed = false

        val moveListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performClick() // Call performClick when the button is pressed
                    when (v.id) {
                        R.id.forwardButton -> moveDrone(1, 0, 0, 0)
                        R.id.backwardButton -> moveDrone(-1, 0, 0, 0)
                        R.id.leftButton -> moveDrone(0, -1, 0, 0)
                        R.id.rightButton -> moveDrone(0, 1, 0, 0)
                        R.id.upButton -> moveDrone(0, 0, 1, 0)
                        R.id.downButton -> moveDrone(0, 0, -1, 0)
                        R.id.turnLeftButton -> moveDrone(0, 0, 0, -1)
                        R.id.turnRightButton -> moveDrone(0, 0, 0, 1)

                    }
                    // Toggle the flag when the button is pressed
                    isButtonPressed = !isButtonPressed
                    v.setBackgroundColor(if (isButtonPressed) Color.GREEN else Color.WHITE)
                }
                MotionEvent.ACTION_UP -> {
                    moveDrone(0, 0, 0, 0)
                    isButtonPressed = !isButtonPressed
                    v.setBackgroundColor(if (isButtonPressed) Color.GREEN else Color.WHITE)
                }
            }
            true
        }

        binding.forwardButton.setOnTouchListener(moveListener)
        binding.backwardButton.setOnTouchListener(moveListener)
        binding.leftButton.setOnTouchListener(moveListener)
        binding.rightButton.setOnTouchListener(moveListener)
        binding.upButton.setOnTouchListener(moveListener)
        binding.downButton.setOnTouchListener(moveListener)
        binding.turnLeftButton.setOnTouchListener(moveListener)
        binding.turnRightButton.setOnTouchListener(moveListener)
    }
    private fun disableMovementButtons() {
        binding.forwardButton.isEnabled = false
        binding.backwardButton.isEnabled = false
        binding.leftButton.isEnabled = false
        binding.rightButton.isEnabled = false
        binding.upButton.isEnabled = false
        binding.downButton.isEnabled = false
        binding.turnLeftButton.isEnabled = false
        binding.turnRightButton.isEnabled = false
    }

//    private fun enableMovementButtons() {
//        binding.forwardButton.isEnabled = true
//        binding.backwardButton.isEnabled = true
//        binding.leftButton.isEnabled = true
//        binding.rightButton.isEnabled = true
//        binding.upButton.isEnabled = true
//        binding.downButton.isEnabled = true
//        binding.turnLeftButton.isEnabled = true
//        binding.turnRightButton.isEnabled = true
//    }

}



//    private fun monitorPilotingInterface() {
//    // Monitor a piloting interface.
//    pilotingItfRef = viewModel.drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
//    // Called when the manual copter piloting Interface is available
//    // and when it changes.
//
//    // Disable the button if the piloting interface is not available.
//    if (it == null) {
//    takeOffLandBt.isEnabled = false
//    } else {
//        managePilotingItfState(it)
//    }}}
