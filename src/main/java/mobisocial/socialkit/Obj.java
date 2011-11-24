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

package mobisocial.socialkit;

import org.json.JSONObject;

/**
 * <p>
 * An Obj is the basic unit of data in the Musubi network. An Obj is composed
 * of various types of data:
 * <ul>
 *   <li>A "type" for this Obj.
 *   <li>A snippet of json for this Obj
 *   <li>A short (&lt;nKB) binary array
 *   <li>An associated unique uri backed by a binary stream.
 * </ul>
 *
 * <p>
 * The fields of the attached {@link JSONObject} should be namespaced by the
 * application defining that attribute's specification. Musubi reserves some
 * common names, as enumerated by this class's defined constants.
 */
public interface Obj {
    /**
     * A text object, as defined by mobisocial.org.
     */
    public static final String TYPE_TEXT = "status";

    /**
     * (Required) The "type" of this message, as defined by the signing application.
     */
    public String getType();

    /**
     * (Optional) JSON data associated with this Obj.
     * @return
     */
    public JSONObject getJson();

    /**
     * (Optional) binary data associated with this Obj.
     * @return
     */
    public byte[] getRaw();

    /**
     * (Optional) Integer for this Obj.
     * @return
     */
    public Integer getInt();
}
