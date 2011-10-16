package mobisocial.socialkit.musubi.obj;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import mobisocial.socialkit.musubi.Obj;

public class BasicObj implements Obj {
    public static final String TYPE_URI = "uri";

    private final String mType;
    private final JSONObject mJson;

    public BasicObj(String type, JSONObject json) {
        mType = type;
        mJson = json;
    }
    @Override
    public String getType() {
        return mType;
    }

    @Override
    public JSONObject getJson() {
        return mJson;
    }

    public static Obj forUri(Uri uri) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("uri", uri);
        } catch (JSONException e) {}
        return new BasicObj(TYPE_URI, obj);
    }
}