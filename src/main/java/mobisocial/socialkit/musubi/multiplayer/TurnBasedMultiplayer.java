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

    public int getLocalMemberIndex() {
        return mLocalMemberIndex;
    }

    public int getGlobalMemberCursor() {
        return mGlobalMemberCursor;
    }

    public boolean isMyTurn() {
        Log.d(TAG, "Checking for turn: " + mLocalMemberIndex + " vs " + mGlobalMemberCursor);
        return mLocalMemberIndex == mGlobalMemberCursor;
    }

    public void takeTurn(JSONObject state, String thumbHtml) {
        try {
            state.put(OBJ_MEMBER_CURSOR,
                    (mGlobalMemberCursor + 1) % mMembers.length);
            if (DBG) Log.d(TAG, "Sent cursor " + state.optInt(OBJ_MEMBER_CURSOR));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update cursor.", e);
        }
        mFeed.postObjectWithHtml(state, thumbHtml);
    }

    public JSONObject getLatestState() {
        return mFeed.getLatestState();
    }

    public void setStateObserver(StateObserver observer) {
        mAppStateObserver = observer;
    }

    private final StateObserver mInternalStateObserver = new StateObserver() {
        @Override
        public void onUpdate(JSONObject newState) {
            try {
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
}