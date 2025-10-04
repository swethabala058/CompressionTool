import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.zip.*;

public class CompressTool {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static int totalOperations = 0;
    private static long totalBytesSaved = 0;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        displayWelcomeBanner();
        
        while (true) {
            displayMainMenu();
            
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a number between 1-7.");
                continue;
            }
            
            switch (choice) {
                case 1:
                    compressGZIP(scanner);
                    break;
                case 2:
                    decompressGZIP(scanner);
                    break;
                case 3:
                    compressZIP(scanner);
                    break;
                case 4:
                    decompressZIP(scanner);
                    break;
                case 5:
                    listFilesInDirectory();
                    break;
                case 6:
                    displayStatistics();
                    break;
                case 7:
                    System.out.println("\n" + getCurrentTime() + " Thank you for using the Text Compression and Decompression Tool!");
                    displayExitBanner();
                    scanner.close();
                    return;
                default:
                    System.out.println("❌ Invalid option. Please choose 1-7.");
            }
            
            System.out.println("\n" + "═".repeat(50));
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void displayWelcomeBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║           TEXT COMPRESSION AND DECOMPRESSION TOOL            ║");
        System.out.println("║                 Advanced Console Edition                     ║");
        System.out.println("║                                                              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println("Version 2.0 | " + LocalDate.now().getYear() + " | Java " + System.getProperty("java.version"));
        System.out.println();
    }
    
