package com.cloudspace.ardrobot.util;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.cloudspace.ardrobot.R;

/**
 * Created by Ken Kyger on 7/9/15.
 * Github - r2DoesInc
 * Email - r2DoesInc@futurehax.com
 */
public class MarginDecoration extends RecyclerView.ItemDecoration {
    private int margin;

    public MarginDecoration(Context context) {
        margin = context.getResources().getDimensionPixelSize(R.dimen.item_margin);
    }

    @Override
    public void getItemOffsets(
            Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(margin, margin, margin, margin);
    }
}