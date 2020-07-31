package android.example.com.kotlinmessenger.messages

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.adapter.LatestChatAdapter
import android.example.com.kotlinmessenger.model.ChatMessages
import android.example.com.kotlinmessenger.settings.AboutAppActivity
import android.example.com.kotlinmessenger.settings.ProfileActivity
import android.example.com.kotlinmessenger.settings.SettingsActivity
import android.example.com.kotlinmessenger.startup.SignInActivity
import android.net.*
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class LatestMessagesActivity : AppCompatActivity() {

    lateinit var fabNewChat: View
    lateinit var latestChatRecycler: RecyclerView
    lateinit var toolbar: Toolbar
    lateinit var rlNoNetwork: RelativeLayout
    lateinit var coordinatorLayout: CoordinatorLayout
    private val TAG = "LatestMessageActivity"
    private var shortAnimationTime: Int = 0
    private val displayList = ArrayList<ChatMessages>()
    private val tempList = ArrayList<ChatMessages>()
    var adapter: LatestChatAdapter = LatestChatAdapter(this, displayList)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        fabNewChat = findViewById(R.id.fabNewChat)
        toolbar = findViewById(R.id.toolbarLatestMessage)
        latestChatRecycler = findViewById(R.id.latestMessagesRecyclerView)
        rlNoNetwork = findViewById(R.id.rlNoNetwork)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        coordinatorLayout.visibility = View.GONE
        shortAnimationTime = resources.getInteger(android.R.integer.config_shortAnimTime)

        isNetworkAvailable()

    }

    private fun isNetworkAvailable() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(network: Network) {
                        lifecycleScope.launch {
                            val isWifi: Boolean = connectivityManager
                                .getNetworkCapabilities(network)!!
                                .hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            val isCellular: Boolean = connectivityManager
                                .getNetworkCapabilities(network)!!
                                .hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            Log.i(TAG, "Network Available. isWifi = $isWifi and isCellular = $isCellular")
                            if (isWifi || isCellular) {
                                networkAvailable()
                            }
                        }
                    }

                    override fun onUnavailable() {
                        lifecycleScope.launch {
                            Log.i(TAG, "Network connection not found")
                            rlNoNetwork.visibility = View.VISIBLE
                        }
                    }

                    override fun onLost(network: Network) {
                        lifecycleScope.launch {
                            Log.i(TAG, "Network connection lost")
                            Log.i(TAG, "animateInNoNetworkLayout()")
                            animateInNoNetworkLayout()
                        }
                    }
                }
            )
        } else {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (networkInfo?.isConnected != null) {
                networkAvailable()
            } else {
                Log.i(TAG, "Network connection not found")
                Log.i(TAG, "animateInNoNetworkLayout()")
                animateInNoNetworkLayout()
            }
        }
    }

    private fun networkAvailable() {
        Log.i(TAG, "animateInCoordinatorLayout()")
        animateInCoordinatorLayout()

        setUpToolbar()

        latestChatRecycler.adapter = adapter

        listenForLatestChat()

        fabNewChat.setOnClickListener {
            val intent = Intent(this@LatestMessagesActivity, NewChatActivity::class.java)
            startActivity(intent)
        }
    }

    private fun animateInCoordinatorLayout() {
        coordinatorLayout.apply {

            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(shortAnimationTime.toLong())
                .setListener(null)
        }
        rlNoNetwork.animate()
            .alpha(0f)
            .setDuration(shortAnimationTime.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    rlNoNetwork.visibility = View.GONE
                    //progressLayout.visibility = View.GONE
                }
            })
    }

    private fun animateInNoNetworkLayout() {
        rlNoNetwork.apply {
            alpha = 0f
            visibility = View.VISIBLE

            animate()
                .alpha(1f)
                .setDuration(shortAnimationTime.toLong())
                .setListener(null)
        }
        coordinatorLayout.animate()
            .alpha(0f)
            .setDuration(shortAnimationTime.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    coordinatorLayout.visibility = View.GONE
                }
            })
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    val latestMessageMap = HashMap<String, ChatMessages>()

    private fun listenForLatestChat() {
        val fromId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance().getReference(
            "/latest-message/$fromId"
        )
        ref.addChildEventListener(object : ChildEventListener{
            override fun onCancelled(error: DatabaseError) {

            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val latestChatMessage = snapshot.getValue(ChatMessages::class.java) ?: return
                displayList.clear()
                latestMessageMap[snapshot.key!!] = latestChatMessage
                latestMessageMap.values.forEach {
                    displayList.add(it)
                }
                latestChatRecycler.adapter?.notifyDataSetChanged()
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val latestChatMessage = snapshot.getValue(ChatMessages::class.java) ?: return
                tempList.add(latestChatMessage)
                latestMessageMap[snapshot.key!!] = latestChatMessage
                displayList.clear()
                latestMessageMap.values.forEach {
                    displayList.add(it)
                }
                latestChatRecycler.adapter?.notifyDataSetChanged()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.my_profile -> {
                //display user profile
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
            }
            R.id.about_app -> {
                //Display App Info
                val intent = Intent(this, AboutAppActivity::class.java)
                startActivity(intent)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        val menuItem = menu!!.findItem(R.id.searchUser)
        if (menuItem != null) {

            val searchView = menuItem.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {

                    if (newText!!.isNotEmpty()) {
                        displayList.clear()
                        val search = newText.toLowerCase(Locale.getDefault())
                        LatestChatAdapter.tempList.keys.forEach { user ->
                            if (user.username.toLowerCase(Locale.getDefault()).contains(search)) {
                                displayList.add(LatestChatAdapter.tempList[user]!!)
                            }
                        }
                        latestChatRecycler.adapter?.notifyDataSetChanged()
                    } else {
                        displayList.clear()
                        displayList.addAll(tempList)
                        latestChatRecycler.adapter?.notifyDataSetChanged()
                    }

                    return true
                }

            })

        }
        return super.onCreateOptionsMenu(menu)
    }

}