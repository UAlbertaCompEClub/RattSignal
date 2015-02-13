package ca.jviau.rattsignal.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.microsoft.windowsazure.notifications.NotificationsHandler;

/**
 * @author Jacob
 * @version 1.0
 * @since 2015-02-10
 */
public class RattSignalHandler extends NotificationsHandler {

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onRegistered(Context context, final String gcmRegistrationId) {
        super.onRegistered(context, gcmRegistrationId);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                SignalActivity.mClient.getPush().register(gcmRegistrationId, null);
                return null;
            }
        }.execute();
    }

    @Override
    public void onReceive(Context context, Bundle bundle) {
        // TODO: read bundle for # of people at RATT, only notify if certain #.
        String nhMessage = bundle.getString("message");
        int count = bundle.getInt("count");

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("push_notifications", false)) {
            sendNotification(context, nhMessage, sp.getBoolean("ring_on_push", false), sp.getBoolean("vibrate_on_push", false));
        }
    }

    private void sendNotification(Context context, String msg, boolean ring, boolean vibrate) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, SignalActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("RATT SIGNAL!")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg)
                .setDefaults(Notification.DEFAULT_LIGHTS
                        | (vibrate ? 0 : Notification.DEFAULT_VIBRATE)
                        | (ring ? 0 : Notification.DEFAULT_SOUND));

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
