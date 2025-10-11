package com.example.compressiontool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/compression")
public class CompressionController {

    @Autowired
    private CompressionService compressionService;

    @Autowired
    private ActivityRepository activityRepository;

    @PostMapping("/compress/gzip")
    public ResponseEntity<?> compressGzip(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Save uploaded file to temp location
            Path tempDir = Files.createTempDirectory("upload");
            File sourceFile = new File(tempDir.toFile(), file.getOriginalFilename());
            file.transferTo(sourceFile);

            // Generate output file
            String baseName = sourceFile.getName().contains(".") ?
                    sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')) :
                    sourceFile.getName();
            File outputFile = compressionService.getUniqueOutputFile(sourceFile.getParentFile(), baseName, ".gz");

            // Compress
            compressionService.compressGZIP(sourceFile, outputFile);

            // Log activity
            long originalSize = sourceFile.length();
            long compressedSize = outputFile.length();
            activityRepository.save(new Activity(OperationType.COMPRESS_GZIP, file.getOriginalFilename(), originalSize, compressedSize));

            // Prepare response with file download
            Path path = Paths.get(outputFile.getAbsolutePath());
            Resource resource = new UrlResource(path.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFile.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString());

            // Clean up temp files
            sourceFile.delete();
            // Note: Output file will be deleted after download or by client

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during compression: " + e.getMessage());
        }
    }

    @PostMapping("/compress/zip")
    public ResponseEntity<?> compressZip(@RequestParam(value = "file", required = false) MultipartFile file,
                                         @RequestParam(value = "path", required = false) String path,
                                         @RequestParam(value = "isDirectory", defaultValue = "false") boolean isDirectory) {
        try {
            File sourceFile;
            Path tempDir = null;
            if (path != null && isDirectory) {
                sourceFile = new File(path);
                if (!sourceFile.exists()) {
                    return ResponseEntity.badRequest().body("Directory path not found");
                }
            } else if (file != null && !file.isEmpty()) {
                tempDir = Files.createTempDirectory("upload");
                sourceFile = new File(tempDir.toFile(), file.getOriginalFilename());
                file.transferTo(sourceFile);
            } else {
                return ResponseEntity.badRequest().body("Provide either a file or a directory path");
            }

            // Generate output file
            String baseName = sourceFile.getName().contains(".") ?
                    sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')) :
                    sourceFile.getName();
            File outputFile = compressionService.getUniqueOutputFile(sourceFile.getParentFile(), baseName, ".zip");

            // Compress
            compressionService.compressZIP(sourceFile, outputFile);

            // Log activity
            long originalSize = compressionService.calculateTotalSize(sourceFile);
            long compressedSize = outputFile.length();
            activityRepository.save(new Activity(OperationType.COMPRESS_ZIP, sourceFile.getName(), originalSize, compressedSize));

            // Prepare response with file download
            Path pathOut = Paths.get(outputFile.getAbsolutePath());
            Resource resource = new UrlResource(pathOut.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFile.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString());

            // Clean up temp files
            if (tempDir != null) {
                sourceFile.delete();
            }
            // Note: Output file will be deleted after download or by client

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during ZIP compression: " + e.getMessage());
        }
    }

    @PostMapping("/decompress/gzip")
    public ResponseEntity<?> decompressGzip(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Validate GZIP
            File tempSource = File.createTempFile("temp_gzip", ".gz");
            file.transferTo(tempSource);
            if (!compressionService.isValidGzipFile(tempSource)) {
                tempSource.delete();
                return ResponseEntity.badRequest().body("Invalid GZIP file");
            }

            // Generate output file
            String baseName = tempSource.getName().replace(".gz", "");
            File outputFile = compressionService.getUniqueOutputFile(tempSource.getParentFile(), baseName, "_decompressed");

            // Decompress
            compressionService.decompressGZIP(tempSource, outputFile);

            // Log activity
            long compressedSize = tempSource.length();
            long originalSize = outputFile.length();
            activityRepository.save(new Activity(OperationType.DECOMPRESS_GZIP, file.getOriginalFilename(), originalSize, compressedSize));

            // Prepare response with file download
            Path path = Paths.get(outputFile.getAbsolutePath());
            Resource resource = new UrlResource(path.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputFile.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString());

            // Clean up
            tempSource.delete();

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during decompression: " + e.getMessage());
        }
    }

    @PostMapping("/decompress/zip")
    public ResponseEntity<?> decompressZip(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Save uploaded file to temp location
            Path tempDir = Files.createTempDirectory("upload");
            File sourceFile = new File(tempDir.toFile(), file.getOriginalFilename());
            file.transferTo(sourceFile);

            // Generate output directory
            String baseName = sourceFile.getName().replace(".zip", "");
            File outputDir = new File(tempDir.toFile(), baseName + "_extracted");

            // Decompress
            long[] stats = compressionService.decompressZIP(sourceFile, outputDir);

            // Log activity
            long compressedSize = sourceFile.length();
            long originalSize = compressionService.calculateTotalSize(outputDir);
            activityRepository.save(new Activity(OperationType.DECOMPRESS_ZIP, file.getOriginalFilename(), originalSize, compressedSize));

            // For simplicity, zip the extracted contents and return as a single file
            // Or return info; here we'll create a zip of extracted files
            File finalOutput = new File(tempDir.toFile(), baseName + "_extracted.zip");
            compressionService.compressZIP(outputDir, finalOutput);

            // Prepare response
            Path path = Paths.get(finalOutput.getAbsolutePath());
            Resource resource = new UrlResource(path.toUri());

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + finalOutput.getName());
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM.toString());

            // Clean up
            sourceFile.delete();
            deleteDirectory(outputDir);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during ZIP decompression: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    public ResponseEntity<String> getInfo(@RequestParam("path") String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return ResponseEntity.badRequest().body("File not found");
            }
            String info = compressionService.getFileInfo(file);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}
