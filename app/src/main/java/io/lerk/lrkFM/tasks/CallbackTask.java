package io.lerk.lrkFM.tasks;

import android.os.AsyncTask;

import io.lerk.lrkFM.Handler;

/**
 * Task that executes a callback in {@link #onPostExecute(Object)}
 *
 * @param <T> the result value of the {@link AsyncTask} and the type of the {@link Handler}.
 * @author Lukas Fülling (lukas@k40s.net)
 */
public abstract class CallbackTask<T> extends AsyncTask<Void, Void, T> {

    /**
     * The callback {@link Handler}.
     */
    private final Handler<T> callback;

    /**
     * Constructor.
     *
     * @param callback the callback
     */
    public CallbackTask(Handler<T> callback) {
        this.callback = callback;
    }

    /**
     * Triggers the callback.
     *
     * @param t the return value
     */
    @Override
    protected void onPostExecute(T t) {
        super.onPostExecute(t);
        callback.handle(t);
    }
}
