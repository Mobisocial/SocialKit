package mobisocial.socialkit.musubi;

import android.content.Context;

public class MusubiContext {
    private final Context mContext;
    public static MusubiContext forAndroidContext(Context context) {
        return new MusubiContext(context);
    }

    public MusubiContext(Context context) {
        mContext = context;
    }

    
}
