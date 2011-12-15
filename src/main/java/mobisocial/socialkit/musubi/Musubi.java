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

import java.security.interfaces.RSAPublicKey;

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
    static boolean DBG = true;

    public static final String AUTHORITY = "org.mobisocial.db";
    private static final String SUPER_APP_ID = "edu.stanford.mobisocial.dungbeetle";
    public static final String EXTRA_FEED_URI = "mobisocial.db.FEED";
    public static final String EXTRA_OBJ_HASH = "mobisocial.db.OBJ_HASH";
    
    private final Context mContext;
    private final ContentProviderThread mContentProviderThread;
    private DbFeed mFeed;
    private DbObj mObj;

    public static boolean isMusubiInstalled(Context context) {
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage("edu.stanford.mobisocial.dungbeetle");
        return context.getPackageManager().queryIntentActivities(intent, 0).size() > 0;
    }

    public static boolean isMusubiIntent(Intent intent) {
        return intent.hasExtra(EXTRA_FEED_URI);
    }

    private Musubi(Context context) {
        mContext = context;
        if (context instanceof Activity) {
            setDataFromIntent(((Activity) context).getIntent());
        }
        mContentProviderThread = new ContentProviderThread();
        mContentProviderThread.start();
    }

    public static Musubi getInstance(Context context) {
        return new Musubi(context);
    }

    public static Musubi getInstance(Activity activity, Intent intent) {
        Musubi m = new Musubi(activity);
        m.setDataFromIntent(intent);
        return m;
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
            JSONObject json = null;
            long senderId = -1;
            long hash = -1;
            String name = null;
            long seqNum = -1;
            Integer intKey = null;

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
                json = new JSONObject(
                        cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_JSON)));
            } catch (IllegalArgumentException e) {
            }
            try {
                senderId = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_CONTACT_ID));
            } catch (IllegalArgumentException e) {
            }
            try {
                hash = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_HASH));
            } catch (IllegalArgumentException e) {
            }
            try {
                name = cursor.getString(cursor.getColumnIndexOrThrow(DbObj.COL_FEED_NAME));
            } catch (IllegalArgumentException e) {
            }
            try {
                seqNum = cursor.getLong(cursor.getColumnIndexOrThrow(DbObj.COL_SEQUENCE_ID));
            } catch (IllegalArgumentException e) {
            }
            try {
                intKey = cursor.getInt(cursor.getColumnIndexOrThrow(DbObj.COL_KEY_INT));
            } catch (IllegalArgumentException e) {
            }

            final Uri feedUri = DbFeed.uriForName(name);

            // Don't require raw field.
            final byte[] raw;
            int rawIndex = cursor.getColumnIndex(DbObj.COL_RAW);
            if (rawIndex == -1) {
                raw = null;
            } else {
                raw = cursor.getBlob(cursor.getColumnIndexOrThrow(DbObj.COL_RAW));
            }
            return new DbObj(this, appId, type, json, localId, hash, raw, senderId, seqNum,
                    feedUri, intKey);
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't parse obj.", e);
            return null;
        }
    }

    public DbObj objForId(long localId) {
        Cursor cursor = mContext.getContentResolver().query(
                DbObj.OBJ_URI,
                new String[] {
                        DbObj.COL_APP_ID, DbObj.COL_TYPE, DbObj.COL_JSON, DbObj.COL_RAW,
                        DbObj.COL_CONTACT_ID, DbObj.COL_SEQUENCE_ID, DbObj.COL_HASH,
                        DbObj.COL_FEED_NAME, DbObj.COL_KEY_INT
                }, DbObj.COL_ID + " = ?", new String[] {
                    String.valueOf(localId)
                }, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
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
            final Integer intKey = cursor.getInt(q++);

            return new DbObj(this, appId, type, json, localId, hash, raw, senderId, seqNum,
                    feedUri, intKey);
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
        String objRef = objUri.getLastPathSegment();
        int sep = objRef.lastIndexOf(':');
        if (sep == -1) {
            return null;
        }
        try {
            Long hash = Long.parseLong(objRef.substring(sep + 1));
            return objForHash(hash); // TODO: use feed information for safer access
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public DbObj objForHash(long hash) {
        Cursor cursor = mContext.getContentResolver().query(
                DbObj.OBJ_URI,
                new String[] {
                        DbObj.COL_APP_ID, DbObj.COL_TYPE, DbObj.COL_JSON, DbObj.COL_RAW,
                        DbObj.COL_CONTACT_ID, DbObj.COL_SEQUENCE_ID, DbObj.COL_ID,
                        DbObj.COL_FEED_NAME, DbObj.COL_KEY_INT
                }, DbObj.COL_HASH + " = ?", new String[] {
                    String.valueOf(hash)
                }, null);
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
            final Integer intKey = cursor.getInt(q++);

            return new DbObj(this, appId, type, json, localId, hash, raw, senderId, seqNum,
                    feedUri, intKey);
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
        // The local user is currently stored specially,
        // and only one local user is allowed.
        DbUser localUser = userForLocalDevice(feedUri);
        if (localUser.getId().equals(personId)) {
            return localUser;
        }
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/members/"
                + feedUri.getLastPathSegment());
        String[] projection = {
                DbUser.COL_ID, DbUser.COL_NAME
        };
        String selection = DbUser.COL_PERSON_ID + " = ?";
        String[] selectionArgs = new String[] {
            personId
        };
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(uri, projection, selection, selectionArgs,
                sortOrder);
        try {
            if (c == null || !c.moveToFirst()) {
                Log.w(Musubi.TAG, "No user found for " + personId, new Throwable());
                Log.w(Musubi.TAG, "Local user is " + localUser.getId());
                return null;
            }
            String name = c.getString(c.getColumnIndexOrThrow(DbUser.COL_NAME));
            long localId = c.getLong(c.getColumnIndexOrThrow(DbUser.COL_ID));
            return DbUser.forFeedDetails(mContext, name, localId, personId, feedUri);
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
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/members/"
                + feedUri.getLastPathSegment());
        String[] projection = {
                DbUser.COL_PERSON_ID, DbUser.COL_NAME
        };
        String selection = DbUser.COL_ID + " = ?";
        String[] selectionArgs = new String[] {
            Long.toString(localId)
        };
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(uri, projection, selection, selectionArgs,
                sortOrder);
        if (c == null) {
            Log.w(Musubi.TAG, "Null cursor for user query " + localId);
            return null;
        }
        try {
            if (!c.moveToFirst()) {
                Log.w(Musubi.TAG, "No user found for " + localId, new Throwable());
                return null;
            }

            String name = c.getString(c.getColumnIndexOrThrow(DbUser.COL_NAME));
            String globalId = c.getString(c.getColumnIndexOrThrow(DbUser.COL_PERSON_ID));
            return DbUser.forFeedDetails(mContext, name, localId, globalId, feedUri);
        } finally {
            c.close();
        }
    }

    public DbUser userForLocalDevice(Uri feedUri) {
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/local_user/"
                + feedUri.getLastPathSegment());
        String[] projection = {
                DbUser.COL_ID, DbUser.COL_NAME, DbUser.COL_PUBLIC_KEY
        };
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor c = mContext.getContentResolver().query(uri, projection, selection, selectionArgs,
                sortOrder);
        try {
            if (c == null || !c.moveToFirst()) {
                Log.w(TAG, "no local user for feed " + feedUri, new Throwable());
                return null;
            }
            long localId = c.getLong(c.getColumnIndexOrThrow(DbUser.COL_ID));
            String name = c.getString(c.getColumnIndexOrThrow(DbUser.COL_NAME));
            String keyStr = c.getString(c.getColumnIndexOrThrow(DbUser.COL_PUBLIC_KEY));
            RSAPublicKey key = RSACrypto.publicKeyFromString(keyStr);
            String personId = RSACrypto.makePersonIdForPublicKey(key);
            return DbUser.forFeedDetails(mContext, name, localId, personId, feedUri);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public DbFeed getAppFeed() {
        Uri feedUri = new Uri.Builder().scheme("content").authority(AUTHORITY)
                .appendEncodedPath("feeds/app^" + mContext.getPackageName()).build();
        return new DbFeed(this, feedUri);
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
}
