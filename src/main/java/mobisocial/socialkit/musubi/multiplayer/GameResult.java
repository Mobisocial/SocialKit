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

import java.util.Map;

import mobisocial.socialkit.obj.MemObj;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>An Obj representing the result of some game.
 * <p>Comprised of well-known parameters for aggregation.
 */
public class GameResult extends MemObj {
    public static final String TYPE = "gameResult";

    public static final String FIELD_WINNER = "winner";
    public static final String FIELD_SCOREBOARD = "scoreboard";

    private GameResult() {
        super(TYPE, new JSONObject());
    }

    public class Builder {
        private final GameResult mGameResult;
        private final TurnBasedMultiplayer mGame;

        private Builder(TurnBasedMultiplayer game) {
            mGameResult  = new GameResult();
            mGame = game;
        }

        public Builder buildFor(TurnBasedMultiplayer game) {
            return new Builder(game);
        }

        public GameResult build() {
            return mGameResult;
        }

        public Builder setWinner(int winner) {
            try {
                mGameResult.getJson().put(FIELD_WINNER, mGame.getMembers()[winner]);
            } catch (JSONException e) {
            }
            return this;
        }

        public Builder setScoreboard(Map<String, Integer> scoreboard) {
            try {
                JSONObject scores = new JSONObject();
                for (String s : scoreboard.keySet()) {
                    scores.put(s, scoreboard.get(s));
                }
                mGameResult.getJson().put(FIELD_SCOREBOARD, scores);
            } catch (JSONException e) {
            }
            return this;
        }
    }
}
