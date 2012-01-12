package mobisocial.socialkit.musubi;

import android.net.Uri;
import edu.stanford.junction.Junction;
import edu.stanford.junction.JunctionException;
import edu.stanford.junction.android.AndroidJunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;

public final class ConnectionManager {
    /**
     * Attaches a Junction runtime to a session generated for the give
     * Obj.
     */
    public Junction joinJunctionForObj(JunctionActor actor, DbObj obj)
            throws JunctionException {
        String uid = obj.getUri().getLastPathSegment();
        uid = uid.replace("^", "_").replace(":", "_");
        Uri uri = new Uri.Builder().scheme("junction")
                .authority("sb.openjunction.org")
                .appendPath("dbf-" + uid).build();
        return AndroidJunctionMaker.bind(uri, actor);
    }
}
