package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.ILineFinder;
import ru.mai.lessons.rpks.exception.LineCountShouldBePositiveException;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LineFinder implements ILineFinder {

    int PART_SIZE = 1 << 20;
    long SNOOZE_PROGRESS_VIEWER = 8L;


    private static final AtomicInteger linesFound;
    private static final AtomicLong currentFilePositionReading;
    private static final AtomicInteger threadTaskNumber;
    private static final Map<String, int[]> threadsProgress;

    static {
        linesFound = new AtomicInteger(0);
        currentFilePositionReading = new AtomicLong(1);
        threadTaskNumber = new AtomicInteger(0);
        threadsProgress = new ConcurrentHashMap<>();
    }

    private String getFilePath(String fileName) {
        return getClass().getClassLoader().getResource(".").getPath() + fileName;
    }


    private void representationProgressThreadTask(long fileSize) {

        try {
            long startTime = System.currentTimeMillis();

            Thread.sleep(SNOOZE_PROGRESS_VIEWER);

            int isEnd = 3;
            while (threadsProgress.size() != 0) {

                int x = 0;
                int countEnded = 0;

                long pos = PART_SIZE;
                long partSize = 0;
                long minDelta = PART_SIZE;

                for (Map.Entry<String, int[]> data : threadsProgress.entrySet()) {

                    if (data.getValue()[0] == data.getValue()[1]) {
                        ++countEnded;
                    }

                    if (minDelta > data.getValue()[1] - data.getValue()[0]) {
                        minDelta = data.getValue()[1] - data.getValue()[0];
                        pos = data.getValue()[0];
                        partSize = data.getValue()[1];
                    }

                    int percent = (int) (((float) data.getValue()[0] / data.getValue()[1]) * 100);

                    System.out.print("Thread-" + ++x + ": [" +
                            (percent != 100 ? (percent < 10 ? "  " : " ") : "") +
                            percent + "%]\t");
                }


                long currentElapsedTime = System.currentTimeMillis() - startTime;
                long currentFilePos = currentFilePositionReading.get();
                long approximateEndTime;

                if (fileSize - currentFilePos < partSize - pos) {
                    approximateEndTime = (long) (currentElapsedTime * ((float) partSize / pos))
                            / (threadTaskNumber.get());
                } else {
                    approximateEndTime = (long) ((currentElapsedTime * ((float) fileSize / currentFilePos)));
                }


                System.out.print("Remain " + (approximateEndTime - currentElapsedTime < 0 ? 0 : approximateEndTime - currentElapsedTime) + "ms");

                System.out.print("\tFound: " + linesFound.get() + " words\t");

                if (countEnded == threadsProgress.size()) {
                    if (isEnd == 0) {
                        System.out.println();
                        return;
                    } else {
                        --isEnd;
                    }
                } else {
                    isEnd = 3;
                }

                System.out.print("\r");

                Thread.sleep(1L);
            }
        } catch (InterruptedException e) {
            log.error("Progress representation error", e);
        }
    }

    private List<Long> findLineThreadTask(String fileName, long seek, String keyWord) {

        String threadName = Thread.currentThread().getName();


        byte[] bytes = new byte[]{};
        int countReadingBytes = 0;

        try (RandomAccessFile inFile = new RandomAccessFile(fileName, "r")) {
            inFile.seek(seek);
            countReadingBytes = inFile.length() - seek < PART_SIZE ? (int) (inFile.length() - seek) : PART_SIZE;
            bytes = new byte[countReadingBytes];
            inFile.read(bytes);
        } catch (IOException e) {
            log.error("Error while reading file in a thread " + threadName, e);
            return new ArrayList<>();
        }

        byte[] upperKey = keyWord.toUpperCase().getBytes();
        byte[] lowerKey = keyWord.toLowerCase().getBytes();

        int[] progress = new int[] {0, countReadingBytes};
        threadsProgress.put(threadName, progress);

        List<Long> positions = new ArrayList<>();

        for (int i = 0; i < bytes.length - upperKey.length; ++i) {

            for (int b = 0; b < upperKey.length; ++b) {
                if (upperKey[b] != bytes[i + b] && lowerKey[b] != bytes[i + b]) {
                    break;
                }
                if (b == upperKey.length - 1) {

                    positions.add(seek + i);
                    linesFound.incrementAndGet();

                    while (i < bytes.length - upperKey.length && !(new String(new byte[]{bytes[i + b]})).equals(System.lineSeparator())) {
                        ++i;
                    }
                }
            }

            if (i % 5 == 0) {
                progress[0] = i;
                threadsProgress.put(threadName, progress);
            }
        }

        progress[0] = progress[1];
        threadsProgress.put(threadName, progress);

        return positions;
    }

    @Override
    public void find(String inputFilename, String outputFilename, String keyWord, int lineCount) throws LineCountShouldBePositiveException {

        if (lineCount < 0) {
            throw new LineCountShouldBePositiveException("Line count should be a positive number");
        }

        inputFilename = getFilePath(inputFilename);
        outputFilename = getFilePath(outputFilename);


        Set<Long> linesToWrite = new TreeSet<>();
        try {
            linesToWrite = findLinesInFile(inputFilename, keyWord);
        } catch (IOException e) {
            log.error("Error while reading file " + inputFilename, e);
            return;
        } catch (ExecutionException | InterruptedException e) {
            log.error("Error while getting futures", e);
            return;
        }

        try {
            clearTheFile(outputFilename);
            writeLinesToFile(outputFilename, inputFilename, lineCount, linesToWrite);
        } catch (IOException e) {
            log.error("Error while writing into file " + outputFilename, e);
        }
    }


    public static void clearTheFile(String fileName) {
        try {
            FileWriter fwOb = new FileWriter(fileName, false);
            PrintWriter pwOb = new PrintWriter(fwOb, false);
            pwOb.flush();
            pwOb.close();
            fwOb.close();
        } catch (IOException ignored) {
        }
    }

    private void writeLinesToFile(String outputFilename, String inputFilename, int lineCount, Set<Long> lines) throws IOException {

        log.info("Writing result to file");

        RandomAccessFile file = new RandomAccessFile(outputFilename, "rw");
        RandomAccessFile inFile = new RandomAccessFile(inputFilename, "r");

        Set<Long> uniqueLinesPositions = new HashSet<>();

        long outFilePointer = 0;

        for (long startPos : lines) {


            long currentPos = startPos - 1;
            int linesRemain = lineCount;

            List<Byte> dataToWrite = new ArrayList<>();

            for (byte b; currentPos > -1 && linesRemain > -1; ) {

                inFile.seek(currentPos--);
                b = inFile.readByte();

                if (new String(new byte[]{b}).equals(System.lineSeparator())) {
                    --linesRemain;
                    if (linesRemain == -1) {
                        break;
                    }
                }

                dataToWrite.add(b);
            }

            if (uniqueLinesPositions.contains(currentPos)) {
                continue;
            }

            if (outFilePointer != 0) {
                file.writeBytes("\n");
            }

            uniqueLinesPositions.add(currentPos);

            file.seek(outFilePointer);
            outFilePointer += dataToWrite.size();

            for (int i = dataToWrite.size() - 1; i > -1; --i) {
                file.write(dataToWrite.get(i));
            }


            dataToWrite = new ArrayList<>();
            currentPos = startPos;
            linesRemain = lineCount;

            for (byte b; currentPos < inFile.length() && linesRemain > -1; ) {

                inFile.seek(currentPos++);
                b = inFile.readByte();

                if (new String(new byte[]{b}).equals(System.lineSeparator())) {
                    --linesRemain;
                    if (linesRemain == -1) {
                        break;
                    }
                }

                dataToWrite.add(b);
            }


            file.seek(outFilePointer);
            outFilePointer += dataToWrite.size() + 1;

            for (int i = 0; i < dataToWrite.size(); ++i) {
                file.write(dataToWrite.get(i));
            }
        }

        inFile.close();
        file.close();

        log.info(uniqueLinesPositions.size() + " lines containing keyword were written");
    }

    private Set<Long> findLinesInFile(String inFile, String keyWord) throws IOException, ExecutionException, InterruptedException {

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

        int intersectionSize = keyWord.getBytes().length;
        long currentPosition = 0;
        List<Future<List<Long>>> futureList = new ArrayList<>();


        RandomAccessFile file = new RandomAccessFile(inFile, "r");
        long fileSize = file.length();
        file.close();

        (new Thread(() -> representationProgressThreadTask(fileSize))).start();

        while (currentPosition < fileSize) {
            long finalCurrentPosition = currentPosition;
            threadTaskNumber.incrementAndGet();
            futureList.add(pool.submit(() -> findLineThreadTask(inFile, finalCurrentPosition, keyWord)));
            currentPosition += PART_SIZE - intersectionSize;
            currentFilePositionReading.set(currentPosition);
        }

        pool.shutdown();

        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            log.error("Some threads are stuck!");
            return new HashSet<>();
        }


        Set<Long> linesToWrite = new TreeSet<>();
        List<Future<List<Long>>> newFutureList = new ArrayList<>();

        while (futureList.size() > 0) {

            newFutureList = new ArrayList<>();
            for (Future<List<Long>> longFuture : futureList) {
                if (longFuture.isDone()) {
                    linesToWrite.addAll(longFuture.get());
                } else {
                    newFutureList.add(longFuture);
                }
            }
            futureList = newFutureList;
        }

        return linesToWrite;
    }
}
