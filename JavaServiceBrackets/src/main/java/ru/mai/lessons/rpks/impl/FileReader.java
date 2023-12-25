package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.IFileReader;
import ru.mai.lessons.rpks.exception.FilenameShouldNotBeEmptyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Slf4j
public class FileReader implements IFileReader {


    @Override
    public List<String> loadContent(String filePath) throws FilenameShouldNotBeEmptyException {

        if (filePath == null || filePath.equals("")) {
            throw new FilenameShouldNotBeEmptyException("Filename is empty!");
        }

        Scanner scanner;
        File file;
        try {
            file = FileGetter.getFile(filePath);
            scanner = new Scanner(new java.io.FileReader(file));
        } catch (FileNotFoundException e) {
            log.error("File " + filePath + " not found!", e);
            return new ArrayList<>();
        }

        List<String> content = new ArrayList<>();

        while (scanner.hasNext()) {
            content.add(scanner.nextLine());
        }

        return content;
    }
}