package mobisocial.socialkit.exp;

import android.os.Parcel;
import android.os.Parcelable;

public class ISKObject implements Parcelable {
	long id_;
	byte[] guid_;
	long fromId_;
	String type_;
	byte[] data_;
	
    public static final Parcelable.Creator<ISKObject> CREATOR = new Parcelable.Creator<ISKObject>() {
        public ISKObject createFromParcel(Parcel in) {
        	ISKObject obj = new ISKObject();
    		// TODO Auto-generated method stub
        	return obj;
        }

        public ISKObject[] newArray(int size) {
            return new ISKObject[size];
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
