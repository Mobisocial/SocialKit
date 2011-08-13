package mobisocial.socialkit;

import java.util.Set;

import org.json.JSONObject;

import edu.stanford.junction.api.activity.JunctionActor;

import android.content.Intent;
import android.net.Uri;

public class SocialKit {

    /**
     * Use the DungBeetle APIs in your application.
     */
    public static class Dungbeetle {
        public static final String LAUNCH_TWO_PLAYERS = "mobisocial.intent.action.LAUNCH_TWO_PLAYERS";
        public static final String LAUNCH_N_PLAYERS = "mobisocial.intent.action.LAUNCH_N_PLAYERS";
        public static final String INTENT_EXTRA_DUNGBEETLE = "mobisocial.dungbeetle"; 

        public static boolean isDungbeetleIntent(Intent intent) {
            return intent.hasExtra(INTENT_EXTRA_DUNGBEETLE);
        }

        public void startInviteMembersActivity() {
            
        }

        public Uri getInvitationUri() {
            return null;
        }

        public Thread getThread() {
            return null;
        }

        public static Dungbeetle getInstance(Intent intent) {
            return null;
        }

        /**
         * A DungBeetle Thread.
         *
         */
        public class Thread {
            private JSONObject mState;

            public Junction getJunction() {
                return null;
            }

            public JSONObject getApplicationState() {
                // TODO: synchronize
                return mState;
            }

            public void setApplicationState(JSONObject appState) {
                mState = appState;
                // TODO
                // postMessage(appState);
            }

            public void postMessage(JSONObject message) {

            }

            /**
             * List of participants available to this thread.
             * @return
             */
            public Set<User> getMembers() {
                return null;
            }
        }

        public class User {
            public JSONObject getAttribute(String id) {
                return null;
            }
        }

        public Intent getIntentForReconfigLaunch() {
            return null;
        }
    }

    public class Junction {
        public void bind(JunctionActor actor) {

        }
    }
}
