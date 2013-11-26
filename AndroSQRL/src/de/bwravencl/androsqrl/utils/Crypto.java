package de.bwravencl.androsqrl.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {

	// XOR two byte arrays
	public static byte[] xor(byte[] a, byte[] b) {
		final int l = a.length;
		byte[] out = new byte[l];

		for (int i = 0; i < l; i++) {
			out[i] = (byte) (a[i] ^ b[i]);
		}

		return out;
	}

	// Make a random byte array
	public static byte[] makeRandom(int len) {
		final byte[] out = new byte[len];

		final SecureRandom sr = new SecureRandom();
		sr.nextBytes(out);

		return out;
	}

	// calculate sha256
	public static byte[] sha256(byte[] input) {
		byte[] out = null;

		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			out = digest.digest(input);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return out;
	}

	// substring for bytes
	public static byte[] subByte(byte[] input, int start, int length) {
		final byte[] out = new byte[length];
		System.arraycopy(input, start, out, 0, length);

		return out;
	}

	// concatenate two byte arrays
	public static byte[] concatBytes(byte[] a, byte[] b) {
		final byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);

		return c;
	}

	// zeros a byte array
	public static void zeroByte(byte[] a) {
		for (int i = a.length - 1; i >= 0; --i) {
			a[i] = 0;
		}
	}
}
