package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.IDirectorySizeChecker;
import ru.mai.lessons.rpks.exception.DirectoryAccessException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DirectorySizeChecker implements IDirectorySizeChecker {

    private File getDirectory(String path) throws DirectoryAccessException {

        path = "src/test/resources/" + path;

        if (!Files.isDirectory(Path.of(path))) {
            throw new DirectoryAccessException("There is no such directory!");
        }

        return new File(path);
    }

    private int getSizeFolder(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return 0;
        }

        int size = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                size += getSizeFolder(file);
            } else {
                size += file.length();
            }
        }

        return size;
    }

    @Override
    public String checkSize(String directoryName) throws DirectoryAccessException {


        File dir;
        try {
            dir = getDirectory(directoryName);
        } catch (DirectoryAccessException e) {
            log.error("No such file or directory: " + directoryName);
            throw e;
        } catch (RuntimeException e) {
            log.error("Something went wrong with parsing directory name");
            throw e;
        }

        return getSizeFolder(dir) + " bytes";
    }
}
