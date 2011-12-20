package mobisocial.socialkit.obj;

import mobisocial.socialkit.musubi.multiplayer.FeedRenderable;

import org.json.JSONObject;

public class AppStateObj extends MemObj {
    public static final String TYPE = "appstate";
    private final FeedRenderable mAppRenderable;
    private final JSONObject mAppData;

    public FeedRenderable getAppRenderable() {
        return mAppRenderable;
    }
    public JSONObject getAppData() {
        return mAppData;
    }

    public AppStateObj(JSONObject data, FeedRenderable renderable) {
        super(TYPE);
        mAppRenderable = renderable;
        mAppData = data;
    }

    @Override
    public JSONObject getJson() {
        FeedRenderable renderable = getAppRenderable();
        if (renderable == null) {
            return getAppData();
        }
        JSONObject data = getAppData();
        if (data == null) {
            data = new JSONObject();
        }
        return renderable.withJson(data) ;
    }
}
