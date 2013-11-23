package de.bwravencl.androsqrl.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Crypto {

	// XOR two byte arrays
	public static byte[] xor(byte[] a, byte[] b) {
		int l = a.length;
		byte[] out = new byte[l];

		for (int i = 0; i < l; i++) {
			out[i] = (byte) (a[i] ^ b[i]);
		}

		return out;
	}

	// Make a random byte array
	public static byte[] makeRandom(int len) {
		byte[] out = new byte[len];

		SecureRandom sr = new SecureRandom();
		sr.nextBytes(out);
		
		return out;
	}

	// calculate sha256
	public static byte[] sha256(byte[] input) {
		byte[] out = null;

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.reset();
			out = digest.digest(input);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return out;
	}

	// substring for bytes
	public static byte[] subByte(byte[] input, int start, int length) {
		byte[] out = new byte[length];
		System.arraycopy(input, start, out, 0, length);
		return out;
	}

	// concatenate two byte arrays
	public static byte[] ByteConcat(byte[] a, byte[] b) {
		byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
}