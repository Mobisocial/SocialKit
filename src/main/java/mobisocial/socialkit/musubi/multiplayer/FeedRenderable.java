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

import mobisocial.socialkit.Obj;
import mobisocial.socialkit.musubi.MemObj;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a feed entry in a way that can be rendered by the
 * Musubi application.
 */
public class FeedRenderable {
    public static final String OBJ_HTML = "html";
    public static final String OBJ_TEXT = "txt";
    public static final String OBJ_B64_JPEG = "b64jpgthumb";

    private String mHtml;
    private String mText;
    private String mB64Image;

    public JSONObject withJson(JSONObject json) {
        if (json == null) json = new JSONObject();
        try {
            if (mHtml != null) {
                json.put(OBJ_HTML, mHtml);
            }
            if (mText != null) {
                json.put(OBJ_TEXT, mText);
            }
            if (mB64Image != null) {
                json.put(OBJ_B64_JPEG, mB64Image);
            }
        } catch (JSONException e) {
        }
        return json;
    }

    public static FeedRenderable fromHtml(String html) {
        FeedRenderable r = new FeedRenderable();
        r.mHtml = html;
        return r;
    }

    public static FeedRenderable fromText(String text) {
        FeedRenderable r = new FeedRenderable();
        r.mText = text;
        return r;
    }

    public static FeedRenderable fromB64Image(String b64JImage) {
        FeedRenderable r = new FeedRenderable();
        r.mB64Image = b64JImage;
        return r;
    }

    @Deprecated
    public Obj getObj() {
        return new MemObj(TurnBasedMultiplayer.TYPE_APP_STATE, withJson(new JSONObject()));
    }
}
