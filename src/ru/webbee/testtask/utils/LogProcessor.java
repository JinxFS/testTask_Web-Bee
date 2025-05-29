package ru.webbee.testtask.utils;

import ru.webbee.testtask.models.LogModel;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class LogProcessor {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static LogModel parseLogLine(String line) {
        Pattern pattern = Pattern.compile(
                "\\[(.*?)\\] (\\w+) ((balance inquiry) (\\d+\\.?\\d*)|" +
                        "(transferred) (\\d+\\.?\\d*) to (\\w+)|" +
                        "(withdrew) (\\d+\\.?\\d*))"
        );
        Matcher matcher = pattern.matcher(line);

        if (!matcher.find()) return null;

        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), DATE_FORMATTER);
        String username = matcher.group(2);

        LogModel.OperationType operationType;
        double amount = 0;
        String targetUsername = null;

        if (matcher.group(4) != null) { // BALANCE
            operationType = LogModel.OperationType.BALANCE;
            amount = Double.parseDouble(matcher.group(5));
        }
        else if (matcher.group(6) != null) { // TRANSFERRED
            operationType = LogModel.OperationType.TRANSFERRED;
            amount = Double.parseDouble(matcher.group(7));
            targetUsername = matcher.group(8);
        }
        else { // WITHDREW
            operationType = LogModel.OperationType.WITHDREW;
            amount = Double.parseDouble(matcher.group(10));
        }

        return new LogModel(timestamp, username, operationType, amount, targetUsername);
    }

    public static List<LogModel> readAllLogs(String dirPath) throws IOException {
        return Files.walk(Paths.get(dirPath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log"))
                .flatMap(path -> {
                    try {
                        return Files.lines(path);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .map(LogProcessor::parseLogLine)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // processes user data and creates user's log
    public static void processUserLogs(List<LogModel> logs, String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir, "transactions_by_users");
        Files.createDirectories(outputPath);

        // group by username
        Map<String, List<LogModel>> userLogsMap = logs.stream()
                .collect(Collectors.groupingBy(LogModel::getUsername));

        // transfer receivers only
        Map<String, List<LogModel>> receivedTransfersMap = logs.stream()
                .filter(log -> log.getOperationType() == LogModel.OperationType.TRANSFERRED)
                .collect(Collectors.groupingBy(LogModel::getTargetUsername));

        for (Map.Entry<String, List<LogModel>> entry : userLogsMap.entrySet()) {
            String username = entry.getKey();
            List<LogModel> userLogs = new ArrayList<>(entry.getValue());

            // converting transfers to receives
            if (receivedTransfersMap.containsKey(username)) {
                receivedTransfersMap.get(username).forEach(transfer -> {
                    userLogs.add(new LogModel(
                            transfer.getTimestamp(),
                            username,
                            LogModel.OperationType.RECEIVED,
                            transfer.getAmount(),
                            transfer.getUsername()
                    ));
                });
            }

            userLogs.sort(Comparator.comparing(LogModel::getTimestamp));

            double balance = calculateBalance(userLogs);

            Path userFile = outputPath.resolve(username + ".log");
            try (BufferedWriter writer = Files.newBufferedWriter(userFile)) {
                for (LogModel log : userLogs) {
                    writer.write(formatLogEntry(log));
                    writer.newLine();
                }

                writer.write(String.format("[%s] %s final balance %.2f",
                        LocalDateTime.now().format(DATE_FORMATTER),
                        username,
                        balance
                ));
            }
        }
    }

    private static String formatLogEntry(LogModel log) {
        switch (log.getOperationType()) {
            case BALANCE:
                return String.format("[%s] %s balance inquiry %.2f",
                        log.getTimestamp().format(DATE_FORMATTER),
                        log.getUsername(),
                        log.getAmount());

            case TRANSFERRED:
                if (log.getTargetUsername() == null) {
                    return String.format("[%s] %s received %.2f from %s",
                            log.getTimestamp().format(DATE_FORMATTER),
                            log.getUsername(),
                            log.getAmount(),
                            log.getTargetUsername());
                } else {
                    return String.format("[%s] %s transferred %.2f to %s",
                            log.getTimestamp().format(DATE_FORMATTER),
                            log.getUsername(),
                            log.getAmount(),
                            log.getTargetUsername());
                }

            case WITHDREW:
                return String.format("[%s] %s withdrew %.2f",
                        log.getTimestamp().format(DATE_FORMATTER),
                        log.getUsername(),
                        log.getAmount());
            case RECEIVED:
                return String.format("[%s] %s received %.2f from %s",
                        log.getTimestamp().format(DATE_FORMATTER),
                        log.getUsername(),
                        log.getAmount(),
                        log.getTargetUsername());
            default:
                throw new IllegalStateException("Unknown operation type");
        }
    }

    private static double calculateBalance(List<LogModel> logs) {
        double balance = 0;
        for (LogModel log : logs) {
            switch (log.getOperationType()) {
                case TRANSFERRED:
                    if (log.getTargetUsername() == null) {
                        // Это полученный перевод
                        balance += log.getAmount();
                    } else {
                        // Это исходящий перевод
                        balance -= log.getAmount();
                    }
                    break;
                case WITHDREW:
                    balance -= log.getAmount();
                    break;
                case RECEIVED:
                    balance += log.getAmount();
                    break;
                case BALANCE:
                    if (log == logs.get(0)) {balance += log.getAmount();}
                    else {balance = log.getAmount();}
                    break;
            }
        }
        return balance;
    }
}