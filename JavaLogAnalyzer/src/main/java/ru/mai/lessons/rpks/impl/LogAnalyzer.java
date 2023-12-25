package ru.mai.lessons.rpks.impl;

import lombok.extern.slf4j.Slf4j;
import ru.mai.lessons.rpks.ILogAnalyzer;
import ru.mai.lessons.rpks.exception.WrongFilenameException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LogAnalyzer implements ILogAnalyzer {

    private final int COUNT_READ_LINES = 10;
    private static final AtomicInteger countFoundLogs;

    static {
        countFoundLogs = new AtomicInteger(0);
    }

    private String getPath(String filename) {
        return getClass().getClassLoader().getResource(".").getPath() + filename;
    }

    @Override
    public List<Integer> analyze(String filename, String deviation) throws WrongFilenameException {

        if (filename == null || filename.isEmpty()) {
            log.error("File was not provided!");
            throw new WrongFilenameException("File was not provided!");
        }

        filename = getPath(filename);


        ConcurrentMap<Integer, Long> allQueries = new ConcurrentHashMap<>();
        List<Integer> countDurations = Collections.synchronizedList(new ArrayList<>(Collections.nCopies(30, 0)));

        try {
            allQueries = getAllQueries(filename, countDurations);
        } catch (FileNotFoundException e) {
            log.error("File " + filename + " does not exist!", e);
            throw new WrongFilenameException("File " + filename + " does not exist!");
        } catch (IOException e) {
            log.error("Error while reading file " + filename, e);
            return new ArrayList<>();
        }

        float median = computeMedian(countDurations, allQueries.size());


        if (deviation == null || deviation.isEmpty()) {
            deviation = (int) (median * 0.5 + 1.5f) + " sec";
        }

        return findDeviations(allQueries, deviation, median);
    }

    private float computeMedian(List<Integer> countDurations, int countQueries) {

        int first, second;

        if ((countQueries & 1) == 1) {
            first = countQueries / 2 + 1;
            second = first;
        } else {
            first = countQueries / 2;
            second = first + 1;
        }

        int count = 0;
        int num = -1;

        for (int i = 0; i < countDurations.size(); ++i) {

            count += countDurations.get(i);

            if (count >= first && num == -1) {
                num = i;
            }

            if (count >= second) {
                return (float) (num + i) / 2;
            }
        }

        return -1;
    }

    private ConcurrentMap<Integer, Long> getAllQueries(String filename, List<Integer> countDurations) throws IOException {

        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {

            ConcurrentMap<Integer, Long> queryDuration = new ConcurrentHashMap<>();

            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1);

            long currentPosition = 0;

            Thread logsCounter = new Thread(this::countLogsThreadTask);
            logsCounter.start();

            while (currentPosition < file.length()) {
                file.seek(currentPosition);
                List<String> logLines = readLines(file);
                executorService.execute(() -> readLogsThreadTask(logLines, queryDuration, countDurations));
                currentPosition = file.getFilePointer();
            }

            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    log.error("Thread pool was processing for too long");
                    return new ConcurrentHashMap<>();
                }
            } catch (InterruptedException e) {
                log.error("Error while waiting for thread pool termination", e);
                return new ConcurrentHashMap<>();
            }

            return queryDuration;
        }
    }

    private void countLogsThreadTask() {

        int count = 0;
        int countdown = 10;

        try {
            while (true) {

                Thread.sleep(50L);

                if (countFoundLogs.get() == count) {
                    --countdown;
                    if (countdown == 0) {
                        System.out.print("\rFound logs count: " + count + "\n");
                        return;
                    }
                    continue;
                } else {
                    countdown = 10;
                }

                count = countFoundLogs.get();
                System.out.print("\rFound logs count: " + count);
            }
        } catch (InterruptedException e) {
            log.error("Log counter thread error", e);
        }
    }


    private List<Integer> findDeviations(ConcurrentMap<Integer, Long> queries, String deviationStr, float median) {

        int deviation = deviationToInteger(deviationStr);
        List<Integer> IDs = new ArrayList<>();
        int count = 0;
        int overall = countFoundLogs.get();

        for (Map.Entry<Integer, Long> logNote : queries.entrySet()) {
            System.out.print("\rProcessed logs: " + count + "/" + overall);
            count += 2;
            if (logNote.getValue() < median - deviation || median + deviation < logNote.getValue()) {
                IDs.add(logNote.getKey());
            }
        }
        System.out.println("\rProcessed logs: " + count + "/" + overall);

        return IDs;
    }

    private int deviationToInteger(String deviationStr) {

        if (deviationStr == null || deviationStr.isEmpty()) {
            return 0;
        }

        String[] values = deviationStr.split(" ");
        int deviation = Integer.parseInt(values[0]);

        if (values[1].equals("sec")) {
            return deviation;
        } else if (values[1].equals("min")) {
            return deviation * 60;
        }
        return 0;
    }

    private List<String> readLines(RandomAccessFile file) throws IOException {

        List<String> lines = new ArrayList<>();

        for (int i = 0; i < COUNT_READ_LINES; i++) {
            String line = file.readLine();
            if (line == null) {
                break;
            }
            lines.add(line);
        }

        return lines;
    }

    private void readLogsThreadTask(List<String> lines, ConcurrentMap<Integer, Long> queryDuration, List<Integer> countDurations) {

        log.debug("Thread " + Thread.currentThread().getName() + " started");
        for (String line : lines) {

            countFoundLogs.incrementAndGet();

            int id = getID(line);
            long time = getTime(line);

            Long startTime = queryDuration.putIfAbsent(id, time);

            if (startTime != null) {
                int queryTime = (int) Math.abs(time - startTime);
                queryDuration.put(id, (long) queryTime);

                if (countDurations.size() <= queryTime) {
                    countDurations.addAll(new ArrayList<>(Collections.nCopies(
                            (queryTime - countDurations.size()) * 2, 0)));

                    countDurations.set(queryTime, 1);
                } else {
                    countDurations.set(queryTime, countDurations.get(queryTime) + 1);
                }
            }


        }
    }

    private int getID(String line) {
        return Integer.parseInt(line.substring(line.lastIndexOf(" ") + 1));
    }

    private long getTime(String line) {
        String[] words = line.split(" ");
        return LocalDateTime.parse(words[0] + "T" + words[1]).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
    }
}
