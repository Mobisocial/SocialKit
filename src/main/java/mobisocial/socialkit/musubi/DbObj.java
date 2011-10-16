package mobisocial.socialkit.musubi;

import java.lang.ref.SoftReference;

import mobisocial.socialkit.Feed;
import mobisocial.socialkit.SignedObj;
import mobisocial.socialkit.User;

import org.json.JSONObject;

import android.net.Uri;

/**
 * A SignedObj implementation that is backed by a database cursor.
 *
 */
public class DbObj implements SignedObj {
    private static final String TAG = "DbSignedObj";

    private final Musubi mMusubi;
    private final String mType;
    private final JSONObject mJson;
    private final long mHash;
    private final byte[] mRaw;
    private final long mLocalId;
    private final long mSenderId;
    private final long mSequenceNumber;
    private final Uri mFeedUri;

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

    // Lazy loaded.
    private SoftReference<DbUser> mSenderReference;
    private SoftReference<DbFeed> mFeedReference;

    DbObj(Musubi musubi, String type, JSONObject json,
            long localId, long hash, byte[] raw, long senderId, long seqNum, Uri feedUri) {
        mMusubi = musubi;
        mType = type;
        mJson = json;
        mLocalId = localId;
        mHash = hash;
        mRaw = raw;
        mSenderId = senderId;
        mFeedUri = feedUri;
        mSequenceNumber = seqNum;
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

    @Override
    public Feed getContainingFeed() {
        DbFeed f = null;
        if (mFeedReference != null) {
            f = mFeedReference.get();
        }
        if (f == null) {
            f = mMusubi.getFeed(mFeedUri);
            mFeedReference = new SoftReference<DbFeed>(f);
        }
        return f;
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

    public static Uri uriForObj(long objId) {
        return OBJ_URI.buildUpon().appendPath("" + objId).build();
    }
}
