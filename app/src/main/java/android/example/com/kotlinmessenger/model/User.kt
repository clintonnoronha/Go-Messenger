package android.example.com.kotlinmessenger.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class User(val uid: String = "",
                val username: String = "",
                val profileImageUrl: String = "",
                val about: String = ""
) : Parcelable