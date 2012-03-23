package mobisocial.socialkit.exp;

import android.os.Parcel;
import android.os.Parcelable;

public class ISKFeed implements Parcelable {
	long id_;
	byte[] guid_;
	String name_;
	
	public static final Parcelable.Creator<ISKFeed> CREATOR = new Parcelable.Creator<ISKFeed>() {
        public ISKFeed createFromParcel(Parcel in) {
        	ISKFeed f = new ISKFeed();
    		// TODO Auto-generated method stub
        	return f;
        }

        public ISKFeed[] newArray(int size) {
            return new ISKFeed[size];
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
