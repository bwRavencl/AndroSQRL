package de.bwravencl.androsqrl.model;

import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.lambdaworks.crypto.SCrypt;

import de.bwravencl.androsqrl.exception.DuplicateIdentityNameException;
import de.bwravencl.androsqrl.exception.IdentityNotFoundException;
import de.bwravencl.androsqrl.exception.InvalidImportString;
import de.bwravencl.androsqrl.exception.WrongPasswordException;
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

	public static final int SCRYPT_EXPORT_PARAMETERS_N = 32;
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

		mixkey = Crypto.sha256(Crypto.concatBytes(Crypto.makeRandom(30),
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
			int scryptParameterN, int scryptParameterR, int scryptParameterP,
			int scryptParameterDkLen) {
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

	// Zero the masterkey so it does not reside in memory
	public void clearMasterKey() {
		if (masterkey != null)
			Crypto.zeroByte(masterkey);
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
			clearMasterKey();

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
	public static Identity load(Context context, String name)
			throws IdentityNotFoundException {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, null);

		if (identityNames == null || !identityNames.contains(name))
			throw new IdentityNotFoundException(name);

		byte[] mixkey = Base64.decode(
				sharedPreferences.getString(PREFERENCES_IDENTITY_MIXKEY + "_"
						+ name, null), Base64.DEFAULT);
		byte[] salt = Base64.decode(
				sharedPreferences.getString(PREFERENCES_IDENTITY_SALT + "_"
						+ name, null), Base64.DEFAULT);
		byte[] verifier = Base64.decode(
				sharedPreferences.getString(PREFERENCES_IDENTITY_VERIFIER + "_"
						+ name, null), Base64.DEFAULT);

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

		return new Identity(name, mixkey, salt, verifier, scryptParameterN,
				scryptParameterR, scryptParameterP, scryptParameterDkLen);

	}

	public static void deleteIdentity(Context context, String name)
			throws IdentityNotFoundException {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);
		final Editor editor = sharedPreferences.edit();

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());

		if (identityNames == null || !identityNames.contains(name))
			throw new IdentityNotFoundException(name);

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
			String newName) throws IdentityNotFoundException {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);
		final Editor editor = sharedPreferences.edit();

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());

		if (identityNames == null || !identityNames.contains(oldName))
			throw new IdentityNotFoundException(oldName);

		if (identityNames.contains(newName))
			throw new IdentityNotFoundException(newName);

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
	public void save(Context context) throws DuplicateIdentityNameException {
		final SharedPreferences sharedPreferences = getSharedPreferences(context);
		final Editor editor = sharedPreferences.edit();

		final Set<String> identityNames = sharedPreferences.getStringSet(
				PREFERENCES_IDENTITY_NAMES, new HashSet<String>());

		if (identityNames.contains(name))
			throw new DuplicateIdentityNameException(name);

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

	public String getExportString(String password) {
		if (deriveMasterKey(password)) {
			final byte[] salt256 = Crypto.sha256(Crypto.makeRandom(30));
			final byte[] exportPasswordSalt = Crypto.subByte(salt256, 0, 8);

			byte[] exportScryptResult = {};
			try {
				exportScryptResult = SCrypt.scrypt(password.getBytes(),
						exportPasswordSalt, SCRYPT_EXPORT_PARAMETERS_N,
						SCRYPT_EXPORT_PARAMETERS_r, SCRYPT_EXPORT_PARAMETERS_p,
						SCRYPT_EXPORT_PARAMETERS_dkLen);
			} catch (GeneralSecurityException e) {
				e.printStackTrace();
			}

			final byte[] exportVerifier = Crypto.sha256(exportScryptResult);

			final byte[] exportMixKey = Crypto.xor(masterkey,
					exportScryptResult);
			clearMasterKey();

			return Base64.encodeToString(exportMixKey, Base64.DEFAULT) + " "
					+ Base64.encodeToString(exportPasswordSalt, Base64.DEFAULT)
					+ " "
					+ Base64.encodeToString(exportVerifier, Base64.DEFAULT)
					+ " " + SCRYPT_EXPORT_PARAMETERS_N + " "
					+ SCRYPT_EXPORT_PARAMETERS_r + " "
					+ SCRYPT_EXPORT_PARAMETERS_p + " "
					+ SCRYPT_EXPORT_PARAMETERS_dkLen;
		} else
			return null;
	}

	public static Identity getIdentityFromString(String name, String password,
			String importString) throws InvalidImportString,
			WrongPasswordException {
		final String[] strings = importString.split(" ");

		// Basic validity check
		if (strings.length != 7)
			throw new InvalidImportString(importString);

		final byte[] importedMixKey = Base64.decode(strings[0], Base64.DEFAULT);
		final byte[] importedSalt = Base64.decode(strings[1], Base64.DEFAULT);
		final byte[] importedVerifier = Base64.decode(strings[2],
				Base64.DEFAULT);
		final int importedScryptParameterN = Integer.parseInt(strings[3]);
		final int importedScryptParameterR = Integer.parseInt(strings[4]);
		final int importedScryptParameterP = Integer.parseInt(strings[5]);
		final int importedScryptParameterDkLen = Integer.parseInt(strings[6]);

		// Some more simple validity checks
		if (importedMixKey == null || importedSalt == null
				|| importedVerifier == null
				|| importedScryptParameterN % 2 != 0)
			throw new InvalidImportString(importString);

		final Identity importedIdentity = new Identity(name, importedMixKey,
				importedSalt, importedVerifier, importedScryptParameterN,
				importedScryptParameterR, importedScryptParameterP,
				importedScryptParameterDkLen);

		if (!importedIdentity.deriveMasterKey(password))
			throw new WrongPasswordException();

		// Convert from export to normal scrypt parameters:
		final byte[] salt256 = Crypto.sha256(Crypto.makeRandom(30));
		final byte[] newPasswordSalt = Crypto.subByte(salt256, 0, 8);

		byte[] newScryptResult = {};
		try {
			newScryptResult = SCrypt.scrypt(password.getBytes(),
					newPasswordSalt, SCRYPT_NORMAL_PARAMETERS_N,
					SCRYPT_NORMAL_PARAMETERS_r, SCRYPT_NORMAL_PARAMETERS_p,
					SCRYPT_NORMAL_PARAMETERS_dkLen);
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}

		final byte[] newVerifier = Crypto.sha256(newScryptResult);

		final byte[] newMixKey = Crypto.xor(importedIdentity.getMasterkey(),
				newScryptResult);
		importedIdentity.clearMasterKey();

		return new Identity(name, newMixKey, newPasswordSalt, newVerifier,
				SCRYPT_NORMAL_PARAMETERS_N, SCRYPT_NORMAL_PARAMETERS_r,
				SCRYPT_NORMAL_PARAMETERS_p, SCRYPT_NORMAL_PARAMETERS_dkLen);
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
