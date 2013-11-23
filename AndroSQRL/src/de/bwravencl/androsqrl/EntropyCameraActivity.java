package de.bwravencl.androsqrl;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import de.bwravencl.androsqrl.R;

import de.bwravencl.androsqrl.utils.Crypto;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;

public class EntropyCameraActivity extends Activity implements
		Camera.PreviewCallback, SurfaceTextureListener {

	public static final String EXTRA_ENTROPY_BYTES = "EXTRA_ENTROPY_BYTES";
	public static final String EXTRA_FRAMES_CAPTURED = "EXTRA_FRAMES_CAPTURED";

	public static final int RESULT_NO_CAMERA = RESULT_FIRST_USER;

	public static final int CAPTURED_DATA_MAXLENGTH = 262144; // 256 kB

	public static final long CAPTURE_DURATION = 5000L;

	private Camera camera;

	private TextureView textureView;

	private byte[] capturedData;

	private int framesCaptured = 0;

	private boolean recording = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_entropy_camera);

		textureView = (TextureView) findViewById(R.id.textureView);
		textureView.setSurfaceTextureListener(this);

		setResult(RESULT_CANCELED);

		if (checkCameraHardware())
			startCapture(CAPTURE_DURATION);
		else {
			setResult(RESULT_NO_CAMERA);
			finish();
		}
	}

	/** Check if this device has a camera */
	private boolean checkCameraHardware() {
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera camera = null;
		try {
			camera = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return camera; // returns null if camera is unavailable
	}

	public void startCapture(long duration) {
		capturedData = null;
		framesCaptured = 0;
		recording = true;

		final TimerTask timerTask = new TimerTask() {

			@Override
			public void run() {
				EntropyCameraActivity.this.stopCapture();
			}
		};
		new Timer().schedule(timerTask, duration);
	}

	public void stopCapture() {
		recording = false;

		// Limit the max length, to ensure we don't hit this issue:
		// https://code.google.com/p/android/issues/detail?id=5878
		if (capturedData.length > CAPTURED_DATA_MAXLENGTH)
			capturedData = Crypto.subByte(capturedData, 0,
					CAPTURED_DATA_MAXLENGTH);

		final Intent data = new Intent();
		data.putExtra(EXTRA_ENTROPY_BYTES, capturedData);
		data.putExtra(EXTRA_FRAMES_CAPTURED, framesCaptured);
		setResult(RESULT_OK, data);

		finish();
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (recording) {
			framesCaptured++;

			if (capturedData == null)
				capturedData = data;
			else
				capturedData = Crypto.xor(capturedData, data);
		}
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
		camera = getCameraInstance();

		try {
			camera.setPreviewTexture(surface);
		} catch (IOException e) {
			e.printStackTrace();
		}
		camera.setPreviewCallback(this);
		camera.setDisplayOrientation(90); // TODO: this value should probably
											// not be hardcoded

		camera.startPreview();
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		camera.stopPreview();
		camera.setPreviewCallback(null);
		camera.release();

		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}
}
