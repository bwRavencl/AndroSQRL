/*Copyright 2013 Matteo Hausner

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

package de.bwravencl.androsqrl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import de.bwravencl.androsqrl.R;
import de.bwravencl.androsqrl.model.Identity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.print.PrintHelper;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;

public class ExportIdentityActivity extends Activity {

	private Menu menu;
	private TextView textView;
	private ProgressBar progressBar;
	private ImageView imageViewQRCode;

	private Identity identity;

	private Bitmap bitmapQRCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_export_identity);

		identity = (Identity) getIntent().getParcelableExtra(
				MainActivity.EXTRA_IDENTITY);
		final String password = (String) getIntent().getStringExtra(
				MainActivity.EXTRA_PASSWORD);

		textView = (TextView) findViewById(R.id.textView);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		imageViewQRCode = (ImageView) findViewById(R.id.imageViewQRCode);

		if (identity != null && password != null)
			new ExportTask().execute(identity, password);
		else
			finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;

		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.export, menu);

		for (int i = 0; i < menu.size(); i++)
			menu.getItem(i).setEnabled(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_save_to_sdcard:
			doSaveToSDCard();
			break;
		case R.id.action_print:
			doPrint();
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	public void finish() {
		super.finish();
		identity.clearMasterKey();
	}

	private void doSaveToSDCard() {
		if (bitmapQRCode != null) {
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			bitmapQRCode.compress(Bitmap.CompressFormat.PNG, 100, bytes);

			final String filenameExtension = ".png";
			String path = Environment.getExternalStorageDirectory()
					+ File.separator + identity.getName() + filenameExtension;
			File file = new File(path);

			String newPath;
			int i = 1;
			while (file.exists()) {
				newPath = path + "(" + i + ")" + filenameExtension;
				file = new File(newPath);
				i++;
			}

			try {
				file.createNewFile();
				final FileOutputStream fos = new FileOutputStream(file);
				fos.write(bytes.toByteArray());
				fos.close();

				Toast.makeText(this,
						"QR-Code saved to:\n" + file.getAbsolutePath(),
						Toast.LENGTH_LONG).show();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void doPrint() {
		if (bitmapQRCode != null) {
			final PrintHelper printHelper = new PrintHelper(this);

			printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
			printHelper.setColorMode(PrintHelper.COLOR_MODE_MONOCHROME);
			printHelper.printBitmap(identity.getName(), bitmapQRCode);
		}
	}

	private class ExportTask extends AsyncTask<Object, Void, String> {

		@Override
		protected String doInBackground(Object... params) {
			final Identity identity = (Identity) params[0];
			final String password = (String) params[1];

			return identity.getExportString(password);
		}

		@Override
		protected void onPostExecute(String result) {
			final Display display = getWindowManager().getDefaultDisplay();
			final Point size = new Point();
			display.getSize(size);
			final int width = size.x;

			final QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(result, null,
					Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), width);

			try {
				bitmapQRCode = qrCodeEncoder.encodeAsBitmap();
				imageViewQRCode.setImageBitmap(bitmapQRCode);
			} catch (WriterException e) {
				e.printStackTrace();
			}

			textView.setText("This QR-Code encodes your identity in an encrypted form.\n\nIn order to backup your identity, you should print the QR-Code on paper and keep it at a safe place.");
			progressBar.setVisibility(View.INVISIBLE);
			imageViewQRCode.setVisibility(View.VISIBLE);

			for (int i = 0; i < menu.size(); i++)
				menu.getItem(i).setEnabled(true);
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}
}
