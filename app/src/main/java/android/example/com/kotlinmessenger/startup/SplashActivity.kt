package android.example.com.kotlinmessenger.startup

import android.content.Intent
import android.example.com.kotlinmessenger.R
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        //makes splash screen appear on full screen removing notification bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        //used to display splash screen for 1 seconds then go to Sign in Activity
        Handler().postDelayed({
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }, 1000)

    }
}
