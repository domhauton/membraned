package com.domhauton.membrane.storage.catalogue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by dominic on 01/02/17.
 */
class CatalogueUtilsTest {

  @Test
  void byteStufferTestBasic() throws Exception {
    String basic = "foobar";
    String stuffed = CatalogueUtils.byteStuff(basic);
    String recovered = CatalogueUtils.byteStuffReverser(stuffed);

    Assertions.assertEquals(basic, stuffed);
    Assertions.assertEquals(recovered, stuffed);
  }

  @Test
  void byteStufferTestStuffed() throws Exception {
    String basic = "foo, bar";
    String stuffed = CatalogueUtils.byteStuff(basic);
    String recovered = CatalogueUtils.byteStuffReverser(stuffed);

    System.out.println(stuffed);

    Assertions.assertEquals(basic, recovered);
    Assertions.assertFalse(stuffed.contains("o, "));
  }

  @Test
  void listEncodeTest() throws Exception {
    List<String> sample = Arrays.asList("foobar,", "hardTest\\,", "normal");
    String encoded = CatalogueUtils.listToString(sample);
    List<String> decoded = CatalogueUtils.stringToList(encoded);

    Assertions.assertEquals(sample, decoded);
  }
}