
package mobisocial.socialkit.musubi;

import org.json.JSONObject;

public abstract class Obj {
    public static final String TYPE_URI = "uri";
    public static final String TYPE_APP_STATE = "appstate";

    public abstract String getType();
    public abstract JSONObject getJson();

    public static class Generic extends Obj {
        private final String mType;
        private final JSONObject mJson;

        public Generic(String type, JSONObject json) {
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
    }
}
