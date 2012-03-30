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

package mobisocial.socialkit.musubi.multiplayer;

import java.util.List;

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.User;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.FeedObserver;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Manages the state machine associated with a turn-based multiplayer application.
 */
public abstract class TurnBasedApp extends Multiplayer {
    final boolean DBG = false;
    static final String OBJ_STATE = "state";
    static final String OBJ_MEMBER_CURSOR = "member_cursor";

    /**
     * AppState objects provide state updates to an application instance.
     */
    static final String TYPE_APP_STATE = "appstate";

    /**
     * Turn interrupts are attempts to update the app's state when a user doesn't
     * own the lock on that app's state.
     */
    static final String TYPE_INTERRUPT_REQUEST = "interrupt";

    private final DbObj mObjContext;
    private final DbFeed mDbFeed;
    private final String mLocalMember;

    private JSONObject mLatestState;
    private String[] mMembers;
    private int mLocalMemberIndex;
    private int mGlobalMemberCursor;
    private Integer mLastTurn;

    /**
     * Prepares a new TurnBasedApp object that can be inserted into a feed.
     * Once inserted, the object can be used to create a new TurnBasedApp
     * instance via {@link TurnBasedApp#TurnBasedApp(DbObj)}.
     */
    public static Obj newInstance(String type, List<DbIdentity> participants, JSONObject initialState) {
        JSONObject json = new JSONObject();
        JSONArray members = new JSONArray();
        for (DbIdentity id : participants) {
            members.put(id.getId());
        }
        try {
            json.put(OBJ_STATE, initialState);
            json.put(OBJ_MEMBERSHIP, members);
            json.put(OBJ_MEMBER_CURSOR, 0);
            json.put(Obj.FIELD_RENDER_TYPE, Obj.RENDER_LATEST);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
        return new MemObj(type, json);
    }

    public TurnBasedApp(DbObj objContext) {
        if (objContext == null) {
            throw new NullPointerException("ObjContext is null");
        }
        mObjContext = objContext;
        mDbFeed = mObjContext.getSubfeed();
        String[] projection = null;
        String selection = "type = ? OR type = ?";
        String[] selectionArgs = new String[] { TYPE_APP_STATE, TYPE_INTERRUPT_REQUEST };
        String sortOrder = DbObj.COL_INT_KEY + " desc";
        mDbFeed.setQueryArgs(projection, selection, selectionArgs, sortOrder);
        Obj obj = mDbFeed.getLatestObj(TYPE_APP_STATE);
        //Log.d(TAG, "The latest obj has " + obj.getIntKey());
        mLocalMember = mDbFeed.getLocalUser().getId();

        if (obj == null) {
            obj = mObjContext;
        }

        // At least one turn has been taken.
        JSONObject json = obj.getJson();
        if (json == null || !json.has(OBJ_MEMBERSHIP)) {
            Log.e(TAG, "App state has no membership.");
            mMembers = null;
            mLocalMemberIndex = -1;
            return;
        }

        JSONArray memberArr = json.optJSONArray(OBJ_MEMBERSHIP);          
        setMembershipFromJson(memberArr);
        mLatestState = json.optJSONObject(OBJ_STATE);
        mLastTurn = (obj.getIntKey() == null) ? 0 : obj.getIntKey();
        if (DBG) Log.d(TAG, "Read latest " + mLastTurn + ", " + mLatestState);
        mGlobalMemberCursor = (json.has(OBJ_MEMBER_CURSOR)) ? json.optInt(OBJ_MEMBER_CURSOR) : 0;
    }

    /**
     * Often called in an activity's onResume method.
     */
    public void enableStateUpdates() {
        mDbFeed.registerStateObserver(mInternalStateObserver);
    }

    /**
     * Often called in an activity's onPause method.
     */
    public void disableStateUpdates() {
        mDbFeed.unregisterStateObserver(mInternalStateObserver);
    }

    @Override
    public User getLocalUser() {
        return mDbFeed.getLocalUser();
    }

    /**
     * Returns a view suitable for display in a feed.
     */
    protected abstract FeedRenderable getFeedView(JSONObject state);

    private void setMembershipFromJson(JSONArray memberArr) {
        mMembers = new String[memberArr.length()];
        for (int i = 0; i < memberArr.length(); i++) {
            mMembers[i] = memberArr.optString(i);
            if (mMembers[i].equals(mLocalMember)) {
                mLocalMemberIndex = i;
            }
        }
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
        if (mGlobalMemberCursor < 0 || mGlobalMemberCursor >= mMembers.length) {
            throw new IllegalStateException("Invalid global member cursor");
        }
        DbIdentity potential = mDbFeed.userForGlobalId(mMembers[mGlobalMemberCursor]);
        if (potential == null) {
            throw new IllegalStateException("app member not a feed member " +
                    "#" + mGlobalMemberCursor + "=" + mMembers[mGlobalMemberCursor]);
        }
        return potential.isOwned();
    }

    /**
     * Attempts to take a turn despite it not being my turn.
     * Send a UpdateOutOfOrderObj with n+1 as int field.
     * 
     * If it's my turn, I listen for interrupt requests and,
     * if I see one that is agreeable, I allow it by rebroadcasting
     * as a state update.
     *
     * @hide
     */
    public void takeTurnOutOfOrder(JSONArray members, int nextPlayer, JSONObject state) {
        JSONObject out = new JSONObject();
        try {
            out.put(OBJ_MEMBER_CURSOR, nextPlayer);
            out.put(OBJ_MEMBERSHIP, members);
            out.put(OBJ_STATE, state);

            if (DBG) Log.d(TAG, "Attempted interrupt #" + mLastTurn);
            mDbFeed.postObj(new MemObj(TYPE_INTERRUPT_REQUEST, out, null, mLastTurn));
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update cursor.", e);
        }
    }

    public boolean takeTurn(JSONArray members, int nextPlayer, JSONObject state) {
        if (!isMyTurn()) {
            return false;
        }
        JSONObject out = new JSONObject();
        try {
            mGlobalMemberCursor = nextPlayer; 
            out.put(OBJ_MEMBER_CURSOR, mGlobalMemberCursor);
            out.put(OBJ_MEMBERSHIP, members);
            out.put(OBJ_STATE, state);
            mLatestState = state;

            postAppStateRenderable(out, getFeedView(state));
            if (DBG) Log.d(TAG, "Sent cursor " + out.optInt(OBJ_MEMBER_CURSOR));
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to update cursor.", e);
            return false;
        }
    }

    /**
     * Updates the state machine with the user's move, passing control
     * to nextPlayer. The state machine is only updated if it is the
     * local user's turn.
     * @return true if a turn was taken.
     */
    public boolean takeTurn(int nextPlayer, JSONObject state) {
        return takeTurn(membersJsonArray(), nextPlayer, state);
    }

    /**
     * Updates the state machine with the user's move, passing control to
     * the next player in {@link #getMembers()}. The state machine
     * is only updated if it is the local user's turn.
     * @return true if a turn was taken.
     */
    public boolean takeTurn(JSONObject state) {
        int next = (mGlobalMemberCursor + 1) % mMembers.length;
        return takeTurn(next, state);
    }

    /**
     * Takes the last turn in this turn-based game.
     *
     * @hide
     */
    public void takeFinalTurn(GameResult result, FeedRenderable display) {
        postAppStateRenderable(result.getJson(), display);
    }

    public JSONArray membersJsonArray() {
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
            Obj obj = mDbFeed.getLatestObj(TYPE_APP_STATE);
            if (obj != null && obj.getJson() != null && obj.getJson().has(OBJ_STATE)) {
                mLatestState = obj.getJson().optJSONObject(OBJ_STATE);
            }
        }
        return mLatestState;
    }

