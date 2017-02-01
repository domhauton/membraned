package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.OutputStreamWriter;
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
    private OutputStreamWriter outputStreamWriter;

    @BeforeEach
    void setUp() {
        fileCatalogue = new FileCatalogue();
        random = new Random(System.currentTimeMillis());
        outputStreamWriter = Mockito.mock(OutputStreamWriter.class);
    }

    @Test
    void putAndRetrieveTest() throws Exception {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT, path, outputStreamWriter);
        FileVersion fv = fileCatalogue.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT);
        Assertions.assertEquals(fv.getMD5ShardList(), hashList1);
    }

    @Test
    void putAndRetrieveTestOverWrite() throws Exception {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);
        FileVersion fv = fileCatalogue.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT2);
        Assertions.assertEquals(fv.getMD5ShardList(), hashList2);
    }

    @Test
    void putAndRetrieveTestOverWriteRewind() throws Exception {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);
        FileCatalogue rewoundFC = fileCatalogue.revertTo(new DateTime(150L));
        FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
        Assertions.assertEquals(fv.getMD5ShardList(), hashList1);
    }

    @Test
    void putAndRetrieveTestOverWriteCollapse() throws Exception {
        Path path = Paths.get("/tmp/membrane/foobar1");

        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);

        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

        Assertions.assertEquals(20, fileCatalogue.getReferencedShards().size());

        FileCatalogue collapsedFC = fileCatalogue.cleanCatalogue(new DateTime(150L));
        FileCatalogue rewoundFC = collapsedFC.revertTo(new DateTime(150L));
        FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
        Assertions.assertEquals(fv.getMD5ShardList(), hashList1);

        Assertions.assertEquals(20, collapsedFC.getReferencedShards().size());

        FileCatalogue collapsedFC2 = fileCatalogue.cleanCatalogue(new DateTime(250L));
        FileCatalogue rewoundFC2 = collapsedFC2.revertTo(new DateTime(150L));
        FileVersion fv2 = rewoundFC2.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv2.getModificationDateTime(), modifiedDT2);
        Assertions.assertEquals(fv2.getMD5ShardList(), hashList2);

        Assertions.assertEquals(10, collapsedFC2.getReferencedShards().size());
    }

    @Test
    void removalOverwriteTest() throws Exception {
        Path path = Paths.get("/tmp/membrane/foobar1");

        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);

        fileCatalogue.removeFile(path, new DateTime(200L));
        Assertions.assertEquals(10, fileCatalogue.getReferencedShards().size());

        FileCatalogue collapsedFC = fileCatalogue.cleanCatalogue(new DateTime(150L));
        FileCatalogue rewoundFC = collapsedFC.revertTo(new DateTime(150L));
        FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

        Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
        Assertions.assertEquals(fv.getMD5ShardList(), hashList1);

        Assertions.assertEquals(10, collapsedFC.getReferencedShards().size());

        FileCatalogue collapsedFC2 = fileCatalogue.cleanCatalogue(new DateTime(250L));
        FileCatalogue rewoundFC2 = collapsedFC2.revertTo(new DateTime(150L));
        FileVersion fv2 = rewoundFC2.getFileVersion(path).orElse(null);
        Assertions.assertNull(fv2);

        Assertions.assertEquals(0, collapsedFC2.getReferencedShards().size());
    }

    @Test
    void retrieveFileHistory() throws Exception {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

        List<JournalEntry> fileVersions = fileCatalogue.getFileVersionHistory(path);
        Assertions.assertTrue(fileVersions.stream()
                .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
                .map(JournalEntry::getShardInfo)
                .map(FileVersion::getMD5ShardList)
                .anyMatch(hashList1::equals));

        Assertions.assertTrue(fileVersions.stream()
                .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
                .map(JournalEntry::getShardInfo)
                .map(FileVersion::getMD5ShardList)
                .anyMatch(hashList2::equals));
    }

    @Test
    void retrieveFileHistoryTrimmed() throws Exception {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

        fileCatalogue = fileCatalogue.cleanCatalogue(new DateTime(250L));

        List<JournalEntry> fileVersions = fileCatalogue.getFileVersionHistory(path);

        Assertions.assertTrue(fileVersions.stream()
                .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
                .map(JournalEntry::getShardInfo)
                .map(FileVersion::getMD5ShardList)
                .noneMatch(hashList1::equals));

        Assertions.assertTrue(fileVersions.stream()
                .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
                .map(JournalEntry::getShardInfo)
                .map(FileVersion::getMD5ShardList)
                .anyMatch(hashList2::equals));
    }

    @Test
    void retrieveFileAtTime() throws Exception {
        List<String> hashList1 = genRandHashSet();
        DateTime modifiedDT1 = new DateTime(100L);
        Path path = Paths.get("/tmp/membrane/foobar1");
        fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
        List<String> hashList2 = genRandHashSet();
        DateTime modifiedDT2 = new DateTime(200L);
        fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

        Optional<FileVersion> fileVersion1 = fileCatalogue.getFileVersion(path, new DateTime(150L));

        Assertions.assertTrue(fileVersion1.isPresent());
        Assertions.assertTrue(fileVersion1.orElse(null).getMD5ShardList().equals(hashList1));

        Optional<FileVersion> fileVersion1b = fileCatalogue.getFileVersion(path, new DateTime(50L));

        Assertions.assertNull(fileVersion1b.orElse(null));

        Optional<FileVersion> fileVersion2 = fileCatalogue.getFileVersion(path, new DateTime(200L));

        Assertions.assertTrue(fileVersion2.isPresent());
        Assertions.assertTrue(fileVersion2.orElse(null).getMD5ShardList().equals(hashList2));
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