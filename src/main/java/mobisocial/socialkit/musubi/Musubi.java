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
    static final String TAG = "SocialKit-DB";
    public static final String AUTHORITY = "org.mobisocial.db";

    public static final String EXTRA_FEED_URI = "mobisocial.db.FEED";
    public static final String EXTRA_OBJ_HASH = "mobisocial.db.OBJ_HASH";

    static boolean DBG = true;
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

    public DbFeed getFeedFromIntent(Intent intent) {
        if (intent.hasExtra(EXTRA_FEED_URI)) {
            return new DbFeed(this, (Uri) intent.getParcelableExtra(EXTRA_FEED_URI));
        } else {
            return null;
        }
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

    ContentProviderThread getContentProviderThread() {
        return mContentProviderThread;
    }

    Context getContext() {
        return mContext;
    }

    public DbFeed getFeed(Uri feedUri) {
        return new DbFeed(this, feedUri);
    }

    public DbObj objForCursor(Cursor cursor) {
        try {
            final long localId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_ID));
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
            return new DbObj(this, type, json, localId, hash, raw, senderId, seqNum, feedUri);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        }
    }

    public DbObj objForId(long localId) {
        Cursor cursor = mContext.getContentResolver().query(DbObj.OBJ_URI,
                new String[] { DbObj.COL_TYPE, DbObj.COL_JSON, DbObj.COL_RAW,
                DbObj.COL_CONTACT_ID, DbObj.COL_SEQUENCE_ID, DbObj.COL_HASH, DbObj.COL_FEED_NAME },
                DbObj.COL_ID + " = ?", new String[] { String.valueOf(localId) }, null);
        try {
            if (!cursor.moveToFirst()) {
                Log.w(TAG, "Obj " + localId + " not found.");
                return null;
            }

            final String type = cursor.getString(0);
            final JSONObject json = new JSONObject(cursor.getString(1));
            final byte[] raw = cursor.getBlob(2);
            final long senderId = cursor.getLong(3);
            final long seqNum = cursor.getLong(4);
            final long hash = cursor.getLong(5);
            final String name = cursor.getString(6);
            final Uri feedUri = DbFeed.uriForName(name);

            return new DbObj(this, type, json, localId, hash, raw, senderId, seqNum, feedUri);
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
                new String[] { DbObj.COL_TYPE, DbObj.COL_JSON, DbObj.COL_RAW,
                DbObj.COL_CONTACT_ID, DbObj.COL_SEQUENCE_ID, DbObj.COL_ID, DbObj.COL_FEED_NAME },
                DbObj.COL_HASH + " = ?", new String[] { String.valueOf(hash) }, null);
        try {
            if (!cursor.moveToFirst()) {
                Log.w(TAG, "Obj " + hash + " not found.");
                return null;
            }

            final String type = cursor.getString(0);
            final JSONObject json = new JSONObject(cursor.getString(1));
            final byte[] raw = cursor.getBlob(2);
            final long senderId = cursor.getLong(3);
            final long seqNum = cursor.getLong(4);
            final long localId = cursor.getLong(5);
            final String name = cursor.getString(6);
            final Uri feedUri = DbFeed.uriForName(name);

            return new DbObj(this, type, json, localId, hash, raw, senderId, seqNum, feedUri);
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
