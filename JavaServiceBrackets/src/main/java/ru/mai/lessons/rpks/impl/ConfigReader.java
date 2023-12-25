package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.IConfigReader;
import ru.mai.lessons.rpks.exception.FilenameShouldNotBeEmptyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;

@Slf4j
public class ConfigReader implements IConfigReader {

    @Override
    public String loadConfig(String configPath) throws FilenameShouldNotBeEmptyException {

        if (configPath == null || configPath.equals("")) {
            throw new FilenameShouldNotBeEmptyException("Filename is empty!");
        }

        String config;
        File file;
        try {
            file = FileGetter.getFile(configPath);
            config = Files.readString(file.toPath());
        } catch (FileNotFoundException e) {
            log.error("Cannot find config file!", e);
            return "";
        } catch (IOException e) {
            log.error("Something went wrong while reading the file!", e);
            return "";
        } catch (InvalidPathException e) {
            log.error("File " + configPath + " not found!", e);
            return "";
        }

        return config;
    }
}