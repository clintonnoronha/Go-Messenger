package android.example.com.kotlinmessenger.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(val uid: String = "",
                val username: String = "",
                val profileImageUrl: String = "",
                val phone: String = "",
                val about: String = ""
) : Parcelable