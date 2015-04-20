package com.cloudspace.ardrobot.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Created by FutureHax on 4/2/15.
 */
public abstract class StateConsciousTouchListener implements OnTouchListener {
    public boolean isDown = false;
    OnDownListener listener;

    public abstract void onRelease();

    public interface OnDownListener {
        void onDown();
    }

    public void setOnDownListener(OnDownListener listener) {
        this.listener = listener;
    }

    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            isDown = true;
            if (listener != null) {
                listener.onDown();
            }
        } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            isDown = false;
            onRelease();
        }
        return true;
    }
}
