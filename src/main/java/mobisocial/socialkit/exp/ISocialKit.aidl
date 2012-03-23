package mobisocial.socialkit.exp;

import mobisocial.socialkit.exp.ISKIdentity;
import mobisocial.socialkit.exp.ISKFeed;
import mobisocial.socialkit.exp.ISKObject;
import mobisocial.socialkit.exp.ISKPrimitive;
import android.os.Parcelable;

interface ISocialKit {
    //identity access
    ISKIdentity[] getOwnedIdentities();
    ISKIdentity[] getIdentitiesWithApp();
    ISKIdentity[] getIdentitiesWithMusubi();
    ISKIdentity[] getAllIdentities();
    byte[] getPicture(in long id);
    byte[] getPictureByGUID(in byte[] id);
    ParcelFileDescriptor getPictureLarge(in long id);
    ParcelFileDescriptor getPictureLargeByGUID(in byte[] id);


    //feed access
    ISKIdentity[] getFeedMembers(in long feed);
    ISKIdentity[] getFeedMembersByGUID(in byte[] feed);
    ISKFeed createFeed();
    ISKFeed[] getFeeds();
    
    //object access, meant for reading it as a stream
    //or random access to previously seed objects
    ISKObject[] getNewObjects(in long feed, long since);
    ISKObject[] getNewObjectsByGUID(in byte[] feed, long since);
    ISKObject[] getNewObjectsWithData(in long feed, long since);
    ISKObject[] getNewObjectsWithDataByGUID(in byte[] feed, long since);
    ISKObject[] getChildren(in long parent, long since);
    ISKObject[] getChildrenByGUID(in byte[] parent, long since);
    ISKObject[] getChildrenWithData(in long parent, long since);
    ISKObject[] getChildrenWithDataByGUID(in byte[] parent, long since);
    ISKObject getParent(in long child);
    ISKObject getParentByGUID(in byte[] child);
    ISKObject getParentWithData(in long child);
    ISKObject getParentWithDataByGUID(in byte[] child);
    ISKObject getObject(long id);
    ISKObject getObjectByGUID(in byte[] guid);
    ISKObject getObjectWithData(long id);
    ISKObject getObjectWithDataByGUID(in byte[] guid);
    
    //facts interface
    void declare(String relation, in ISKPrimitive[] key, in ISKPrimitive value);
    void watch(String relation, in ISKPrimitive[] keyPrefix);
    ISKPrimitive query(String relation, in ISKPrimitive[] key);
    ISKPrimitive[] queryMany(String relation, in ISKPrimitive[] keyPrefix);
    
    //obj interface
    ISKObject broadcast(in ISKObject object);
    ISKObject send(in long feed, in ISKObject object);
    ISKObject sendByGUID(in byte[] feed, in ISKObject object);
    void update(in long id, in ISKObject object);
    void updateByGUID(in byte[] guid, in ISKObject object);
    void delete(in long id);
    void deleteByGUID(in byte[] guid);
    
    //ephemeral session interface
    byte[] read(in byte[] junctionAESKey, long timeout);
    void write(in byte[] junctionAESKey, in byte[] data);
}