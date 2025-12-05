package com.parrot.hellodrone
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class CustomButton(context: Context, attrs: AttributeSet) : AppCompatButton(context, attrs) {
    override fun performClick(): Boolean {
        // Call the super class's performClick method
        return super.performClick()
    }
}
