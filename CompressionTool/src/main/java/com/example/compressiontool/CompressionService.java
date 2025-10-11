package com.example.compressiontool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.compressiontool.Activity;
import com.example.compressiontool.OperationType;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.zip.*;

@Service
public class CompressionService {

    @Autowired
    private ActivityRepository activityRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void compressGZIP(File sourceFile, File destFile) throws IOException {
        long originalSize = sourceFile.length();
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(destFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, bytesRead);
            }
            gzos.finish();
            fos.flush();
            fos.getFD().sync();
        }

        long compressedSize = destFile.length();

        Activity activity = new Activity();
        activity.setOperationType(OperationType.COMPRESS_GZIP);
        activity.setFileName(sourceFile.getName());
        activity.setOriginalSize(originalSize);
        activity.setResultSize(compressedSize);
        activity.setTimestamp(LocalDateTime.now());
        activityRepository.save(activity);
    }

    public long decompressGZIP(File sourceFile, File destFile) throws IOException {
        Files.createDirectories(destFile.getParentFile().toPath());

        long originalSize = sourceFile.length();
        long decompressedSize = 0;
        try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(sourceFile));
             FileOutputStream fos = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = gzis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                decompressedSize += bytesRead;
            }
            fos.flush();
            fos.getFD().sync();
        }

        if (!destFile.exists() || destFile.length() == 0) {
            throw new IOException("Output file was not created or is empty");
        }

        Activity activity = new Activity();
        activity.setOperationType(OperationType.DECOMPRESS_GZIP);
        activity.setFileName(sourceFile.getName());
        activity.setOriginalSize(originalSize);
        activity.setResultSize(decompressedSize);
        activity.setTimestamp(LocalDateTime.now());
        activityRepository.save(activity);

        return decompressedSize;
    }

    public void compressZIP(File source, File destFile) throws IOException {
        long originalSize = calculateTotalSize(source);
        try (FileOutputStream fos = new FileOutputStream(destFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            if (source.isDirectory()) {
                zipDirectory(source, source.getName(), zos);
            } else {
                addFileToZip(source, source.getName(), zos);
            }
        }

        long compressedSize = destFile.length();

        Activity activity = new Activity();
        activity.setOperationType(OperationType.COMPRESS_ZIP);
        activity.setFileName(source.getName());
        activity.setOriginalSize(originalSize);
        activity.setResultSize(compressedSize);
        activity.setTimestamp(LocalDateTime.now());
        activityRepository.save(activity);
    }

    public long[] decompressZIP(File sourceFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        long originalSize = sourceFile.length();
        int fileCount = 0;
        int dirCount = 0;
        long totalExtractedSize = 0;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(sourceFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String filePath = destDir.getAbsolutePath() + File.separator + entry.getName();
                File outputFile = new File(filePath);

                // Security check for zip slip
                if (!outputFile.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("Potential zip slip attack detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                    dirCount++;
                } else {
                    outputFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long fileSize = 0;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            fileSize += bytesRead;
                        }
                        fos.flush();
                        fos.getFD().sync();
                        totalExtractedSize += fileSize;
                    }
                    fileCount++;
                }
                zis.closeEntry();
            }
        }

        Activity activity = new Activity();
        activity.setOperationType(OperationType.DECOMPRESS_ZIP);
        activity.setFileName(sourceFile.getName());
        activity.setOriginalSize(originalSize);
        activity.setResultSize(totalExtractedSize);
        activity.setTimestamp(LocalDateTime.now());
        activityRepository.save(activity);

        return new long[]{fileCount, dirCount, totalExtractedSize};
    }

    private void zipDirectory(File directory, String baseName, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                zipDirectory(file, baseName + "/" + file.getName(), zos);
            } else {
                addFileToZip(file, baseName + "/" + file.getName(), zos);
            }
        }
    }

    private void addFileToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
        }
        zos.closeEntry();
    }

    public long calculateTotalSize(File file) {
        long size = 0;
        if (file.isFile()) {
            return file.length();
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    size += calculateTotalSize(f);
                }
            }
        }
        return size;
    }

    public String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public File getUniqueOutputFile(File parentDir, String baseName, String extension) {
        File outputFile = new File(parentDir, baseName + extension);
        int counter = 1;
        while (outputFile.exists()) {
            outputFile = new File(parentDir, baseName + "_" + counter + extension);
            counter++;
        }
        return outputFile;
    }

    public boolean isValidGzipFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[2];
            if (fis.read(header) != 2) return false;
            return header[0] == (byte) 0x1F && header[1] == (byte) 0x8B;
        }
    }

    public String getFileInfo(File file) {
        StringBuilder info = new StringBuilder();
        info.append("File Name: ").append(file.getName()).append("\n");
        info.append("File Size: ").append(formatBytes(file.length())).append("\n");
        info.append("File Type: ").append(file.isDirectory() ? "Directory" : getFileExtension(file)).append("\n");
        info.append("Last Modified: ").append(Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER)).append("\n");
        info.append("File Path: ").append(file.getAbsolutePath());
        return info.toString();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return file.isDirectory() ? "Directory" : "File";
        }
        return name.substring(lastIndexOf).toUpperCase() + " File";
    }
}
