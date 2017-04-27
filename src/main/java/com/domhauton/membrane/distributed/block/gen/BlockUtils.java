package com.domhauton.membrane.distributed.block.gen;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Dominic Hauton on 08/03/17.
 */
abstract class BlockUtils {
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NON_PRIVATE);
  private static final LZ4Factory LZ_4_FACTORY = LZ4Factory.fastestInstance();

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

  static byte[] compress(byte[] data, CompressionAlgo algo) throws BlockException {
    try {
      byte[] compressedData;

      switch (algo) {
        case LZ4_FAST:
          compressedData = lz4Compression(data);
          break;
        case SNAPPY:
          compressedData = Snappy.compress(data);
          break;
        default:
          compressedData = data;
      }

      if (compressedData.length >= data.length) {
        throw new IOException("Compression ineffective.");
      } else {
        return compressedData;
      }
    } catch (IOException e) {
      throw new BlockException("Compression stopped.", e);
    }
  }

  static byte[] decompress(byte[] compressed, int len, String algo) throws BlockException {
    CompressionAlgo compressionAlgo;
    try {
      compressionAlgo = CompressionAlgo.valueOf(algo);
    } catch (IllegalArgumentException e) {
      compressionAlgo = CompressionAlgo.NONE;
    }
    return decompress(compressed, len, compressionAlgo);
  }

  static byte[] decompress(byte[] compressed, int len, CompressionAlgo algo) throws BlockException {
    try {
      switch (algo) {
        case LZ4_FAST:
          return lz4Decompression(compressed, len);
        case SNAPPY:
          return Snappy.uncompress(compressed);
        default:
          return compressed;
      }
    } catch (IOException e) {
      throw new BlockException("Decompression not possible.", e);
    }
  }

  static byte[] lz4Compression(byte[] data) {
    LZ4Compressor compressor = LZ_4_FACTORY.fastCompressor();
    int maxCompressedLength = compressor.maxCompressedLength(data.length);
    byte[] compressed = new byte[maxCompressedLength];
    int compressedLength = compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);
    return Arrays.copyOfRange(compressed, 0, compressedLength);
  }

  static byte[] lz4Decompression(byte[] data, int decompressedLength) {
    // decompress data
    LZ4FastDecompressor decompressor = LZ_4_FACTORY.fastDecompressor();
    byte[] restored = new byte[decompressedLength];
    decompressor.decompress(data, 0, restored, 0, decompressedLength);
    return restored;
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


  public static Set<String> calculateBestShards(String[] shardIds, int[] shardSizes, int blockSize) {
    boolean[][] retainShard = new boolean[shardSizes.length][blockSize + 1];
    int[][] tmpTable = new int[shardSizes.length + 1][blockSize + 1];

    for (int shardIdx = 1; shardIdx <= shardSizes.length; shardIdx++) {
      for (int size = 1; size <= blockSize; size++) {
        if (shardSizes[shardIdx - 1] > size) {
          tmpTable[shardIdx][size] = tmpTable[shardIdx - 1][size];
        } else {
          int keepShardSize = shardSizes[shardIdx - 1] + tmpTable[shardIdx - 1][size - shardSizes[shardIdx - 1]];
          int ignoreShardSize = tmpTable[shardIdx - 1][size];
          if (keepShardSize > ignoreShardSize) {
            retainShard[shardIdx - 1][size] = true;
            tmpTable[shardIdx][size] = keepShardSize;
          } else {
            tmpTable[shardIdx][size] = ignoreShardSize;
          }
        }
      }
    }

    Set<String> selectedShards = new HashSet<>();
    for (int i = shardSizes.length - 1; i >= 0; i--) {
      if (retainShard[i][blockSize]) {
        selectedShards.add(shardIds[i]);
        blockSize = blockSize - shardSizes[i];
      }
    }
    return selectedShards;
  }

  enum CompressionAlgo {
    LZ4_FAST, SNAPPY, NONE
  }
}
