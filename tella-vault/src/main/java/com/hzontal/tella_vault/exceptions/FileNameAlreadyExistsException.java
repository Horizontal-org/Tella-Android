package com.hzontal.tella_vault.exceptions;

/**
 * Thrown when a file or folder with the same name already exists in the same parent.
 * Unchecked so it can propagate from vault layer without changing interface signatures.
 */
public class FileNameAlreadyExistsException extends RuntimeException {

    public FileNameAlreadyExistsException(String message) {
        super(message);
    }

    public FileNameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
