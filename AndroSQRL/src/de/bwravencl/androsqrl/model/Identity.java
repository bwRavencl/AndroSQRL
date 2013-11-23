package de.bwravencl.androsqrl.model;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.lambdaworks.crypto.SCrypt;

import de.bwravencl.androsqrl.utils.Crypto;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Base64;

public class Identity implements Parcelable {

	public static final String PREFERENCES_IDENTITY_NAMES = "PREFERENCES_IDENTITY_NAMES";
	public static final String PREFERENCES_IDENTITY_MASTERKEY = "PREFERENCES_IDENTITY_MASTERKEY";
	public static final String PREFERENCES_IDENTITY_MIXKEY = "PREFERENCES_IDENTITY_MIXKEY";
	public static final String PREFERENCES_IDENTITY_SALT = "PREFERENCES_IDENTITY_SALT";
	public static final String PREFERENCES_IDENTITY_VERIFIER = "PREFERENCES_IDENTITY_VERIFIER";
	public static final String PREFERENCES_IDENTITY_ITERATIONS = "PREFERENCES_IDENTITY_ITERATIONS";
	public static final String PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N = "PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N";
	public static final String PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r = "PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r";
	public static final String PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p = "PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p";
	public static final String PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen = "PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen";

	public static final int MIN_PASSWORD_LENGTH = 6;

	public static final int SCRYPT_NORMAL_PARAMETERS_N = 16;
	public static final int SCRYPT_NORMAL_PARAMETERS_r = 8;
	public static final int SCRYPT_NORMAL_PARAMETERS_p = 12;
	public static final int SCRYPT_NORMAL_PARAMETERS_dkLen = 32;

	public static final int SCRYPT_EXPORT_PARAMETERS_N = 18;
	public static final int SCRYPT_EXPORT_PARAMETERS_r = 8;
	public static final int SCRYPT_EXPORT_PARAMETERS_p = 90;
	public static final int SCRYPT_EXPORT_PARAMETERS_dkLen = SCRYPT_NORMAL_PARAMETERS_dkLen;

	public static final Parcelable.Creator<Identity> CREATOR = new Parcelable.Creator<Identity>() {
		public Identity createFromParcel(Parcel in) {
			return new Identity(in);
		}

		public Identity[] newArray(int size) {
			return new Identity[size];
		}
	};

	private String name;
	private byte[] masterkey = new byte[32]; // gets derived from mixKey and
												// password
	private byte[] mixkey = new byte[32]; // The key that is XORed to make the
											// master key
	private byte[] salt = new byte[8]; // Salt to be mixed with the password
	private byte[] verifier = new byte[32]; // Used for verifying the password

	private int scryptParameterN = SCRYPT_NORMAL_PARAMETERS_N;
	private int scryptParameterR = SCRYPT_NORMAL_PARAMETERS_r;
	private int scryptParameterP = SCRYPT_NORMAL_PARAMETERS_p;
	private int scryptParameterDkLen = SCRYPT_NORMAL_PARAMETERS_dkLen;

