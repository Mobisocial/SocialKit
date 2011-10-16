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

    public MemObj(String type, JSONObject json) {
        mType = type;
        mJson = json;
        mRaw = null;
    }

    public MemObj(String type, JSONObject json, byte[] raw) {
        mType = type;
        mJson = json;
        mRaw = raw;
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
}