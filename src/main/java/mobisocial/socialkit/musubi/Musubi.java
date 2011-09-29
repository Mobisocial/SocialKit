
package mobisocial.socialkit.musubi;

import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Use the DungBeetle APIs in your application.
 */
public class Musubi {
    static final String TAG = "SocialKit-DB";
    public static final String AUTHORITY = "org.mobisocial.db";

    public static final String EXTRA_FEED_URI = "mobisocial.db.FEED";

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
        return intent.hasExtra(EXTRA_FEED_URI);
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
        return new Feed(this, (Uri) mIntent.getParcelableExtra(EXTRA_FEED_URI));
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

    public Object getMultiplayer(Intent details) {
        return new Multiplayer(details);
    }

    public static class Multiplayer {
        public static final String ACTION_TWO_PLAYERS = "mobisocial.intent.action.TWO_PLAYERS";
        public static final String ACTION_MULTIPLAYER = "mobisocial.intent.action.MULTIPLAYER";

        public static final String EXTRA_MEMBERS = "members";
        public static final String EXTRA_LOCAL_MEMBER_INDEX = "local_member_index";
        public static final String EXTRA_GLOBAL_MEMBER_CURSOR = "global_member_cursor";

        final Intent mLaunchIntent;
        final String[] mMembers;
        final Uri mFeedUri;
        final int mLocalMemberIndex;
        final int mGlobalMemberCursor;

        public Multiplayer(Intent intent) {
            mLaunchIntent = intent;
            // TODO: intent.getStringArrayExtra("membership") ~ fixed, open, etc.
            mMembers = intent.getStringArrayExtra(EXTRA_MEMBERS);
            mFeedUri = intent.getParcelableExtra(EXTRA_FEED_URI);
            mLocalMemberIndex = intent.getIntExtra(EXTRA_LOCAL_MEMBER_INDEX, -1);
            mGlobalMemberCursor = intent.getIntExtra(EXTRA_GLOBAL_MEMBER_CURSOR, -1);
        }

        public int getLocalMemberIndex() {
            return mLocalMemberIndex;
        }

        public boolean isMyTurn() {
            return true;
        }

        public JSONObject getState() {
            return null;
        }
    }
}
