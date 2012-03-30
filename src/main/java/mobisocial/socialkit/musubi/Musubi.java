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

import java.util.LinkedHashMap;

import mobisocial.socialkit.SQLClauseHelper;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Use the DungBeetle APIs in your application.
 */
public class Musubi {
    public static final String TAG = "SocialKit-DB";
    static boolean DBG = true;

    /**
     * The name of the Musubi content provider.
     */
    public static final String AUTHORITY = "org.musubi.db";
    private static final String SUPER_APP_ID = "mobisocial.musubi";
    public static final String EXTRA_FEED_URI = "feedUri";
    public static final String EXTRA_OBJ_URI = "objUri";
    
    private final Context mContext;
    private final ContentProviderThread mContentProviderThread;
    private DbFeed mFeed;
    private DbObj mObj;

    private static final Uri CONTACTS_URI = Uri.parse("content://" + AUTHORITY + "/identities");
    private static final LinkedHashMap<Long, DbIdentity> sUserCache = new UserCache();
    private final ContentObserver mContactUpdateObserver;

    public static boolean isMusubiInstalled(Context context) {
        try {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(SUPER_APP_ID);
            return context.getPackageManager().queryIntentActivities(intent, 0).size() > 0;
        } catch (Throwable t) {
            return false;
        }
    }

    public DbObj objFromIntent(Intent intent) {
        Log.d(TAG, "fetching obj from " + intent);
        if (intent.hasExtra(EXTRA_OBJ_URI)) {
            try {
                Uri objUri = intent.getParcelableExtra(EXTRA_OBJ_URI);
                return objForUri(objUri);
            } catch (Exception e) {
                if (DBG) Log.e(TAG, "couldnt get obj from uri", e);
            }
        }
        if (intent.getType() != null && intent.getType().startsWith("vnd.musubi.obj/")) {
            if (intent.getData() != null) {
                return objForUri(intent.getData());
            }
        }
        if (DBG) Log.e(TAG, "no obj found");
        return null;
    }

    public static boolean isMusubiIntent(Intent intent) {
        return intent.hasExtra(EXTRA_FEED_URI) || intent.hasExtra(EXTRA_OBJ_URI);
    }

