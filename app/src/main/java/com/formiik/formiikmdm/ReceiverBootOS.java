package com.formiik.formiikmdm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReceiverBootOS extends BroadcastReceiver {
    public ReceiverBootOS() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(!ServiceBlockApp.serviceRunning) context.startService(new Intent(context, ServiceBlockApp.class));
    }
}
