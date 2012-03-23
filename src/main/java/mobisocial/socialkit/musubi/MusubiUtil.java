package mobisocial.socialkit.musubi;

import java.nio.ByteBuffer;

public class MusubiUtil {

    static String andClauses(String A, String B) {
        if(A == null && B == null) return "1 = 1";
        if(A == null) return B;
        if(B == null) return A;
        return "(" + A + ") AND (" + B + ")";
    }

    static String convertToHex(byte[] data) {
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

    public static long shortHash(byte[] data) {
        if (data.length < 8) {
            throw new IllegalArgumentException("Data too short");
        }
        return ByteBuffer.wrap(data).getLong();
    }
}
