package android.example.com.kotlinmessenger.messages

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.model.User
import android.example.com.kotlinmessenger.adapter.UserItemAdapter
import android.example.com.kotlinmessenger.startup.SignInActivity
import android.example.com.kotlinmessenger.startup.SplashActivity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList

class NewChatActivity : AppCompatActivity() {

    lateinit var newChatRecycler: RecyclerView
    lateinit var toolbar: Toolbar
    private var uid: String? = null
    private val displayList = ArrayList<User>()
    private val tempList = ArrayList<User>()
    lateinit var adapter: UserItemAdapter
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recycler_view_new_chat)

        newChatRecycler = findViewById(R.id.recyclerNewChat)
        toolbar = findViewById(R.id.toolbarNewChat)
        sharedPreferences = getSharedPreferences(getString(R.string.shared_intent), Context.MODE_PRIVATE)
        uid = FirebaseAuth.getInstance().uid

        setUpToolbar()

        //sharing data(texts) from other apps to chat log
        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                handleSentText(intent)
            }
        }

        fetchUserDetails()
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Select User"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun checkIfUserSignIn(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        return user != null
    }

    private fun handleSentText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            if (checkIfUserSignIn()) {
                //if user is signed in then save incoming text and send it to any user's chat log
                sharedPreferences.edit().putString(UserItemAdapter.INTENT_ACTION_SEND, it).apply()
            }
            else {
                //if user is not signed in send them to the app to sign in first
                val intent = Intent(this, SplashActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(
                    this,
                    "Please sign in first",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.select_user_menu, menu)

        val menuItem = menu!!.findItem(R.id.searchUserContact)
        if (menuItem != null) {

            val searchView = menuItem.actionView as SearchView

            val etSearchName = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            etSearchName.hint = "Search..."

            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {

                    if (newText!!.isNotEmpty()) {

                        displayList.clear()
                        val search = newText.toLowerCase(Locale.getDefault())
                        //val adapter = GroupAdapter<ViewHolder>()
                        tempList.forEach {

                            if (it.username.toLowerCase(Locale.getDefault()).contains(search)) {
                                displayList.add(it)
                            }

                        }

                        newChatRecycler.adapter?.notifyDataSetChanged()

                    } else {

                        displayList.clear()
                        displayList.addAll(tempList)
                        newChatRecycler.adapter?.notifyDataSetChanged()

                    }

                    return true
                }

            })

        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun fetchUserDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                //val adapter = GroupAdapter<ViewHolder>()
                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    if (user != null && uid != user.uid) {
                        tempList.add(user)
                        displayList.add(user)
                        adapter = (UserItemAdapter(this@NewChatActivity, displayList))
                        newChatRecycler.adapter = adapter
                    }
                }
            }
        })
    }

}