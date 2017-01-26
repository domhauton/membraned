package com.domhauton.membrane.prospector;

import com.domhauton.membrane.config.items.WatchFolder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Created by dominic on 23/01/17.
 */
public class Prospector {
    private final static String SEP = java.nio.file.FileSystems.getDefault().getSeparator();
    private final static int MAX_UPDATES = 1000;

    private final Logger logger;

    private final WatchService watchService;
    private final BiMap<WatchKey, Path> keys;
    private final Set<WatchFolder> watchFolders;


    public Prospector() throws IOException {
        logger = LogManager.getLogger();
        watchService = FileSystems.getDefault().newWatchService();
        keys = HashBiMap.create();
        watchFolders = new HashSet<>();
    }

    public Set<Path> rediscoverFolders() {
        Set<Path> allFolders = watchFolders.stream()
                .flatMap(x -> findMatchingFolders(x).stream())
                .collect(Collectors.toSet());
        Collection<Path> newFolders = CollectionUtils.subtract(allFolders, keys.values());
        Collection<Path> removedFolders = CollectionUtils.subtract(keys.values(), allFolders);
        newFolders.stream()
                .peek(x -> logger.trace("Found new folder {}", x))
                .forEach(this::registerPath);
        removedFolders.stream()
                .peek(x -> logger.trace("Removing old folder"))
                .map(x -> keys.inverse().get(x))
                .peek(keys::remove)
                .forEach(WatchKey::cancel);
        return new HashSet<>(newFolders);
    }

    public Set<Path> checkChanges() {
        Set<Path> returnPaths = new HashSet<>();
        for(int i = 0; i < MAX_UPDATES; i++) {
            WatchKey key = null;
            try{
                // Pause for a slight bit to allow FS to report changes if very recent.
                key= watchService.poll(5, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.debug("Watch Service interrupted during poll.");
            }
            if(key == null) {
                logger.debug("{} files in {} watchFolders updated during sweep", returnPaths.size(), i);
                return returnPaths;
            }

            Path fullPath = keys.getOrDefault(key, null);

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                WatchEvent<Path> ev = cast(event);
                Path path = ev.context();
                if(!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    logger.trace("Prospector detected file {} at [{}{}{}]", kind, fullPath, SEP, path);
                    returnPaths.add(path);
                }
            }

            boolean valid = key.reset();
            if(!valid) {
                logger.info("Watch Service key for {} invalid. No longer monitoring.", fullPath);
                keys.remove(key);
            }
        }
        logger.debug("Exceeded {} updates during sweep", MAX_UPDATES);
        return returnPaths;
    }

    public Set<Path> addFolder(WatchFolder watchFolder) {
        logger.info("Adding watch folder: {}", watchFolder.getDirectory());
        watchFolders.add(watchFolder);
        Set<Path> matchingFolders = findMatchingFolders(watchFolder);
        matchingFolders.forEach(this::registerPath);
        return matchingFolders;
    }

    Optional<WatchKey> registerPath(Path path) {
        try{
            WatchKey watchKey = path.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            logger.info("Watching Path [{}]", path.toString());
            keys.put(watchKey, path);
            return Optional.of(watchKey);
        } catch (IOException e) {
            logger.error("Failed to start watching path due to IO error. [{}]", path.toString());
            return Optional.empty();
        }
    }

    Set<Path> findMatchingFolders(final WatchFolder watchFolder) {
        final Path searchRoot = Paths.get(findRoot(watchFolder.getDirectory()));
        String[] splitPattern = watchFolder.getDirectory().split(SEP);
        Set<Path> matchingFolders = findMatchingFolders(searchRoot, splitPattern, watchFolder.getRecursive());
        logger.trace("Found {} directories matching {}", matchingFolders.size(), watchFolder.getDirectory());
        return matchingFolders;
    }

    Set<Path> findMatchingFolders(final Path searchRoot, final String[] pattern, final boolean recursive) {
        final Set<Path> retSet = new HashSet<>();
        try {
            Files.walkFileTree(searchRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    logger.trace("Checking {}", dir.toString());
                    String[] splitDir = dir.toString().split(SEP);
                    if(directoriesMatch(pattern, splitDir, recursive)) {
                        retSet.add(dir);
                    }
                    if(!recursive && splitDir.length > pattern.length) { // Don't go too deep
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }
            });
        } catch (IOException e) {
            logger.error("Could not find watchFolders matching [{}]. No access to root [{}].", searchRoot.toString());
        }
        return retSet;
    }

    private String findRoot(String dir) {
        String[] splitPattern = dir.split(SEP);
        List<String> folderList = new LinkedList<>();
        for(String section : splitPattern) {
            if(section.equals("*")) {
                break;
            } else {
                folderList.add(section);
            }
        }
        return String.join(SEP, folderList);
    }

    private boolean directoriesMatch(String[] splitPattern, String[] splitDirectory, boolean recursive) {
        boolean notRecursiveAndLengthDiff = !recursive && splitPattern.length != splitDirectory.length;
        boolean recursiveAndTooShort = recursive && splitDirectory.length < splitPattern.length;
        if( notRecursiveAndLengthDiff || recursiveAndTooShort ) {
            return false;
        }
        // Match section by section now
        return IntStream.range(0, splitPattern.length).boxed()
                .allMatch(i -> !splitPattern[i].equals("*") || !splitDirectory[i].equalsIgnoreCase(splitPattern[i]));
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
}
