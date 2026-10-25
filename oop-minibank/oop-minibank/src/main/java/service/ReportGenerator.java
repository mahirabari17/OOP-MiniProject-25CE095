package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/** Reads every *.log file in a folder and writes an end-of-day summary report. */
public class ReportGenerator {
    private ReportGenerator() {
    }

    public static String generate(Path logsDir, Path reportFile) throws IOException {
        long deposits = 0;
        long withdrawals = 0;
        long transfers = 0;
        int lines = 0;
        StringBuilder files = new StringBuilder();

        if (Files.isDirectory(logsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, "*.log")) {
                for (Path file : stream) {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    if (attrs.size() == 0) {
                        continue;                       // skip empty files
                    }
                    files.append(String.format("  %s  (%d bytes, modified %s)%n",
                            file.getFileName(), attrs.size(), attrs.lastModifiedTime()));
                    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] p = line.trim().split("\\s+");
                            if (p.length < 3) {
                                continue;
                            }
                            long amount = Long.parseLong(p[p.length - 1]);
                            switch (p[0]) {
                                case "DEPOSIT" -> deposits += amount;
                                case "WITHDRAW" -> withdrawals += amount;
                                case "TRANSFER" -> transfers += amount;
                                default -> { }
                            }
                            lines++;
                        }
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("======= MINIBANK END-OF-DAY REPORT =======\n");
        sb.append("Log files read     :\n").append(files.length() == 0 ? "  (none)\n" : files.toString());
        sb.append("Transactions read  : ").append(lines).append('\n');
        sb.append("Total deposits     : ").append(deposits).append('\n');
        sb.append("Total withdrawals  : ").append(withdrawals).append('\n');
        sb.append("Total transferred  : ").append(transfers).append('\n');
        sb.append("Net change         : ").append(deposits - withdrawals).append('\n');
        sb.append("==========================================");

        if (reportFile.getParent() != null) {
            Files.createDirectories(reportFile.getParent());
        }
        Files.writeString(reportFile, sb.toString(), StandardCharsets.UTF_8);
        return sb.toString();
    }
}
