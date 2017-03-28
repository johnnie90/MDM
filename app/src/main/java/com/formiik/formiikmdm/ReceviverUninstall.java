package com.formiik.formiikmdm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ReceviverUninstall extends BroadcastReceiver {
    public ReceviverUninstall() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /*

        Log.d("ReceviverUninstall", "onReceive");

        String packageName = intent.getStringExtra("android.intent.extra.EXTRA_UID");

        Log.d("ReceviverUninstall", packageName);
        */

        String unistalledPackage = intent.getData().getSchemeSpecificPart().toString();

        Log.d("ReceviverUninstall",unistalledPackage );

        if(unistalledPackage.equalsIgnoreCase("formiik.com.mobiik.www"))
            Toast.makeText(context, "Formiik MDM: Se desinstalo Formiik",Toast.LENGTH_LONG).show();
    }
}
