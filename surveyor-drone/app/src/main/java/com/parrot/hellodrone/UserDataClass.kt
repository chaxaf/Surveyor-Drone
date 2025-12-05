package com.parrot.hellodrone

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserDataClass(val username: String?, val image: Uri?, val userKey: String?): Parcelable