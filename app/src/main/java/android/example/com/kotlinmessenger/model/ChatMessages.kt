package android.example.com.kotlinmessenger.model

data class ChatMessages(
    val fromId: String = "",
    val id: String = "",
    val text: String = "",
    val timestamp: Long = -1,
    val toId: String = ""
)