
package mobisocial.socialkit.musubi;

import java.security.PublicKey;
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
 * A Musubi feed of objects.
 * 
 */
public class Feed {
    static final String TAG = Musubi.TAG;
    private static final boolean DBG = true;

    private final Musubi mMusubi;
    private final Uri mUri;
    private final String mFeedName;
    private final Set<StateObserver> mObservers = new HashSet<StateObserver>();
    private boolean mObservingProvider = false;
    JunctionActor mActor;
    Junction mJunction;

    private String mSelection = null;
    private String[] mSelectionArgs = null;

    Feed(Musubi musubi, Uri feedUri) {
        mMusubi = musubi;
        mUri = feedUri;
        mFeedName = mUri.getLastPathSegment();

        mContentObserver = new ContentObserver(new Handler(
                mMusubi.getContext().getMainLooper())) {

            @Override
            public void onChange(boolean selfChange) {
                doContentChanged();
            }
        };
    }

    public final Uri getUri() {
        return mUri;
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

    public JSONObject getLatestObj() {
        Cursor cursor = query();
        if (cursor.moveToFirst()) {
            String entry = cursor.getString(cursor.getColumnIndexOrThrow("json"));
            try {
                JSONObject wrapper = new JSONObject(entry);
                return wrapper;
            } catch (JSONException e) {
                Log.wtf(TAG, "Error parsing json from db");
            }
        }
        return null;
    }

    public void setSelection(String selection, String[] selectionArgs) {
        mSelection = selection;
        mSelectionArgs = selectionArgs;
    }

    /**
     * Issues a query over this feed's objects.
     */
    public Cursor query(String[] projection, String selection, String[] selectionArgs,
            String order) {
        return mMusubi.getContext().getContentResolver().query(mUri, projection, selection,
                selectionArgs, order);
    }

    /**
     * Issues a query over this feed's objects.
     */
    public Cursor query(String selection, String[] selectionArgs) {
        String order = "_id desc LIMIT 1"; // TODO: fix.
        return mMusubi.getContext().getContentResolver().query(mUri, null, selection,
                selectionArgs, order);
    }

    /**
     * Issues a query over this feed's objects.
     */
    public Cursor query() {
        return query(mSelection, mSelectionArgs);
    }

    public void registerStateObserver(StateObserver observer) {
        synchronized (Feed.this) {
            mObservers.add(observer);
        }
        if (!mObservingProvider) {
            if (DBG) Log.d(TAG, "Enabling feed observer...");
            mObservingProvider = true;
            mMusubi.getContext().getContentResolver().registerContentObserver(mUri, false,
                    mContentObserver);
        }
    }

    public boolean removeStateObserver(StateObserver observer) {
        return mObservers.remove(observer);
    }

    public void postObj(Obj obj) {
        postInternal(obj.getType(), obj.getJson());
    }

    public void postAppStateRenderable(JSONObject state, FeedRenderable thumbnail) {
        try {
            JSONObject b = new JSONObject(state.toString());
            thumbnail.toJson(b);
            postInternal(Obj.TYPE_APP_STATE, b);
        } catch (JSONException e) {
            Log.e(TAG, "Error posting obj", e);
        }
    }

    private void postInternal(String type, JSONObject obj) {
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("json", obj.toString());
        mMusubi.getContentProviderThread().insert(mUri, values);
    }

    public User getLocalUser() {
        return User.getLocalUser(mMusubi.getContext(), mUri);
    }

    /**
     * List of remote participants available to this feed.
     */
    public Set<User> getRemoteUsers() {
        Uri feedMembersUri = Uri.parse("content://" + Musubi.AUTHORITY +
                "/members/" + mFeedName);
        Cursor cursor;
        try {
            String selection = null;
            String[] selectionArgs = null;
            String order = null;
            cursor = mMusubi.getContext().getContentResolver().query(feedMembersUri, null,
                    selection, selectionArgs, order);
        } catch (Exception e) {
            Log.e(TAG, "Error getting membership", e);
            return null;
        }
        HashSet<User> users = new HashSet<User>();
        if (!cursor.moveToFirst()) {
            return users; // TODO: doesn't include local user.
        }

        int nameIndex = cursor.getColumnIndex("name");
        int pubKeyIndex = cursor.getColumnIndex("public_key");
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(nameIndex);
            String pubKey = cursor.getString(pubKeyIndex);
            PublicKey k = User.publicKeyFromString(pubKey);
            users.add(new User(mMusubi.getContext(), false, name,
                    User.makePersonIdForPublicKey(k), mUri));
            cursor.moveToNext();
        }
        return users;
    }

    private void doContentChanged() {
        JSONObject obj = null;
        try {
            String selection = null;
            String[] selectionArgs = null;
            String order = "_id desc LIMIT 1"; // TODO: fix.
            Cursor c = mMusubi.getContext().getContentResolver().query(mUri, null, selection,
                    selectionArgs, order);
            if (c.moveToFirst()) {
                String entry = c.getString(c.getColumnIndexOrThrow("json"));
                obj = new JSONObject(entry);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying for app state", e);
            return;
        }

        synchronized (Feed.this) {
            for (StateObserver observer : mObservers) {
                observer.onUpdate(obj);
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
