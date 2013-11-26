package de.bwravencl.androsqrl.exception;

public class DuplicateIdentityNameException extends Exception {

	private static final long serialVersionUID = 5302304073498136746L;

	public DuplicateIdentityNameException(String name) {
		super("Identity with name='" + name + "' already existing");
	}
}
