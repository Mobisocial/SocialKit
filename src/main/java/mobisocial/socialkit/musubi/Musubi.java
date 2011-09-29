
package mobisocial.socialkit.musubi;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Use the DungBeetle APIs in your application.
 */
public class Musubi {
    static final String TAG = "SocialKit-DB";
    public static final String AUTHORITY = "org.mobisocial.db";

    public static final String EXTRA_FEED_URI = "mobisocial.db.FEED";

    private static boolean DBG = true;
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

    public Object getMultiplayer(Context context, Intent details) {
        return new Multiplayer(context, details);
    }

    public static class Multiplayer {
        private final Musubi mMusubi;
        public static final String ACTION_TWO_PLAYERS = "mobisocial.intent.action.TWO_PLAYERS";
        public static final String ACTION_MULTIPLAYER = "mobisocial.intent.action.MULTIPLAYER";

        public static final String EXTRA_MEMBERS = "members";
        public static final String EXTRA_LOCAL_MEMBER_INDEX = "local_member_index";
        public static final String EXTRA_GLOBAL_MEMBER_CURSOR = "global_member_cursor";

        public static final String OBJ_MEMBER_CURSOR = "member_cursor";

        final Intent mLaunchIntent;
        final String[] mMembers;
        final Uri mFeedUri;
        final int mLocalMemberIndex;
        int mGlobalMemberCursor;
        private StateObserver mAppStateObserver;

        public Multiplayer(Context context, Intent intent) {
            mMusubi = Musubi.getInstance(context, intent);
            mLaunchIntent = intent;
            // TODO: intent.getStringArrayExtra("membership") ~ fixed, open, etc.
            mMembers = intent.getStringArrayExtra(EXTRA_MEMBERS);
            mFeedUri = intent.getParcelableExtra(EXTRA_FEED_URI);
            mLocalMemberIndex = intent.getIntExtra(EXTRA_LOCAL_MEMBER_INDEX, -1);
            mGlobalMemberCursor = intent.getIntExtra(EXTRA_GLOBAL_MEMBER_CURSOR, -1);

            mMusubi.getFeed().registerStateObserver(mInternalStateObserver);
        }

        public int getLocalMemberIndex() {
            return mLocalMemberIndex;
        }

        public int getGlobalMemberCursor() {
            return mGlobalMemberCursor;
        }

        public boolean isMyTurn() {
            Log.d(TAG, "Checking for turn: " + mLocalMemberIndex + " vs " + mGlobalMemberCursor);
            return mLocalMemberIndex == mGlobalMemberCursor;
        }

        public void takeTurn(JSONObject state, String thumbHtml) {
            try {
                state.put(OBJ_MEMBER_CURSOR,
                        (mGlobalMemberCursor + 1) % mMembers.length);
                if (DBG) Log.d(TAG, "Sent cursor " + state.optInt(OBJ_MEMBER_CURSOR));
            } catch (JSONException e) {
                Log.e(TAG, "Failed to update cursor.", e);
            }
            mMusubi.getFeed().postObjectWithHtml(state, thumbHtml);
        }

        public void setStateObserver(StateObserver observer) {
            mAppStateObserver = observer;
        }

        private final StateObserver mInternalStateObserver = new StateObserver() {
            @Override
            public void onUpdate(JSONObject newState) {
                try {
                    mGlobalMemberCursor = newState.getInt(OBJ_MEMBER_CURSOR);
                    if (DBG) Log.d(TAG, "Updated cursor to " + mGlobalMemberCursor);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to get member_cursor.", e);
                }

                if (mAppStateObserver != null) {
                    mAppStateObserver.onUpdate(newState);
                }
            }
        };
    }
}
