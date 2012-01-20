package mobisocial.socialkit.musubi;

import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import mobisocial.socialkit.util.FastBase64;

/**
 * TODO: kill me
 * @hide
 */
public class RSACrypto {

    public static RSAPublicKey publicKeyFromString(String str) {
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

    public static RSAPrivateKey privateKeyFromString(String str){
        try{
            byte[] privKeyBytes = FastBase64.decode(str);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
            return (RSAPrivateKey)keyFactory.generatePrivate(privateKeySpec);
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
        md.update(input, 0, input.length);
        byte[] sha1hash = md.digest();
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
