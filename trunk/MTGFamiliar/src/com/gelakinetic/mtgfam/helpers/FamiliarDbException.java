package com.gelakinetic.mtgfam.helpers;

/*
 * Extend Exception instead of RuntimeException to force the compiler to whine
 * about lack of try/catch blocks
 */
public class FamiliarDbException extends Exception {

	public Exception innerException;
	public FamiliarDbException(Exception e) {
		this.innerException = e;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 5953780555438726164L;

}