    /**
     * Handles newly received state updates.
     */
    protected abstract void onStateUpdate(JSONObject json);

    /**
     * Handles an interrupt request. It may be useful to override this method
     * to customize for your needs, be mindful of concurrency issues.
     *
     * @hide
     */
    protected void handleInterrupt(int turnRequested, DbObj obj) {
        if (DBG) Log.d(TAG, "Incoming interrupt!");
        if (!isMyTurn()) {
            if (DBG) Log.d(TAG, "not my turn.");
            return;
        }

        if (turnRequested != mLastTurn) {
            if (DBG) Log.d(TAG, "stale state.");
            return;
        }
        if (DBG) Log.d(TAG, "interrupting with " + obj);
        JSONObject out = obj.getJson();
        FeedRenderable thumb = getFeedView(out.optJSONObject(OBJ_STATE));
        postAppStateRenderable(out, thumb);
    }

    private final FeedObserver mInternalStateObserver = new FeedObserver() {
        @Override
        public void onUpdate(DbObj obj) {
            if (DBG) Log.d(TAG, "Update with " + obj.getType());
            Integer turnTaken = obj.getIntKey();
            if (turnTaken == null) {
                Log.w(TAG, "no turn taken.");
                return;
            }
            if (TYPE_INTERRUPT_REQUEST.equals(obj.getType())) {
                handleInterrupt(turnTaken, obj);
                return;
            }
            if (turnTaken <= mLastTurn) {
                Log.w(TAG, "Turn " + turnTaken + " is at/before known turn " + mLastTurn);
                return;
            }
            mLastTurn = turnTaken;
            JSONObject newState = obj.getJson();
            if (newState == null || !newState.has(OBJ_STATE)) return;
            try {
                if (newState.has(OBJ_MEMBERSHIP)) {
                    setMembershipFromJson(newState.getJSONArray(OBJ_MEMBERSHIP));
                }
                mLatestState = newState.optJSONObject(OBJ_STATE);
                mGlobalMemberCursor = newState.getInt(OBJ_MEMBER_CURSOR);
                if (DBG) Log.d(TAG, "Updated cursor to " + mGlobalMemberCursor);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get member_cursor from " + newState);
            }

            onStateUpdate(mLatestState);
        }
    };

    /**
     * Returns the array of member identifiers.
     */
    public String[] getMembers() {
        return mMembers;
    }

    public DbIdentity getUser(int memberIndex) {
        return mDbFeed.userForGlobalId(mMembers[memberIndex]);
    }

    private void postAppStateRenderable(JSONObject state, FeedRenderable thumbnail) {
        try {
            JSONObject b = new JSONObject(state.toString());
            if (thumbnail != null) {
                thumbnail.addToJson(b);
            }
            mDbFeed.postObj(new MemObj(TYPE_APP_STATE, b, null, mLastTurn + 1));
        } catch (JSONException e) {}
    }

    public interface StateObserver {
        public void onUpdate(JSONObject state);
    }
}