package com.domhauton.membrane.distributed.auth;

/**
 * Created by dominic on 11/02/17.
 */

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.security.Key;


public class PemFile {

    private PemObject pemObject;

    PemFile(Key key, String description) {
        this.pemObject = new PemObject(description, key.getEncoded());
    }

    public void write(Path filename) throws IOException {
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(filename.toFile());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                PemWriter pemWriter = new PemWriter(outputStreamWriter))
        {
            pemWriter.writeObject(this.pemObject);
        }
    }


}