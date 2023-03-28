package com.rvalerio.fgchecker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Calendar;
import java.util.List;

public class Utils {
    private Utils() {

    }

    public static boolean postLollipopMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static ActivityManager.RunningAppProcessInfo getTopProcessInfo(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes != null && processes.size() > 0) {
            for (ActivityManager.RunningAppProcessInfo info : processes) {
                if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return info;
                }
            }
        }
        return null;
    }

    //该代码经使用发现检测不精准，请使用下面最新检测方法-更新日期2022/06/22
     @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
     public static UsageStats getTopUsageStats(Context context) {
         UsageStatsManager manager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
         if (manager != null) {
             //Get the app record in the last month
             Calendar calendar = Calendar.getInstance();
             final long end = calendar.getTimeInMillis();
             calendar.add(Calendar.MONTH, -1);
             final long start = calendar.getTimeInMillis();

             List<UsageStats> usageStats = manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end);
             if (usageStats == null || usageStats.isEmpty()) {
                 return null;
             }

             UsageStats lastStats = null;
             for (UsageStats stats : usageStats) {
                 // if from notification bar, class name will be null
                 if (stats.getPackageName() == null) {
                     continue;
                 }
                 final long lastTime = stats.getLastTimeUsed();
                 if (lastStats == null || lastStats.getLastTimeUsed() < lastTime) {
                     lastStats = stats;
                 }
             }
             return lastStats;
         }
         return null;
     }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    public static String getForegroundPackageName(Context context) {
        //Get the app record in the last month
        Calendar calendar = Calendar.getInstance();
        final long end = calendar.getTimeInMillis();
        calendar.add(Calendar.MONTH, -1);
        final long start = calendar.getTimeInMillis();

        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        UsageEvents usageEvents = usageStatsManager.queryEvents(start, end);
        UsageEvents.Event event = new UsageEvents.Event();
        String packageName = null;
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                packageName = event.getPackageName();
            }
        }
        return packageName;
    }

    public static String getTopProcessPackageName(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            /** 在2022/06/22该段代码已弃用
             UsageStats usageStats = getTopUsageStats(context);
             if (usageStats != null) {
             return usageStats.getPackageName();
             }**/
            return getForegroundPackageName(context);
        } else {
            ActivityManager.RunningAppProcessInfo info = getTopProcessInfo(context);
            if (info != null) {
                return info.processName;
            }
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        boolean granted = mode == AppOpsManager.MODE_ALLOWED;
        return granted;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean canUsageStats(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getPackageName());
        }
        if (mode == AppOpsManager.MODE_DEFAULT && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == AppOpsManager.MODE_ALLOWED);
        }
    }


}
