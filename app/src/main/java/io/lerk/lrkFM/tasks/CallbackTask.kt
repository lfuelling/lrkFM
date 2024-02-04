package io.lerk.lrkFM.tasks

import android.os.AsyncTask


/**
 * Task that executes a callback in [.onPostExecute]
 *
 * @param <T> the result value of the [AsyncTask] and the type of the handler.
 * @author Lukas Fülling (lukas@k40s.net)
</T> */
abstract class CallbackTask<T>
/**
 * Constructor.
 *
 * @param callback the callback
 */(
    /**
     * The callback handler.
     */
    private val callback: (T) -> Unit
) : AsyncTask<Void?, Void?, T>() {
    /**
     * Triggers the callback.
     *
     * @param t the return value
     */
    override fun onPostExecute(t: T) {
        super.onPostExecute(t)
        callback(t)
    }
}
