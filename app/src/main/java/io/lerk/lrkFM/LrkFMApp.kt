package io.lerk.lrkFM

import android.app.Application
import android.content.Context
import java.lang.ref.WeakReference

/**
 * Main application class.
 *
 * @author Lukas Fülling (lukas@k40s.net)
 */
class LrkFMApp : Application() {
    override fun onCreate() {
        super.onCreate()
        context = WeakReference(this)
    }

    companion object {
        const val CHANNEL_ID = "lrkFM"
        private var context: WeakReference<Context>? = null
        fun getContext(): Context? {
            return context!!.get()
        }
    }
}
