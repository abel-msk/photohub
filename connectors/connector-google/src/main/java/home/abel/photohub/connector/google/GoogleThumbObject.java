package home.abel.photohub.connector.google;

import java.net.URL;

import com.google.gdata.data.media.mediarss.MediaThumbnail;

public class GoogleThumbObject {
	MediaThumbnail gThumbnail = null;
	URL url = null;
	int weight = 0;
	
	public GoogleThumbObject(MediaThumbnail gThumbnail) throws Exception {
		this.gThumbnail = gThumbnail;
		this.url = new URL(gThumbnail.getUrl());
		this.weight = gThumbnail.getHeight() * gThumbnail.getWidth();
	};
	
	public int getWeight() {
		return weight;
	}
	public boolean isLE(double W, double H) {
		return getWeight() <= W * H;
	}
	
	public boolean isGE(double W, double H) {
		return getWeight() >= W * H;
	}
	
	public URL getUrl() {
		return this.url;
	}
	
	public int getWidth() {
		return this.gThumbnail.getWidth();
	}
	
	public int getHeight() {
		return this.gThumbnail.getHeight();
	}
}
