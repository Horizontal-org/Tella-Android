package com.hzontal.tella_vault;

public class VaultException extends Exception {
    public VaultException() {
    }

    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }

    public VaultException(Throwable cause) {
        super(cause);
    }
}
