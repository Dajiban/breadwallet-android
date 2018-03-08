package io.digibyte.tools.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.digibyte.DigiByte;
import io.digibyte.presenter.activities.BreadActivity;
import io.digibyte.tools.listeners.SyncReceiver;
import io.digibyte.wallet.BRPeerManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/19/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class SyncManager
{
    public interface onStatusListener
    {
        void onSyncManagerStart();
        void onSyncManagerUpdate();
        void onSyncManagerFinished();
    }

    private static final String TAG = SyncManager.class.getName();
    private static SyncManager instance;
    private static final long SYNC_PERIOD = TimeUnit.HOURS.toMillis(24);
    private static SyncProgressTask syncTask;

    private double theProgress;
    private boolean theRunningFlag;
    private long theLastBlockTimestamp;

    public double getProgress() { return theProgress; }
    public boolean isRunning() { return theRunningFlag; }
    public long getLastBlockTimestamp() { return theLastBlockTimestamp; }

    private ArrayList<onStatusListener> theListeners = new ArrayList<>();
    public void addListener(onStatusListener aListener) { theListeners.add(aListener); }
    public void removeListener(onStatusListener aListener) { theListeners.remove(aListener); }

    public static SyncManager getInstance()
    {
        if (instance == null)
        {
            instance = new SyncManager();
        }
        return instance;
    }

    private void createAlarm(Context app, long time)
    {
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if(null != alarmManager)
        {
            Intent intent = new Intent(app, SyncReceiver.class);
            intent.setAction(SyncReceiver.SYNC_RECEIVER);//my custom string action name
            PendingIntent pendingIntent = PendingIntent.getService(app, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, time, time + TimeUnit.MINUTES.toMillis(1), pendingIntent);//first start will start asap
        }
    }

    public synchronized void updateAlarms(Context app)
    {
        createAlarm(app, System.currentTimeMillis() + SYNC_PERIOD);
    }

    public synchronized void startSyncingProgressThread()
    {
        Log.d(TAG, "startSyncingProgressThread:" + Thread.currentThread().getName());

        try
        {
            if (syncTask != null)
            {
                if (theRunningFlag)
                {
                    Log.e(TAG, "startSyncingProgressThread: syncTask.running == true, returning");
                    return;
                }
                syncTask.interrupt();
                syncTask = null;
            }
            syncTask = new SyncProgressTask();
            syncTask.start();

        }
        catch (IllegalThreadStateException ex)
        {
            ex.printStackTrace();
        }

    }

    public synchronized void stopSyncingProgressThread()
    {
        Log.d(TAG, "stopSyncingProgressThread");
        if (!(DigiByte.getBreadContext() instanceof BreadActivity))
        {
            Log.e(TAG, "stopSyncingProgressThread: ctx is null");
            return;
        }
        try
        {
            if (syncTask != null)
            {
                syncTask.interrupt();
                syncTask = null;
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private class SyncProgressTask extends Thread
    {
        @Override
        public void run()
        {
            if (theRunningFlag)
            {
                return;
            }

            try
            {
                theProgress = 0;
                theRunningFlag = true;

                new Handler(Looper.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (onStatusListener listener : theListeners)
                        {
                            listener.onSyncManagerStart();
                        }
                    }
                });

                while (theRunningFlag)
                {
                    int startHeight = BRSharedPrefs.getStartHeight(DigiByte.getBreadContext());
                    theProgress = BRPeerManager.syncProgress(startHeight);
                    theLastBlockTimestamp = BRPeerManager.getInstance().getLastBlockTimestamp();

                    new Handler(Looper.getMainLooper()).post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            for (onStatusListener listener : theListeners)
                            {
                                listener.onSyncManagerUpdate();
                            }
                        }
                    });

                    if (theProgress == 1)
                    {
                        theRunningFlag = false;
                    }
                }

                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {
                    Log.e(TAG, "run: Thread.sleep was Interrupted:" + Thread.currentThread().getName(), e);
                }

                Log.d(TAG, "run: SyncProgress task finished:" + Thread.currentThread().getName());
            }
            finally
            {
                theProgress = 0;
                theRunningFlag = false;

                new Handler(Looper.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for (onStatusListener listener : theListeners)
                        {
                            listener.onSyncManagerFinished();
                        }
                    }
                });
            }
        }
    }
}
