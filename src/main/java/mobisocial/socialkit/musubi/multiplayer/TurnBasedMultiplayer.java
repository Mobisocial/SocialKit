package mobisocial.socialkit.musubi.multiplayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.MemObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.musubi.Musubi.StateObserver;
import mobisocial.socialkit.musubi.DbUser;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Manages the state machine associated with a turn-based multiplayer application.
 */
public class TurnBasedMultiplayer extends Multiplayer {
    public static final String OBJ_MEMBER_CURSOR = "member_cursor";
    public static final String TYPE_APP_STATE = "appstate";

    private JSONObject mLatestState;
    final String[] mMembers;
    final Uri mBaseFeedUri;
    final long mObjHash;
    final String mLocalMember;
    final int mLocalMemberIndex;
    private StateObserver mAppStateObserver;
    private final DbFeed mAppFeed;
    private int mGlobalMemberCursor;
    private final Musubi mMusubi;

    public TurnBasedMultiplayer(Musubi musubi, Intent intent) {
        mMusubi = musubi;
        mBaseFeedUri = intent.getParcelableExtra(Musubi.EXTRA_FEED_URI);
        mObjHash = intent.getLongExtra(Musubi.EXTRA_OBJ_HASH, -1);

        Uri appFeed = DbFeed.uriForName(mBaseFeedUri.getLastPathSegment() + ":" + mObjHash);
        mAppFeed = mMusubi.getFeed(appFeed);
        String selection = Obj.FIELD_TYPE + " = ?";
        String[] selectionArgs = new String[] { TYPE_APP_STATE };
        mAppFeed.setSelection(selection, selectionArgs);
        mAppFeed.registerStateObserver(mInternalStateObserver);
        JSONObject obj = mAppFeed.getLatestObj();
        mLocalMember = mMusubi.userForLocalDevice(mBaseFeedUri).getId();

        if (obj == null) {
            // TODO: Temporary.
            if (intent.hasExtra("obj")) {
                try {
                    obj = new JSONObject(intent.getStringExtra("obj"));
                } catch (JSONException e) {
                }
            }
        }
        
        if (obj == null) {
            Log.e(TAG, "App state is null.");
            mMembers = null;
            mLocalMemberIndex = -1;
            return;
        }
        if (!obj.has(OBJ_MEMBERSHIP)) {
            Log.e(TAG, "App state has no members.");
            mMembers = null;
            mLocalMemberIndex = -1;
            return;
        }
        JSONArray memberArr = obj.optJSONArray(OBJ_MEMBERSHIP);          
        mMembers = new String[memberArr.length()];
        int localMemberIndex = -1;
        for (int i = 0; i < memberArr.length(); i++) {
            mMembers[i] = memberArr.optString(i);
            if (mMembers[i].equals(mLocalMember)) {
                localMemberIndex = i;
            }
        }
        mLocalMemberIndex = localMemberIndex;
        mGlobalMemberCursor = (obj.has(OBJ_MEMBER_CURSOR)) ? obj.optInt(OBJ_MEMBER_CURSOR) : 0;
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
        JSONObject out = new JSONObject();
        try {
            mGlobalMemberCursor = nextPlayer; 
            out.put(OBJ_MEMBER_CURSOR, mGlobalMemberCursor);
            out.put(OBJ_MEMBERSHIP, membersJsonArray());
            out.put("state", state);
            mLatestState = state;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update cursor.", e);
        }

        postAppStateRenderable(out, thumbnail);
        if (DBG) Log.d(TAG, "Sent cursor " + out.optInt(OBJ_MEMBER_CURSOR));
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
            JSONObject obj = mAppFeed.getLatestObj();
            if (obj != null && obj.has("state")) {
                mLatestState = obj.optJSONObject("state");
            }
        }
        Log.d(TAG, "returning latest state " + mLatestState);
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
            if (newState == null || !newState.has("state")) return;
            try {
                Log.d(TAG, "CHECKING OVER " + newState);
                mLatestState = newState.optJSONObject("state");
                mGlobalMemberCursor = newState.getInt(OBJ_MEMBER_CURSOR);
                if (DBG) Log.d(TAG, "Updated cursor to " + mGlobalMemberCursor);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get member_cursor from " + newState);
            }

            if (mAppStateObserver != null) {
                mAppStateObserver.onUpdate(mLatestState);
            }
        }
    };

    /**
     * Returns the array of member identifiers.
     */
    public String[] getMembers() {
        return mMembers;
    }

    public DbUser getUser(int memberIndex) {
        if (memberIndex == mLocalMemberIndex) {
            return mMusubi.userForLocalDevice(mBaseFeedUri);
        }
        return mMusubi.userForGlobalId(mBaseFeedUri, mMembers[memberIndex]);
    }

    private void postAppStateRenderable(JSONObject state, FeedRenderable thumbnail) {
        try {
            JSONObject b = new JSONObject(state.toString());
            thumbnail.toJson(b);
            mAppFeed.postObj(new MemObj(TYPE_APP_STATE, b));
        } catch (JSONException e) {}
    }
}