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
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * Manages the state machine associated with a turn-based multiplayer application.
 */
public abstract class TurnBasedApp extends Multiplayer {
    protected boolean DBG = false;
    protected static final int NO_TURN = -1;

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

    private final Musubi mMusubi;
    private final ContentObserver mObserver;
    private final DbObj mObjContext;
    private final DbFeed mDbFeed;
    private final String mLocalMember;
    private boolean mObservingUpdates;

    private JSONObject mLatestState;
    private String[] mMembers;
    private int mLocalMemberIndex;
    private int mGlobalMemberCursor;
    private int mLastTurn = NO_TURN;

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
        return new MemObj(type, json, null, 0);
    }

    public TurnBasedApp(Musubi musubi, DbObj objContext) {
        if (musubi == null || objContext == null) {
            throw new NullPointerException("ObjContext is null");
        }

        mMusubi = musubi;
        mObjContext = objContext;
        mDbFeed = mObjContext.getSubfeed();
        mObserver = new TurnObserver(new Handler(mMusubi.getContext().getMainLooper()));
        mLocalMember = mDbFeed.getLocalUser().getId();
    }

    DbObj fetchLatestState() {
        String[] projection = null;
        String selection = "type = ?";
        String[] args = new String[] { TYPE_APP_STATE };
        String sortOrder = DbObj.COL_INT_KEY + " desc";
        Cursor cursor = mDbFeed.query(projection, selection, args, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                return mMusubi.objForCursor(cursor);
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    DbObj fetchLatestInterrupt() {
        String[] projection = null;
        String selection = "type = ?";
        String[] args = new String[] { TYPE_INTERRUPT_REQUEST };
        String sortOrder = DbObj.COL_INT_KEY + " desc";
        Cursor cursor = mDbFeed.query(projection, selection, args, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                return mMusubi.objForCursor(cursor);
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Often called in an activity's onResume method.
     */
    public void enableStateUpdates() {
        Uri uri = mDbFeed.getUri();
        mMusubi.getContext().getContentResolver().registerContentObserver(uri, false, mObserver);
        mObserver.onChange(false); // keep getLatestState() synchronized
        mObservingUpdates = true;
    }

    /**
     * Often called in an activity's onPause method.
     */
    public void disableStateUpdates() {
        mMusubi.getContext().getContentResolver().unregisterContentObserver(mObserver);
        mObservingUpdates = false;
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
        if (!mObservingUpdates) {
            mObserver.onChange(true);
        }
        return mLatestState;
    }

    public int getLastTurnNumber() {
        if (!mObservingUpdates) {
            mObserver.onChange(true);
        }
        return mLastTurn;
    }

    /**
     * Override this method to handle state updates to this turn-basd app.
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

        if (turnRequested != getLastTurnNumber()) {
            if (DBG) Log.d(TAG, "stale state.");
            return;
        }
        if (DBG) Log.d(TAG, "interrupting with " + obj);
        JSONObject out = obj.getJson();
        FeedRenderable thumb = getFeedView(out.optJSONObject(OBJ_STATE));
        postAppStateRenderable(out, thumb);
    }

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

    /**
     * Monitors this turn-based app's subfeed for state updates and populates related
     * state member variables.
     */
    class TurnObserver extends ContentObserver {
        public TurnObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateState(selfChange);
            if (!selfChange) attemptInterrupt();
        }

        void updateState(boolean explicitRequest) {
            DbObj obj = fetchLatestState();
            if (DBG) Log.e(TAG, "fetched " + obj);
            if (obj == null) {
                obj = mObjContext;
            }

            JSONObject json = obj.getJson();
            if (json == null || !json.has(OBJ_MEMBERSHIP)) {
                if (DBG) Log.e(TAG, "App state has no membership.");
                mMembers = null;
                mLocalMemberIndex = -1;
                mGlobalMemberCursor = 0;
                return;
            }
            try {
                setMembershipFromJson(json.getJSONArray(OBJ_MEMBERSHIP));
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing membership", e);
                return;
            }

            Integer turnTaken = obj.getIntKey();
            if (turnTaken == null) {
                if (DBG) Log.e(TAG, "no turn taken.");
                return;
            }

            if (mLastTurn != NO_TURN && turnTaken <= mLastTurn) {
                if (DBG) Log.d(TAG, "Turn " + turnTaken + " is at/before known turn " + mLastTurn);
                return;
            }
            mLastTurn = turnTaken;
            JSONObject newState = obj.getJson();
            if (newState == null || !newState.has(OBJ_STATE)) {
                if (DBG) Log.w(TAG, "No state for update " + obj);
                return;
            }
            try {
                mLatestState = newState.optJSONObject(OBJ_STATE);
                mGlobalMemberCursor = newState.getInt(OBJ_MEMBER_CURSOR);
                if (DBG) Log.i(TAG, "Updated cursor to " + mGlobalMemberCursor);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get member_cursor from " + newState);
            }

            if (!explicitRequest) {
                onStateUpdate(mLatestState);
            }
        }

        void attemptInterrupt() {
            DbObj interrupt = fetchLatestInterrupt();
            if (interrupt == null) {
                return;
            }
            Integer turnRequested = interrupt.getIntKey();
            if (turnRequested == null || turnRequested < mLastTurn) {
                return;
            }
            handleInterrupt(turnRequested, interrupt);
        }
    }
}