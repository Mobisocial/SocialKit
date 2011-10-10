package mobisocial.socialkit.musubi.multiplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.musubi.Feed;
import mobisocial.socialkit.musubi.FeedRenderable;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.Musubi.StateObserver;
import mobisocial.socialkit.musubi.User;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Manages the state machine associated with a turn-based multiplayer application.
 */
public class TurnBasedMultiplayer extends Multiplayer {
    public static final String OBJ_MEMBER_CURSOR = "member_cursor";

    private JSONObject mLatestState;
    final String[] mMembers;
    final Uri mFeedUri;
    final String mLocalMember;
    final int mLocalMemberIndex;
    private StateObserver mAppStateObserver;
    private final Feed mFeed;
    private int mGlobalMemberCursor;

    public TurnBasedMultiplayer(Context context, Intent intent) {
        mFeedUri = intent.getParcelableExtra(Musubi.EXTRA_FEED_URI);
        mFeed = Musubi.getInstance(context, intent).getFeed(mFeedUri);
        mFeed.registerStateObserver(mInternalStateObserver);
        JSONObject state = getLatestState();
        mLocalMember = User.getLocalUser(context, mFeedUri).getId();

        if (state == null) {
            // TODO: Temporary.
            if (intent.hasExtra("obj")) {
                try {
                    state = new JSONObject(intent.getStringExtra("obj"));
                } catch (JSONException e) {
                }
            }
        }
        
        if (state == null) {
            Log.e(TAG, "App state is null.");
            mMembers = null;
            mLocalMemberIndex = -1;
            return;
        }
        if (!state.has(OBJ_MEMBERSHIP)) {
            Log.e(TAG, "App state has no members.");
            mMembers = null;
            mLocalMemberIndex = -1;
            return;
        }
        JSONArray memberArr = state.optJSONArray(OBJ_MEMBERSHIP);          
        mMembers = new String[memberArr.length()];
        int localMemberIndex = -1;
        for (int i = 0; i < memberArr.length(); i++) {
            mMembers[i] = memberArr.optString(i);
            if (mMembers[i].equals(mLocalMember)) {
                localMemberIndex = i;
            }
        }
        mLocalMemberIndex = localMemberIndex;
        mGlobalMemberCursor = (state.has(OBJ_MEMBER_CURSOR)) ? state.optInt(OBJ_MEMBER_CURSOR) : 0;
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
        return mLocalMemberIndex == mGlobalMemberCursor;
    }

    /**
     * Updates the state machine with the user's move, passing control
     * to nextPlayer. The state machine is only updated if it is the
     * local user's turn.
     * @return true if a turn was taken.
     */
    public boolean takeTurn(int nextPlayer, JSONObject state, FeedRenderable thumbnail) {
        if (!isMyTurn()) {
            return false;
        }
        try {
            mGlobalMemberCursor = nextPlayer; 
            state.put(OBJ_MEMBER_CURSOR, mGlobalMemberCursor);
            state.put(OBJ_MEMBERSHIP, membersJsonArray());
            mLatestState = state;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update cursor.", e);
        }
        mFeed.postStateWithRenderable(state, thumbnail);
        if (DBG) Log.d(TAG, "Sent cursor " + state.optInt(OBJ_MEMBER_CURSOR));
        return true;
    }

    /**
     * Updates the state machine with the user's move, passing control to
     * the next player in {@link #getMembers()}. The state machine
     * is only updated if it is the local user's turn.
     * @return true if a turn was taken.
     */
    public boolean takeTurn(JSONObject state, FeedRenderable thumbnail) {
        int next = (mGlobalMemberCursor + 1) % mMembers.length;
        return takeTurn(next, state, thumbnail);
    }

    private JSONArray membersJsonArray() {
        JSONArray r = new JSONArray();
        for (String m : mMembers) {
            r.put(m);
        }
        return r;
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
                mLatestState = newState.optJSONObject("state");
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