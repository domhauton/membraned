package com.domhauton.membrane.prospector;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * Created by dominic on 26/01/17.
 */
public class ManagedFile {
    private final Path filePath;
    private HashCode crc32;

    public ManagedFile(Path filePath) {
        this.filePath = filePath;
    }

    private HashCode hashFile() throws IOException {
        File loadedFile = filePath.toFile();
        return Files.hash(loadedFile, Hashing.md5());
    }
}
