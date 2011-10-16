package mobisocial.socialkit;

import android.net.Uri;

/**
 * <p>This interface will change to better separate Musubi from the POSI standards.
 * The new interface will look more like:
 * <ul>
 *   <li>public String getName()
 *   <li>public String getMembershipType(); // fixed, groupserver, owned, etc.
 * 
 * <p> 
 * Implementations will have other methods like getMembers(), addMember() etc.
 * <p>
 * Sending a message to a feed is done via an ObjTransport, using postObj(Feed feed, SignedObj obj).
 * <p>
 * Signing an Obj also requires access to the feed: SignedObj signed = Musubi.signObj(User from, Feed to, Obj obj);
 */
public interface Feed {
    public Uri getUri();
    public void postObj(Obj obj);
}
