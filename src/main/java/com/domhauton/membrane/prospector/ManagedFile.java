package com.domhauton.membrane.prospector;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteProcessor;
import com.google.common.io.ByteSource;
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
    private final ByteSource byteSource;

    public ManagedFile(Path filePath) {
        this.filePath = filePath;
        byteSource = Files.asByteSource(filePath.toFile());
    }

    public byte[] getBytes() throws IOException {
        return byteSource.read();
    }

    private HashCode getStrongHash() throws IOException {
        return Hashing.md5().hashBytes(getBytes());
    }
}
