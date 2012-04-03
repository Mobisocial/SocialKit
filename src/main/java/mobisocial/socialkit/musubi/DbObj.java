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
import mobisocial.socialkit.musubi.Musubi.DbThing;

import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * A SignedObj implementation that is backed by a database.
 *
 */
public class DbObj implements SignedObj {
    private final Musubi mMusubi;
    private final String mAppId;
    private final Long mParentId;
    private final long mLocalId;
    private final long mSenderId;
    private final Uri mFeedUri;

    private final String mType;
    private final JSONObject mJson;
    private final byte[] mRaw;
    private final Integer mIntKey;
    private final String mName;

    private final long mTimestamp;
    private final byte[] mUniversalHash;

    public static final Uri OBJ_URI = Uri.parse("content://" + Musubi.AUTHORITY + "/objects");
    public static final String TABLE = "objects";
    public static final String COL_ID = "_id";
    public static final String COL_TYPE = "type";
	public static final String COL_FEED_ID = "feed_id";
    public static final String COL_IDENTITY_ID = "identity_id";
    public static final String COL_DEVICE_ID = "device_id";
    public static final String COL_PARENT_ID = "parent_id";
    public static final String COL_JSON = "json";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_APP_ID = "app_id";
    public static final String COL_ENCODED_ID = "encoded_id";
    public static final String COL_UNIVERSAL_HASH = "universal_hash";
    public static final String COL_SHORT_UNIVERSAL_HASH = "short_universal_hash";
    public static final String COL_DELETED = "deleted";
    public static final String COL_RAW = "raw";
    public static final String COL_INT_KEY = "int_key";
    public static final String COL_STRING_KEY = "string_key";
    public static final String COL_LAST_MODIFIED_TIMESTAMP = "last_modified_timestamp";
    public static final String COL_RENDERABLE = "renderable";


    // Lazy loaded.
    private SoftReference<DbIdentity> mSenderReference;
    private SoftReference<DbFeed> mContainingFeed;

    /**
     * @hide
     */
    public DbObj(Musubi musubi, String appId, long feedId, Long parentId, long senderId, long localId, String type, JSONObject json,
            byte[] raw, Integer intKey, String stringKey, long timestamp, byte[] hash) {
        mMusubi = musubi;
        mAppId = appId;
        mType = type;
        mName = stringKey;
        mJson = json;
        mLocalId = localId;
        mParentId = parentId;
        mUniversalHash = hash;
        mRaw = raw;
        mSenderId = senderId;
        mIntKey = intKey;
        mTimestamp = timestamp;
        mFeedUri = DbFeed.uriForId(feedId);
    }

    public long getSenderId() {
        return mSenderId;
    }

    @Override
    public String getType() {
        return mType;
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
    public Integer getIntKey() {
        return mIntKey;
    }

    @Override
    public String getStringKey() {
        return mName;
    }
    
    public String getName() {
    	return mName;
    }

    public Long getParentId() {
        return mParentId;
    }

    /**
     * Returns the main feed that bounds this object.
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
        Uri subfeedUri = mFeedUri.buildUpon()
                .appendQueryParameter(DbObj.COL_PARENT_ID, String.valueOf(mLocalId)).build();
        return mMusubi.getFeed(subfeedUri);
    }

    @Override
    public long getHash() {
    	if(mUniversalHash == null)
    		return 0;
		return MusubiUtil.shortHash(mUniversalHash);
    }

    public String getUniversalHashString() {
    	if(mUniversalHash == null)
    		return null;
		return MusubiUtil.convertToHex(mUniversalHash);
    }

    /**
     * Returns the database's local id for this Obj.
     */
    public long getLocalId() {
        return mLocalId;
    }

    @Override
    public DbIdentity getSender() {
        DbIdentity user = null;
        if (mSenderReference != null) {
            user = mSenderReference.get();
        }
        if (user == null) {
            Uri uri = Musubi.uriForItem(DbThing.IDENTITY, mSenderId);
            String selection = null;
            String[] selectionArgs = null;
            String sortOrder = null;
        	
    		Cursor c = mMusubi.getContext().getContentResolver().query(uri,
    		        DbIdentity.COLUMNS, selection, selectionArgs, sortOrder);
    		try {
    		    if (!c.moveToFirst()) {
    		        return null;
    		    }

    		    user = DbIdentity.fromStandardCursor(mMusubi.getContext(), c);
    		    mSenderReference = new SoftReference<DbIdentity>(user);
    		} finally {
    		    c.close();
    		}
        }
        return user;
    }

    public Uri getUri() {
        // TODO: no more long in uri! use proper hex encoding
        return OBJ_URI.buildUpon().appendPath(Long.toString(mLocalId)).build();
    }

    @Override
    public String getAppId() {
        return mAppId;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Prepares ContentValues that can be delivered to Musubi's Content Provider
     * for insertion into a SocialDb feed.
     */
    public static ContentValues toContentValues(Uri feedUri, Long parentObjId, Obj obj) {
        ContentValues values = new ContentValues();
        values.put(DbObj.COL_TYPE, obj.getType());
        if (obj.getStringKey() != null) {
            values.put(DbObj.COL_STRING_KEY, obj.getStringKey());
        }
        if (obj.getJson() != null) {
            values.put(DbObj.COL_JSON, obj.getJson().toString());
        }
        if (obj.getIntKey() != null) {
            values.put(DbObj.COL_INT_KEY, obj.getIntKey());
        }
        if (obj.getRaw() != null) {
            values.put(DbObj.COL_RAW, obj.getRaw());
        }
        if (parentObjId != null) {
            values.put(DbObj.COL_PARENT_ID, parentObjId);
        }
        try {
            Long feedId = Long.parseLong(feedUri.getLastPathSegment());
            values.put(DbObj.COL_FEED_ID, feedId);
        } catch (Exception e) {
            throw new IllegalArgumentException("No feed id found for " + feedUri, e);
        }
        return values;
    }

    @Override
    public String toString() {
        return getUri().toString() + ", " + getType() + ", " + getJson();
    }
}
