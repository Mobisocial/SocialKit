
package mobisocial.socialkit.musubi;

import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

import mobisocial.socialkit.util.FastBase64;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class User {
    public static final String COL_NAME = "name";
    public static final String COL_PUBLIC_KEY = "public_key";
    public static final String COL_PERSON_ID = "person_id";

    private final String mId;
    private final String mName;

    User(String name, String personId) {
        mName = name;
        mId = personId;
    }

    public String getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public static User getUser(Context context, Uri feedUri, String personId) {
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/member_details/" +
                feedUri.getLastPathSegment() + "/" + personId);
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor c = context.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder);
        try {
            if (!c.moveToFirst()) {
                Log.w(Musubi.TAG, "No user found for " + personId);
                return null;
            }
            String name = c.getString(c.getColumnIndexOrThrow(COL_NAME));
            return new User(name, personId);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static User getLocalUser(Context context, Uri feedUri) {
        Uri uri = Uri.parse("content://" + Musubi.AUTHORITY + "/local_user/" +
                feedUri.getLastPathSegment());
        String[] projection = null;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = null;
        Cursor c = context.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            String name = c.getString(c.getColumnIndexOrThrow(COL_NAME));
            String keyStr  = c.getString(c.getColumnIndexOrThrow(COL_PUBLIC_KEY));
            PublicKey key = publicKeyFromString(keyStr);
            String personId = makePersonIdForPublicKey(key);
            return new User(name, personId);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public static RSAPublicKey publicKeyFromString(String str){
        try{
            byte[] pubKeyBytes = FastBase64.decode(str);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(pubKeyBytes);
            return (RSAPublicKey)keyFactory.generatePublic(publicKeySpec);                
        }
        catch(Exception e){
            throw new IllegalStateException("Error loading public key: " + e);
        }
    }

    // TODO: This is synchronized with bumblebee's Util.makePersonId.
    // bumblebee should depend on SocialKit and call this method.
    public static String makePersonIdForPublicKey(PublicKey key) {
        String me = null;
        try {
            me = SHA1(key.getEncoded());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not compute SHA1 of public key.", e);
        }
        return me.substring(0, 10);
    }

    private static String SHA1(byte[] input) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(input, 0, input.length);
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }

                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
