package com.domhauton.membrane.distributed.auth;

import com.google.common.base.Objects;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Created by dominic on 13/02/17.
 */
public class MembraneAuthInfo {

    static final String INNER_PATH = File.separator + "auth";

    private static final String RSA_PRIVATE_FILE_DESCRIPTION = "RSA PRIVATE KEY";
    private static final String RSA_PUBIC_FILE_DESCRIPTION = "RSA PUBLIC KEY";

    private static final String RSA_PUBIC_FILE_NAME = "membrane_rsa.pub";
    private static final String RSA_PRIVATE_FILE_NAME = "membrane_rsa";
    private static final String CERT_FILE_NAME = "membrane.cert";

    private final X509Certificate x509Certificate;
    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private byte[] x509CertificateEncoded;
    private byte[] privateKeyEncoded;

    public MembraneAuthInfo(X509Certificate x509Certificate, RSAPublicKey publicKey, RSAPrivateKey privateKey) throws AuthException {
        this.x509Certificate = x509Certificate;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        try {
            this.x509CertificateEncoded = getEncodedCertificate(x509Certificate);
            this.privateKeyEncoded = getEncodedPrivateKey(privateKey);
        } catch (Exception e) {
            throw new AuthException("Could not encode cert to bytes. " + e.getMessage());
        }
    }

    public MembraneAuthInfo(Path path) throws AuthException {
        Path fullPath = Paths.get(path.toString() + INNER_PATH);
        Path certPath = Paths.get(fullPath + File.separator + CERT_FILE_NAME);
        Path privPath = Paths.get(fullPath + File.separator + RSA_PRIVATE_FILE_NAME);
        Path pubPath = Paths.get(fullPath + File.separator + RSA_PUBIC_FILE_NAME);
        this.x509Certificate = AuthFileUtils.loadCertificate(certPath);
        this.privateKey = AuthFileUtils.loadPrivateKey(privPath);
        this.publicKey = AuthFileUtils.loadPublicKey(pubPath);
    }

    private byte[] getEncodedPrivateKey(RSAPrivateKey privateKey) throws IOException {
        StringWriter privateKeyOut = new StringWriter();
        JcaPEMWriter privateWriter = new JcaPEMWriter(privateKeyOut);
        // Vert.x requires exactly "PRIVATE KEY"
        privateWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
        privateWriter.close();
        return privateKeyOut.toString().getBytes();
    }

    public byte[] getEncodedPrivateKey() {
        return privateKeyEncoded;
    }

    private byte[] getEncodedCertificate(X509Certificate certificate) throws IOException, CertificateEncodingException {
        StringWriter privateKeyOut = new StringWriter();
        JcaPEMWriter privateWriter = new JcaPEMWriter(privateKeyOut);
        // Vert.x requires exactly "CERTIFICATE"
        privateWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
        privateWriter.close();
        return privateKeyOut.toString().getBytes();
    }

    public byte[] getEncodedCert() {
        return x509CertificateEncoded;
    }

    X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    public void write(Path path) throws IOException {
        Path fullPath = Paths.get(path.toString() + INNER_PATH);

        Path certPath = Paths.get(fullPath + File.separator + CERT_FILE_NAME);
        Path privPath = Paths.get(fullPath + File.separator + RSA_PRIVATE_FILE_NAME);
        Path pubPath = Paths.get(fullPath + File.separator + RSA_PUBIC_FILE_NAME);

        AuthFileUtils.writePrivateKey(privPath, privateKey);
        AuthFileUtils.writePublicKey(pubPath, publicKey);
        AuthFileUtils.writeCertificate(certPath, x509Certificate);
    }

    @Override
    public String toString() {
        return "MembraneAuthInfo{" +
                "x509Certificate=" + new String(x509Certificate.getSignature()) +
                ", publicKey=" + new String(publicKey.getEncoded()) +
                ", privateKey=" + new String(privateKey.getEncoded()) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MembraneAuthInfo that = (MembraneAuthInfo) o;
        return Objects.equal(x509Certificate, that.x509Certificate) &&
                Objects.equal(publicKey, that.publicKey) &&
                Objects.equal(privateKey, that.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(x509Certificate, publicKey, privateKey);
    }
}