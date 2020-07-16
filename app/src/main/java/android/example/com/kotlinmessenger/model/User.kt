package android.example.com.kotlinmessenger.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(val uid: String = "", val username: String = "", val profileImageUrl: String = ""): Parcelable