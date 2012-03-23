package mobisocial.socialkit.exp;

import android.os.Parcel;
import android.os.Parcelable;

public class ISKPrimitive implements Parcelable {
	boolean null_;
	Long longValue_;
	Double doubleValue_;
	String stringValue_;
	byte[] blobValue_;
	
    public static final Parcelable.Creator<ISKPrimitive> CREATOR = new Parcelable.Creator<ISKPrimitive>() {
        public ISKPrimitive createFromParcel(Parcel in) {
        	ISKPrimitive prim = new ISKPrimitive();
    		// TODO Auto-generated method stub
        	return prim;
        }

        public ISKPrimitive[] newArray(int size) {
            return new ISKPrimitive[size];
        }
    };

    @Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}
}
