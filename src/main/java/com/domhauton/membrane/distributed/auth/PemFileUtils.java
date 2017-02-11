package com.domhauton.membrane.distributed.auth;

/**
 * Created by dominic on 11/02/17.
 */

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.security.Key;
import java.security.cert.Certificate;

public abstract class PemFileUtils {
    static void write(Path filename, Key key, String description) throws IOException {
        PemObject pemObject = new PemObject(description, key.getEncoded());
        write(filename, pemObject);
    }

    static void write(Path filename, Certificate certificate) throws IOException {
        write(filename, (Object) certificate);
    }

    private static void write(Path filename, Object pemObject) throws IOException {
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(filename.toFile());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                JcaPEMWriter pemWriter = new JcaPEMWriter(outputStreamWriter)) {
            pemWriter.writeObject(pemObject);
        }
    }

    static PemObject read(Path filename) throws IOException {
        try (
                FileInputStream fileInputStream = new FileInputStream(filename.toFile());
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                PEMParser pemParser = new PEMParser(inputStreamReader)) {
            return pemParser.readPemObject();
        }
    }
}