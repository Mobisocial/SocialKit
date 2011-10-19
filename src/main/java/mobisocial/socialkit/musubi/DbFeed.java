/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mobisocial.socialkit.musubi;

import java.util.HashSet;
import java.util.Set;

import mobisocial.socialkit.Feed;
import mobisocial.socialkit.Obj;

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
public class DbFeed implements Feed {
    static final String TAG = Musubi.TAG;
    private static final boolean DBG = true;

    private final Musubi mMusubi;
    private final Uri mUri;
    private final String mFeedName;
    private final Set<FeedObserver> mObservers = new HashSet<FeedObserver>();
    private boolean mObservingProvider = false;
    JunctionActor mActor;
    Junction mJunction;

    private String mSelection = null;
    private String[] mSelectionArgs = null;

    DbFeed(Musubi musubi, Uri feedUri) {
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

    public DbObj getLatestObj() {
        Cursor cursor = query();
        if (cursor != null && cursor.moveToFirst()) {
            try {
                return mMusubi.objForCursor(cursor);
            } finally {
                cursor.close();
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

    public void registerStateObserver(FeedObserver observer) {
        synchronized (DbFeed.this) {
            mObservers.add(observer);
        }
        if (!mObservingProvider) {
            if (DBG) Log.d(TAG, "Enabling feed observer on " + mUri);
            mObservingProvider = true;
            mMusubi.getContext().getContentResolver().registerContentObserver(mUri, false,
                    mContentObserver);
        }
    }

    public boolean removeStateObserver(FeedObserver observer) {
        return mObservers.remove(observer);
    }

    public void postObj(Obj obj) {
        ContentValues values = new ContentValues();
        values.put("type", obj.getType());
        values.put("json", obj.getJson().toString());
        mMusubi.getContentProviderThread().insert(mUri, values);
    }

    public DbUser getLocalUser() {
        return mMusubi.userForLocalDevice(mUri);
    }

    /**
     * List of remote participants available to this feed.
     */
    public Set<DbUser> getRemoteUsers() {
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
        HashSet<DbUser> users = new HashSet<DbUser>();
        if (!cursor.moveToFirst()) {
            return users; // TODO: doesn't include local user.
        }

        int nameIndex = cursor.getColumnIndex(DbUser.COL_NAME);
        int globalIdIndex = cursor.getColumnIndex(DbUser.COL_PERSON_ID);
        int localIdIndex = cursor.getColumnIndex(DbUser.COL_ID);
        while (!cursor.isAfterLast()) {
            String name = cursor.getString(nameIndex);
            String globalId = cursor.getString(globalIdIndex);
            long localId = cursor.getLong(localIdIndex);
            users.add(new DbUser(mMusubi.getContext(), false, name,
                    localId, globalId, mUri));
            cursor.moveToNext();
        }
        return users;
    }

    private void doContentChanged() {
        if (DBG) Log.d(TAG, "noticed change to feed " + mUri);
        Obj obj = null;
        try {
            String selection = null;
            String[] selectionArgs = null;
            String order = "_id desc LIMIT 1"; // TODO: fix.
            Cursor c = mMusubi.getContext().getContentResolver().query(mUri, null, selection,
                    selectionArgs, order);
            if (c.moveToFirst()) {
                String entry = c.getString(c.getColumnIndexOrThrow("json"));
                obj = mMusubi.objForCursor(c);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying for app state", e);
            return;
        }

        synchronized (DbFeed.this) {
            for (FeedObserver observer : mObservers) {
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

    public static Uri uriForName(String feedName) {
        return Uri.parse("content://" + Musubi.AUTHORITY + "/feeds/" + feedName);
    }
}
