package mobisocial.socialkit.musubi.multiplayer;

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

    /* package */ void toJson(JSONObject json) throws JSONException {
        if (mHtml != null) {
            json.put(OBJ_HTML, mHtml);
        }
        if (mText != null) {
            json.put(OBJ_TEXT, mText);
        }
        if (mB64Image != null) {
            json.put(OBJ_B64_JPEG, mB64Image);
        }
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
}