	// Constructor used when creating a new identity from scratch
	public Identity(String name, String password, byte[] extraEntropyBytes) {
		this.name = name;

		mixkey = Crypto.sha256(Crypto.ByteConcat(Crypto.makeRandom(30),
				extraEntropyBytes));
		final byte[] salt256 = Crypto.sha256(Crypto.makeRandom(30));
		salt = Crypto.subByte(salt256, 0, 8);

		try {
			verifier = Crypto
					.sha256(SCrypt.scrypt(password.getBytes(), salt,
							SCRYPT_NORMAL_PARAMETERS_N,
							SCRYPT_NORMAL_PARAMETERS_r,
							SCRYPT_NORMAL_PARAMETERS_p,
							SCRYPT_NORMAL_PARAMETERS_dkLen));
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

	// Constructor used when loading an existing identity from storage
	public Identity(String name, byte[] mixkey, byte[] salt, byte[] verifier,
			int iterations, int scryptParameterN, int scryptParameterR,
			int scryptParameterP, int scryptParameterDkLen) {
		this.name = name;
		this.mixkey = mixkey;
		this.salt = salt;
		this.verifier = verifier;
		this.scryptParameterN = scryptParameterN;
		this.scryptParameterR = scryptParameterR;
		this.scryptParameterP = scryptParameterP;
		this.scryptParameterDkLen = scryptParameterDkLen;
	}

	public Identity(Parcel in) {
		name = in.readString();
		in.readByteArray(masterkey);
		in.readByteArray(mixkey);
		in.readByteArray(salt);
		in.readByteArray(verifier);
		scryptParameterN = in.readInt();
		scryptParameterR = in.readInt();
		scryptParameterP = in.readInt();
		scryptParameterDkLen = in.readInt();
	}

	public String getName() {
		return name;
	}

	public byte[] getMasterkey() {
		return masterkey;
	}

	public boolean deriveMasterKey(String password) {
		if (password == null || password.length() == 0)
			return false;

		byte[] scryptResult = {};
		try {
			scryptResult = SCrypt.scrypt(password.getBytes(), salt,
					scryptParameterN, scryptParameterR, scryptParameterP,
					scryptParameterDkLen);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			return false;
		}

		byte[] passwordCheck = Crypto.sha256(scryptResult);
		boolean passwordCheckSuccess = Arrays.equals(passwordCheck, verifier);
		if (!passwordCheckSuccess)
			return false;

		masterkey = Crypto.xor(mixkey, scryptResult);
		return true;
	}

	public void changePassword(Context context, String oldPassword,
			String newPassword) throws GeneralSecurityException {
		if (oldPassword == null || oldPassword.length() == 0
				|| newPassword == null || newPassword.length() == 0
				|| oldPassword.equals(newPassword))
			return;

		if (deriveMasterKey(oldPassword)) {
			final byte[] salt256 = Crypto.sha256(Crypto.makeRandom(30));
			salt = Crypto.subByte(salt256, 0, 8);

			byte[] newScryptResult = SCrypt.scrypt(newPassword.getBytes(),
					salt, scryptParameterN, scryptParameterR, scryptParameterP,
					scryptParameterDkLen);
			verifier = Crypto.sha256(newScryptResult);

			mixkey = Crypto.xor(masterkey, newScryptResult);

			final SharedPreferences sharedPreferences = getSharedPreferences(context);
			final Editor editor = sharedPreferences.edit();

			editor.putString(PREFERENCES_IDENTITY_MIXKEY + "_" + name,
					Base64.encodeToString(mixkey, Base64.DEFAULT));
			editor.putString(PREFERENCES_IDENTITY_SALT + "_" + name,
					Base64.encodeToString(salt, Base64.DEFAULT));
			editor.putString(PREFERENCES_IDENTITY_VERIFIER + "_" + name,
					Base64.encodeToString(verifier, Base64.DEFAULT));

			editor.commit();
		}
	}

	public static Set<String> loadIdentityNames(Context context) {
		return getSharedPreferences(context).getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());
	}

	// Load from storage
	public static Identity load(Context context, String name) throws Exception {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, null);

		if (identityNames == null || !identityNames.contains(name))
			throw new Exception("Identity with name='" + name + "' not found");

		byte[] mixkey = Base64.decode(
				sharedPreferences.getString(PREFERENCES_IDENTITY_MIXKEY + "_"
						+ name, null), Base64.DEFAULT);
		byte[] salt = Base64.decode(
				sharedPreferences.getString(PREFERENCES_IDENTITY_SALT + "_"
						+ name, null), Base64.DEFAULT);
		byte[] verifier = Base64.decode(
				sharedPreferences.getString(PREFERENCES_IDENTITY_VERIFIER + "_"
						+ name, null), Base64.DEFAULT);
		int iterations = sharedPreferences.getInt(
				PREFERENCES_IDENTITY_ITERATIONS + "_" + name, 0);

		int scryptParameterN = sharedPreferences.getInt(
				PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N + "_" + name,
				SCRYPT_NORMAL_PARAMETERS_N);
		int scryptParameterR = sharedPreferences.getInt(
				PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r + "_" + name,
				SCRYPT_NORMAL_PARAMETERS_r);
		int scryptParameterP = sharedPreferences.getInt(
				PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p + "_" + name,
				SCRYPT_NORMAL_PARAMETERS_p);
		int scryptParameterDkLen = sharedPreferences.getInt(
				PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen + "_"
						+ name, SCRYPT_NORMAL_PARAMETERS_dkLen);

		return new Identity(name, mixkey, salt, verifier, iterations,
				scryptParameterN, scryptParameterR, scryptParameterP,
				scryptParameterDkLen);

	}

