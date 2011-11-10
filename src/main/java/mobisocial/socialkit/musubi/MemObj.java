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

package mobisocial.socialkit.musubi;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import mobisocial.socialkit.Obj;

/**
 * An Obj implementation held in memory.
 * @author bjdodson
 *
 */
public class MemObj implements Obj {
    public static final String TYPE_URI = "uri";

    private final String mType;
    private final JSONObject mJson;
    private final byte[] mRaw;
    private final Integer mIntKey;

    public MemObj(String type) {
        mType = type;
        mJson = null;
        mRaw = null;
        mIntKey = null;
    }

    public MemObj(String type, JSONObject json) {
        mType = type;
        mJson = json;
        mRaw = null;
        mIntKey = null;
    }

    public MemObj(String type, JSONObject json, byte[] raw) {
        mType = type;
        mJson = json;
        mRaw = raw;
        mIntKey = null;
    }

    public MemObj(String type, JSONObject json, byte[] raw, Integer intKey) {
        mType = type;
        mJson = json;
        mRaw = raw;
        mIntKey = intKey;
    }

    @Override
    public String getType() {
        return mType;
    }

    @Override
    public JSONObject getJson() {
        return mJson;
    }

    /**
     * Returns an Obj representing a Uri.
     */
    public static Obj forUri(Uri uri) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("uri", uri);
        } catch (JSONException e) {}
        return new MemObj(TYPE_URI, obj);
    }

    @Override
    public byte[] getRaw() {
        return mRaw;
    }

    @Override
    public Integer getInt() {
        return mIntKey;
    }
}