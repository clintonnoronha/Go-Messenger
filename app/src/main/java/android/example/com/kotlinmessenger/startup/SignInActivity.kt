package android.example.com.kotlinmessenger.startup

import android.content.Context
import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.example.com.kotlinmessenger.messages.LatestMessagesActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    lateinit var txtCreateAccount: TextView
    lateinit var etEmailSignIn: EditText
    lateinit var etPasswordSignIn: EditText
    lateinit var btnSignIn: Button
    lateinit var progressLayout: RelativeLayout
    lateinit var progressBar: ProgressBar
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        //If user is already signed in go to LatestMessagesActivity
        userLogInVerification()

        //finding the respective view Ids
        txtCreateAccount = findViewById(R.id.txtStatic)
        etEmailSignIn = findViewById(R.id.etEmail_sign_in)
        etPasswordSignIn = findViewById(R.id.etPassword_sign_in)
        btnSignIn = findViewById(R.id.btnSignIn)
        progressLayout = findViewById(R.id.progressLayout)
        progressBar = findViewById(R.id.progressBar)
        toolbar = findViewById(R.id.toolbarSignIn)

        progressLayout.visibility = View.GONE

        setUpToolbar()

        //go to register activity if user doesn't have an account
        txtCreateAccount.setOnClickListener {
            val intent = Intent(this@SignInActivity, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        btnSignIn.setOnClickListener {
            //Hide Keyboard
            val ref = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            ref.hideSoftInputFromWindow(
                btnSignIn.windowToken,
                InputMethodManager.RESULT_UNCHANGED_SHOWN
            )
            performSignIn()
        }

    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun userLogInVerification() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val intent = Intent(this@SignInActivity, LatestMessagesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }

    private fun performSignIn() {
        val email = etEmailSignIn.text.toString()
        val pass = etPasswordSignIn.text.toString()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(
                this,
                "Enter the credentials",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        //progress bar displayed till authentication is complete
        progressLayout.visibility = View.VISIBLE


        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("Sign", "Successfully signed in!!")
                    progressLayout.visibility = View.GONE
                    val intent = Intent(this, LatestMessagesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                } else {
                    return@addOnCompleteListener
                }
            }
            .addOnFailureListener {
                progressLayout.visibility = View.GONE
                Log.d("Sign", "${it.message}")
                Toast.makeText(this@SignInActivity, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
