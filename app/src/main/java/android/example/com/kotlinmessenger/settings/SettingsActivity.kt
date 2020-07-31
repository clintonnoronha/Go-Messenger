package android.example.com.kotlinmessenger.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.startup.SignInActivity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.EmailAuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    lateinit var toolbar: Toolbar
    lateinit var switchLightDark: Switch
    lateinit var sharedPreferences: SharedPreferences
    lateinit var btnSignOut: Button
    lateinit var btnDeactivateAcc: Button
    lateinit var etConfirmPassword: EditText
    lateinit var btnConfirm: Button
    lateinit var llConfirmPassword: LinearLayout
    private var darkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //finding ids of views
        toolbar = findViewById(R.id.toolbarSettings)
        switchLightDark = findViewById(R.id.switchLightDark)
        btnSignOut = findViewById(R.id.btnSignOut)
        btnDeactivateAcc = findViewById(R.id.btnDeactivateAcc)
        btnDeactivateAcc.visibility = View.VISIBLE
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        llConfirmPassword = findViewById(R.id.llConfirmPassword)
        llConfirmPassword.visibility = View.GONE
        btnConfirm = findViewById(R.id.btnConfirm)
        sharedPreferences = getSharedPreferences(getString(R.string.theme_mode), Context.MODE_PRIVATE)
        darkTheme = sharedPreferences.getBoolean("DARK_MODE", false)

        switchLightDark.isChecked = darkTheme

        setUpToolbar()

        switchLightDark.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                sharedPreferences.edit().putBoolean("DARK_MODE", true).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                sharedPreferences.edit().putBoolean("DARK_MODE", false).apply()
            }
        }

        btnDeactivateAcc.setOnClickListener {
            llConfirmPassword.visibility = View.VISIBLE
            btnDeactivateAcc.visibility = View.GONE
        }

        btnSignOut.setOnClickListener {
            performSignOut()
        }

        btnConfirm.setOnClickListener {
            //Hide Keyboard
            val ref = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            ref.hideSoftInputFromWindow(
                btnConfirm.windowToken,
                InputMethodManager.RESULT_UNCHANGED_SHOWN
            )
            deactivateAccount()
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Settings"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun performSignOut() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Sign Out")
        dialog.setMessage("Are you sure you want to sign out?")
        dialog.setPositiveButton("YES") { dialog, item ->
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, SignInActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        dialog.setNegativeButton("CANCEL") {dialog, item ->
            dialog.dismiss()
        }
        dialog.create()
        dialog.show()
    }

    private fun deactivateAccount() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Final Warning")
        dialog.setMessage("Your account will be permanently deleted. Are you sure you want to proceed?")
        dialog.setPositiveButton("YES") { dialog, item ->
            val user = FirebaseAuth.getInstance().currentUser!!
            val password = etConfirmPassword.text.toString()
            if (password.isNotEmpty()) {
                val credentials = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credentials)
                    .addOnCompleteListener {
                        user.delete()
                            .addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "Account deleted permanently.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this, SignInActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this,
                                    "Some error occurred, please try again later.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            this,
                            it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                Toast.makeText(
                    this,
                    "Enter your password!!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        dialog.setNegativeButton("CANCEL") {dialog, item ->
            dialog.dismiss()
        }
        dialog.create()
        dialog.show()
    }
}