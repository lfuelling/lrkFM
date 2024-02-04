package io.lerk.lrkFM.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.lerk.lrkFM.R
import io.lerk.lrkFM.activities.file.FileActivity
import io.lerk.lrkFM.activities.themed.ThemedAppCompatActivity

/**
 * Intro.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class IntroActivity : ThemedAppCompatActivity() {
    /**
     * {@inheritDoc}
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val storagePermissionGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val notificationsPermissionGranted = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED

        val fab = findViewById<FloatingActionButton>(R.id.fab)

        if (storagePermissionGranted && notificationsPermissionGranted) {
            fab.setImageResource(R.drawable.ic_chevron_right_white_24dp)
        }
        fab.setOnClickListener { view: View? ->
            if(c <= 0) {
                if (!storagePermissionGranted) {
                    FileActivity.Companion.verifyStoragePermissions(this@IntroActivity)
                }
                if (!notificationsPermissionGranted) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        0
                    )
                }
                fab.setImageResource(R.drawable.ic_chevron_right_white_24dp)
                c++
            } else {
                launchMainAndFinish(savedInstanceState)
            }
        }
    }

    /**
     * Launch [FileActivity] and call [.finish].
     */
    private fun launchMainAndFinish(savedInstanceState: Bundle?) {
        val intent = Intent(this@IntroActivity, FileActivity::class.java)
        intent.putExtra("firstStartDone", true)
        if (savedInstanceState != null) {
            intent.putExtras(savedInstanceState)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private var c = 0
    }
}
