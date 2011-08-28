package mobisocial.socialkit.musubi;

import org.json.JSONObject;

public class AppState {
    public JSONObject state;
    public String thumbnailText;
    public String thumbnailImage;
    public String thumbnailHtml;
    public String arg;

    private AppState() {
        
    }

    public AppState(JSONObject state) {
        this.state = state;
    }

    public static class Builder {
        private final AppState mmAppState;

        public Builder() {
            mmAppState = new AppState();
        }
        public Builder setState(JSONObject state) {
            mmAppState.state = state;
            return this;
        }

        public Builder setThumbnailText(String text) {
            mmAppState.thumbnailText = text;
            return this;
        }

        public Builder setThumbnailB64Image(String img) {
            mmAppState.thumbnailImage = img;
            return this;
        }

        public Builder setThumbnailHtml(String html) {
            mmAppState.thumbnailHtml = html;
            return this;
        }

        public Builder setArgument(String arg) {
            mmAppState.arg = arg;
            return this;
        }

        public AppState build() {
            return mmAppState;
        }
    }
}
