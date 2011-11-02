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

import java.security.PublicKey;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
    public static final String AUTHORITY = "org.mobisocial.db";

    public static final String EXTRA_FEED_URI = "mobisocial.db.FEED";
    public static final String EXTRA_OBJ_HASH = "mobisocial.db.OBJ_HASH";

    static boolean DBG = true;
    private final Context mContext;
    private final ContentProviderThread mContentProviderThread;
    private DbFeed mFeed;
    private DbObj mObj;

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

    private Musubi(Activity activity) {
        mContext = activity;
        setFeedFromIntent(activity.getIntent());
        mContentProviderThread = new ContentProviderThread();
        mContentProviderThread.start();
    }

    private Musubi(Context context) {
        mContext = context;
        mContentProviderThread = new ContentProviderThread();
        mContentProviderThread.start();
    }

    public static Musubi getInstance(Context context) {
        return new Musubi(context);
    }

    public static Musubi getInstance(Activity activity) {
        return new Musubi(activity);
    }

    public static Musubi getInstance(Activity activity, Intent intent) {
        Musubi m = new Musubi(activity);
        m.setFeedFromIntent(intent);
        return m;
    }

    ContentProviderThread getContentProviderThread() {
        return mContentProviderThread;
    }

    Context getContext() {
        return mContext;
    }

    public DbFeed getFeed(Uri feedUri) {
        return new DbFeed(this, feedUri);
    }

    public void setFeedFromIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_FEED_URI)) {
            mFeed = new DbFeed(this, (Uri) intent.getParcelableExtra(EXTRA_FEED_URI));
            if (mFeed.getUri().getLastPathSegment().contains(":")) {
                long hash = Long.parseLong(mFeed.getUri().getLastPathSegment().split(":")[1]);
                mObj = objForHash(hash);
            }
        }
        if (mObj == null) {
            if (intent.hasExtra(EXTRA_OBJ_HASH)) {
                long hash = intent.getLongExtra(EXTRA_OBJ_HASH, 0);
                mObj = objForHash(hash);
            }
        }
    }

    public void setFeed(DbFeed feed) {
        mFeed = feed;
    }

    public boolean hasFeed() {
        return mFeed != null;
    }

    public DbFeed getFeed() {
        return mFeed;
    }

    public boolean hasObj() {
        return mObj != null;
    }

    public DbObj getObj() {
        return mObj;
    }

    public DbObj objForCursor(Cursor cursor) {
        try {
            final long localId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_ID));
            final String appId = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_APP_ID));
            final String type = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_TYPE));
            final JSONObject json = new JSONObject(
                    cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_JSON)));
            final long senderId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_CONTACT_ID));
            final long hash = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_HASH));
            final String name = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_FEED_NAME));
            final long seqNum = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_SEQUENCE_ID));
            final Uri feedUri = DbFeed.uriForName(name);

            // Don't require raw field.
            final byte[] raw;
            int rawIndex = cursor.getColumnIndex(DbObj.COL_RAW);
            if (rawIndex == -1) {
                raw = null;
            } else {
                raw = cursor.getBlob(cursor.getColumnIndexOrThrow(DbObj.COL_RAW));
            }
            return new DbObj(this, appId, type, json, localId, hash, raw, senderId, seqNum, feedUri);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        }
    }

    public DbObj objForId(long localId) {
        Cursor cursor = mContext.getContentResolver().query(DbObj.OBJ_URI,
                new String[] { DbObj.COL_APP_ID, DbObj.COL_TYPE, DbObj.COL_JSON, DbObj.COL_RAW,
                DbObj.COL_CONTACT_ID, DbObj.COL_SEQUENCE_ID, DbObj.COL_HASH, DbObj.COL_FEED_NAME },
                DbObj.COL_ID + " = ?", new String[] { String.valueOf(localId) }, null);
        try {
            if (!cursor.moveToFirst()) {
                Log.w(TAG, "Obj " + localId + " not found.");
                return null;
            }

            int q = 0;
            final String appId = cursor.getString(q++);
            final String type = cursor.getString(q++);
            final JSONObject json = new JSONObject(cursor.getString(q++));
            final byte[] raw = cursor.getBlob(q++);
            final long senderId = cursor.getLong(q++);
            final long seqNum = cursor.getLong(q++);
            final long hash = cursor.getLong(q++);
            final String name = cursor.getString(q++);
            final Uri feedUri = DbFeed.uriForName(name);

            return new DbObj(this, appId, type, json, localId, hash, raw, senderId, seqNum, feedUri);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public DbObj objForHash(long hash) {
        Cursor cursor = mContext.getContentResolver().query(DbObj.OBJ_URI,
                new String[] { DbObj.COL_APP_ID, DbObj.COL_TYPE, DbObj.COL_JSON, DbObj.COL_RAW,
                DbObj.COL_CONTACT_ID, DbObj.COL_SEQUENCE_ID, DbObj.COL_ID, DbObj.COL_FEED_NAME },
                DbObj.COL_HASH + " = ?", new String[] { String.valueOf(hash) }, null);
        try {
            if (!cursor.moveToFirst()) {
                Log.w(TAG, "Obj " + hash + " not found.");
                return null;
            }

            int q = 0;
            final String appId = cursor.getString(q++);
            final String type = cursor.getString(q++);
            final JSONObject json = new JSONObject(cursor.getString(q++));
            final byte[] raw = cursor.getBlob(q++);
            final long senderId = cursor.getLong(q++);
            final long seqNum = cursor.getLong(q++);
            final long localId = cursor.getLong(q++);
            final String name = cursor.getString(q++);
            final Uri feedUri = DbFeed.uriForName(name);

            return new DbObj(this, appId, type, json, localId, hash, raw, senderId, seqNum, feedUri);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public DbUser userForGlobalId(Uri feedUri, String personId) {
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/members/" +
                feedUri.getLastPathSegment());
        String[] projection = { DbUser.COL_ID, DbUser.COL_NAME };
        String selection = DbUser.COL_PERSON_ID + " = ?";
        String[] selectionArgs = new String[] { personId };
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder);
        try {
            if (!c.moveToFirst()) {
                // The local user is not currently stored in the contacts database.
                DbUser localUser = userForLocalDevice(feedUri);
                if (localUser.getId().equals(personId)) {
                    return localUser;
                }
                Log.w(Musubi.TAG, "No user found for " + personId);
                return null;
            }
            String name = c.getString(c.getColumnIndexOrThrow(DbUser.COL_NAME));
            long localId = c.getLong(c.getColumnIndexOrThrow(DbUser.COL_ID));
            return new DbUser(mContext, false, name, localId, personId, feedUri);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public DbUser userForLocalId(Uri feedUri, long localId) {
        if (localId == DbUser.LOCAL_USER_ID) {
            return userForLocalDevice(feedUri);
        }
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/members/" +
                feedUri.getLastPathSegment());
        String[] projection = { DbUser.COL_PERSON_ID, DbUser.COL_NAME };
        String selection = DbUser.COL_ID + " = ?";
        String[] selectionArgs = new String[] { Long.toString(localId) };
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder);
        try {
            if (!c.moveToFirst()) {
                Log.w(Musubi.TAG, "No user found for " + localId);
                return null;
            }

            String name = c.getString(c.getColumnIndexOrThrow(DbUser.COL_NAME));
            String globalId = c.getString(c.getColumnIndexOrThrow(DbUser.COL_PERSON_ID));
            return new DbUser(mContext, false, name, localId, globalId, feedUri);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public DbUser userForLocalDevice(Uri feedUri) {
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/local_user/" +
                feedUri.getLastPathSegment());
        String[] projection = { DbUser.COL_ID, DbUser.COL_NAME, DbUser.COL_PUBLIC_KEY };
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            long localId = c.getLong(c.getColumnIndexOrThrow(DbUser.COL_ID));
            String name = c.getString(c.getColumnIndexOrThrow(DbUser.COL_NAME));
            String keyStr  = c.getString(c.getColumnIndexOrThrow(DbUser.COL_PUBLIC_KEY));
            PublicKey key = DbUser.publicKeyFromString(keyStr);
            String personId = DbUser.makePersonIdForPublicKey(key);
            return new DbUser(mContext, true, name, localId, personId, feedUri);
        } finally {
            if (c != null) {
                c.close();
            }
        }
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
