package mobisocial.socialkit.musubi.multiplayer;

import java.util.ArrayList;
import java.util.List;

import mobisocial.socialkit.User;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONObject;

import android.database.Cursor;

/**
 * Maintains a high score list for an application. High scores are shared
 * with all friends of the local user who are known to have the application
 * installed.
 */
public class HighScoreList {
    public static final String TYPE_HIGHSCORE = "highscore";
    private final Musubi mMusubi;
    private final DbFeed mFeed;

    /**
     * Returns a high score list for this application.
     */
    public HighScoreList(Musubi musubiContext) {
        mMusubi = musubiContext;
        mFeed = musubiContext.getAppFeed();
    }

    /**
     * Posts a new high score from the local user.
     */
    public void addScore(int score) {
        addScore(score, null);
    }

    /**
     * Posts a new high score from the local user with some
     * associated metadata.
     */
    public void addScore(int score, JSONObject meta) {
        MemObj obj = new MemObj(TYPE_HIGHSCORE, meta, null, score);
        mFeed.postObj(obj);
    }

    /**
     * Returns the list of high scores, sorted by descending score.
     */
    public List<HighScore> getHighScores() {
        String[] projection = null;
        String selection = DbObj.COL_TYPE + " = ?";
        String[] selectionArgs = new String[] { TYPE_HIGHSCORE };
        String sortOrder = DbObj.COL_KEY_INT + " desc";
        Cursor c = mFeed.query(projection, selection, selectionArgs, sortOrder);
        ArrayList<HighScore> scores = new ArrayList<HighScore>(c.getCount());
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                DbObj obj = mMusubi.objForCursor(c);
                scores.add(new HighScore(obj));
                c.moveToNext();
            }
        }
        return scores;
    }

    /**
     * A high score posted by a user.
     *
     */
    public class HighScore {
        private final long mUserId;
        private final int mScore;
        private final JSONObject mMeta;
        private final long mTimestamp;

        private HighScore(DbObj obj) {
            mUserId = obj.getSender().getLocalId();
            mScore = obj.getInt();
            mMeta = obj.getJson();
            mTimestamp = obj.getTimestamp();
        }
        public User getUser() {
            return mMusubi.userForLocalId(mFeed.getUri(), mUserId);
        }

        public int getScore() {
            return mScore;
        }

        public JSONObject getMeta() {
            return mMeta;
        }

        public long getTimestamp() {
            return mTimestamp;
        }
    }
}