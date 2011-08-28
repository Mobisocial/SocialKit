package mobisocial.socialkit.musubi;

import org.json.JSONObject;

public class AppState {
    JSONObject mState;
    String mThumbnailText;
    String mThumbnailImage;
    String mThumbnailHtml;
    String mArg;

    private AppState() {
        
    }

    public AppState(JSONObject state) {
        mState = state;
    }

    public static class Builder {
        private final AppState mmAppState;

        public Builder() {
            mmAppState = new AppState();
        }
        public Builder setState(JSONObject state) {
            mmAppState.mState = state;
            return this;
        }

        public Builder setThumbnailText(String text) {
            mmAppState.mThumbnailText = text;
            return this;
        }

        public Builder setThumbnailB64Image(String img) {
            mmAppState.mThumbnailImage = img;
            return this;
        }

        public Builder setThumbnailHtml(String html) {
            mmAppState.mThumbnailHtml = html;
            return this;
        }

        public Builder setArgument(String arg) {
            mmAppState.mArg = arg;
            return this;
        }

        public AppState build() {
            return mmAppState;
        }
    }
}
