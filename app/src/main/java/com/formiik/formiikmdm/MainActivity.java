package com.formiik.formiikmdm;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;

public class MainActivity extends AppCompatActivity implements ServiceBlockApp.serviceCreated {

    private ListView                        lv_apps;
    private ToggleButton                    toggleButton;
    private TextView                        txt_formiik_version;

    private AdapterApps                     adapterApps;

    static final int SECURITY_SETTINGS_REQUEST = 1;
    static final int USAGE_ACCESS_SETTINGS_REQUEST = 2;
    static final int REQUEST_CODE_ASK_PERMISSIONS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !checkPermissionsDangerous(this)) {
            requestAllPermissionsDangerous();
        }

        /*
        ListView para mostrar la lista de apps instaladas en el celular
        Se llena con adapterApps
         */
        lv_apps = (ListView) findViewById(R.id.lv_apps);

        /*Toggle button para seleccionar si la lsita negra (lista de bloqueo) se considera desde
        las apps seleccionadas con checkbox o mediante una listaa de palabras ya definida
         */
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        //toggleButton.setChecked(false);


        /*TextView para ver la version actual de Formiik
         */
        txt_formiik_version = (TextView) findViewById(R.id.txt_formiik_version);

        /*
        Listener para saber cuando el servicio fue iniciado
         */
        ServiceBlockApp.listener = this;

         /*Servicio para ejecutar en segundo plano un asyntask para la deteccion de que otra app se abre
        Dependiendo si la app abierta esta o no en la lista negra se bloqueara su launch
        Si el servicio ya esta corriendo se procede a cargar a lsita de apps de acuerdo a lo almacenado en sharedpref
         */
        if(!ServiceBlockApp.serviceRunning){
            startService(new Intent(MainActivity.this, ServiceBlockApp.class));
        } else {
            serviceCreated();
        }

        /*Broacast para saber cuando se completo la descarga de un apk
         */
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestAllPermissionsDangerous() {
        requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                REQUEST_CODE_ASK_PERMISSIONS);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean checkPermissionsDangerous(Context context) {
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults.length > 0) {
                    for (int gr : grantResults) {
                        // Check if request is granted or not
                        if (gr != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                    }
                }

                break;
            default:
                return;
        }
    }

    /*metodo que se activa con boton para detener el servicio
     */
    public void altoService(View view){
        if(ServiceBlockApp.serviceRunning) stopService(new Intent(MainActivity.this, ServiceBlockApp.class));
    }

    /*metodo para descargar el apk desde una URL
     */
    public void updateAPK(View view) {


        if(!isUnknownSourcesEnabled()){
            enableUnknownSources();
            return;
        }

        if(!isUsageAccessEnabled()){
            enableUsageAccess();
            return;
        }

        if(!isUpdated()) {

            String url = "http://pluto.androidapksfree.com/boa/com.facebook.katana_v68.0.0.37.59-25391152_Android-4.0.3.apk";

            String apkName = "/facebook.apk";
            String path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
            File f = new File(path + apkName);
            if (f.exists()) {
                Toast.makeText(getApplicationContext(), "APK old deleted", Toast.LENGTH_SHORT).show();
                f.delete();
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription("Se esta descargando la última versión");
            request.setTitle("Descargando");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            }

            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "facebook.apk");

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
        }else{
            Toast.makeText(this,getString(R.string.formiik_updated),Toast.LENGTH_LONG).show();
        }

    }


    private boolean isUnknownSourcesEnabled(){

        boolean isEnabled = false;

        try {
            isEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS) == 1;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        return isEnabled;
    }

    private void enableUnknownSources(){

        if (!isUnknownSourcesEnabled()) {

            Toast.makeText(getApplicationContext(), "Habilita origenes desconocidos", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS);
            startActivityForResult(intent, SECURITY_SETTINGS_REQUEST);

        } else{
            Toast.makeText(getApplicationContext(), "Ya esta habilitado", Toast.LENGTH_SHORT).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean isUsageAccessEnabled(){

        boolean isEnabled;

        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getApplicationContext().getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);

            isEnabled = mode == AppOpsManager.MODE_ALLOWED;

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }

        return isEnabled;
    }

    private void enableUsageAccess(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (!isUsageAccessEnabled()) {

                Toast.makeText(getApplicationContext(), "Habilita acceso a Formiik", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivityForResult(intent, USAGE_ACCESS_SETTINGS_REQUEST);

            } else{
                Toast.makeText(getApplicationContext(), "Ya esta habilitado", Toast.LENGTH_SHORT).show();
            }

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == SECURITY_SETTINGS_REQUEST) {

            Log.e("RESULT CODE", "SECURITY_SETTINGS_REQUEST");
            Log.e("HABILITADO RESULT", "" + isUnknownSourcesEnabled());

            if (!isUnknownSourcesEnabled()) {
                Toast.makeText(getApplicationContext(), "No se habilito origenes desconocidos", Toast.LENGTH_SHORT).show();
            }

        } else if (requestCode == USAGE_ACCESS_SETTINGS_REQUEST) {

            Log.e("RESULT CODE", "USAGE_ACCESS_SETTINGS_REQUEST");
            Log.e("HABILITADO RESULT", "" + isUsageAccessEnabled());

            if (!isUsageAccessEnabled()) {
                Toast.makeText(getApplicationContext(), "No se habilito el permiso a Formiik", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isUpdated(){

        String wsFormiikVersion = "5.2.beta7";

        if (ServiceBlockApp.formiikVersion.equalsIgnoreCase(wsFormiikVersion)) return true;
        else return false;
    }

    /*Broadcast para iniciar la instalacion del neuvo apk
     */
    BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {

            Toast.makeText(getApplicationContext(), "Descargado" , Toast.LENGTH_SHORT).show();

            String apkName = "/facebook.apk";
            String path = String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));

            Intent promptInstall = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                    .setDataAndType(Uri.fromFile(new File(path + apkName)),
                            "application/vnd.android.package-archive");
            promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(promptInstall);
        }
    };

    @Override
    public void serviceCreated() {
         /*Metodo para cargar la lista negra ya definida y para cargar la lista de apps instaladas
        Mediante SharedPreferences se almaceno el valor del check box para cada app
         */
        ServiceBlockApp.updateLists();

        txt_formiik_version.setText(ServiceBlockApp.formiikVersion);

        adapterApps = new AdapterApps(this, ServiceBlockApp.appsList);
        lv_apps.setAdapter(adapterApps);

        toggleButton.setChecked(ServiceBlockApp.fromBlackList);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                SharedPreferences.Editor editor = ServiceBlockApp.sharedPref.edit();

                if (isChecked) {
                    ServiceBlockApp.fromBlackList = true;
                    editor.putString("fromBlackList", "true");

                    String message = "";
                    for (int i = 0; i < ServiceBlockApp.blackList.size(); i++) {
                        message += ServiceBlockApp.blackList.get(i) + "\n";
                    }

                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                } else {
                    editor.putString("fromBlackList", "false");
                    ServiceBlockApp.fromBlackList = false;
                }

                editor.commit();
            }
        });
    }
}
