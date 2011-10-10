
package mobisocial.socialkit.musubi;

import org.json.JSONObject;

public class Obj {
    public static final String TYPE_URI = "uri";
    public static final String TYPE_APP_STATE = "appstate";

    public final JSONObject json;
    public final String type;

    public Obj(String type, JSONObject json) {
        this.type = type;
        this.json = json;
    }
}
