package com.jianan.parkwhere.util;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A lifecycle-aware observable that sends updates only once
 * Useful for events like navigation or displaying Snackbars
 * Unlike regular LiveData, it avoids emitting updates on configuration changes (e.g. rotation)
 *
 * @param <T> the type of data held by this event
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {
    private static final String TAG = "SingleLiveEvent";
    private final AtomicBoolean mPending = new AtomicBoolean(false);

    /**
     * Observe this SingleLiveEvent
     * Only one observer will be notified of changes, even if multiple are registered
     *
     * @param owner the LifecycleOwner that controls the observer
     * @param observer the observer that will receive the event
     */
    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.");
        }

        // Observe the internal MutableLiveData
        super.observe(owner, t -> {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t);
            }
        });
    }

    /**
     * Set a new value for the event
     * Marks it as pending so that the observer will be notified only once
     *
     * @param t the new value
     */
    @MainThread
    public void setValue(@Nullable T t) {
        mPending.set(true);
        super.setValue(t);
    }

    /**
     * Used for cases where T is Void to make calls cleaner.
     */
    @MainThread
    public void call() {
        setValue(null);
    }
}