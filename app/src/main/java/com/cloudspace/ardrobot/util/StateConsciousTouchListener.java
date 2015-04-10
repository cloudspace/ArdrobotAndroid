package com.cloudspace.ardrobot.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Created by FutureHax on 4/2/15.
 */
public abstract class StateConsciousTouchListener implements OnTouchListener {
    public boolean isDown = false;

    public abstract void onRelease();

    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            isDown = true;
        } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            isDown = false;
            onRelease();
        }
        return true;
    }
}