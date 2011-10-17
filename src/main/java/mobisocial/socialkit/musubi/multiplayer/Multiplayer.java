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

public abstract class Multiplayer {
    public static final String ACTION_TWO_PLAYERS = "mobisocial.intent.action.TWO_PLAYERS";
    public static final String ACTION_MULTIPLAYER = "mobisocial.intent.action.MULTIPLAYER";

    public static final String OBJ_MEMBERSHIP = "membership";
    public static final String OBJ_MEMBER_CURSOR = "member_cursor";

    static final boolean DBG = true;
    static final String TAG = "musubi";

    public abstract String[] getMembers();
}
