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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.Musubi.DbThing;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * A Musubi feed of objects.
 * 
 */
public final class DbFeed {
    static final String TAG = Musubi.TAG;
    private static final boolean DBG = true;
    private static final String MIME_TYPE = "vnd.android.cursor.item/vnd.mobisocial.feed";

    private final Musubi mMusubi;
    private final Uri mFeedUri;
    private final Long mFeedId;
    private final Long mParentObjectId;
    private final ContentObserver mContentObserver;
    private final Set<FeedObserver> mObservers = new HashSet<FeedObserver>();
    private boolean mObservingProvider = false;

    private String[] mProjection = null;
    private String mSelection = null;
    private String[] mSelectionArgs = null;
    private String mSortOrder = DbObj.COL_ID + " desc";

    DbFeed(Musubi musubi, Uri feedUri) {
        mMusubi = musubi;
        mFeedUri = feedUri;

        String mime = mMusubi.getContext().getContentResolver().getType(feedUri);
        if (!MIME_TYPE.equals(mime)) {
            throw new IllegalArgumentException("Uri " + feedUri + " type must be a feed, not " + mime);
        }

        try {
            mFeedId = Long.parseLong(mFeedUri.getLastPathSegment());
            String pid = mFeedUri.getQueryParameter(DbObj.COL_PARENT_ID);
            Long lpid = null;
            if (pid != null) {
                try {
                    lpid = Long.parseLong(pid);
                    
                } catch (NumberFormatException e) {
                }
            }
            mParentObjectId = lpid;
        } catch (Exception e) {
            throw new IllegalArgumentException("Feed id not found.");
        }

        mContentObserver = new ContentObserver(new Handler(
                mMusubi.getContext().getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                doContentChanged();
            }
        };
    }

    /**
     * Returns the uri representing this feed.
     * @see {@link #getObjectsUri()}
     */
    public final Uri getUri() {
        return mFeedUri;
    }

    /**
     * Returns a uri for querying over this feed's objects.
     */
    public final Uri getObjectsUri() {
        Uri.Builder uri = Musubi.uriForDir(DbThing.OBJECT).buildUpon()
                .appendQueryParameter(DbObj.COL_FEED_ID, ""+mFeedId);
        if (mParentObjectId != null) {
            uri.appendQueryParameter(DbObj.COL_PARENT_ID, "" + mParentObjectId);
        }
        return uri.build();
    }

