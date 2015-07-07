package com.cloudspace.ardrobot.util;

import android.view.View;

public interface OnRecyclerViewItemClickListener<T> {
    void onItemClick(View view, T model);
}