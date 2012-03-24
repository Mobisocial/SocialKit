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
 */
public abstract class DbIdentity implements User {
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

    public abstract long getLocalId();
    public abstract Bitmap getPicture();

    public static DbIdentity forFeedDetails(Context context, String name, long localId, String personId,
            Uri feedUri) {
        return new InFeedDbIdentity(context, name, localId, personId, feedUri);
    }

    static class InFeedDbIdentity extends DbIdentity {
        private static Context sLatestContext;
        private final long mLocalId;
        private final String mId;
        private final String mName;
        private final Uri mFeedUri;
        private Bitmap mPicture;

        InFeedDbIdentity(Context context, String name, long localId, String personId,
                Uri feedUri) {
            sLatestContext = context.getApplicationContext();
            mName = name;
            mId = personId;
            mFeedUri = feedUri;
            sLatestContext = context;
            mLocalId = localId;
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

        @Override
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
}
