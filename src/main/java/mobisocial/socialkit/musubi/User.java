package mobisocial.socialkit.musubi;

public class User {
    private final long mId;
    private final String mName;

    User(String name, String publicKey) {
        mId = publicKey.hashCode();
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public long getId() {
        return mId;
    }
}