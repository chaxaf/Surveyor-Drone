package com.parrot.hellodrone

import android.widget.Button
import android.widget.ImageButton
import androidx.lifecycle.ViewModel

class FlightControlViewModel : ViewModel() {

    lateinit var takeOffLandBt: Button
    lateinit var forwardButton: CustomButton

    lateinit var backwardButton : CustomButton
    lateinit var leftButton : CustomButton
    lateinit var rightButton :  CustomButton
    lateinit var upButton : CustomButton
    lateinit var downButton :   CustomButton
    lateinit var turnLeftButton : CustomButton
    lateinit var turnRightButton : CustomButton

    lateinit var emergencyLandingButton : ImageButton

}