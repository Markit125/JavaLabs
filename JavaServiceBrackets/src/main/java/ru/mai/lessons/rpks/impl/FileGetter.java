package ru.mai.lessons.rpks.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;

public class FileGetter {

    public static File getFile(String filePath) throws FileNotFoundException {
        ClassLoader loader = FileReader.class.getClassLoader();

        try {
            java.net.URL url = loader.getResource(filePath);

            if (url == null) {
                throw new FileNotFoundException("File not found or its name is incorrect!");
            }

            return new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new FileNotFoundException("File not found or its name is incorrect!");
        }
    }
}
