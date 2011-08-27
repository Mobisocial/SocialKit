package mobisocial.socialkit.musubi;

import org.json.JSONObject;

public class User {
    private final String mName;

    public User(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public JSONObject getAttribute(String id) {
        return null;
    }
}