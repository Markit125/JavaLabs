package ru.mai.lessons.rpks.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.IBracketsDetector;
import ru.mai.lessons.rpks.result.ErrorLocationPoint;

import java.util.*;

@Slf4j
public class BracketsDetector implements IBracketsDetector {

    private static class Brackets {
        @JsonProperty("left")
        public final String left;
        @JsonProperty("right")
        public final String right;

        @JsonCreator
        public Brackets(@JsonProperty(value = "left") String left, @JsonProperty(value = "right") String right) {
            this.left = left;
            this.right = right;
        }
    }

    private static class Data {
        @JsonProperty("bracket")
        public final List<Brackets> brackets;

        @JsonCreator
        public Data(@JsonProperty(value = "bracket") List<Brackets> brackets) {
            this.brackets = brackets;
        }

    }

    private Map<String, String> readConfig(String config) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> pairsOfBrackets = new HashMap<>();

        for (Brackets br : mapper.readValue(config, Data.class).brackets) {
            pairsOfBrackets.put(br.left, br.right);
        }

        return pairsOfBrackets;
    }

    private class BracketWithLocation {
        public String bracket;
        public final int line;
        public final int column;

        public BracketWithLocation(String bracket, int line, int column) {
            this.bracket = bracket;
            this.line = line;
            this.column = column;
        }
    }

    @Override
    public List<ErrorLocationPoint> check(String config, List<String> content) {

        if (config == null) {
            log.error("config file is null!");
            return new ArrayList<>();
        }

        if (config.isEmpty()) {
            log.error("config file is empty!");
            return new ArrayList<>();
        }

        if (content.size() == 0) {
            log.error("content is empty!");
            return new ArrayList<>();
        }

        Map<String, String> pairs = new HashMap<>();
        try {
            pairs = readConfig(config);
        } catch (JsonProcessingException e) {
            log.error("config file is invalid! Unable to parse!");
            return new ArrayList<>();
        }

        Map<String, String> reversedPairs = new HashMap<>();
        pairs.forEach((key, value) -> reversedPairs.put(value, key));
        List<ErrorLocationPoint> errors = new ArrayList<>();

        int line = 0;
        for (String str : content) {

            ArrayDeque<BracketWithLocation> bracketsStack = new ArrayDeque<>();

            for (int column = 0; column < str.length(); ++column) {

                String currentCharacter = Character.toString(str.charAt(column));

                if (pairs.containsKey(currentCharacter)) {
                    log.debug("found in pairs: " + currentCharacter);
                    if (pairs.containsValue(currentCharacter) && !bracketsStack.isEmpty() &&
                            Objects.equals(bracketsStack.peek().bracket, currentCharacter)) {
                        bracketsStack.pop();
                    } else {
                        bracketsStack.push(new BracketWithLocation(currentCharacter, line + 1, column + 1));
                    }

                } else if (reversedPairs.containsKey(currentCharacter)) {

                    log.info("found in reversedPairs: " + currentCharacter);
                    if (!bracketsStack.isEmpty() &&
                            Objects.equals(bracketsStack.peek().bracket, reversedPairs.get(currentCharacter))) {
                        bracketsStack.pop();
                    } else {
                        errors.add(new ErrorLocationPoint(line + 1, column + 1));
                        log.info("Error found: " + line + ":" + column);
                    }
                }
            }

            ++line;


            if (!bracketsStack.isEmpty()) {
                BracketWithLocation currentError = bracketsStack.pop();
                while (!bracketsStack.isEmpty()) {
                    if (Objects.equals(pairs.get(currentError.bracket), reversedPairs.get(currentError.bracket))) {
                        BracketWithLocation temp = bracketsStack.pop();

                        if (!bracketsStack.isEmpty()) {
                            if (Objects.equals(temp.bracket, bracketsStack.peek().bracket)) {
                                bracketsStack.pop();
                                continue;
                            }
                            currentError = bracketsStack.pop();
                            bracketsStack.push(temp);
                        } else {
                            currentError = temp;
                        }

                    } else {
                        currentError = bracketsStack.pop();
                    }
                }
                errors.add(new ErrorLocationPoint(currentError.line, currentError.column));
            }
        }

        return errors;
    }
}
