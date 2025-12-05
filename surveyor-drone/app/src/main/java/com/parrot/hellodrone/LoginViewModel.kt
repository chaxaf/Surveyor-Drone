package com.parrot.hellodrone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.IOException

class LoginViewModel  : ViewModel() {
    var imageUri: Uri?
    var username: String
    var password: String = ""

    init{
        imageUri = null
        username = ""
        password = ""
    }

    var key: String = ""
    private val _uploadSuccsess = MutableLiveData<Boolean?>()
    val uploadSuccsess: LiveData<Boolean?>
        get() = _uploadSuccsess

    private val _profilePresent = MutableLiveData<Boolean?>()
    val profilePresent: LiveData<Boolean?>
        get() = _profilePresent

    // FIREBASE
    var storageRef = FirebaseStorage.getInstance().getReference()
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val profileRef: DatabaseReference = database.getReference("Profiles")

    fun sendDataToFireBase(context: Context?) {
        var uploadSuccess: Boolean = false

        profileRef.child(key).child("username").setValue(username)
        profileRef.child(key).child("password").setValue(password)

        val profileImageRef = storageRef.child("ProfileImages/" + username + ".jpg")
        val matrix = Matrix()
        matrix.postRotate(90F)
        var imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
        val ratio: Float = 13F
        val imageBitmapScaled = Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / ratio).toInt(), (imageBitmap.height / ratio).toInt(), false
        )
        imageBitmap = Bitmap.createBitmap(
            imageBitmapScaled, 0, 0,
            (imageBitmap.width / ratio).toInt(), (imageBitmap.height / ratio).toInt(),
            matrix, true
        )
        val stream = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val imageByteArray = stream.toByteArray()
        val uploadProfileImage = profileImageRef.putBytes(imageByteArray)

        uploadProfileImage.addOnFailureListener {
            _uploadSuccsess.value = false
        }.addOnSuccessListener { taskSnapshot ->
            profileRef.child(key).child("photo URL").setValue(
                (FirebaseStorage.getInstance()
                    .getReference()).toString() + "ProfileImages/" + username + ".jpg"
            )
            _uploadSuccsess.value = true
        }
    }

    fun fetchProfile() {

        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (user in dataSnapshot.children) {
                    val usernameDatabase = user.child("username").getValue(String::class.java)
                    if (usernameDatabase!= null && username == usernameDatabase) {
                        val passwordDatabase = user.child("password").getValue(String::class.java)
                        if (passwordDatabase!= null && password == passwordDatabase) {
                            key = user.key.toString()
                            _profilePresent.value = true
                            break
                        }else {
                        }
                    }
                }
                if(_profilePresent.value != true){
                    _profilePresent.value = false
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    fun resetUserData(){
        username = ""
        password = ""
        //imageUri = null
        //key = ""
        _uploadSuccsess.value = null
        _profilePresent.value = null
    }

    fun isConnected(): Boolean {
        val command = "ping -c 1 google.com"
        return try {
            Runtime.getRuntime().exec(command).waitFor() == 0
        } catch (e: IOException) {
            false
        } catch (e: InterruptedException) {
            false
        }
    }
    fun getUserKey(): String {
        return key
    }
}

