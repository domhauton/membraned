package com.domhauton.membrane.prospector;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by dominic on 31/01/17.
 */
abstract class ProspectorTestUtils {
    static final int CREATED_FILES_COUNT = 3;
    static final int EXPECTED_SHARD_COUNT = CREATED_FILES_COUNT * 2;
    static final int MODIFIED_FILES_COUNT = 2;
    static final int FILE_SIZE_MB = 65;

    static String createRandomFolder(String baseDir) throws Exception {
        String tmpDir = baseDir;
        Path tmpPath = Paths.get(baseDir);
        while(Files.exists(tmpPath, LinkOption.NOFOLLOW_LINKS)) {
            tmpDir = baseDir + File.separator + new BigInteger(32, new SecureRandom()).toString(32);
            tmpPath = Paths.get(tmpDir);
        }
        Files.createDirectory(tmpPath);
        return tmpDir;
    }

    static void createTestFiles(String baseDir) throws Exception {
        Collection<Path> paths = IntStream.range(0, CREATED_FILES_COUNT).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + File.separator + num))
                .collect(Collectors.toList());

        writeDataToPaths(paths);
    }

    static void modifyTestFiles(String baseDir) throws Exception {
        Collection<Path> paths = IntStream.range(0, MODIFIED_FILES_COUNT).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + File.separator + num))
                .collect(Collectors.toList());

        writeDataToPaths(paths);
    }

    private static void writeDataToPaths(Collection<Path> paths) throws IOException {
        for(Path path : paths) {
            byte[] data = new byte[1024 * 1024 * FILE_SIZE_MB];
            Random random = new Random();
            random.nextBytes(data);
            Files.write(path, data);
        }
    }


    static void removeTestFiles(String baseDir) throws Exception {
        IntStream.range(0, CREATED_FILES_COUNT).boxed()
                .map(Object::toString)
                .map(num -> Paths.get(baseDir + File.separator + num))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
