package mobisocial.socialkit;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionActor;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class SocialKit {
    /**
     * Use the DungBeetle APIs in your application.
     */
    public static class Dungbeetle {
        private static final String TAG = "SocialKit-DB";

        public static final String LAUNCH_TWO_PLAYERS = "mobisocial.intent.action.LAUNCH_TWO_PLAYERS";
        public static final String LAUNCH_N_PLAYERS = "mobisocial.intent.action.LAUNCH_N_PLAYERS";
        public static final String INTENT_EXTRA_DUNGBEETLE = "mobisocial.db.FEED";
        public static final String INTENT_EXTRA_USER_NUMBER = "ms.db.id";

        private final Intent mIntent;
        private final Context mContext;

        public static boolean isDungbeetleIntent(Intent intent) {
            return intent.hasExtra(INTENT_EXTRA_DUNGBEETLE);
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
            return new Feed((Uri)mIntent.getParcelableExtra("mobisocial.db.FEED"));
        }

        private Dungbeetle(Context context, Intent intent) {
            mContext = context;
            mIntent = intent;
        }

        public static Dungbeetle getInstance(Context context, Intent intent) {
            return new Dungbeetle(context, intent);
        }

        /**
         * A DungBeetle Feed.
         *
         */
        public class Feed {
            private JSONObject mState;
            private final Uri mUri;
            private final Set<StateObserver> mObservers = new HashSet<StateObserver>();
            private boolean mObservingProvider = false;

            private Feed(Uri feedUri) {
                mUri = feedUri;
                try {
                    String selection = null;
                    String[] selectionArgs = null;
                    String order = "_id desc LIMIT 1"; // TODO: fix.
                    Cursor c = mContext.getContentResolver().query(mUri, null, selection,
                            selectionArgs, order);
                    if (c.moveToFirst()) {
                        String entry = c.getString(c.getColumnIndexOrThrow("json"));
                        JSONObject obj = new JSONObject(entry);
                        mState = obj.optJSONObject("state");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading app state", e);
                }
            }

            public Junction getJunction() {
                return null;
            }

            public JSONObject getApplicationState() {
                synchronized (Feed.this) {
                    return mState;
                }
            }

            public void registerStateObserver(StateObserver observer) {
                synchronized (Feed.this) {
                    mObservers.add(observer);
                }
                if (!mObservingProvider) {
                    mObservingProvider = true;
                    mContext.getContentResolver().registerContentObserver(mUri, false, mContentObserver);
                }
            }

            public void setApplicationState(JSONObject appState) {
                JSONObject json = new JSONObject();
                try {
                    json.put("state", appState);
                    json.put("txt", "this is an update");
                } catch (Exception e) {
                    Log.wtf(TAG, "Error creating json for database");
                    return;
                }

                ContentValues values = new ContentValues();
                values.put("type", "invite_app_session");
                values.put("json", json.toString());
                mContext.getContentResolver().insert(mUri, values);
            }

            /**
             * @hide
             */
            public void postMessage(JSONObject message) {

            }

            /**
             * List of participants available to this thread.
             * @return
             */
            public Set<User> getMembers() {
                return null;
            }

            /**
             * @hide
             */
            public int getMemberNumber() {
                return mIntent.getIntExtra(INTENT_EXTRA_USER_NUMBER, -1);
            }

            private ContentObserver mContentObserver = new ContentObserver(
                    new Handler(mContext.getMainLooper())) {

                @Override
                public void onChange(boolean selfChange) {
                    JSONObject state = null;
                    try {
                        String selection = null;
                        String[] selectionArgs = null;
                        String order = "_id desc LIMIT 1"; // TODO: fix.
                        Cursor c = mContext.getContentResolver().query(mUri, null,
                                selection, selectionArgs, order);
                        if (c.moveToFirst()) {
                            String entry = c.getString(c.getColumnIndexOrThrow("json"));
                            JSONObject obj = new JSONObject(entry);
                            state = obj.optJSONObject("state");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error querying for app state", e);
                        return;
                    }
                    if (state == null) {
                        Log.e(TAG, "no app state");
                        return;
                    }
                    synchronized (Feed.this) {
                        for (StateObserver observer : mObservers) {
                            observer.onUpdate(state);
                        }
                    }
                }
            };
        }

        /**
         * @hide
         */
        public Intent getIntentForReconfigLaunch() {
            return null;
        }

        public class User {
            public JSONObject getAttribute(String id) {
                return null;
            }
        }

        public interface StateObserver {
            public void onUpdate(JSONObject newState);
        }
    }

    public class Junction {
        public void bind(JunctionActor actor) {

        }
    }
}
