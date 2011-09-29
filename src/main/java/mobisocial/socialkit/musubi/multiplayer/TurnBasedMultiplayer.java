package mobisocial.socialkit.musubi.multiplayer;

import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.musubi.Feed;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.Musubi.StateObserver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Manages the state machine associated with a turn-based,
 * multiplayer application.
 *
 */
public class TurnBasedMultiplayer extends Multiplayer {
    public static final String ACTION_TWO_PLAYERS = "mobisocial.intent.action.TWO_PLAYERS";
    public static final String ACTION_MULTIPLAYER = "mobisocial.intent.action.MULTIPLAYER";

    public static final String EXTRA_MEMBERS = "members";
    public static final String EXTRA_LOCAL_MEMBER_INDEX = "local_member_index";
    public static final String EXTRA_GLOBAL_MEMBER_CURSOR = "global_member_cursor";

    public static final String OBJ_MEMBER_CURSOR = "member_cursor";

    private JSONObject mLatestState;
    final Intent mLaunchIntent;
    final String[] mMembers;
    final Uri mFeedUri;
    final int mLocalMemberIndex;
    int mGlobalMemberCursor;
    private StateObserver mAppStateObserver;
    private final Feed mFeed;

    public TurnBasedMultiplayer(Context context, Intent intent) {
        mLaunchIntent = intent;
        // TODO: intent.getStringArrayExtra("membership") ~ fixed, open, etc.
        mMembers = intent.getStringArrayExtra(EXTRA_MEMBERS);
        mFeedUri = intent.getParcelableExtra(Musubi.EXTRA_FEED_URI);
        mLocalMemberIndex = intent.getIntExtra(EXTRA_LOCAL_MEMBER_INDEX, -1);
        mGlobalMemberCursor = intent.getIntExtra(EXTRA_GLOBAL_MEMBER_CURSOR, -1);
        mFeed = Musubi.getInstance(context, intent).getFeed(mFeedUri);
        mFeed.registerStateObserver(mInternalStateObserver);
    }

    /**
     * Returns the index within the membership list that represents the
     * local user.
     */
    public int getLocalMemberIndex() {
        return mLocalMemberIndex;
    }

    /**
     * Returns a cursor within the membership list that points to
     * the user with control of the state machine.
     */
    public int getGlobalMemberCursor() {
        return mGlobalMemberCursor;
    }

    /**
     * Returns true if the local member index equals the membership cursor.
     * In other words, its the local user's turn.
     */
    public boolean isMyTurn() {
        Log.d(TAG, "Checking for turn: " + mLocalMemberIndex + " vs " + mGlobalMemberCursor);
        return mLocalMemberIndex == mGlobalMemberCursor;
    }

    /**
     * Updates the state machine with the user's move. The state machine
     * is only updated if it is the local user's turn.
     * @return true if a turn was taken.
     */
    public boolean takeTurn(JSONObject state, String thumbHtml) {
        if (!isMyTurn()) {
            return false;
        }
        try {
            mGlobalMemberCursor = (mGlobalMemberCursor + 1) % mMembers.length; 
            state.put(OBJ_MEMBER_CURSOR, mGlobalMemberCursor);
            mLatestState = state;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update cursor.", e);
        }
        mFeed.postObjectWithHtml(state, thumbHtml);
        if (DBG) Log.d(TAG, "Sent cursor " + state.optInt(OBJ_MEMBER_CURSOR));
        return true;
    }

    /**
     * Returns the latest application state.
     */
    public JSONObject getLatestState() {
        if (mLatestState == null) {
            mLatestState = mFeed.getLatestState();
        }
        return mLatestState;
    }

    /**
     * Registers a callback to observe changes to the state machine.
     */
    public void setStateObserver(StateObserver observer) {
        mAppStateObserver = observer;
    }

    private final StateObserver mInternalStateObserver = new StateObserver() {
        @Override
        public void onUpdate(JSONObject newState) {
            try {
                mLatestState = newState;
                mGlobalMemberCursor = newState.getInt(OBJ_MEMBER_CURSOR);
                if (DBG) Log.d(TAG, "Updated cursor to " + mGlobalMemberCursor);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get member_cursor.", e);
            }

            if (mAppStateObserver != null) {
                mAppStateObserver.onUpdate(newState);
            }
        }
    };

    /**
     * Returns the array of member identifiers.
     */
    public String[] getMembers() {
        return mMembers;
    }
}