package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by dominic on 31/01/17.
 */
class FileCatalogueTest {

    private Random random;
    private FileCatalogue fileCatalogue;

    @BeforeEach
    void setUp() {
        fileCatalogue = new FileCatalogue();
        random = new Random(System.currentTimeMillis());
    }

    @Test
    void putAndRetrieveTest() {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT, path);
        FileVersion fv = fileCatalogue.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT);
        Assertions.assertEquals(fv.getShardHash(), hashList1);
        Assertions.assertEquals(fv.getStoredPath(), path);
    }

    @Test
    void putAndRetrieveTestOverWrite() {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path);
        FileVersion fv = fileCatalogue.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT2);
        Assertions.assertEquals(fv.getShardHash(), hashList2);
        Assertions.assertEquals(fv.getStoredPath(), path);
    }

    @Test
    void putAndRetrieveTestOverWriteRewind() {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path);
        FileCatalogue rewoundFC = fileCatalogue.revertTo(new DateTime(150L));
        FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
        Assertions.assertEquals(fv.getShardHash(), hashList1);
        Assertions.assertEquals(fv.getStoredPath(), path);
    }

    @Test
    void putAndRetrieveTestOverWriteCollapse() {
        Path path = Paths.get("/tmp/membrane/foobar1");

        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        fileCatalogue.addFile(hashList1, modifiedDT1, path);

        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path);

        Assertions.assertEquals(20, fileCatalogue.getReferencedShards().size());

        FileCatalogue collapsedFC = fileCatalogue.cleanCatalogue(new DateTime(150L));
        FileCatalogue rewoundFC = collapsedFC.revertTo(new DateTime(150L));
        FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
        Assertions.assertEquals(fv.getShardHash(), hashList1);
        Assertions.assertEquals(fv.getStoredPath(), path);

        Assertions.assertEquals(20, collapsedFC.getReferencedShards().size());

        FileCatalogue collapsedFC2 = fileCatalogue.cleanCatalogue(new DateTime(250L));
        FileCatalogue rewoundFC2 = collapsedFC2.revertTo(new DateTime(150L));
        FileVersion fv2 = rewoundFC2.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv2.getModificationDateTime(), modifiedDT2);
        Assertions.assertEquals(fv2.getShardHash(), hashList2);
        Assertions.assertEquals(fv2.getStoredPath(), path);

        Assertions.assertEquals(10, collapsedFC2.getReferencedShards().size());
    }

    @Test
    void removalOverwriteTest() {
        Path path = Paths.get("/tmp/membrane/foobar1");

        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        fileCatalogue.addFile(hashList1, modifiedDT1, path);

        fileCatalogue.removeFile(path, new DateTime(200L));
        Assertions.assertEquals(10, fileCatalogue.getReferencedShards().size());

        FileCatalogue collapsedFC = fileCatalogue.cleanCatalogue(new DateTime(150L));
        FileCatalogue rewoundFC = collapsedFC.revertTo(new DateTime(150L));
        FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
        Assertions.assertEquals(fv.getShardHash(), hashList1);
        Assertions.assertEquals(fv.getStoredPath(), path);

        Assertions.assertEquals(10, collapsedFC.getReferencedShards().size());

        FileCatalogue collapsedFC2 = fileCatalogue.cleanCatalogue(new DateTime(250L));
        FileCatalogue rewoundFC2 = collapsedFC2.revertTo(new DateTime(150L));
        FileVersion fv2 = rewoundFC2.getFileVersion(path).orElse(null);
        Assertions.assertNull(fv2);

        Assertions.assertEquals(0, collapsedFC2.getReferencedShards().size());
    }

    private List<String> genRandHashSet() {
        return IntStream.range(0, 10)
                .boxed()
                .map(x -> genRandHash())
                .map(HashCode::toString)
                .collect(Collectors.toList());
    }

    private HashCode genRandHash() {
        byte[] data = new byte[128];
        random.nextBytes(data);
        return Hashing.md5().hashBytes(data);
    }
}