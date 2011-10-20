package mobisocial.socialkit.musubi;

import mobisocial.socialkit.musubi.multiplayer.FeedRenderable;
import mobisocial.socialkit.musubi.multiplayer.Multiplayer;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class AppObj extends MemObj {
    public static final String TYPE = "appstate";
    public static final String FIELD_INTENT_ACTION = "intentAction";

    public abstract FeedRenderable getRenderable();
    public abstract JSONObject getData();

    public AppObj() {
        super(TYPE);
    }

    @Override
    public JSONObject getJson() {
        JSONObject data = getData();
        if (data == null) {
            data = new JSONObject();
        }
        try {
            data.put(FIELD_INTENT_ACTION, Multiplayer.ACTION_CONNECTED);
        } catch (JSONException e) {}
        FeedRenderable renderable = getRenderable();
        return renderable.withJson(data) ;
    }
}
