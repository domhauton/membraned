package com.domhauton.membrane.storage.metadata;

/**
 * Created by dominic on 30/01/17.
 */
public enum FileOperation {
    ADD(FileOperation.REMOVE),
    REMOVE(FileOperation.ADD);

    private FileOperation reverse;

    FileOperation(FileOperation reverse) {
        this.reverse = reverse;
    }

    public FileOperation reverse() {
        return reverse;
    }
}
