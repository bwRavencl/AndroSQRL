/*
 * Copyright 2013 Matteo Hausner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.bwravencl.androsqrl.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.bwravencl.androsqrl.exception.InvalidUrlException;

public class AuthRequest {

	private boolean isHttps;
	private String schemelessUrl; // contains everything except scheme

	public AuthRequest(String url) throws InvalidUrlException {
		if (!isValidUrl(url))
			throw new InvalidUrlException(url);

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

	public static final boolean isValidUrl(String url) {
		if (url == null)
			return false;

		final Pattern pattern = Pattern
				.compile("^?qrl://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]\\?webnonce=[-a-zA-Z0-9]*");
		final Matcher matcher = pattern.matcher(url);

		return matcher.matches();
	}
}
