package mobisocial.socialkit.musubi.multiplayer;

public abstract class Multiplayer {
    public static final String ACTION_TWO_PLAYERS = "mobisocial.intent.action.TWO_PLAYERS";
    public static final String ACTION_MULTIPLAYER = "mobisocial.intent.action.MULTIPLAYER";

    public static final String OBJ_MEMBERSHIP = "membership";
    public static final String OBJ_MEMBER_CURSOR = "member_cursor";

    static final boolean DBG = true;
    static final String TAG = "musubi";

    public abstract String[] getMembers();
}
