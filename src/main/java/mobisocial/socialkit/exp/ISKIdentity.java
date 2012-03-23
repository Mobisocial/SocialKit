package mobisocial.socialkit.exp;

import android.os.Parcel;
import android.os.Parcelable;

public class ISKIdentity implements Parcelable {
	long id_;
	byte[] guid_;
	String name_;

    public static final Parcelable.Creator<ISKIdentity> CREATOR = new Parcelable.Creator<ISKIdentity>() {
        public ISKIdentity createFromParcel(Parcel in) {
        	ISKIdentity ident = new ISKIdentity();
    		// TODO Auto-generated method stub
        	return ident;
        }

        public ISKIdentity[] newArray(int size) {
            return new ISKIdentity[size];
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
