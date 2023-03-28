package com.rvalerio.fgchecker.detectors;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.rvalerio.fgchecker.Utils;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class LollipopDetector implements Detector {

    private static final String TAG = "LollipopDetector";
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public String getForegroundApp(final Context context) {
        if (!Utils.hasUsageStatsPermission(context))
        {
            Log.e(TAG, "hasUsageStatsPermission: " + false);
            return null;
        }

        String foregroundApp = null;

        //UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Service.USAGE_STATS_SERVICE);
        UsageStatsManager mUsageStatsManager = ContextCompat.getSystemService(context, UsageStatsManager.class);
        if (mUsageStatsManager == null) {
            Log.e(TAG, "mUsageStatsManager: null");
            return null;
        }
        long time = System.currentTimeMillis();

        List<UsageStats> appList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (mySortedMap != null && !mySortedMap.isEmpty()) {
                UsageStats us = mySortedMap.get(mySortedMap.lastKey());
                if (us != null) {
                    String currentApp = us.getPackageName();
                    Log.e(TAG, "currentApp: " + currentApp);
                }
            }
        }

        UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 3600, time);
        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if(event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundApp = event.getPackageName();
                Log.w(TAG, "getForegroundApp: " + foregroundApp);
            }
            else if(event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundApp = event.getPackageName();
                Log.e(TAG, "getForegroundApp: " + foregroundApp);
            } else {
                Log.i(TAG, "getForegroundApp: " + foregroundApp);
            }
        }
        Log.i(TAG, "foregroundApp: " + foregroundApp);
        return foregroundApp ;
    }
}
