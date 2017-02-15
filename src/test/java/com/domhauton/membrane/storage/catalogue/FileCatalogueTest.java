package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileOperation;
import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import com.domhauton.membrane.storage.catalogue.metadata.MD5HashLengthPair;
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
import java.util.List;
import java.util.Optional;
import java.util.Random;
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
    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT = new DateTime(100L);
    Path path = Paths.get("/tmp/membrane/foobar1");
    fileCatalogue.addFile(hashList1, modifiedDT, path, outputStreamWriter);
    FileVersion fv = fileCatalogue.getFileVersion(path).orElse(null);

    Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT);
    Assertions.assertEquals(hashList1, fv.getMD5HashLengthPairs());
  }

  @Test
  void putAndRetrieveTestOverWrite() throws Exception {
    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    Path path = Paths.get("/tmp/membrane/foobar1");
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
    List<MD5HashLengthPair> hashList2 = genRandHashSet();
    DateTime modifiedDT2 = new DateTime(200L);
    fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);
    FileVersion fv = fileCatalogue.getFileVersion(path).orElse(null);

    Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT2);
    Assertions.assertEquals(hashList2, fv.getMD5HashLengthPairs());
  }

  @Test
  void putAndRetrieveTestOverWriteRewind() throws Exception {
    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    Path path = Paths.get("/tmp/membrane/foobar1");
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
    List<MD5HashLengthPair> hashList2 = genRandHashSet();
    DateTime modifiedDT2 = new DateTime(200L);
    fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);
    FileCatalogue rewoundFC = fileCatalogue.revertTo(new DateTime(150L));
    FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

    Assertions.assertEquals(fv.getModificationDateTime(), modifiedDT1);
    Assertions.assertEquals(hashList1, fv.getMD5HashLengthPairs());
  }

  @Test
  void putAndRetrieveTestOverWriteCollapse() throws Exception {
    Path path = Paths.get("/tmp/membrane/foobar1");

    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);

    List<MD5HashLengthPair> hashList2 = genRandHashSet();
    DateTime modifiedDT2 = new DateTime(200L);
    fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

    Assertions.assertEquals(20, fileCatalogue.getReferencedShards().size());

    FileCatalogue collapsedFC = fileCatalogue.cleanCatalogue(new DateTime(150L));
    FileCatalogue rewoundFC = collapsedFC.revertTo(new DateTime(150L));
    FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);
    FileVersion fvb = collapsedFC.getFileVersion(path, new DateTime(201L)).orElse(null);
    Assertions.assertNull(fv);
    Assertions.assertNotNull(fvb);

    Assertions.assertEquals(10, collapsedFC.getReferencedShards().size());

    FileCatalogue collapsedFC2 = fileCatalogue.cleanCatalogue(new DateTime(250L));
    FileCatalogue rewoundFC2 = collapsedFC2.revertTo(new DateTime(150L));
    FileVersion fv2 = rewoundFC2.getFileVersion(path).orElse(null);

    Assertions.assertNull(fv2);

    Assertions.assertEquals(0, collapsedFC2.getReferencedShards().size());
  }

  @Test
  void removalOverwriteTest() throws Exception {
    Path path = Paths.get("/tmp/membrane/foobar1");

    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);

    fileCatalogue.removeFile(path, new DateTime(200L));
    Assertions.assertEquals(10, fileCatalogue.getReferencedShards().size());

    FileCatalogue collapsedFC = fileCatalogue.cleanCatalogue(new DateTime(150L));
    FileCatalogue rewoundFC = collapsedFC.revertTo(new DateTime(150L));
    FileVersion fv = rewoundFC.getFileVersion(path).orElse(null);

    Assertions.assertNull(fv);

    Assertions.assertEquals(0, collapsedFC.getReferencedShards().size());

    FileCatalogue collapsedFC2 = fileCatalogue.cleanCatalogue(new DateTime(250L));
    FileCatalogue rewoundFC2 = collapsedFC2.revertTo(new DateTime(150L));
    FileVersion fv2 = rewoundFC2.getFileVersion(path).orElse(null);
    Assertions.assertNull(fv2);

    Assertions.assertEquals(0, collapsedFC2.getReferencedShards().size());
  }

  @Test
  void retrieveFileHistory() throws Exception {
    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    Path path = Paths.get("/tmp/membrane/foobar1");
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
    List<MD5HashLengthPair> hashList2 = genRandHashSet();
    DateTime modifiedDT2 = new DateTime(200L);
    fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

    List<JournalEntry> fileVersions = fileCatalogue.getFileVersionHistory(path);

    Assertions.assertTrue(fileVersions.stream()
            .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .anyMatch(hashList1::equals));

    Assertions.assertTrue(fileVersions.stream()
            .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .anyMatch(hashList2::equals));
  }

  @Test
  void retrieveFileHistoryTrimmed() throws Exception {
    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    Path path = Paths.get("/tmp/membrane/foobar1");
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
    List<MD5HashLengthPair> hashList2 = genRandHashSet();
    DateTime modifiedDT2 = new DateTime(200L);
    fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

    fileCatalogue = fileCatalogue.cleanCatalogue(new DateTime(150L));

    List<JournalEntry> fileVersions = fileCatalogue.getFileVersionHistory(path);
    System.out.println(fileVersions);
    Assertions.assertTrue(fileVersions.stream()
            .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .noneMatch(hashList1::equals));

    Assertions.assertTrue(fileVersions.stream()
            .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .anyMatch(hashList2::equals));

    fileCatalogue = fileCatalogue.cleanCatalogue(new DateTime(250L));
    fileVersions = fileCatalogue.getFileVersionHistory(path);
    Assertions.assertTrue(fileVersions.stream()
            .filter(x -> x.getFileOperation().equals(FileOperation.ADD))
            .map(JournalEntry::getShardInfo)
            .map(FileVersion::getMD5HashLengthPairs)
            .noneMatch(hashList2::equals));

  }

  @Test
  void retrieveFileAtTime() throws Exception {
    List<MD5HashLengthPair> hashList1 = genRandHashSet();
    DateTime modifiedDT1 = new DateTime(100L);
    Path path = Paths.get("/tmp/membrane/foobar1");
    fileCatalogue.addFile(hashList1, modifiedDT1, path, outputStreamWriter);
    List<MD5HashLengthPair> hashList2 = genRandHashSet();
    DateTime modifiedDT2 = new DateTime(200L);
    fileCatalogue.addFile(hashList2, modifiedDT2, path, outputStreamWriter);

    Optional<FileVersion> fileVersion1 = fileCatalogue.getFileVersion(path, new DateTime(150L));

    Assertions.assertTrue(fileVersion1.isPresent());
    Assertions.assertTrue(fileVersion1.orElse(null).getMD5HashLengthPairs().equals(hashList1));

    Optional<FileVersion> fileVersion1b = fileCatalogue.getFileVersion(path, new DateTime(50L));

    Assertions.assertNull(fileVersion1b.orElse(null));

    Optional<FileVersion> fileVersion2 = fileCatalogue.getFileVersion(path, new DateTime(200L));

    Assertions.assertTrue(fileVersion2.isPresent());
    Assertions.assertTrue(fileVersion2.orElse(null).getMD5HashLengthPairs().equals(hashList2));
  }

  private List<MD5HashLengthPair> genRandHashSet() {
    return IntStream.range(0, 10)
            .boxed()
            .map(x -> genRandHash())
            .map(HashCode::toString)
            .map(x -> new MD5HashLengthPair(x, 128))
            .collect(Collectors.toList());
  }

  private HashCode genRandHash() {
    byte[] data = new byte[128];
    random.nextBytes(data);
    return Hashing.md5().hashBytes(data);
  }
}