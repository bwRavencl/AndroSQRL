package de.bwravencl.androsqrl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import com.sqrl.android_sqrl.R;

import de.bwravencl.androsqrl.model.Identity;
import de.bwravencl.androsqrl.model.AuthRequest;

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

public class AuthActivity extends Activity {

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
		setContentView(R.layout.activity_auth);

		identity = (Identity) getIntent().getParcelableExtra(
				LoginActivity.EXTRA_IDENTITY);
		url = getIntent().getStringExtra(LoginActivity.EXTRA_URL);

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
					new CreateSignatureTask().execute(url);
			}
		});

		if (url != null) {
			handleURL();
		} else {
			final SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);

			ZXScanHelper.setPlaySoundOnRead(sharedPreferences.getBoolean(
					"pref_title_scanner_beep", true));
			ZXScanHelper.setVibrateOnRead(sharedPreferences.getBoolean(
					"pref_title_scanner_vibrate", true));

			ZXScanHelper.scan(this, REQUEST_SCAN_QR_CODE);
		}
	}

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

	private void handleURL() {
		authRequest = new AuthRequest(url);

		textViewMessage.setText("Do you really want to authenticate to:\n\n"
				+ authRequest.getDomain() + " ?");
		buttonConfirm.setEnabled(true);
	}

	private class CreateSignatureTask extends AsyncTask<String, Void, String[]> {
		@Override
		protected String[] doInBackground(String... params) {
			final String url = params[0];

			final byte[] privateKey = createPrivateKey(authRequest.getDomain(),
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
	}

	// Create the private key from URL and secret key
	public static byte[] createPrivateKey(String domain, byte[] key) {
		byte[] hmac = null;

		try {
			SecretKeySpec pKey = new SecretKeySpec(key, "HmacSHA256");
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(pKey);
			hmac = mac.doFinal(domain.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return hmac;
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
			nameValuePairs.add(new BasicNameValuePair("signature", signature));
			nameValuePairs.add(new BasicNameValuePair("publicKey", publicKey));

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
						textViewMessage.setText("Authentication successful!");
						textViewMessage.setTextColor(Color.GREEN);
						startFinishingTimer(10000L);
					} else {
						textViewMessage
								.setText("Authentication unsuccessful!\n\nDetails:\n\n"
										+ out);
						textViewMessage.setTextColor(Color.RED);
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
