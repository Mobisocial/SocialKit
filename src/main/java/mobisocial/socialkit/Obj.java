
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
    public static final String FIELD_TYPE = "type";

    /**
     * A text object, as defined by mobisocial.org.
     */
    public static final String TYPE_TEXT = "status";

    public String getType();
    public JSONObject getJson();
    public byte[] getRaw();
}
