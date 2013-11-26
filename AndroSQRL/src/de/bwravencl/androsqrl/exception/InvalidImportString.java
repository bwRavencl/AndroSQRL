package de.bwravencl.androsqrl.exception;

public class InvalidImportString extends Exception {

	private static final long serialVersionUID = 3207833127208251707L;

	public InvalidImportString(String importString) {
		super("Invalid String '" + importString + "'");
	}
}
