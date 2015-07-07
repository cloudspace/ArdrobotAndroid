package com.cloudspace.ardrobot.util;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;

public class Typewriter extends TextView {

    private CharSequence mText;
    private int mIndex;
    private int mStartingIndex;
    private long mDelay = 500; //Default 500ms delay


    public Typewriter(Context context) {
        super(context);
    }

    public Typewriter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private Handler mHandler = new Handler();
    private Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            if(mStartingIndex + mIndex > mText.length()) {
                mIndex = 0;
            }
            CharSequence message = mText.subSequence(0, mStartingIndex + mIndex++);
            setText(message);
            if(mIndex <= mText.length()) {
                mHandler.postDelayed(characterAdder, mDelay);
            }
        }
    };

    public void animateText(CharSequence text) {
      animateText(text, 0);
    }

    public void setTextInternally(CharSequence text) {
        mHandler.removeCallbacks(characterAdder);
        super.setText(text);
    }

    public void animateText(CharSequence text, int startingIndex) {
        mText = text;
        mIndex = 0;
        mStartingIndex = startingIndex;
        setText("");
        mHandler.removeCallbacks(characterAdder);
        mHandler.postDelayed(characterAdder, mDelay);
    }

    public void setCharacterDelay(long millis) {
        mDelay = millis;
    }
}