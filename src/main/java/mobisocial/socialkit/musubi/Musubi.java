
package mobisocial.socialkit.musubi;

import java.util.List;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Use the DungBeetle APIs in your application.
 */
public class Musubi {
    static final String TAG = "SocialKit-DB";
    public static final String AUTHORITY =
            "edu.stanford.mobisocial.dungbeetle.DungBeetleContentProvider";
    public static final String LAUNCH_TWO_PLAYERS = "mobisocial.intent.action.TWO_PLAYERS";
    public static final String LAUNCH_N_PLAYERS = "mobisocial.intent.action.N_PLAYERS";
    public static final String INTENT_EXTRA_FEED = "mobisocial.db.FEED";

    private final Intent mIntent;
    private final Context mContext;
    private final ContentProviderThread mContentProviderThread;

    public static boolean isMusubiInstalled(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage("edu.stanford.mobisocial.dungbeetle");
        return context.getPackageManager().queryIntentActivities( intent, 0).size() > 0;
    }

    public static boolean isMusubiIntent(Intent intent) {
        return intent.hasExtra(INTENT_EXTRA_FEED);
    }

    /**
     * @hide
     */
    public void startInviteMembersActivity() {

    }

    /**
     * @hide
     */
    public Uri getInvitationUri() {
        return null;
    }

    public Feed getFeed() {
        return new Feed(this, (Uri) mIntent.getParcelableExtra(INTENT_EXTRA_FEED));
    }

    public Feed getFeed(Uri feedUri) {
        return new Feed(this, feedUri);
    }

    private Musubi(Context context, Intent intent) {
        mContext = context;
        mIntent = intent;
        mContentProviderThread = new ContentProviderThread();
        mContentProviderThread.start();
    }

    public static Musubi getInstance(Context context, Intent intent) {
        return new Musubi(context, intent);
    }

    ContentProviderThread getContentProviderThread() {
        return mContentProviderThread;
    }

    Context getContext() {
        return mContext;
    }

    public interface StateObserver {
        public void onUpdate(JSONObject newState);
    }

    class ContentProviderThread extends Thread {
        public Handler mHandler;

        public void run() {
            Looper.prepare();

            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    Insertion i = (Insertion)msg.obj;
                    mContext.getContentResolver().insert(i.uri, i.cv);
                }
            };

            Looper.loop();
        }

        public void insert(Uri uri, ContentValues cv) {
            Insertion i = new Insertion(uri, cv);
            Message m = mHandler.obtainMessage();
            m.obj = i;
            mHandler.sendMessage(m);
        }

        private class Insertion {
            Uri uri;
            ContentValues cv;

            public Insertion(Uri uri, ContentValues cv) {
                this.uri = uri;
                this.cv = cv;
            }
        }
    }
}
