package mobisocial.socialkit;


/**
 * An Obj that has been encoded and signed by a {@link User} to be shared with
 * a set of remote users with a common {@link Feed}.
 *
 */
public interface SignedObj extends Obj {

    /**
     * Returns a hash of the user's signature over this obj, bound within a
     * single containing feed.
     */
    public long getHash();

    /**
     * Returns the {@link User} who signed this obj.
     * @return
     */
    public User getSender();

    /**
     * The number of messages the sender has sent to the feed
     * given by {@link #getContainingFeed()}.
     */
    public long getSequenceNumber();

    /**
     * The feed in which this Obj is contained.
     * @return
     */
    public Feed getContainingFeed();
}
