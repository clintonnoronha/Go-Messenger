package android.example.com.kotlinmessenger.settings

import android.content.pm.PackageManager
import android.example.com.kotlinmessenger.R
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class AboutAppActivity : AppCompatActivity() {

    lateinit var txtVersionNo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        //makes about app screen appear on full screen removing notification bar
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        txtVersionNo = findViewById(R.id.txtVersionNo)

        txtVersionNo.text = getAppVersionNo()

    }
    private fun getAppVersionNo(): String {
        val manager = this.packageManager
        val info = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)
        return info.versionName
    }
}