    /**
     * @hide
     */
    public DbObj getLatestObj() {
        Log.d(TAG, "latest of " + mFeedId + "/" + mParentObjectId);
        String sortOrder = DbObj.COL_ID + " desc"; // TODO: consistent ordering
        Cursor cursor = query(mProjection, mSelection, mSelectionArgs, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                return mMusubi.objForCursor(cursor);
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * @hide
     */
    public DbObj getLatestObj(String type) {
        Log.d(TAG, "querying latest on " + this);
        String sortOrder = DbObj.COL_ID + " desc";
        String selection = "type = ?";
        String[] args = new String[] { type };
        Cursor cursor = query(mProjection, selection, args, sortOrder);
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

    public void setQueryArgs(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        mProjection = projection;
        mSelection = selection;
        mSelectionArgs = selectionArgs;
        mSortOrder = sortOrder;
    }

    /**
     * Issues a query over this feed's objects.
     */
    public Cursor query(String[] projection, String selection, String[] selectionArgs,
            String order) {
        Uri uri = getObjectsUri();
        Log.d(TAG, "querying objects of " + uri);
        return mMusubi.getContext().getContentResolver().query(uri, projection, selection,
                selectionArgs, order);
    }

    /**
     * Issues a query over this feed's objects.
     */
    public Cursor query(String selection, String[] selectionArgs) {
        return query(null, selection, selectionArgs, "_id desc");
    }

    /**
     * Issues a query over this feed's objects.
     */
    public Cursor query() {
        return query(mProjection, mSelection, mSelectionArgs, mSortOrder);
    }

    /**
     * @hide
     */
    public void registerStateObserver(FeedObserver observer) {
        synchronized (DbFeed.this) {
            mObservers.add(observer);
        }
        if (!mObservingProvider) {
            if (DBG) Log.d(TAG, "Enabling feed observer on " + mFeedUri);
            mObservingProvider = true;
            mMusubi.getContext().getContentResolver().registerContentObserver(mFeedUri, false,
                    mContentObserver);
        }
    }

    /**
     * @hide
     */
    public boolean unregisterStateObserver(FeedObserver observer) {
        boolean removed = mObservers.remove(observer);
        if (mObservers.size() == 0) {
            mObservingProvider = false;
            mMusubi.getContext().getContentResolver().unregisterContentObserver(mContentObserver);
        }
        return removed;
    }

    /**
     * Inserts an object into this feed using a background thread.
     */
    public void postObj(Obj obj) {
        ContentValues values = DbObj.toContentValues(mFeedUri, mParentObjectId, obj);
        Uri objectsUri = Musubi.uriForDir(DbThing.OBJECT);
        mMusubi.getContentProviderThread().insert(objectsUri, values);
    }

    public Uri postObjSync(Obj obj) {
    	ContentValues values = DbObj.toContentValues(mFeedUri, mParentObjectId, obj);
        Uri objectsUri = Musubi.uriForDir(DbThing.OBJECT);
        return mMusubi.getContentProviderThread().insertSync(objectsUri, values);
    }

    /**
     * Inserts an object into this feed on the current thread.
     */
    public Uri insert(Obj obj) {
        ContentValues values = DbObj.toContentValues(mFeedUri, mParentObjectId, obj);
        Uri objectsUri = Musubi.uriForDir(DbThing.OBJECT);
        return mMusubi.getContext().getContentResolver().insert(objectsUri, values);
    }

    public DbIdentity getLocalUser() {
        return mMusubi.userForLocalDevice(mFeedUri);
    }

    public DbIdentity userForGlobalId(String personId) {
        return mMusubi.userForGlobalId(mFeedUri, personId);
    }

    /**
     * List of remote participants available to this feed.
     */
    public List<DbIdentity> getMembers() {
        Uri feedMembersUri = Musubi.uriForItem(DbThing.MEMBER, mFeedId);
        Cursor cursor = null;
        try {
            String selection = DbObj.COL_FEED_ID + " = ?";
            String[] selectionArgs = new String[] { Long.toString(mFeedId) };
            String order = null;
            cursor = mMusubi.getContext().getContentResolver().query(feedMembersUri,
                    DbIdentity.COLUMNS, selection, selectionArgs, order);

            List<DbIdentity> users = new ArrayList<DbIdentity>(cursor.getCount());
            while (cursor.moveToNext()) {
                users.add(DbIdentity.fromStandardCursor(mMusubi.getContext(), cursor));
            }
            return users;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void doContentChanged() {
        if (DBG) Log.d(TAG, "noticed change to feed " + mFeedUri);
        DbObj obj = null;
        try {
            String selection = DbObj.COL_FEED_ID + "=?";
            String[] selectionArgs = new String[] { Long.toString(ContentUris.parseId(mFeedUri)) };
            Uri uri = Musubi.uriForDir(DbThing.OBJECT);
            String order = "_id desc LIMIT 1"; // TODO: fix.
            Cursor c = mMusubi.getContext().getContentResolver().query(uri, null, selection,
                    selectionArgs, order);
            if (c.moveToFirst()) {
                obj = mMusubi.objForCursor(c);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying for app state", e);
            return;
        }
        if (obj == null) {
            return;
        }
        synchronized (DbFeed.this) {
            for (FeedObserver observer : mObservers) {
                observer.onUpdate(obj);
            }
        }
    }

    public long getLocalId() {
        return mFeedId;
    }

    public static Uri uriForId(long feedId) {
        return Uri.parse("content://" + Musubi.AUTHORITY + "/feeds/" + feedId);
    }

    @Override
    public String toString() {
        return "[feed id:" + mFeedId + ", parent:" + mParentObjectId + "]";
    }
}
