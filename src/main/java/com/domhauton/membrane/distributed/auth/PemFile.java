package com.domhauton.membrane.distributed.auth;

/**
 * Created by dominic on 11/02/17.
 */

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.security.Key;
import java.security.cert.Certificate;

public class PemFile {

    private Object pemObject;

    PemFile(Key key, String description) {
        pemObject = new PemObject(description, key.getEncoded());
    }

    PemFile(Certificate certificate) {
        this.pemObject = pemObject;
    }

    public void write(Path filename) throws IOException {
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(filename.toFile());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                JcaPEMWriter pemWriter = new JcaPEMWriter(outputStreamWriter))
        {
            pemWriter.writeObject(this.pemObject);
        }
    }


}