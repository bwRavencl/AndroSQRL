package de.bwravencl.androsqrl.model;

public class AuthRequest {

	private boolean isHttps;
	private String schemelessUrl; // contains everything except scheme

	public AuthRequest(String url) {
		this.schemelessUrl = removeScheme(url);
	}

	public boolean isHttps() {
		return isHttps;
	}

	// The part to be signed
	public String getSchemelessUrl() {
		return schemelessUrl;
	}

	public String getReturnUrl() {
		String returnUrl = schemelessUrl.substring(0,
				schemelessUrl.indexOf("?"));

		if (isHttps)
			returnUrl = "https://" + returnUrl;
		else
			returnUrl = "http://" + returnUrl;

		return returnUrl;
	}

	// get domain form URL
	public String getDomain() {
		return schemelessUrl.substring(0, schemelessUrl.indexOf("/"));
	}

	// remove the sqrl:// part from the URL and set isHTTPS
	private String removeScheme(String url) {
		// sqrl://
		if (url.substring(0, 1).compareTo("s") == 0) {
			url = url.substring(7);
			isHttps = true;
		}
		// qrl://
		else {
			url = url.substring(6);
			isHttps = false;
		}

		return url;
	}
}
