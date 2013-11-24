package de.bwravencl.androsqrl;

import de.bwravencl.androsqrl.R;

import de.bwravencl.androsqrl.model.Identity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CreateIdentityActivity extends Activity {

	public static final int REQUEST_GENERATE_CAMERA_ENTROPY = 1;

	private EditText editTextName;
	private EditText editTextPassword;
	private EditText editTextConfirmPassword;
	private Button buttonCreateIdentity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_identity);

		editTextName = (EditText) findViewById(R.id.editTextName);
		editTextPassword = (EditText) findViewById(R.id.editTextPassword);
		editTextConfirmPassword = (EditText) findViewById(R.id.editTextConfirmPassword);

		buttonCreateIdentity = (Button) findViewById(R.id.buttonCreateIdentity);
		buttonCreateIdentity.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final String name = editTextName.getText().toString();
				final String password = editTextPassword.getText().toString();
				final String confirmPassword = editTextConfirmPassword
						.getText().toString();
				if (name == null || name.length() == 0) {
					final AlertDialog alertDialog = new AlertDialog.Builder(
							CreateIdentityActivity.this).create();
					alertDialog.setCancelable(false);
					alertDialog
							.setMessage("The name field is empty.\n\nPlease enter a valid name for your identity.");
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
					alertDialog.show();
				} else if (!Identity.loadIdentityNames(
						CreateIdentityActivity.this).contains(name)) {
					if (password.compareTo(confirmPassword) == 0) {
						if (password != null
								&& password.length() >= Identity.MIN_PASSWORD_LENGTH) {
							final Intent intent = new Intent(
									CreateIdentityActivity.this,
									EntropyCameraActivity.class);
							startActivityForResult(intent,
									REQUEST_GENERATE_CAMERA_ENTROPY);
						} else {
							final AlertDialog alertDialog = new AlertDialog.Builder(
									CreateIdentityActivity.this).create();
							alertDialog.setCancelable(false);
							alertDialog
									.setMessage("The provided password does not comply with the minimum password length requirements.\n\nPlease choose a password with at least "
											+ Identity.MIN_PASSWORD_LENGTH
											+ " characters.");
							alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
									getString(android.R.string.ok),
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											dialog.dismiss();
										}
									});
							alertDialog.show();
						}
					} else {
						final AlertDialog alertDialog = new AlertDialog.Builder(
								CreateIdentityActivity.this).create();
						alertDialog.setCancelable(false);
						alertDialog
								.setMessage("The provided passwords do not match.\n\nPlease make sure that the retyped password matches the actual password.");
						alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
								getString(android.R.string.ok),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.dismiss();
									}
								});
						alertDialog.show();
					}
				} else {
					final AlertDialog alertDialog = new AlertDialog.Builder(
							CreateIdentityActivity.this).create();
					alertDialog.setCancelable(false);
					alertDialog
							.setMessage("An identity with the same name already exists.\n\nPlease choose a unique name.");
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
							getString(android.R.string.ok),
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							});
					alertDialog.show();
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_GENERATE_CAMERA_ENTROPY)
			if (resultCode == RESULT_OK
					|| resultCode == EntropyCameraActivity.RESULT_NO_CAMERA) {
				final String name = editTextName.getText().toString();
				final String password = editTextPassword.getText().toString();
				byte[] extraEntropyBytes = {};

				if (resultCode != EntropyCameraActivity.RESULT_NO_CAMERA)
					extraEntropyBytes = data
							.getByteArrayExtra(EntropyCameraActivity.EXTRA_ENTROPY_BYTES);

				final Identity identity = new Identity(name, password,
						extraEntropyBytes);
				try {
					identity.save(this);
					setResult(RESULT_OK);
				} catch (Exception e) {
					e.printStackTrace();
				}
				finish();
			} else {
				final AlertDialog alertDialog = new AlertDialog.Builder(
						CreateIdentityActivity.this).create();
				alertDialog.setCancelable(false);
				alertDialog
						.setMessage("Entropy generation sequence aborted.\n\nThe new identity was not stored.");
				alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL,
						getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
				alertDialog.show();
			}
	}
}
