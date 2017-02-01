package com.domhauton.membrane.storage.catalogue;

import com.domhauton.membrane.storage.catalogue.metadata.FileVersion;
import org.joda.time.DateTime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by dominic on 01/02/17.
 */
public class CatalogueUtils {
    private static final String DELIMITER = ",";
    private static final String SPLIT_REGEX = "(?<!\\\\)" + Pattern.quote(DELIMITER);

    static String byteStuff(String input) {
        return input.replaceAll("\\\\", "\\\\").replaceAll(DELIMITER, "\\\\" + DELIMITER);
    }

    static String byteStuffReverser(String input) {
        return input.replaceAll("\\\\" + DELIMITER , DELIMITER).replaceAll("\\\\\\\\", "\\");
    }

    static List<String> stringToList(String input) {
        List<String> split = Arrays.asList(input.split(SPLIT_REGEX));

        return split.stream().map(CatalogueUtils::byteStuffReverser).collect(Collectors.toList());
    }

    static String listToString(List<String> input) {
        return String.join(",", input.stream().map(CatalogueUtils::byteStuff).collect(Collectors.toList()));
    }
    public static Map<Path, FileVersion> generateInputMap(List<String> input) {
        Map<Path, FileVersion> inputMap = new HashMap<>(input.size());
        input.forEach(x -> deserializeEntry(x, inputMap));
        return inputMap;
    }

    static String serializeEntry(Path path, DateTime modDateTime, List<String> shards) {
        LinkedList<String> fieldList = new LinkedList<>();
        fieldList.add(path.toString());
        fieldList.add(Long.toString(modDateTime.getMillis()));
        fieldList.addAll(shards);
        return CatalogueUtils.listToString(fieldList);
    }

    private static Map<Path, FileVersion> deserializeEntry(String input, Map<Path, FileVersion> inputMap)
            throws IllegalArgumentException {
        List<String> fieldList = CatalogueUtils.stringToList(input);
        if(fieldList.size() >= 3) {
            try {
                Path filePath = Paths.get(fieldList.get(0));
                DateTime modDT = new DateTime(Long.parseLong(fieldList.get(1)));
                List<String> shardList = fieldList.subList(2, fieldList.size());
                FileVersion fv = new FileVersion(shardList, modDT);
                inputMap.put(filePath, fv);
                return inputMap;
            } catch (Exception e) {
                throw new IllegalArgumentException("Entry invalid: " + input);
            }
        } else {
            throw new IllegalArgumentException("Field invalid. Not enough fields (" + fieldList.size() + ") : " + input);
        }
    }

}
