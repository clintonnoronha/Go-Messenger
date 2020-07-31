package android.example.com.kotlinmessenger.startup

import android.content.Intent
import android.example.com.kotlinmessenger.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    lateinit var toolbar: Toolbar
    lateinit var etResetEmail: TextView
    lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        toolbar = findViewById(R.id.toolbarForgotPassword)
        etResetEmail = findViewById(R.id.etResetEmail)
        btnReset = findViewById(R.id.btnReset)

        setUpToolbar()

        btnReset.setOnClickListener {
            sendResetPasswordEmail()
        }

    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Reset Password"
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun sendResetPasswordEmail() {
        val email = etResetEmail.text.toString()
        if (email.isNotEmpty()) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener {
                    val dialog = AlertDialog.Builder(this)
                    dialog.setTitle("Email sent")
                    dialog.setMessage("Reset Password email has been sent to you email account.")
                    dialog.setPositiveButton("Ok") { dialog, item ->
                        val intent = Intent(this, SignInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    }
                    dialog.create()
                    dialog.show()
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show()
        }
    }

}