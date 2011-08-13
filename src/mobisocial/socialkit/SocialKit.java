package mobisocial.socialkit;

import java.util.Set;

import android.content.Intent;
import android.net.Uri;

public class SocialKit {

    /**
     * Use the DungBeetle APIs in your application.
     */
    public static class Dungbeetle {
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
            public Junction getJunction() {
                return null;
            }

            public Object getApplicationStorage(Application app) {
                return null;
            }

            public void setApplicationSnapshot(Application app) {
                
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
            
        }
    }

    public class Junction {
        public void sendMessageToSesion(Object obj) {

        }
    }

    public class Application {

    }
}
