package de.bwravencl.androsqrl;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.bwravencl.androsqrl.R;

import de.bwravencl.androsqrl.model.Identity;
import eu.livotov.zxscan.ZXScanHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class LoginActivity extends Activity {

	public static final String EXTRA_IDENTITY = "EXTRA_IDENTITY";
	public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";
	public static final String EXTRA_URL = "EXTRA_URL";

	public static final int REQUEST_CREATE_IDENTITY = 1;
	public static final int REQUEST_SCAN_QR_CODE_FOR_IMPORT = 2;

	private final List<String> identityNames = new ArrayList<String>();

	private Spinner spinnerIdentity;
	private EditText editTextPassword;
	private Button buttonLogin;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		spinnerIdentity = (Spinner) findViewById(R.id.spinnerIdentity);
		editTextPassword = (EditText) findViewById(R.id.editTextPassword);
		editTextPassword
				.setOnEditorActionListener(new OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						doLogin();
						return false;
					}
				});

		buttonLogin = (Button) findViewById(R.id.buttonLogin);
		buttonLogin.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doLogin();
			}
		});

		// Check if an identity is existing
		if (Identity.getNumIdentities(this) == 0) {
			// If not open CreateIdentityActitvity
			Intent intent = new Intent(LoginActivity.this,
					CreateIdentityActivity.class);
			startActivityForResult(intent, REQUEST_CREATE_IDENTITY);
		}

		initSpinner();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.login, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_new_identity:
			// Open new activity to register new user
			final Intent intentNewIdentity = new Intent(LoginActivity.this,
					CreateIdentityActivity.class);
			startActivityForResult(intentNewIdentity, REQUEST_CREATE_IDENTITY);
			break;
		case R.id.action_delete_identity:
			doDeleteIdentity();
			break;
		case R.id.action_rename_identity:
			doRenameIdentityStep1();
			break;
		case R.id.action_change_password:
			doChangePasswordStep1();
			break;
		case R.id.action_export_identity:
			doExportIdentity();
			break;
		case R.id.action_import_identity:
			doImportIdentityStep1();
			break;
		case R.id.action_settings:
			// Open new activity to change settings
			final Intent intentSettings = new Intent(LoginActivity.this,
					SettingsActivity.class);
			startActivity(intentSettings);
			break;
		case R.id.action_about:
			Toast.makeText(this, "Not implemented yet!", Toast.LENGTH_SHORT)
					.show();
			break;
		default:
			break;
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CREATE_IDENTITY) {
			if (Identity.loadIdentityNames(this).size() == 0)
				finish();
			else
				initSpinner();
		} else if (requestCode == REQUEST_SCAN_QR_CODE_FOR_IMPORT
				&& resultCode == RESULT_OK) {
			final String importString = ZXScanHelper.getScannedCode(data);

			doImportIdentityStep2(importString);
		}
	}

	private void initSpinner() {
		identityNames.clear();

		for (String n : Identity.loadIdentityNames(this))
			identityNames.add(n);

		Collections.sort(identityNames);

		if (identityNames.size() < 2)
			spinnerIdentity.setEnabled(false);
		else
			spinnerIdentity.setEnabled(true);

		final ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, identityNames);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerIdentity.setAdapter(dataAdapter);
	}

	private Identity loadIdentity(String name, String password) {
		Identity identity = null;
		try {
			identity = Identity.load(this, name);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (identity != null && identity.deriveMasterKey(password)) {
			return identity;
		} else {
			return null;
		}
	}

	private void doLogin() {
		final String name = identityNames.get(spinnerIdentity
				.getSelectedItemPosition());
		final String password = editTextPassword.getText().toString();
		editTextPassword.setText(null);

		final Identity identity = loadIdentity(name, password);

		if (identity == null) {
			Toast.makeText(this, "You entered a wrong password!",
					Toast.LENGTH_LONG).show();
		} else {
			final Intent intentAuth = new Intent(this, AuthActivity.class);
			final Intent intentLogin = getIntent();
			final String action = intentLogin.getAction();

			intentAuth.putExtra(EXTRA_IDENTITY, identity);

			if (Intent.ACTION_VIEW.equals(action)) {
				final String url = intentLogin.getData().toString();
				intentAuth.putExtra(EXTRA_URL, url);
			} else {
				// Show toast multiple times to increase the duration
				for (int i = 0; i < 2; i++)
					Toast.makeText(this,
							"Please scan the SQRL code with your camera.",
							Toast.LENGTH_LONG).show();
			}

			startActivity(intentAuth);
		}
	}

	private void doDeleteIdentity() {
		final String name = identityNames.get(spinnerIdentity
				.getSelectedItemPosition());

		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewDelete = factory.inflate(
				R.layout.alert_dialog_password, null);
		final EditText editTextPasswordAlert = (EditText) identityViewDelete
				.findViewById(R.id.editTextPasswordAlert);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Do you really want to delete your identity '" + name
						+ "'?\n\nPlease enter your password:")
				.setCancelable(true)
				.setView(identityViewDelete)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String password = editTextPasswordAlert
										.getText().toString();
								final Identity identity = loadIdentity(name,
										password);

								if (identity == null) {
									Toast.makeText(
											LoginActivity.this,
											"You entered a wrong password!\nIdentity was not deleted.",
											Toast.LENGTH_LONG).show();
								} else {
									try {
										Identity.deleteIdentity(
												LoginActivity.this, name);
									} catch (Exception e) {
										e.printStackTrace();
									}

									Toast.makeText(LoginActivity.this,
											"Identity has been deleted!",
											Toast.LENGTH_LONG).show();

									restartActivity();
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builder.show();
	}

	private void doRenameIdentityStep1() {
		final String name = identityNames.get(spinnerIdentity
				.getSelectedItemPosition());

		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewPassword = factory.inflate(
				R.layout.alert_dialog_password, null);
		final EditText editTextPasswordAlert = (EditText) identityViewPassword
				.findViewById(R.id.editTextPasswordAlert);
		final AlertDialog.Builder builderPassword = new AlertDialog.Builder(
				this);
		builderPassword
				.setMessage(
						"Do you really want to rename your identity '" + name
								+ "'?\n\nPlease enter your password:")
				.setCancelable(true)
				.setView(identityViewPassword)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String password = editTextPasswordAlert
										.getText().toString();
								final Identity identity = loadIdentity(name,
										password);

								if (identity == null) {
									Toast.makeText(LoginActivity.this,
											"You entered a wrong password!",
											Toast.LENGTH_LONG).show();
								} else {
									doRenameIdentityStep2(identity);
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builderPassword.show();
	}

	private void doRenameIdentityStep2(final Identity identity) {
		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewName = factory.inflate(
				R.layout.alert_dialog_name, null);
		final EditText editTextNameAlert = (EditText) identityViewName
				.findViewById(R.id.editTextNameAlert);
		final AlertDialog.Builder builderName = new AlertDialog.Builder(
				LoginActivity.this);
		builderName
				.setMessage(
						"Please enter a new name for your identity '"
								+ identity.getName() + "':")
				.setCancelable(true)
				.setView(identityViewName)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String newName = editTextNameAlert
										.getText().toString();

								if (newName == null) {
									final AlertDialog alertDialog = new AlertDialog.Builder(
											LoginActivity.this).create();
									alertDialog.setCancelable(false);
									alertDialog
											.setMessage("The new name field must not be empty.\n\nPlease provide a valid name.");
									alertDialog
											.setButton(
													AlertDialog.BUTTON_NEUTRAL,
													getString(android.R.string.ok),
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															doRenameIdentityStep2(identity);
														}
													});
									alertDialog.show();
								} else if (Identity.loadIdentityNames(
										LoginActivity.this).contains(newName)) {
									final AlertDialog alertDialog = new AlertDialog.Builder(
											LoginActivity.this).create();
									alertDialog.setCancelable(false);
									alertDialog
											.setMessage("An identity with the same name already exists.\n\nPlease choose a unique name.");
									alertDialog
											.setButton(
													AlertDialog.BUTTON_NEUTRAL,
													getString(android.R.string.ok),
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															doRenameIdentityStep2(identity);
														}
													});
									alertDialog.show();
								} else {
									try {
										Identity.renameIdentity(
												LoginActivity.this,
												identity.getName(), newName);

										Toast.makeText(LoginActivity.this,
												"Identity has been renamed!",
												Toast.LENGTH_LONG).show();

										restartActivity();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builderName.show();
	}

	private void doChangePasswordStep1() {
		final String name = identityNames.get(spinnerIdentity
				.getSelectedItemPosition());

		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewPassword = factory.inflate(
				R.layout.alert_dialog_password, null);
		final EditText editTextPasswordAlert = (EditText) identityViewPassword
				.findViewById(R.id.editTextPasswordAlert);
		final AlertDialog.Builder builderPassword = new AlertDialog.Builder(
				this);
		builderPassword
				.setMessage(
						"Do you really want to change the password for your identity '"
								+ name
								+ "'?\n\nPlease enter your current password:")
				.setCancelable(true)
				.setView(identityViewPassword)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String password = editTextPasswordAlert
										.getText().toString();
								final Identity identity = loadIdentity(name,
										password);

								if (identity == null) {
									Toast.makeText(LoginActivity.this,
											"You entered a wrong password!",
											Toast.LENGTH_LONG).show();
								} else {
									doChangePasswordStep2(identity, password);
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builderPassword.show();
	}

	private void doChangePasswordStep2(final Identity identity,
			final String oldPassword) {
		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewNewPassword = factory.inflate(
				R.layout.alert_dialog_password, null);
		final EditText editTextNewPasswordAlert = (EditText) identityViewNewPassword
				.findViewById(R.id.editTextPasswordAlert);
		final AlertDialog.Builder builderNewPassword = new AlertDialog.Builder(
				LoginActivity.this);
		builderNewPassword
				.setMessage(
						"Please enter a new password for your identity '"
								+ identity.getName() + "':")
				.setCancelable(true)
				.setView(identityViewNewPassword)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String newPassword = editTextNewPasswordAlert
										.getText().toString();

								if (newPassword != null
										&& newPassword.length() >= Identity.MIN_PASSWORD_LENGTH) {
									try {
										identity.changePassword(
												LoginActivity.this,
												oldPassword, newPassword);

										Toast.makeText(LoginActivity.this,
												"Password has been changed!",
												Toast.LENGTH_LONG).show();

										restartActivity();
									} catch (GeneralSecurityException e) {
										e.printStackTrace();
									}
								} else {
									final AlertDialog alertDialog = new AlertDialog.Builder(
											LoginActivity.this).create();
									alertDialog.setCancelable(false);
									alertDialog
											.setMessage("The provided password does not comply with the minimum password length requirements.\n\nPlease choose a password with at least "
													+ Identity.MIN_PASSWORD_LENGTH
													+ " characters.");
									alertDialog
											.setButton(
													AlertDialog.BUTTON_NEUTRAL,
													getString(android.R.string.ok),
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															doChangePasswordStep2(
																	identity,
																	oldPassword);
														}
													});
									alertDialog.show();
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builderNewPassword.show();
	}

	private void doExportIdentity() {
		final String name = identityNames.get(spinnerIdentity
				.getSelectedItemPosition());

		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewExport = factory.inflate(
				R.layout.alert_dialog_password, null);
		final EditText editTextPasswordAlert = (EditText) identityViewExport
				.findViewById(R.id.editTextPasswordAlert);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Do you really want to export your identity '" + name
						+ "'?\n\nPlease enter your password:")
				.setCancelable(true)
				.setView(identityViewExport)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String password = editTextPasswordAlert
										.getText().toString();
								final Identity identity = loadIdentity(name,
										password);

								if (identity == null) {
									Toast.makeText(LoginActivity.this,
											"You entered a wrong password!",
											Toast.LENGTH_LONG).show();
								} else {
									final Intent intentExportIdentity = new Intent(
											LoginActivity.this,
											ExportIdentityActivity.class);
									intentExportIdentity.putExtra(
											EXTRA_IDENTITY, identity);
									intentExportIdentity.putExtra(
											EXTRA_PASSWORD, password);

									startActivity(intentExportIdentity);
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builder.show();
	}

	private void doImportIdentityStep1() {
		// Show toast multiple times to increase the duration
		for (int i = 0; i < 2; i++)
			Toast.makeText(this,
					"Please scan an Identity-QR-Code with your camera.",
					Toast.LENGTH_LONG).show();

		final SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		ZXScanHelper.setPlaySoundOnRead(sharedPreferences.getBoolean(
				"pref_title_scanner_beep", true));
		ZXScanHelper.setVibrateOnRead(sharedPreferences.getBoolean(
				"pref_title_scanner_vibrate", true));

		ZXScanHelper.scan(this, REQUEST_SCAN_QR_CODE_FOR_IMPORT);
	}

	private void doImportIdentityStep2(final String importString) {
		final LayoutInflater factory = LayoutInflater.from(this);
		final View identityViewName = factory.inflate(
				R.layout.alert_dialog_name, null);
		final EditText editTextNameAlert = (EditText) identityViewName
				.findViewById(R.id.editTextNameAlert);
		final AlertDialog.Builder builderName = new AlertDialog.Builder(
				LoginActivity.this);
		builderName
				.setMessage("Please enter a name for the imported identity:")
				.setCancelable(true)
				.setView(identityViewName)
				.setPositiveButton(getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								final String name = editTextNameAlert.getText()
										.toString();

								if (name == null) {
									final AlertDialog alertDialog = new AlertDialog.Builder(
											LoginActivity.this).create();
									alertDialog.setCancelable(false);
									alertDialog
											.setMessage("The new name field must not be empty.\n\nPlease provide a valid name.");
									alertDialog
											.setButton(
													AlertDialog.BUTTON_NEUTRAL,
													getString(android.R.string.ok),
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															doImportIdentityStep2(importString);
														}
													});
									alertDialog.show();
								} else if (Identity.loadIdentityNames(
										LoginActivity.this).contains(name)) {
									final AlertDialog alertDialog = new AlertDialog.Builder(
											LoginActivity.this).create();
									alertDialog.setCancelable(false);
									alertDialog
											.setMessage("An identity with the same name already exists.\n\nPlease choose a unique name.");
									alertDialog
											.setButton(
													AlertDialog.BUTTON_NEUTRAL,
													getString(android.R.string.ok),
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(
																DialogInterface dialog,
																int which) {
															doImportIdentityStep2(importString);
														}
													});
									alertDialog.show();
								} else {
									try {
										final Identity importIdentity = Identity
												.getIdentityFromString(name,
														importString);
										importIdentity.save(LoginActivity.this);

										Toast.makeText(LoginActivity.this,
												"Identity has been imported!",
												Toast.LENGTH_LONG).show();

										restartActivity();
									} catch (Exception e) {
										e.printStackTrace();

										Toast.makeText(LoginActivity.this,
												"Error: Import unsucessful!",
												Toast.LENGTH_LONG).show();
									}
								}
							}
						})
				.setNegativeButton(getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.dismiss();
							}
						});
		builderName.show();
	}

	private void restartActivity() {
		final Intent intent = getIntent();
		finish();
		startActivity(intent);
	}
}
