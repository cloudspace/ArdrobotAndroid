package com.cloudspace.ardrobot.util;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cloudspace.ardrobot.R;

import java.util.ArrayList;

// Adapter for holding devices found through scanning.
public class LeDeviceListAdapter extends RecyclerView.Adapter<LeDeviceListAdapter.ViewHolder> implements View.OnClickListener {
    private ArrayList<BluetoothDevice> mLeDevices;
    OnRecyclerViewItemClickListener<BluetoothDevice> itemClickListener;
    Handler.Callback callback;

    public LeDeviceListAdapter(Handler.Callback cb) {
        mLeDevices = new ArrayList();
        callback = cb;
    }

    public void addDevice(BluetoothDevice device) {
        if (mLeDevices.isEmpty()) {
            callback.handleMessage(new Message());
        }
        if (!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return mLeDevices.size();
    }

    public void setOnItemClickListener(OnRecyclerViewItemClickListener<BluetoothDevice> listener) {
        this.itemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_device, parent, false);
        v.setOnClickListener(this);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final BluetoothDevice item = mLeDevices.get(position);
        holder.itemView.setTag(item);
        String name = (item.getName() == null || item.getName().isEmpty()) ? "(Unknown)" : item.getName();
        holder.deviceName.setText(name);
        holder.deviceAddress.setText(item.getAddress());
    }

    @Override
    public void onClick(View view) {
        if (itemClickListener != null) {
            itemClickListener.onItemClick(view, (BluetoothDevice) view.getTag());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceAddress;

        public ViewHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.device_name);
            deviceAddress = (TextView) itemView.findViewById(R.id.device_address);
        }
    }
}