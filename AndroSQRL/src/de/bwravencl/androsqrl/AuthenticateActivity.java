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

package de.bwravencl.androsqrl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;

import com.github.dazoe.android.Ed25519;

import de.bwravencl.androsqrl.R;

import de.bwravencl.androsqrl.exception.InvalidUrlException;
import de.bwravencl.androsqrl.model.Identity;
import de.bwravencl.androsqrl.model.AuthRequest;
import de.bwravencl.androsqrl.utils.ZXOrientationFixCallback;
import eu.livotov.zxscan.ZXScanHelper;

import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class AuthenticateActivity extends Activity {

	public static final int REQUEST_SCAN_QR_CODE = 1;

	private TextView textViewIdentityName;
	private TextView textViewMessage;
	private Button buttonConfirm;

	private Identity identity;
	private String url;

	private AuthRequest authRequest;

	private String publicKey = "";
	private String signature = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authenticate);

		identity = (Identity) getIntent().getParcelableExtra(
				MainActivity.EXTRA_IDENTITY);
		url = getIntent().getStringExtra(MainActivity.EXTRA_URL);

		textViewIdentityName = (TextView) findViewById(R.id.textViewIdentityName);
		textViewIdentityName.setText(identity.getName());

		textViewMessage = (TextView) findViewById(R.id.textViewMessage);

		buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
		buttonConfirm.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				textViewMessage
						.setText("Authenticating...\n\nPlease wait, this will only take a few seconds...");
				buttonConfirm.setEnabled(false);

				final String url = authRequest.getSchemelessUrl();
				if (url != null)
					new AuthenticateTask().execute(url);
			}
		});

		if (url != null) {
			handleURL();
		} else {
			final SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);

			ZXScanHelper.setUserCallback(new ZXOrientationFixCallback());
			ZXScanHelper.setBlockCameraRotation(false);
			ZXScanHelper.setPlaySoundOnRead(sharedPreferences.getBoolean(
					"pref_title_scanner_beep", true));
			ZXScanHelper.setVibrateOnRead(sharedPreferences.getBoolean(
					"pref_title_scanner_vibrate", true));

			ZXScanHelper.scan(this, REQUEST_SCAN_QR_CODE);
		}
	}

	@Override
	public void finish() {
		super.finish();
		identity.clearMasterKey();
	}

	// Finishes the Activity after 'duration' ms
	private void startFinishingTimer(long duration) {
		// Show toast multiple times to increase the duration
		for (int i = 0; i < 2; i++)
			Toast.makeText(
					this,
					"Returning to login screen in " + duration / 1000L
							+ " s ...", Toast.LENGTH_LONG).show();

		final TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				finish();
			}
		};
		new Timer().schedule(timerTask, duration);
	}

	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (requestCode == REQUEST_SCAN_QR_CODE) {
			if (resultCode == RESULT_OK) {
				url = ZXScanHelper.getScannedCode(data);
				handleURL();
			} else
				finish(); // Cancelled by user -> abort
		}
	}

	// Called once we have an URL to work with, either by Extra or by the
	// Scanner Activity
	private void handleURL() {
		try {
			authRequest = new AuthRequest(url);

			textViewMessage
					.setText("Do you really want to authenticate to:\n\n"
							+ authRequest.getDomain() + " ?");
			buttonConfirm.setEnabled(true);
		} catch (InvalidUrlException e) {
			e.printStackTrace();

			// Show toast multiple times to increase the duration
			for (int i = 0; i < 2; i++)
				Toast.makeText(this, "Invalid SQRL code!", Toast.LENGTH_LONG)
						.show();
			finish();
		}
	}

	// AsyncTask to perform the actual authentication
	private class AuthenticateTask extends AsyncTask<String, Void, String[]> {
		@Override
		protected String[] doInBackground(String... params) {
			final String url = params[0];

			final byte[] privateKey = getPrivateKey(authRequest.getDomain(),
					identity.getMasterkey());

			byte[] publicKey = null;
			byte[] signature = null;

			try {
				publicKey = Ed25519.PublicKeyFromPrivateKey(privateKey);
				signature = Ed25519.Sign(url.getBytes(), privateKey);
			} catch (Exception e) {
				e.printStackTrace();
			}

			final String publicKeyString = Base64.encodeToString(publicKey,
					Base64.DEFAULT);
			final String signatureString = Base64.encodeToString(signature,
					Base64.DEFAULT);

			return new String[] { publicKeyString, signatureString };
		}

		@Override
		protected void onPostExecute(String[] result) {
			publicKey = result[0];
			signature = result[1];

			postData(authRequest.getReturnUrl(),
					authRequest.getSchemelessUrl(), signature, publicKey);
		}

		// Create the private key from URL and secret key
		private byte[] getPrivateKey(String domain, byte[] key) {
			byte[] privateKey = null;

			try {
				final SecretKeySpec secretKeySpec = new SecretKeySpec(key,
						"HmacSHA256");
				final Mac mac = Mac.getInstance("HmacSHA256");
				mac.init(secretKeySpec);
				privateKey = mac.doFinal(domain.getBytes());
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}

			return privateKey;
		}

		// Send signature and pubkey to server
		private void postData(String url, String message, String signature,
				String publicKey) {
			final HttpPost httppost = new HttpPost(url);

			try {
				// Add data to post
				final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("message", message));
				nameValuePairs.add(new BasicNameValuePair("signature",
						signature));
				nameValuePairs.add(new BasicNameValuePair("publicKey",
						publicKey));

				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				final HttpThread httpThread = new HttpThread(httppost);
				httpThread.start();
				synchronized (httpThread) {
					try {
						httpThread.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					final HttpResponse response = httpThread.getResponse();

					int status = response.getStatusLine().getStatusCode();

					if (status == HttpStatus.SC_OK) {
						ByteArrayOutputStream ostream = new ByteArrayOutputStream();
						response.getEntity().writeTo(ostream);

						String out = ostream.toString();
						// See if the page returned "Verified"
						if (out.contains("Verified")) {
							textViewMessage
									.setText("Authentication successful!");
							textViewMessage.setTextColor(getResources()
									.getColor(R.color.DKGREEN));
							startFinishingTimer(10000L);
						} else {
							textViewMessage
									.setText("Authentication unsuccessful!\n\nDetails:\n\n"
											+ out);
							textViewMessage.setTextColor(getResources()
									.getColor(R.color.DKRED));
						}
					} else {
						textViewMessage
								.setText("There has been a problem contating the server.");
						textViewMessage.setTextColor(Color.RED);
					}
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private class HttpThread extends Thread {

			private final HttpClient httpClient = new DefaultHttpClient();
			private HttpPost httpPost;
			private HttpResponse httpResponse;

			public HttpThread(HttpPost httpPost) {
				this.httpPost = httpPost;
			}

			@Override
			public void run() {
				synchronized (this) {
					try {
						httpResponse = httpClient.execute(httpPost);
						notify();
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			public HttpResponse getResponse() {
				return httpResponse;
			}
		};
	}
}
