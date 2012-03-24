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

import mobisocial.socialkit.User;
import mobisocial.socialkit.musubi.Musubi.DbThing;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

/**
 * A User object with details backed by a database cursor.
 *
 */
public class DbIdentity implements User {
    // XXX
    // XXX this abstraction is rubbish. Get rid of User interface and just have a concrete DbIdentity.
    // XXX
    /**
     * Columns in the view of from the Content Provider for Objects.
     */
    static final String COL_IDENTITY_ID = "identity_id";
    static final String COL_NAME = "name";
    static final String COL_THUMBNAIL = "thumbnail";
    static final String COL_ID_HASH = "principal_hash";
    static final String COL_ID_SHORT_HASH = "principal_short_hash";
    static final String COL_OWNED = "owned";
    static final String COL_CLAIMED = "claimed";
    static final String COL_BLOCKED = "blocked";
    static final String COL_WHITELISTED = "whitelisted";

    static String[] COLUMNS = new String[] {
        COL_IDENTITY_ID,
        COL_NAME,
        COL_THUMBNAIL,
        COL_ID_HASH,
        COL_ID_SHORT_HASH,
        COL_OWNED,
        COL_CLAIMED,
        COL_BLOCKED,
        COL_WHITELISTED
    };

    static final int identityId = 0;
    static final int name = 1;
    static final int thumbnail = 2;
    static final int id_hash = 3;
    static final int id_short_hash = 4;
    static final int owned = 5;
    static final int claimed = 6;
    static final int blocked = 7;
    static final int whitelisted = 8;

    /**
     * Returns a DbIdentity for the cursor backed by a projection with columns {@link #COLUMNS}.
     */
    static DbIdentity fromStandardCursor(Context context, Cursor c) {
        sLatestContext = context;
        long theId = c.getLong(identityId);
        byte[] idHash = c.getBlob(id_hash);
        String theIdString = MusubiUtil.convertToHex(idHash);
        String theName = c.getString(name);
        boolean theOwned = c.getInt(owned) == 1;
        boolean theClaimed = c.getInt(claimed) == 1;
        boolean theBlocked = c.getInt(blocked) == 1;
        boolean theWhitelisted = c.getInt(whitelisted) == 1;
        // theUgly
        return new DbIdentity(context, theName, theId, theIdString, theOwned, theClaimed,
                theWhitelisted, theBlocked);
        
    }

    private static Context sLatestContext;
    private final long mLocalId;
    private final String mId;
    private final String mName;
    private final boolean mOwned;
    private final boolean mClaimed;
    private final boolean mWhitelisted;
    private final boolean mBlocked;
    private Bitmap mPicture;

    DbIdentity(Context context, String name, long localId, String personId, boolean owned,
            boolean claimed, boolean whitelisted, boolean blocked) {
        sLatestContext = context.getApplicationContext();
        mName = name;
        mId = personId;
        sLatestContext = context;
        mLocalId = localId;
        mOwned = owned;
        mClaimed = claimed;
        mWhitelisted = whitelisted;
        mBlocked = blocked;
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String toString() {
        return "[id: " + mId + ", name: " + mName + ", local: " + mLocalId + "]";
    }

    /**
     * Returns the local database id for this user.
     */
    public long getLocalId() {
        return mLocalId;
    }

    @Override
    public String getName() {
        return mName;
    }

    public boolean isOwned() {
        return mOwned;
    }

    public boolean isClaimed() {
        return mClaimed;
    }

    public boolean isBlocked() {
        return mBlocked;
    }

    public boolean isWhitelisted() {
        return mWhitelisted;
    }

    public Bitmap getPicture() {
        if (mPicture != null) {
            return mPicture;
        }

        Uri uri = Musubi.uriForItem(DbThing.IDENTITY, mLocalId);
        String[] projection = new String[] { DbIdentity.COL_THUMBNAIL };
        Cursor c = sLatestContext.getContentResolver().query(uri, projection, null, null, null);
        try {
            if (c.moveToFirst()) {
                byte[] thumbnail = c.getBlob(0);
                mPicture = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
            }
        } finally {
            c.close();
        }
        return mPicture;
    }

    @Override
    public String getAttribute(String attr) {
        Log.e(getClass().getSimpleName(), "user::getAttribute not supported");
        // TODO: hit the contact_attributes table;
        // access control must be managed by musubi.
        return null;
    };
}