    private static void displayExitBanner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                     Operation Summary                        ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Operations: %-40d ║\n", totalOperations);
        System.out.printf("║ Total Bytes Saved: %-38s ║\n", formatBytes(totalBytesSaved));
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }
    
    private static void displayMainMenu() {
        System.out.println("\n" + "═".repeat(50));
        System.out.println("📋 MAIN MENU (" + getCurrentTime() + ")");
        System.out.println("═".repeat(50));
        System.out.println("1. 📦 Compress File (GZIP)");
        System.out.println("2. 📤 Decompress File (GZIP)");
        System.out.println("3. 🗂️  Compress Files/Directory (ZIP)");
        System.out.println("4. 📂 Decompress Archive (ZIP)");
        System.out.println("5. 📊 List Files in Current Directory");
        System.out.println("6. 📈 View Statistics");
        System.out.println("7. 🚪 Exit");
        System.out.println("═".repeat(50));
        System.out.print("Choose an option (1-7): ");
    }
    
    private static void displayStatistics() {
        System.out.println("\n📈 COMPRESSION STATISTICS");
        System.out.println("═".repeat(50));
        System.out.printf("Total operations performed: %d\n", totalOperations);
        System.out.printf("Total bytes saved: %s\n", formatBytes(totalBytesSaved));
        System.out.printf("Current directory: %s\n", System.getProperty("user.dir"));
        System.out.printf("Available memory: %s\n", formatBytes(Runtime.getRuntime().freeMemory()));
        System.out.println("═".repeat(50));
    }
    
    private static String getCurrentTime() {
        return "[" + LocalTime.now().format(TIME_FORMATTER) + "]";
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private static void listFilesInDirectory() {
        System.out.println("\n📁 FILES IN CURRENT DIRECTORY");
        System.out.println("═".repeat(50));
        
        File currentDir = new File(".");
        File[] files = currentDir.listFiles();
        
        if (files == null || files.length == 0) {
            System.out.println("No files found in current directory.");
            System.out.println("Directory: " + System.getProperty("user.dir"));
            return;
        }
        
        Arrays.sort(files, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) return -1;
            if (!f1.isDirectory() && f2.isDirectory()) return 1;
            return f1.getName().compareToIgnoreCase(f2.getName());
        });
        
        for (File file : files) {
            String icon = file.isDirectory() ? "📁" : "📄";
            String size = file.isDirectory() ? "(dir)" : "(" + formatBytes(file.length()) + ")";
            String modified = Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            
            System.out.printf("%s %-30s %-12s %s\n", icon, file.getName(), size, modified);
        }
        System.out.println("═".repeat(50));
        System.out.printf("Total: %d items\n", files.length);
    }
    
    private static void compressGZIP(Scanner scanner) {
        System.out.println("\n🎯 GZIP COMPRESSION");
        System.out.println("═".repeat(50));
        
        System.out.print("Enter source file path: ");
        String sourcePath = scanner.nextLine().trim();

        if (sourcePath.isEmpty()) {
            System.out.println("❌ No file path provided.");
            return;
        }

        // Handle relative paths by converting to absolute path
        File sourceFile = new File(sourcePath);
        if (!sourceFile.isAbsolute()) {
            sourceFile = new File(System.getProperty("user.dir"), sourcePath);
        }

        System.out.println("🔍 Looking for file: " + sourceFile.getAbsolutePath());
        
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            System.out.println("❌ Error: File '" + sourceFile.getAbsolutePath() + "' not found!");
            System.out.println("📁 Current directory: " + System.getProperty("user.dir"));
            System.out.println("\n📋 Available files in current directory:");
            listFilesInDirectory();
            return;
        }

        // ALWAYS save in the same directory as source file
        String sourceDir = sourceFile.getParent();
        String fileName = sourceFile.getName();
        String destPath = sourceDir + File.separator + fileName + ".gz";

        // Handle file name conflicts
        File destFile = new File(destPath);
        int counter = 1;
        while (destFile.exists()) {
            String nameWithoutExt = fileName.contains(".") ? 
                fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            destPath = sourceDir + File.separator + nameWithoutExt + "_" + counter + ".gz";
            destFile = new File(destPath);
            counter++;
        }

        System.out.println("💾 Source: " + sourceFile.getAbsolutePath());
        System.out.println("💾 Output: " + destPath);
        System.out.println(getCurrentTime() + " Starting compression...");
        
        try {
            long startTime = System.currentTimeMillis();
            long originalSize = sourceFile.length();
            
            // Verify we can write to destination directory
            File destDir = new File(sourceDir);
            if (!destDir.canWrite()) {
                System.out.println("❌ Error: No write permission in directory: " + sourceDir);
                return;
            }

            try (FileInputStream fis = new FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(destPath);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                System.out.print("Progress: [");
                while ((bytesRead = fis.read(buffer)) != -1) {
                    gzos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    int progress = (int) ((totalBytesRead * 50) / originalSize);
                    System.out.print("=".repeat(Math.min(50, progress)) + ">" + " ".repeat(Math.max(0, 50 - progress)) + "]");
                    System.out.printf(" %d%%\r", Math.min(100, (progress * 2)));
                }
            }
            
            long endTime = System.currentTimeMillis();
            File compressedFile = new File(destPath);
            long compressedSize = compressedFile.length();
            long bytesSaved = originalSize - compressedSize;
            
            System.out.println("\n✅ " + getCurrentTime() + " Compression completed!");
            System.out.println("═".repeat(50));
            System.out.printf("Original size:    %s\n", formatBytes(originalSize));
            System.out.printf("Compressed size:  %s\n", formatBytes(compressedSize));
            System.out.printf("Compression ratio: %.1f%%\n", (1 - (double)compressedSize / originalSize) * 100);
            System.out.printf("Space saved:      %s\n", formatBytes(bytesSaved));
            System.out.printf("Time taken:       %d ms\n", (endTime - startTime));
            System.out.println("📍 Source:        " + sourceFile.getAbsolutePath());
            System.out.println("📍 Compressed:    " + compressedFile.getAbsolutePath());
            
            // Verify the file was actually created
            if (compressedFile.exists() && compressedFile.length() > 0) {
                System.out.println("✅ Verification: File successfully created and saved!");
            } else {
                System.out.println("❌ Verification: File was not created properly!");
            }

            totalOperations++;
            totalBytesSaved += bytesSaved;
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during compression: " + e.getMessage());
            // Clean up failed file
            new File(destPath).delete();
        }
    }
    
    private static void decompressGZIP(Scanner scanner) {
        System.out.println("\n🎯 GZIP DECOMPRESSION");
        System.out.println("═".repeat(50));

        System.out.print("Enter compressed file path (.gz): ");
        String sourcePath = scanner.nextLine().trim();

        if (sourcePath.isEmpty()) {
            System.out.println("❌ No file path provided.");
            return;
        }

        // Handle relative paths
        File sourceFile = new File(sourcePath);
        if (!sourceFile.isAbsolute()) {
            sourceFile = new File(System.getProperty("user.dir"), sourcePath);
        }

        System.out.println("🔍 Looking for file: " + sourceFile.getAbsolutePath());
        
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            System.out.println("❌ Error: File '" + sourceFile.getAbsolutePath() + "' not found!");
            System.out.println("📁 Current directory: " + System.getProperty("user.dir"));
            System.out.println("\n📋 Available files in current directory:");
            listFilesInDirectory();
            return;
        }

        // ALWAYS save in the same directory as source file with "_decompressed" suffix
        String sourceDir = sourceFile.getParent();
        String fileName = sourceFile.getName();
        String destPath;
        
        if (fileName.toLowerCase().endsWith(".gz")) {
            // Remove .gz extension and add _decompressed
            String baseName = fileName.substring(0, fileName.length() - 3);
            destPath = sourceDir + File.separator + baseName + "_decompressed";
        } else {
            // For non-gz files, just add _decompressed
            destPath = sourceDir + File.separator + fileName + "_decompressed";
        }
        
        // Handle file name conflicts
        File destFile = new File(destPath);
        int counter = 1;
        while (destFile.exists()) {
            String nameWithoutExt = destPath.contains(".") ? 
                destPath.substring(0, destPath.lastIndexOf('.')) : destPath;
            String extension = destPath.contains(".") ? 
                destPath.substring(destPath.lastIndexOf('.')) : "";
            destPath = nameWithoutExt + "_" + counter + extension;
            destFile = new File(destPath);
            counter++;
        }

        System.out.println("💾 Source: " + sourceFile.getAbsolutePath());
        System.out.println("💾 Output: " + destPath);
        System.out.println(getCurrentTime() + " Starting decompression...");
        
        try {
            long startTime = System.currentTimeMillis();
            long compressedSize = sourceFile.length();
            long decompressedSize = 0;
            
            // Verify write permissions
            File destDir = new File(sourceDir);
            if (!destDir.canWrite()) {
                System.out.println("❌ Error: No write permission in directory: " + sourceDir);
                return;
            }

            try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(sourceFile));
                 FileOutputStream fos = new FileOutputStream(destPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                System.out.print("Progress: [");
                while ((bytesRead = gzis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    decompressedSize += bytesRead;
                    System.out.print("▓");
                }
                fos.flush();
            }
            
            long endTime = System.currentTimeMillis();
            
            System.out.println("]");
            System.out.println("✅ " + getCurrentTime() + " Decompression completed!");
            System.out.println("═".repeat(50));
            System.out.printf("Compressed size:   %s\n", formatBytes(compressedSize));
            System.out.printf("Decompressed size: %s\n", formatBytes(decompressedSize));
            System.out.printf("Size difference:   %s\n", formatBytes(decompressedSize - compressedSize));
            System.out.printf("Time taken:        %d ms\n", (endTime - startTime));
            System.out.println("📍 Source:         " + sourceFile.getAbsolutePath());
            System.out.println("📍 Decompressed:   " + new File(destPath).getAbsolutePath());
            
            // Verify file creation
            File outputFile = new File(destPath);
            if (outputFile.exists() && outputFile.length() > 0) {
                System.out.println("✅ Verification: File successfully created and saved!");
                System.out.println("✅ File naming: Used '_decompressed' suffix as requested");
            } else {
                System.out.println("❌ Verification: File was not created properly!");
            }
            
            totalOperations++;
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during decompression: " + e.getMessage());
            new File(destPath).delete();
        }
    }
    
    private static void compressZIP(Scanner scanner) {
        System.out.println("\n🎯 ZIP COMPRESSION");
        System.out.println("═".repeat(50));
        
        System.out.print("Enter source file/directory path: ");
        String sourcePath = scanner.nextLine().trim();
        
        if (sourcePath.isEmpty()) {
            System.out.println("❌ No path provided.");
            return;
        }
        
        // Handle relative paths
        File sourceFile = new File(sourcePath);
        if (!sourceFile.isAbsolute()) {
            sourceFile = new File(System.getProperty("user.dir"), sourcePath);
        }

        System.out.println("🔍 Looking for: " + sourceFile.getAbsolutePath());
        
        if (!sourceFile.exists()) {
            System.out.println("❌ Error: File or directory '" + sourceFile.getAbsolutePath() + "' not found!");
            System.out.println("📁 Current directory: " + System.getProperty("user.dir"));
            return;
        }
        
        long originalSize = calculateTotalSize(sourceFile);
        
        // ALWAYS save in the same directory as source
        String sourceDir = sourceFile.getParent();
        String fileName = sourceFile.getName();
        String destPath = sourceDir + File.separator + fileName + ".zip";
        
        // Handle naming conflicts
        File destFile = new File(destPath);
        int counter = 1;
        while (destFile.exists()) {
            destPath = sourceDir + File.separator + fileName + "_" + counter + ".zip";
            destFile = new File(destPath);
            counter++;
        }
        
        System.out.println("💾 Source: " + sourceFile.getAbsolutePath());
        System.out.println("💾 Output: " + destPath);
        System.out.println(getCurrentTime() + " Starting ZIP compression...");
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Verify write permissions
            File destDir = new File(sourceDir);
            if (!destDir.canWrite()) {
                System.out.println("❌ Error: No write permission in directory: " + sourceDir);
                return;
            }

            try (FileOutputStream fos = new FileOutputStream(destPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                if (sourceFile.isDirectory()) {
                    System.out.println("📁 Zipping directory: " + sourceFile.getName());
                    compressDirectory(sourceFile, sourceFile.getName(), zos);
                } else {
                    System.out.println("📄 Zipping file: " + sourceFile.getName());
                    compressSingleFile(sourceFile, zos);
                }
            }
            
            long endTime = System.currentTimeMillis();
            File compressedFile = new File(destPath);
            long compressedSize = compressedFile.length();
            long bytesSaved = originalSize - compressedSize;
            
            System.out.println("✅ " + getCurrentTime() + " ZIP compression completed!");
            System.out.println("═".repeat(50));
            System.out.printf("Original size:    %s\n", formatBytes(originalSize));
            System.out.printf("Compressed size:  %s\n", formatBytes(compressedSize));
            System.out.printf("Compression ratio: %.1f%%\n", (1 - (double)compressedSize / originalSize) * 100);
            System.out.printf("Space saved:      %s\n", formatBytes(bytesSaved));
            System.out.printf("Time taken:       %d ms\n", (endTime - startTime));
            System.out.println("📍 Source:        " + sourceFile.getAbsolutePath());
            System.out.println("📍 ZIP file:      " + compressedFile.getAbsolutePath());
            
            // Verify file creation
            if (compressedFile.exists() && compressedFile.length() > 0) {
                System.out.println("✅ Verification: ZIP file successfully created!");
            } else {
                System.out.println("❌ Verification: ZIP file was not created properly!");
            }
            
            totalOperations++;
            totalBytesSaved += bytesSaved;
            
        } catch (IOException e) {
            System.out.println("❌ " + getCurrentTime() + " Error during ZIP compression: " + e.getMessage());
            new File(destPath).delete();
        }
    }
    
    private static void decompressZIP(Scanner scanner) {
        System.out.println("\n🎯 ZIP DECOMPRESSION");
        System.out.println("═".repeat(50));
        
        System.out.print("Enter ZIP file path: ");
        String zipFilePath = scanner.nextLine().trim();
        
        if (zipFilePath.isEmpty()) {
            System.out.println("❌ No file path provided.");
            return;
        }
        
        // Handle relative paths
        File zipFile = new File(zipFilePath);
        if (!zipFile.isAbsolute()) {
            zipFile = new File(System.getProperty("user.dir"), zipFilePath);
        }

        System.out.println("🔍 Looking for file: " + zipFile.getAbsolutePath());
        
        if (!zipFile.exists() || !zipFile.isFile()) {
            System.out.println("❌ Error: ZIP file '" + zipFile.getAbsolutePath() + "' not found!");
            System.out.println("📁 Current directory: " + System.getProperty("user.dir"));
            return;
        }
        
        long compressedSize = zipFile.length();
        
        // ALWAYS extract to same directory as ZIP file with "_decompressed" suffix
        String zipDir = zipFile.getParent();
        String zipFileName = zipFile.getName();
        String baseName = zipFileName.contains(".") ? 
            zipFileName.substring(0, zipFileName.lastIndexOf('.')) : zipFileName;
        String destDirectory = zipDir + File.separator + baseName + "_decompressed";
        
        // Handle directory name conflicts
        File destDir = new File(destDirectory);
        int counter = 1;
        while (destDir.exists()) {
            destDirectory = zipDir + File.separator + baseName + "_decompressed_" + counter;
            destDir = new File(destDirectory);
            counter++;
        }
        
        System.out.println("💾 ZIP file: " + zipFile.getAbsolutePath());
        System.out.println("💾 Extract to: " + destDirectory);
        
        // Create destination directory
        if (!destDir.mkdirs()) {
            System.out.println("❌ Error: Could not create destination directory: " + destDirectory);
            return;
        }

        // Verify write permissions
        File parentDir = new File(zipDir);
        if (!parentDir.canWrite()) {
            System.out.println("❌ Error: No write permission in directory: " + zipDir);
            return;
        }

        System.out.println(getCurrentTime() + " Starting ZIP decompression...");
        
        try {
            long startTime = System.currentTimeMillis();
            int fileCount = 0;
            int dirCount = 0;
            long totalExtractedSize = 0;
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
                ZipEntry zipEntry;
                
                System.out.print("Extracting: ");
                
                while ((zipEntry = zis.getNextEntry()) != null) {
                    String entryName = zipEntry.getName();
                    String filePath = destDirectory + File.separator + entryName;
                    
                    // Security check
                    File outputFile = new File(filePath);
                    String canonicalDestPath = outputFile.getCanonicalPath();
                    if (!canonicalDestPath.startsWith(destDir.getCanonicalPath() + File.separator)) {
                        System.out.println("\n❌ Security: Skipping malicious path - " + entryName);
                        zis.closeEntry();
                        continue;
                    }
                    
                    if (zipEntry.isDirectory()) {
                        if (!outputFile.exists() && !outputFile.mkdirs()) {
                            System.out.println("\n⚠️  Warning: Could not create directory - " + entryName);
                        } else {
                            dirCount++;
                        }
                    } else {
                        File parentDirFile = outputFile.getParentFile();
                        if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
                            System.out.println("\n❌ Error: Could not create parent directory for - " + entryName);
                            zis.closeEntry();
                            continue;
                        }
                        
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long fileSize = 0;
                            
                            while ((bytesRead = zis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                fileSize += bytesRead;
                            }
                            fos.flush();
                            totalExtractedSize += fileSize;
                            fileCount++;
                            System.out.print("▓");
                        }
                    }
                    zis.closeEntry();
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            System.out.println();
            System.out.println("✅ " + getCurrentTime() + " ZIP decompression completed!");
            System.out.println("═".repeat(50));
            System.out.printf("Compressed size:   %s\n", formatBytes(compressedSize));
            System.out.printf("Extracted size:    %s\n", formatBytes(totalExtractedSize));
            System.out.printf("Files extracted:   %d\n", fileCount);
            System.out.printf("Directories:       %d\n", dirCount);
            System.out.printf("Time taken:        %d ms\n", (endTime - startTime));
            System.out.println("📍 ZIP location:   " + zipFile.getAbsolutePath());
            System.out.println("📍 Extracted to:   " + destDir.getAbsolutePath());
            
            // Verify extraction
            File[] extractedFiles = destDir.listFiles();
            if (extractedFiles != null && extractedFiles.length > 0) {
                System.out.println("✅ Verification: " + extractedFiles.length + " items extracted successfully!");
                System.out.println("✅ Folder naming: Used '_decompressed' suffix as requested");
            } else {
                System.out.println("⚠️  Warning: No files found in extraction directory!");
            }
            
            totalOperations++;
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during ZIP decompression: " + e.getMessage());
        }
    }
    
    private static void compressDirectory(File directory, String baseName, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                compressDirectory(file, baseName + "/" + file.getName(), zos);
            } else {
                compressSingleFile(file, baseName + "/" + file.getName(), zos);
            }
        }
    }
    
    private static void compressSingleFile(File file, ZipOutputStream zos) throws IOException {
        compressSingleFile(file, file.getName(), zos);
    }
    
    private static void compressSingleFile(File file, String entryName, ZipOutputStream zos) throws IOException {
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
        System.out.println("  ➕ Added: " + entryName);
    }
    
    private static long calculateTotalSize(File file) {
        long size = 0;
        try {
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
        } catch (SecurityException e) {
            System.out.println("❌ Access denied to: " + file.getPath());
        }
        return size;
    }
}