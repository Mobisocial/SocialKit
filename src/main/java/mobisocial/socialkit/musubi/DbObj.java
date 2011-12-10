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

import java.lang.ref.SoftReference;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.SignedObj;

import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * A SignedObj implementation that is backed by a database cursor.
 *
 */
public class DbObj implements SignedObj {
    private static final String TAG = "DbSignedObj";

    private final Musubi mMusubi;
    private final String mAppId;
    private final String mType;
    private final JSONObject mJson;
    private final long mHash;
    private final byte[] mRaw;
    private final long mLocalId;
    private final long mSenderId;
    private final long mSequenceNumber;
    private final Uri mFeedUri;
    private final Integer mIntKey;

    public static final Uri OBJ_URI = Uri.parse("content://" + Musubi.AUTHORITY + "/obj");
    public static final String TABLE = "objects";
    public static final String COL_ID = "_id";
    public static final String COL_TYPE = "type";
    public static final String COL_SEQUENCE_ID = "sequence_id";
    public static final String COL_FEED_NAME = "feed_name";
    public static final String COL_CONTACT_ID = "contact_id";
    public static final String COL_DESTINATION = "destination";
    public static final String COL_JSON = "json";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_APP_ID = "app_id";
    public static final String COL_SENT = "sent";
    public static final String COL_ENCODED = "encoded";
    public static final String COL_CHILD_FEED_NAME = "child_feed";
    public static final String COL_HASH = "hash";
    public static final String COL_DELETED = "deleted";
    public static final String COL_RAW = "raw";
    public static final String COL_KEY_INT = "key_int";
    public static final String COL_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";

    // Lazy loaded.
    private SoftReference<DbUser> mSenderReference;
    private SoftReference<DbFeed> mContainingFeed;

    DbObj(Musubi musubi, String appId, String type, JSONObject json,
            long localId, long hash, byte[] raw, long senderId, long seqNum,
            Uri feedUri, Integer intKey) {
        mMusubi = musubi;
        mAppId = appId;
        mType = type;
        mJson = json;
        mLocalId = localId;
        mHash = hash;
        mRaw = raw;
        mSenderId = senderId;
        mFeedUri = feedUri;
        mSequenceNumber = seqNum;
        mIntKey = intKey;
    }

    @Override
    public JSONObject getJson() {
        return mJson;
    }

    @Override
    public byte[] getRaw() {
        return mRaw;
    }

    @Override
    public String getType() {
        return mType;
    }

    /**
     * Returns the parent feed that bounds this object.
     */
    public DbFeed getContainingFeed() {
        DbFeed f = null;
        if (mContainingFeed != null) {
            f = mContainingFeed.get();
        }
        if (f == null) {
            f = mMusubi.getFeed(mFeedUri);
            mContainingFeed = new SoftReference<DbFeed>(f);
        }
        return f;
    }

    /**
     * Returns the subfeed that has this object as its head.
     */
    public DbFeed getSubfeed() {
        Uri related = Uri.parse(mFeedUri.toString() + ":" + mHash);
        return mMusubi.getFeed(related);
    }

    @Override
    public long getHash() {
        return mHash;
    }

    /**
     * Returns the database's local id for this Obj.
     */
    public long getLocalId() {
        return mLocalId;
    }

    @Override
    public DbUser getSender() {
        DbUser user = null;
        if (mSenderReference != null) {
            user = mSenderReference.get();
        }
        if (user == null) {
            // TODO: Look up User for mSenderId;
            user = mMusubi.userForLocalId(mFeedUri, mSenderId);
            mSenderReference = new SoftReference<DbUser>(user);
        }
        return user;
    }

    @Override
    public long getSequenceNumber() {
        return mSequenceNumber;
    }

    public Uri getUri() {
        // TODO: no more long in uri! use proper hex encoding
        return OBJ_URI.buildUpon().appendPath(getFeedName() + ":" + mHash).build();
    }

    @Override
    public String getAppId() {
        return mAppId;
    }

    @Override
    public String getFeedName() {
        return mFeedUri.getLastPathSegment();
    }

    @Override
    public Integer getInt() {
        return mIntKey;
    }

    public static ContentValues toContentValues(Obj obj) {
        ContentValues values = new ContentValues();
        values.put(DbObj.COL_TYPE, obj.getType());
        if (obj.getJson() != null) {
            values.put(DbObj.COL_JSON, obj.getJson().toString());
        }
        if (obj.getInt() != null) {
            values.put(DbObj.COL_KEY_INT, obj.getInt());
        }
        if (obj.getRaw() != null) {
            values.put(DbObj.COL_RAW, obj.getRaw());
        }
        return values;
    }
}