	public static void deleteIdentity(Context context, String name)
			throws Exception {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);
		final Editor editor = sharedPreferences.edit();

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());

		if (identityNames == null || !identityNames.contains(name))
			throw new Exception("Identity with name='" + name + "' not found");

		identityNames.remove(name);
		editor.putStringSet(PREFERENCES_IDENTITY_NAMES, identityNames);

		editor.remove(PREFERENCES_IDENTITY_MIXKEY + "_" + name);
		editor.remove(PREFERENCES_IDENTITY_SALT + "_" + name);
		editor.remove(PREFERENCES_IDENTITY_VERIFIER + "_" + name);
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N + "_"
				+ name);
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r + "_"
				+ name);
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p + "_"
				+ name);
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen + "_"
				+ name);

		editor.apply();
	}

	public static void renameIdentity(Context context, String oldName,
			String newName) throws Exception {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);
		final Editor editor = sharedPreferences.edit();

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());

		if (identityNames == null || !identityNames.contains(oldName))
			throw new Exception("Identity with name='" + oldName
					+ "' not found");

		if (identityNames.contains(newName))
			throw new Exception("Identity with name='" + newName
					+ "' already existing");

		identityNames.remove(oldName);
		identityNames.add(newName);
		editor.putStringSet(PREFERENCES_IDENTITY_NAMES, identityNames);

		editor.putString(
				PREFERENCES_IDENTITY_MIXKEY + "_" + newName,
				sharedPreferences.getString(PREFERENCES_IDENTITY_MIXKEY + "_"
						+ oldName, null));
		editor.remove(PREFERENCES_IDENTITY_MIXKEY + "_" + oldName);

		editor.putString(
				PREFERENCES_IDENTITY_SALT + "_" + newName,
				sharedPreferences.getString(PREFERENCES_IDENTITY_SALT + "_"
						+ oldName, null));
		editor.remove(PREFERENCES_IDENTITY_SALT + "_" + oldName);

		editor.putString(
				PREFERENCES_IDENTITY_VERIFIER + "_" + newName,
				sharedPreferences.getString(PREFERENCES_IDENTITY_VERIFIER + "_"
						+ oldName, null));
		editor.remove(PREFERENCES_IDENTITY_VERIFIER + "_" + oldName);

		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N + "_"
				+ newName, sharedPreferences
				.getInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N + "_"
						+ oldName, SCRYPT_NORMAL_PARAMETERS_N));
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N + "_"
				+ oldName);

		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r + "_"
				+ newName, sharedPreferences
				.getInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r + "_"
						+ oldName, SCRYPT_NORMAL_PARAMETERS_r));
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r + "_"
				+ oldName);

		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p + "_"
				+ newName, sharedPreferences
				.getInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p + "_"
						+ oldName, SCRYPT_NORMAL_PARAMETERS_p));
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p + "_"
				+ oldName);

		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen + "_"
				+ newName, sharedPreferences.getInt(
				PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen + "_"
						+ oldName, SCRYPT_NORMAL_PARAMETERS_dkLen));
		editor.remove(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen + "_"
				+ oldName);

		editor.apply();
	}

	// Saves a newly created identity to storage
	public void save(Context context) throws Exception {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);
		final Editor editor = sharedPreferences.edit();

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());

		if (identityNames.contains(name))
			throw new Exception("Identity with name='" + name
					+ "' already existing");

		identityNames.add(name);
		editor.putStringSet(PREFERENCES_IDENTITY_NAMES, identityNames);
		editor.putString(PREFERENCES_IDENTITY_MIXKEY + "_" + name,
				Base64.encodeToString(mixkey, Base64.DEFAULT));
		editor.putString(PREFERENCES_IDENTITY_SALT + "_" + name,
				Base64.encodeToString(salt, Base64.DEFAULT));
		editor.putString(PREFERENCES_IDENTITY_VERIFIER + "_" + name,
				Base64.encodeToString(verifier, Base64.DEFAULT));
		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_N + "_"
				+ name, scryptParameterN);
		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_r + "_"
				+ name, scryptParameterR);
		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_p + "_"
				+ name, scryptParameterP);
		editor.putInt(PREFERENCES_IDENTITY_SCRYPT_NORMAL_PARAMETERS_dkLen + "_"
				+ name, scryptParameterDkLen);

		editor.commit();
	}

	public static int getNumIdentities(Context context) {
		return loadIdentityNames(context).size();
	}

	public void backupToFile() {

	}

	public void restoreFromFile() {
		// Load form imported file
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeByteArray(masterkey);
		dest.writeByteArray(mixkey);
		dest.writeByteArray(salt);
		dest.writeByteArray(verifier);
		dest.writeInt(scryptParameterN);
		dest.writeInt(scryptParameterR);
		dest.writeInt(scryptParameterP);
		dest.writeInt(scryptParameterDkLen);
	}

	private static SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
}
