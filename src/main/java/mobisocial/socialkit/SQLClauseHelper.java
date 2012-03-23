package mobisocial.socialkit;

import java.util.Iterator;


public class SQLClauseHelper {
    public static String andClauses(String A, String B) {
        if(A == null && B == null) return "1 = 1";
        if(A == null) return B;
        if(B == null) return A;
        return "(" + A + ") AND (" + B + ")";
    }

    public static String[] andArguments(String[] A, String[] B) {
        if (A == null) return B;
        if (B == null) return A;
        String[] C = new String[A.length + B.length];
        System.arraycopy(A, 0, C, 0, A.length);
        System.arraycopy(B, 0, C, A.length, B.length);
        return C;
    }

    public static String[] andArguments(String[] A, Object... B) {
        if (B == null) return A;

        int aLen = (A == null) ? 0 : A.length;
        int bLen = (B == null) ? 0 : B.length;        
        String[] C = new String[aLen + bLen];
        if (aLen > 0) {
            System.arraycopy(A, 0, C, 0, A.length);
        }
        for (int i = 0; i < B.length; i++) {
            C[aLen + i] = B[i].toString();
        }
        return C;
    }

    public static void appendArray(StringBuilder sb, Iterator<?> i) {
    	sb.append("(");
    	if(i.hasNext()) {
    		sb.append(i.next());
    	}
    	while(i.hasNext()) {
    		sb.append(',');
    		sb.append(i.next());
    	}
    	sb.append(")");
    }
}
