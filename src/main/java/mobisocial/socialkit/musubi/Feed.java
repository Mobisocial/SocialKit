
package mobisocial.socialkit.musubi;

import java.util.HashSet;
import java.util.Set;

import mobisocial.socialkit.musubi.Musubi.StateObserver;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.android.AndroidJunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;

/**
 * A Musubi Feed.
 * 
 * 
 *      BaseFeed
 *  AsciiArt   TicTacToe
 *  
 *  FilteredFeed() { getTypes() { return {"appstate","@renderable",...} } }
 */
public class Feed {
    static final String TAG = Musubi.TAG;
    private static final String TYPE_APP_STATE = "appstate";
    private final Musubi mMusubi;
    private final Cursor mCursor;
    private final Uri mUri;
    private final Set<StateObserver> mObservers = new HashSet<StateObserver>();
    private boolean mObservingProvider = false;
    JunctionActor mActor;
    Junction mJunction;

    Feed(Musubi musubi, Uri feedUri) {
        mMusubi = musubi;
        mUri = feedUri;

        mContentObserver = new ContentObserver(new Handler(
                mMusubi.getContext().getMainLooper())) {

            @Override
            public void onChange(boolean selfChange) {
                doContentChanged();
            }
        };

        Cursor cursor;
        try {
            String selection = null;
            String[] selectionArgs = null;
            String order = "_id desc LIMIT 1"; // TODO: fix.
            cursor = mMusubi.getContext().getContentResolver().query(mUri, null, selection,
                    selectionArgs, order);
        } catch (Exception e) {
            Log.e(TAG, "Error loading app state", e);
            cursor = null;
        }
        mCursor = cursor;
    }

    public Junction getJunction() {
        if (mJunction != null) {
            return mJunction;
        }

        if (mActor == null) {
            mActor = new DbJunctionActor();
        }

        Uri uri = Uri.parse("junction://sb.openjunction.org/dbf-" + mUri.getLastPathSegment());
        try {
            mJunction = AndroidJunctionMaker.bind(uri, mActor);
        } catch (JunctionException e) {
            Log.e(TAG, "Error connecting to junction");
        }
        return mJunction;
    }

    public JSONObject getLatestState() {
        if (mCursor.moveToFirst()) {
            String entry = mCursor.getString(mCursor.getColumnIndexOrThrow("json"));
            try {
                JSONObject wrapper = new JSONObject(entry);
                return wrapper.optJSONObject("state");
            } catch (JSONException e) {
                Log.wtf(TAG, "Error parsing json from db");
            }
        }
        return null;
    }

    /**
     * @hide
     */
    public JSONObject getLatest(String type) {
        return null;
    }

    public void registerStateObserver(StateObserver observer) {
        synchronized (Feed.this) {
            mObservers.add(observer);
        }
        if (!mObservingProvider) {
            mObservingProvider = true;
            mMusubi.getContext().getContentResolver().registerContentObserver(mUri, false,
                    mContentObserver);
        }
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void postObject(JSONObject appState) {
        postObject(appState, new JSONObject());
    }

    public void postObjectWithHtml(JSONObject appState, String thumbnailHtml) {
        JSONObject b = new JSONObject();
        try {
            b.put("html", thumbnailHtml);
        } catch (JSONException e) {
        }
        postObject(appState, b);
    }

    public void postObjectWithText(JSONObject appState, String thumbnailTxt) {
        JSONObject b = new JSONObject();
        try {
            b.put("txt", thumbnailTxt);
        } catch (JSONException e) {
        }
        postObject(appState, b);
    }

    private void postObject(JSONObject appState, JSONObject meta) {
        try {
            meta.put("state", appState);
        } catch (Exception e) {
            Log.wtf(TAG, "Error creating json for database");
            return;
        }

        ContentValues values = new ContentValues();
        values.put("type", TYPE_APP_STATE);
        values.put("json", meta.toString());
        mMusubi.getContentProviderThread().insert(mUri, values);
    }

    /**
     * List of participants available to this thread.
     * 
     * @return
     */
    public Set<User> getMembers() {
        return null;
    }

    private void doContentChanged() {
        JSONObject state = null;
        try {
            String selection = null;
            String[] selectionArgs = null;
            String order = "_id desc LIMIT 1"; // TODO: fix.
            Cursor c = mMusubi.getContext().getContentResolver().query(mUri, null, selection,
                    selectionArgs, order);
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

    private final ContentObserver mContentObserver;

    class DbJunctionActor extends JunctionActor {
        @Override
        public void onMessageReceived(MessageHeader h, JSONObject json) {
            // TODO: trigger user-defined handlers
        }
    }
}
