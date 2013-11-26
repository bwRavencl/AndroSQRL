package de.bwravencl.androsqrl.exception;

public class IdentityNotFoundException extends Exception {

	private static final long serialVersionUID = -3356606023639699116L;

	public IdentityNotFoundException(String name) {
		super("Identity with name='" + name + "' not found");
	}
}
