package android.example.com.squawker.fcm;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.example.com.squawker.R;
import android.example.com.squawker.provider.SquawkContract;
import android.example.com.squawker.provider.SquawkProvider;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Calendar;
import java.util.Map;

// COMPLETED (1) Make a new Service in the fcm package that extends from FirebaseMessagingService.
public class SquawkerService extends FirebaseMessagingService {
    private static final String LOG_TAG = SquawkerService.class.getSimpleName();

    // COMPLETED (2) As part of the new Service - Override onMessageReceived. This method will
    // be triggered whenever a squawk is received. You can get the data from the squawk
    // message using getData(). When you send a test message, this data will include the
    // following key/value pairs:
    // test: true
    // author: Ex. "TestAccount"
    // authorKey: Ex. "key_test"
    // message: Ex. "Hello world"
    // date: Ex. 1484358455343

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // COMPLETED (3) As part of the new Service - If there is message data, get the data using
        // the keys and do two things with it :
        Map<String, String> data = remoteMessage.getData();
        if (data != null) {
            Log.d(LOG_TAG, String.format("Message received: %s %s %s %s", data.get("date"), data.get("test"), data.get("author"), data.get("message")));
            // 1. Display a notification with the first 30 character of the message
            // If you don't know how to make notifications or interact with a content provider
            // look at the notes in the classroom for help.
            String channelId = getString(R.string.channel_name);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String description = getString(R.string.channel_description);
                int importance = NotificationManagerCompat.IMPORTANCE_DEFAULT;
                @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(channelId, channelId, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_duck)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(shortMessage(data.get("message")))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
            notificationManagerCompat.notify(0, notificationBuilder.build());

            // 2. Use the content provider to insert a new message into the local database
            // Hint: You shouldn't be doing content provider operations on the main thread.
            ContentValues values = new ContentValues();
            values.put(SquawkContract.COLUMN_AUTHOR, data.get("author"));
            values.put(SquawkContract.COLUMN_AUTHOR_KEY, data.get("authorKey"));
            values.put(SquawkContract.COLUMN_MESSAGE, data.get("message"));
            long date = Calendar.getInstance().getTimeInMillis();
            try {
                date = Long.parseLong(data.get("date"));
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, String.format("Date in message should be long, defaulting to current time: %s (%s)", data.get("date"), e.getMessage()));
            }
            values.put(SquawkContract.COLUMN_DATE, date);
            getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, values);
        }

        super.onMessageReceived(remoteMessage);
    }

    private String shortMessage(String message) {
        final int MAX = 30 - 3;
        StringBuilder shortMessage = new StringBuilder();
        for (String word : message.split(" ")) {
            int remaining = MAX - shortMessage.length();
            if (remaining - 1 > word.length()) {
                if (shortMessage.length() != 0) shortMessage.append(" ");
                shortMessage.append(word);
            } else if (remaining > MAX / 2) {
                if (shortMessage.length() != 0) shortMessage.append(" ");
                if (remaining + 3 > word.length()) {
                    shortMessage.append(word);
                } else {
                    shortMessage.append(word.substring(0, remaining));
                }
            }
        }
        if (message.length() > shortMessage.length()) {
            shortMessage.append("...");
        }
        return shortMessage.toString();
    }

}
