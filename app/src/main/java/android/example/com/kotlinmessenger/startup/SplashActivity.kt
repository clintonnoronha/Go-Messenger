package android.example.com.kotlinmessenger.startup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.example.com.kotlinmessenger.R
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SplashActivity : AppCompatActivity() {

    lateinit var sharedPreferences: SharedPreferences
    private var darkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        sharedPreferences = getSharedPreferences(getString(R.string.theme_mode), Context.MODE_PRIVATE)
        darkTheme = sharedPreferences.getBoolean("DARK_MODE", false)

        if (darkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        //makes splash screen appear on full screen removing notification bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        //used to display splash screen for 1 seconds then go to Sign in Activity
        Handler().postDelayed({
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }, 2000)

    }
}
