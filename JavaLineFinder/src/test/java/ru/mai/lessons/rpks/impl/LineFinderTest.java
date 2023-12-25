package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.mai.lessons.rpks.ILineFinder;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Slf4j
public class LineFinderTest {
  private static final String INPUT_FILENAME = "inputFile.txt";
  private static final String EXPECTED_CORGI_OUTPUT_FILENAME = "expectedCorgiOutputFile.txt";
  private static final String EXPECTED_BOAST_1_LINE_FILENAME = "expectedBoastOutputFile1.txt";
  private static final String EXPECTED_BOAST_2_LINES_FILENAME = "expectedBoastOutputFile2.txt";
  private static final String EXPECTED_BOAST_3_LINES_FILENAME = "expectedBoastOutputFile3.txt";
  private static final String EXPECTED_HANDS_1_LINE_FILENAME = "expectedHandsOutputFile1.txt";
  private static final String EXPECTED_NEVER_1_LINE_FILENAME = "expectedNeverOutputFile1.txt";

  private ILineFinder lineFinder;

  @BeforeClass
  public void setUp() {
    lineFinder = new LineFinder();
  }

  @Test(description = "Проверяем успешный поиск строки в файле по заданному ключевому слову")
  public void testPositiveFind()
      throws IOException, URISyntaxException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "корги";
    int lineCount = 0;
    String outputFilename = getOutputFilename(keyWord, lineCount);
    byte[] expected = Files.readAllBytes(getPath(EXPECTED_CORGI_OUTPUT_FILENAME));

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);
    byte[] actual = Files.readAllBytes(getPath(outputFilename));

    // THEN
    assertTrue(Arrays.equals(actual, expected));
  }

  @DataProvider(name = "linesNumberFromStart")
  private Object[][] getLinesNumberFromStart() {
    return new Object[][] {
        {1, EXPECTED_BOAST_1_LINE_FILENAME},
        {2, EXPECTED_BOAST_2_LINES_FILENAME},
        {3, EXPECTED_BOAST_3_LINES_FILENAME}
    };
  }

  @Test(dataProvider = "linesNumberFromStart",
        description = "Проверяем успешный вывод указанного количества строк до и после найденной "
                      + "строки. Проверяем поиск в начале текста.")
  public void testPositiveFindStartMultipleLines(int lineCount, String expectedFilename)
      throws IOException, URISyntaxException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "похвастаться";
    String outputFilename = getOutputFilename(keyWord, lineCount);
    byte[] expected = Files.readAllBytes(getPath(expectedFilename));

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);
    byte[] actual = Files.readAllBytes(getPath(outputFilename));

    // THEN
    assertTrue(Arrays.equals(actual, expected));
  }

  @Test(description = "Проверяем успешный вывод указанного количества строк до и после найденной "
                      + "строки. Проверяем поиск в конце текста.")
  public void testPositiveFindMultipleLinesAtTheEnd()
      throws IOException, URISyntaxException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "руки";
    int lineCount = 1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    byte[] expected = Files.readAllBytes(getPath(EXPECTED_HANDS_1_LINE_FILENAME));

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);
    byte[] actual = Files.readAllBytes(getPath(outputFilename));

    // THEN
    assertTrue(Arrays.equals(actual, expected));
  }

  @Test(description = "Проверяем успешный вывод указанного количества строк до и после нескольких "
                      + "найденных строк. Проверяем поиск в середине текста.")
  public void testPositiveFindMiddleMultipleLines()
      throws IOException, URISyntaxException, LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "никогда";
    int lineCount = 1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    byte[] expected = Files.readAllBytes(getPath(EXPECTED_NEVER_1_LINE_FILENAME));

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);
    byte[] actual = Files.readAllBytes(getPath(outputFilename));

    // THEN
    assertTrue(Arrays.equals(actual, expected));
  }

  @DataProvider(name = "linesNotFoundCases")
  private Object[][] getLinesNotFoundCases() {
    return new Object[][] {
        {"кошка"},
        {""}
    };
  }

  @Test(dataProvider = "linesNotFoundCases",
        description = "Проверяем корректную обработку текста, когда не найдено ни одной строки. "
                      + "Ожидаем, что созданный файл пустой.")
  public void testPositiveWordNotFound(String keyWord) throws LineCountShouldBePositiveException {
    // GIVEN
    int lineCount = 1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    long startTime = System.currentTimeMillis();
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);
    log.info("Поиск отработал за {} ms.", System.currentTimeMillis() - startTime);

    // THEN
    File file = new File(outputFilename);
    assertEquals(file.length(), 0);
  }

  @Test(expectedExceptions = LineCountShouldBePositiveException.class,
        description = "Проверяем валидацию значения lineCount")
  public void testNegativeLineCountValue() throws LineCountShouldBePositiveException {
    // GIVEN
    String keyWord = "королева";
    int lineCount = -1;
    String outputFilename = getOutputFilename(keyWord, lineCount);

    // WHEN
    lineFinder.find(INPUT_FILENAME, outputFilename, keyWord, lineCount);

    // THEN ожидаем получение исключения
  }

  //region Вспомогательные методы
  private String getOutputFilename(String keyWord, int lineCount) {
    return String.format("outputFilename_%s_%d_lines.txt", keyWord, lineCount);
  }

  private Path getPath(String filename) throws URISyntaxException {
    File file = new File(filename);
    return Paths.get(Objects.requireNonNull(getClass().getResource("/" + file)).toURI());
  }
  //endregion
}