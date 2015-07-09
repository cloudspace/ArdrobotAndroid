package com.cloudspace.ardrobot.util;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudspace.ardrobot.ControllerActivity;
import com.cloudspace.ardrobot.ExternalCoreActivity;
import com.cloudspace.ardrobot.R;
import com.cloudspace.ardrobot.SensorsControllerActivity;

// Adapter for holding devices found through scanning.
public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.ViewHolder> implements View.OnClickListener {
    OnRecyclerViewItemClickListener<Option> itemClickListener;

    public OptionsAdapter() {
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return Option.values().length;
    }

    public void setOnItemClickListener(OnRecyclerViewItemClickListener<Option> listener) {
        this.itemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.option, parent, false);
        v.setOnClickListener(this);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Option item = Option.values()[position];
        holder.itemView.setTag(item);
        holder.name.setText(item.getText());
        holder.icon.setImageResource(item.getIconResource());
    }

    @Override
    public void onClick(View view) {
        if (itemClickListener != null) {
            itemClickListener.onItemClick(view, (Option) view.getTag());
        }
    }

    public int getSpanSize(int position) {
        return Option.values()[position].spanSize;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView icon;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            icon = (ImageView) itemView.findViewById(R.id.icon);

        }
    }

    public enum Option {
        TILT(1, "Tilt Controller", R.drawable.ic_tilt, SensorsControllerActivity.class),
        INTERNAL(1, "Internal Master", R.drawable.ic_internal, null),
        TOUCH(2, "Touch Controller", R.drawable.ic_touch, ControllerActivity.class),
        EXTERNAL(2, "External Master", R.drawable.ic_external, ExternalCoreActivity.class);

        String text;
        int iconResource;
        public int spanSize;
        Class classToLaunch;

        public String getText() {
            return text;
        }

        public int getIconResource() {
            return iconResource;
        }

        Option(int spanSize, String text, int iconResource, Class classToLaunch) {
            this.spanSize = spanSize;
            this.text = text;
            this.iconResource = iconResource;
            this.classToLaunch = classToLaunch;
        }


        public void handleClick(Context context) {
            if (classToLaunch != null) {
                context.startActivity(new Intent(context, classToLaunch));
            } else {
                Toast.makeText(context, "Not available", Toast.LENGTH_LONG).show();
            }
        }
    }
}