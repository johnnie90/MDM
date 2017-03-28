package com.formiik.formiikmdm;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.*;

public class ServiceBlockApp extends Service {

    private  MyTask                 myTask;
    private RecentUseComparator     mRecentComp;
    private static Context          context;
    public static SharedPreferences sharedPref;
    private static final String     sharedName = "sharedBlock";

    public static serviceCreated    listener;
    public static Boolean           serviceRunning = false;

    private String                  currentApp = "";

    public static boolean           fromBlackList;

    public static String            formiikVersion;

    public static List<Map<String,String>>  appsList;
    public static List<String>              blackList;


    @Override
    public void onCreate() {
        super.onCreate();

        myTask = new MyTask();
        context = getApplicationContext();
        sharedPref = context.getSharedPreferences(sharedName, Context.MODE_PRIVATE);

        formiikVersion = getString(R.string.formiik_no_installed);
        try {
            formiikVersion = getPackageManager().getPackageInfo("formiik.com.mobiik.www", 0).versionName;
        }catch (Exception e){
            e.printStackTrace();
        }

        Log.d("onCreate Service", " Formiik MDM version: " + BuildConfig.VERSION_NAME + " Formiik version: " + formiikVersion);


        /*se obtiene el valor guardado en sharedpreferences para saber en que estado se quedo el togglebutton
         */
        if(ServiceBlockApp.sharedPref.getString("fromBlackList","false").equalsIgnoreCase("false")){
            fromBlackList = false;
        }else{
            fromBlackList = true;
        }

        updateLists();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkPermission(getApplicationContext());
        myTask.execute();
        serviceRunning = true;

        if(listener != null) listener.serviceCreated();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        serviceRunning = false;
        myTask.cancel(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private class MyTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {

            super.onPreExecute();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRecentComp = new RecentUseComparator();
            }

        }

        @Override
        protected String doInBackground(String... params) {

            while (!isCancelled()) {

                try {
                    Thread.sleep(1000);


                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                        currentApp = getTopPackage();

                    }else {

                        ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> RunningTask = mActivityManager
                                .getRunningTasks(1);
                        ActivityManager.RunningTaskInfo ar = RunningTask.get(0);
                        currentApp = ar.topActivity.getPackageName();
                    }

                    //Log.d("currentApp", currentApp);

                    /*
                    if (currentApp.contains("installer")) {

                        Intent blockScreen = new Intent(getApplicationContext(), BlockApp.class);
                        blockScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(blockScreen);

                        Log.d("Service", "UninstallerActivity");
                    }*/


                    if (fromBlackList) {
                        for (int i = 0; i < blackList.size(); i++) {
                            if (currentApp.contains(blackList.get(i))) {

                                Intent blockScreen = new Intent(getApplicationContext(), BlockApp.class);
                                blockScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(blockScreen);
                            }
                        }
                    } else {
                        try {
                            for (int i = 0; i < appsList.size(); i++) {
                                if (currentApp.equalsIgnoreCase(appsList.get(i).get("name"))
                                        && appsList.get(i).get("block").equalsIgnoreCase("true")) {

                                    Intent blockScreen = new Intent(getApplicationContext(), BlockApp.class);
                                    blockScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(blockScreen);

                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        String getTopPackage(){
            long ts = System.currentTimeMillis();
            UsageStatsManager mUsageStatsManager = (UsageStatsManager)getSystemService("usagestats");
            List<UsageStats> usageStats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, ts-5000, ts);
            if (usageStats == null || usageStats.size() == 0) {
                return "noPackage";
            }
            Collections.sort(usageStats, mRecentComp);
            return usageStats.get(0).getPackageName();
        }

    }

    static class RecentUseComparator implements Comparator<UsageStats> {

        @Override
        public int compare(UsageStats lhs, UsageStats rhs) {
            return (lhs.getLastTimeUsed() > rhs.getLastTimeUsed()) ? -1 : (lhs.getLastTimeUsed() == rhs.getLastTimeUsed()) ? 0 : 1;
        }
    }


    public static void updateLists(){
        appsList    = loadAppsList();
        blackList = loadBlackList();
    }

    private static List<Map<String,String>>  loadAppsList() {

        //sharedPref = context.getSharedPreferences(sharedName, Context.MODE_PRIVATE);

        List<Map<String,String>>  appsList = new ArrayList<>();

        SharedPreferences.Editor editor = sharedPref.edit();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> appList = context.getPackageManager().queryIntentActivities(mainIntent, 0);
        Collections.sort(appList, new ResolveInfo.DisplayNameComparator(context.getPackageManager()));
        List<PackageInfo> packs = context.getPackageManager().getInstalledPackages(0);

        for (int i = 0; i < packs.size(); i++) {

            Map<String, String> map = new HashMap<String, String>();

            map.put("name", packs.get(i).packageName);

            if(sharedPref.getString(packs.get(i).packageName,"false").equalsIgnoreCase("false")) {
                map.put("block", "false");
                editor.putString(packs.get(i).packageName, "false");
            }else{
                map.put("block", "true");
            }

            appsList.add(map);
        }

        editor.commit();

        return appsList;
    }

    private static List<String>  loadBlackList() {
        List<String> blackList = new ArrayList<>();

        blackList.add("facebook");
        blackList.add("whatsapp");
        blackList.add("vending");

        return  blackList;
    }


    private static void checkPermission(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if(!needPermissionForBlocking(context)) {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    private static boolean needPermissionForBlocking(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);

            return  (mode == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return true;
        }
    }


    public interface serviceCreated{
        void serviceCreated();
    }
}
