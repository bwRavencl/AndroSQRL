package de.bwravencl.androsqrl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import de.bwravencl.androsqrl.R;
import de.bwravencl.androsqrl.model.Identity;
import android.os.Bundle;
import android.widget.ImageView;
import android.app.Activity;
import android.graphics.Bitmap;

public class ExportIdentityActivity extends Activity {

	public static final int QR_CODE_DIMENSION = 500;

	private ImageView imageViewQRCode;

	private Identity identity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_export_identity);

		identity = (Identity) getIntent().getParcelableExtra(
				LoginActivity.EXTRA_IDENTITY);

		imageViewQRCode = (ImageView) findViewById(R.id.imageViewQRCode);

		final QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(
				identity.toString(), null, Contents.Type.TEXT,
				BarcodeFormat.QR_CODE.toString(), QR_CODE_DIMENSION);

		try {
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			imageViewQRCode.setImageBitmap(bitmap);
		} catch (WriterException e) {
			e.printStackTrace();
		}
	}

}
