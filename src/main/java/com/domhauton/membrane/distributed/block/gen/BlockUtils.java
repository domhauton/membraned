package com.domhauton.membrane.distributed.block.gen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
abstract class BlockUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NON_PRIVATE);

  static BlockContainer bytes2Block(byte[] encryptedBlockData, String key) throws BlockException {
    try {
      byte[] plainMessage = decrypt(encryptedBlockData, key);
      return objectMapper.readValue(plainMessage, BlockContainer.class);
    } catch (IOException e) {
      throw new BlockException("Could not decode shard. Error: " + e.getMessage(), e);
    }
  }

  static byte[] block2Bytes(BlockContainer block, String key) throws BlockException {
    try {
      byte[] bytes = objectMapper.writeValueAsString(block).getBytes();
      return encrypt(bytes, key);
    } catch (JsonProcessingException e) { // Should never be spontaneously generated
      throw new BlockException("Could not encode shard. Error: " + e.getMessage(), e);
    }
  }

  static byte[] compress(byte[] data) throws BlockException {
    try {
      byte[] compressedData = Snappy.compress(data);
      if (compressedData.length >= data.length) {
        throw new IOException("Compression ineffective.");
      } else {
        return compressedData;
      }
    } catch (IOException e) {
      throw new BlockException("Compression stopped.", e);
    }
  }

  static byte[] decompress(byte[] compressed) throws BlockException {
    try {
      return Snappy.uncompress(compressed);
    } catch (IOException e) {
      throw new BlockException("Decompression not possible.", e);
    }
  }

  static byte[] encrypt(byte[] toEncrypt, String password) throws BlockException {
    PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new TwofishEngine()));
    KeyParameter keyParameter = new KeyParameter(password2bytes(password));
    cipher.init(true, keyParameter);
    return cipherData(cipher, toEncrypt);
  }

  static byte[] decrypt(byte[] toDecrypt, String password) throws BlockException {
    PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new TwofishEngine()));
    KeyParameter key = new KeyParameter(password2bytes(password));
    cipher.init(false, key);
    return cipherData(cipher, toDecrypt);
  }

  private static byte[] cipherData(PaddedBufferedBlockCipher cipher, byte[] data) throws BlockException {
    int minSize = cipher.getOutputSize(data.length);
    byte[] outBuf = new byte[minSize];
    int length1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
    try {
      int length2 = cipher.doFinal(outBuf, length1);
      int actualLength = length1 + length2;
      byte[] result = new byte[actualLength];
      System.arraycopy(outBuf, 0, result, 0, result.length);
      return result;
    } catch (Exception e) {
      throw new BlockException("Unable to encrypt/decrypt the block data.", e);
    }
  }

  private static byte[] password2bytes(String key) {
    return Hashing.sha256().hashBytes(key.getBytes()).asBytes();
  }
}
