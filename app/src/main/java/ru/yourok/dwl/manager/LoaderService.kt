package ru.yourok.dwl.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import ru.yourok.dwl.settings.Settings
import ru.yourok.dwl.utils.Utils
import ru.yourok.m3u8loader.R
import ru.yourok.m3u8loader.activitys.mainActivity.MainActivity
import kotlin.concurrent.thread


/**
 * Created by yourok on 19.11.17.
 */
class LoaderService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    private var isUpdates = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.hasExtra("exit"))
            stopSelf()
        startUpdate()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isUpdates = false
        sendNotificationComplete()
        super.onDestroy()
    }

    private fun startUpdate() {
        synchronized(isUpdates) {
            if (isUpdates)
                return
            isUpdates = true
        }
        thread {
            while (isUpdates) {
                sendNotification()
                Thread.sleep(100)
            }
        }
    }

    private fun sendNotificationComplete() {
        var msg = getString(R.string.loading_complete)
        if (!Notifyer.error.isNullOrEmpty())
            msg = getString(R.string.loading_error)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val notificationBuilder = NotificationCompat.Builder(this)
                .setContentTitle(msg)
                .setContentText(Notifyer.error)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
        val notificationManager = Settings.context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun sendNotification() {
        val index = Manager.getCurrentLoader()
        if (index == -1)
            return
        val state = Manager.getLoaderStat(index) ?: return


        var percent = 0
        if (state.size > 0)
            percent = (state.loadedFragments * 100 / state.fragments)

        val status = "%d/%d %s/sec %d%%".format(state.loadedFragments, state.fragments, Utils.byteFmt(state.speed), percent)

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val notificationBuilder = NotificationCompat.Builder(this)
                .setContentTitle(state.name)
                .setContentText(status)
                .setProgress(100, percent, false)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
        val notificationManager = Settings.context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    companion object {
        fun start() {
            Settings.context?.let {
                val intent = Intent(it, LoaderService::class.java)
                it.startService(intent)
            }
        }

        fun stop() {
            Settings.context?.let {
                val intent = Intent(it, LoaderService::class.java)
                intent.putExtra("exit", true)
                it.startService(intent)
            }
        }
    }
}