    public Musubi(Context context) {
        mContext = context.getApplicationContext();
        if (context instanceof Activity) {
            setDataFromIntent(((Activity) context).getIntent());
        }
        mContentProviderThread = new ContentProviderThread();
        mContentProviderThread.start();
        mContactUpdateObserver = new ContentObserver(mContentProviderThread.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                sUserCache.clear();
            }
        };
        mContext.getContentResolver().registerContentObserver(CONTACTS_URI, true,
                mContactUpdateObserver);
    }

    ContentProviderThread getContentProviderThread() {
        return mContentProviderThread;
    }

    Context getContext() {
        return mContext;
    }

    public static Intent getMarketIntent() {
        Uri marketUri = Uri.parse("market://details?id=" + SUPER_APP_ID);
        return new Intent(Intent.ACTION_VIEW, marketUri);
    }

    public DbFeed getFeed(Uri feedUri) {
        return new DbFeed(this, feedUri);
    }

    public void setDataFromIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_FEED_URI)) {
            mFeed = new DbFeed(this, (Uri) intent.getParcelableExtra(EXTRA_FEED_URI));
        }
        if (mObj == null) {
            if (intent.hasExtra(EXTRA_OBJ_URI)) {
                mObj = objForUri((Uri)intent.getParcelableExtra(EXTRA_OBJ_URI));
            } else {
                mObj = objForUri(intent.getData());
            }
        }
    }

    public static Musubi forIntent(Context context, Intent intent) {
        Musubi m = new Musubi(context);
        m.setDataFromIntent(intent);
        return m;
    }

    public void setFeed(DbFeed feed) {
        mFeed = feed;
    }

    public DbFeed getFeed() {
        return mFeed;
    }

    public DbObj getObj() {
        return mObj;
    }

    public DbObj objForCursor(Cursor cursor) {
        try {
            long localId = -1;
            String appId = null;
            String type = null;
            String name = null;
            JSONObject json = null;
            long senderId = -1;
            byte[] hash = null;
            long feedId = -1;
            Integer intKey = null;
            long timestamp = -1;

            try {
                localId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_ID));
            } catch (IllegalArgumentException e) {
            }
            try {
                appId = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_APP_ID));
            } catch (IllegalArgumentException e) {
            }
            try {
                type = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_TYPE));
            } catch (IllegalArgumentException e) {
            }
            try {
                name = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_STRING_KEY));
            } catch (IllegalArgumentException e) {
            }
            try {
                json = new JSONObject(
                        cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_JSON)));
            } catch (IllegalArgumentException e) {
            }
            try {
                senderId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_IDENTITY_ID));
            } catch (IllegalArgumentException e) {
            }
            try {
                hash = cursor.getBlob(cursor.getColumnIndexOrThrow(DbObj.COL_UNIVERSAL_HASH));
            } catch (IllegalArgumentException e) {
            }
            try {
                feedId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_FEED_ID));
            } catch (IllegalArgumentException e) {
            }
            try {
                intKey = cursor.getInt(cursor.getColumnIndexOrThrow(DbObj.COL_INT_KEY));
            } catch (IllegalArgumentException e) {
            }
            try {
                timestamp = cursor.getInt(cursor.getColumnIndexOrThrow(DbObj.COL_TIMESTAMP));
            } catch (IllegalArgumentException e) {
            }

            // Don't require raw field.
            final byte[] raw;
            int rawIndex = cursor.getColumnIndex(DbObj.COL_RAW);
            if (rawIndex == -1) {
                raw = null;
            } else {
                raw = cursor.getBlob(cursor.getColumnIndexOrThrow(DbObj.COL_RAW));
            }
            return new DbObj(this, appId, type, name, json, localId, hash, raw, senderId,
                    feedId, intKey, timestamp);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        }
    }

    public DbObj objForId(long localId) {
        Cursor cursor = mContext.getContentResolver().query(
                DbObj.OBJ_URI,
                new String[] {
                        DbObj.COL_APP_ID, DbObj.COL_TYPE, DbObj.COL_STRING_KEY, DbObj.COL_JSON,
                        DbObj.COL_RAW, DbObj.COL_IDENTITY_ID, DbObj.COL_UNIVERSAL_HASH,
                        DbObj.COL_FEED_ID, DbObj.COL_INT_KEY, DbObj.COL_TIMESTAMP
                }, DbObj.COL_ID + " = ?", new String[] { String.valueOf(localId) }, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "Obj " + localId + " not found.");
                return null;
            }

            int q = 0;
            final String appId = cursor.getString(q++);
            final String type = cursor.getString(q++);
            final String name = cursor.getString(q++);
            final JSONObject json = new JSONObject(cursor.getString(q++));
            final byte[] raw = cursor.getBlob(q++);
            final long senderId = cursor.getLong(q++);
            final byte[] hash = cursor.getBlob(q++);
            final long feedId = cursor.getLong(q++);
            final Integer intKey = cursor.getInt(q++);
            final long timestamp = cursor.getLong(q++);

            return new DbObj(this, appId, type, name, json, localId, hash, raw,
                    senderId, feedId, intKey, timestamp);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public DbObj objForUri(Uri objUri) {
        try {
            return objForId(ContentUris.parseId(objUri));
        } catch (Exception e) {
            if (DBG) Log.e(TAG, "Bad uri " + objUri);
            return null;
        }
    }

    public DbIdentity userForGlobalId(Uri feedUri, String personId) {
        byte[] idHash = MusubiUtil.convertToByteArray(personId);
        long shortHash = MusubiUtil.shortHash(idHash);

        Uri uri = new Uri.Builder().scheme("content").authority(Musubi.AUTHORITY)
                .appendPath(DbThing.MEMBER.toString()).appendPath(feedUri.getLastPathSegment())
                .build();
        String selection = DbIdentity.COL_ID_SHORT_HASH + " = ?";
        String[] selectionArgs = new String[] { Long.toString(shortHash) };
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(uri, DbIdentity.COLUMNS, selection,
                selectionArgs, sortOrder);
        try {
            while (c != null && c.moveToNext()) {
                DbIdentity mate = DbIdentity.fromStandardCursor(mContext, c);
                if (mate.getId().equals(personId)) {
                    return mate;
                }
            }
            Log.e(TAG, "id not found #" + shortHash);
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public DbIdentity userForLocalId(Uri feedUri, long localId) {
        DbIdentity cachedUser = sUserCache.get(localId);
        if (cachedUser != null) {
            return cachedUser;
        }

        String feedName;
        if (feedUri != null) {
            feedName = feedUri.getLastPathSegment();
        } else {
            feedName = "friend";
        }
        Uri uri = uriForItem(DbThing.IDENTITY, localId);
        String[] projection = { DbIdentity.COL_ID_HASH, DbIdentity.COL_NAME };
        String selection = DbIdentity.COL_IDENTITY_ID + " = ?";
        String[] selectionArgs = new String[] { Long.toString(localId) };
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(uri, projection, selection, selectionArgs,
                sortOrder);
        if (c == null) {
            Log.w(Musubi.TAG, "Null cursor for user query " + localId);
            return null;
        }
        try {
            if (!c.moveToFirst()) {
                Log.w(Musubi.TAG, "No user found for " + localId + " in " + feedName,
                        new Throwable());
                return null;
            }

            DbIdentity user = DbIdentity.fromStandardCursor(mContext, c);
            sUserCache.put(localId, user);
            return user;
        } finally {
            c.close();
        }
    }

    /**
     * Returns the DbUser that is currently logged in to this app.
     */
    public DbIdentity userForLocalDevice(Uri feedUri) {
        if (feedUri == null) {
            feedUri = Musubi.uriForItem(DbThing.FEED, 0);
        }
        Long feedId = Long.parseLong(feedUri.getLastPathSegment());
        Uri uri = uriForItem(DbThing.MEMBER, feedId);
        String selection = DbIdentity.COL_OWNED + " = 1";
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(uri, DbIdentity.COLUMNS, selection,
                selectionArgs, sortOrder);
        try {
            if (c == null || !c.moveToFirst()) {
                Log.w(TAG, "no local user for feed " + feedUri, new Throwable());
                return null;
            }
            DbIdentity id = DbIdentity.fromStandardCursor(mContext, c);
            return id;
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public Cursor queryAppData(String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        String pkg = mContext.getPackageName();
        selection = SQLClauseHelper.andClauses(selection, DbObj.COL_APP_ID + "=?");
        selectionArgs = SQLClauseHelper.andArguments(selectionArgs, pkg);
        Uri uri = uriForDir(DbThing.OBJECT);
        return mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    class ContentProviderThread extends Thread {
        public Handler mHandler;

        public void run() {
            Looper.prepare();

            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    Insertion i = (Insertion) msg.obj;
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

    @SuppressWarnings("serial")
	private static class UserCache extends LinkedHashMap<Long, DbIdentity> {
        private static final int MAX_ENTRIES = 10;
        public UserCache() {
            super(10, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, DbIdentity> eldest) {
            return size() > MAX_ENTRIES;
        }
    }

    /**
     * Connects to a real-time multi-way, globally ordered data stream
     * associated with the given object.
     */
    /*
    public Junction junctionForObj(JunctionActor actor, DbObj obj)
            throws JunctionException {
        String uid = obj.getUri().getLastPathSegment();
        uid = uid.replace("^", "_").replace(":", "_");
        Uri uri = new Uri.Builder().scheme("junction")
                .authority("sb.openjunction.org")
                .appendPath("dbf-" + uid).build();
        return AndroidJunctionMaker.bind(uri, actor);
    }*/

    public static Uri uriForItem(DbThing type, long id) {
        return new Uri.Builder()
            .scheme("content").authority(AUTHORITY).appendPath(type.toString()).appendPath(""+id)
            .build();
    }

    public static Uri uriForDir(DbThing type) {
        return new Uri.Builder()
            .scheme("content").authority(AUTHORITY).appendPath(type.toString()).build();
    }

    public static String mimeTypeFor(String musubiType) {
        return "vnd.musubi.obj/" + musubiType;
    }

    public enum DbThing { 
        OBJECT, FEED, IDENTITY, MEMBER;

        @Override
        public String toString() {
            switch (this) {
                case OBJECT:
                    return "objects";
                case FEED:
                    return "feeds";
                case IDENTITY:
                    return "identities";
                case MEMBER:
                    return "feed_members";
                default:
                    return null;
            }
        }
    }
}
