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
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

/**
 * A User object with details backed by a database cursor.
 */
public abstract class DbUser implements User {
    static final String COL_ID = "_id";
    static final String COL_NAME = "name";
    static final String COL_PUBLIC_KEY = "public_key";
    static final String COL_PRIVATE_KEY = "private_key";
    static final String COL_PERSON_ID = "person_id";
    static final String COL_PICTURE = "picture";
    static final long LOCAL_USER_ID = -666;

    public abstract long getLocalId();
    public abstract Bitmap getPicture();

    public static DbUser forFeedDetails(Context context, String name, long localId, String personId,
            Uri feedUri) {
        return new InFeedDbUser(context, name, localId, personId, feedUri);
    }

    static class InFeedDbUser extends DbUser {
        private static Context sLatestContext;
        private final long mLocalId;
        private final String mId;
        private final String mName;
        private final Uri mFeedUri;
        private final boolean mIsLocalUser;
        private Bitmap mPicture;

        InFeedDbUser(Context context, String name, long localId, String personId,
                Uri feedUri) {
            sLatestContext = context.getApplicationContext();
            mIsLocalUser = (localId == LOCAL_USER_ID);
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

            Uri uri;
            String selection = null;
            String[] selectionArgs = null;
            if (!mIsLocalUser) {
                uri = Uri.parse("content://" + Musubi.AUTHORITY + "/members/" +
                        mFeedUri.getLastPathSegment());
                selection = COL_ID + " = ?";
                selectionArgs = new String[] { Long.toString(mLocalId) };
            } else {
                uri = Uri.parse("content://" + Musubi.AUTHORITY + "/local_user/" +
                        mFeedUri.getLastPathSegment());
            }
            String[] projection = { COL_PICTURE };
            String sortOrder = null;
            Cursor c = sLatestContext.getContentResolver().query(
                    uri, projection, selection, selectionArgs, sortOrder);
            try {
                if (c == null || !c.moveToFirst()) {
                    Log.w(Musubi.TAG, "No picture found for " + mId);
                    return null;
                }
                byte[] pic = c.getBlob(c.getColumnIndexOrThrow(COL_PICTURE));
                if(pic == null) {
                    Log.w(Musubi.TAG, "No picture found for " + mId);
                    return null;
                }
                mPicture = BitmapFactory.decodeByteArray(pic, 0, pic.length);
                return mPicture;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        @Override
        public String getAttribute(String attr) {
            if (ATTR_RSA_PUBLIC_KEY.equals(attr)) {
                Uri uri;
                String selection = null;
                String[] selectionArgs = null;
                if (mIsLocalUser) {
                    uri = Uri.parse("content://" + Musubi.AUTHORITY + "/local_user/" +
                            mFeedUri.getLastPathSegment());
                } else {
                    uri = Uri.parse("content://" + Musubi.AUTHORITY + "/members/" +
                            mFeedUri.getLastPathSegment());
                    selection = COL_ID + " = ?";
                    selectionArgs = new String[] { Long.toString(mLocalId) };
                }
                String[] projection = { COL_PUBLIC_KEY };
                String sortOrder = null;
                Cursor c = sLatestContext.getContentResolver().query(
                        uri, projection, selection, selectionArgs, sortOrder);
                try {
                    if (!c.moveToFirst()) {
                        return null;
                    }
                    return c.getString(c.getColumnIndexOrThrow(COL_PUBLIC_KEY));
                } finally {
                    if (c != null) {
                        c.close();
                    }
                }
            }
            if (ATTR_RSA_PRIVATE_KEY.equals(attr)) {
                if (mIsLocalUser) {
                    Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/local_user/" +
                            mFeedUri.getLastPathSegment());
                    String[] projection = { COL_PRIVATE_KEY };
                    String selection = null;
                    String[] selectionArgs = null;
                    String sortOrder = null;
                    Cursor c = sLatestContext.getContentResolver().query(
                            uri, projection, selection, selectionArgs, sortOrder);
                    try {
                        if (c == null || !c.moveToFirst()) {
                            return null;
                        }
                        return c.getString(c.getColumnIndexOrThrow(COL_PRIVATE_KEY));
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }
                } else {
                    Log.d("DbUser", "Unknown private key for " + mLocalId);
                }
            }

            // TODO: hit the contact_attributes table;
            // access control must be managed by musubi.
            return null;
        };
    }
}
