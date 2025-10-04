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
        
        // Sort files by type and name
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
        
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            System.out.println("❌ Error: File '" + sourcePath + "' not found!");
            System.out.println("Current directory: " + System.getProperty("user.dir"));
            return;
        }
        
        String defaultName = sourceFile.getName() + ".gz";
        System.out.printf("Enter destination file path [%s]: ", defaultName);
        String destPath = scanner.nextLine().trim();
        
        if (destPath.isEmpty()) {
            destPath = defaultName;
        }
        
        if (!destPath.toLowerCase().endsWith(".gz")) {
            destPath += ".gz";
        }
        
        System.out.println(getCurrentTime() + " Starting compression...");
        
        try {
            long startTime = System.currentTimeMillis();
            long originalSize = sourceFile.length();
            
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
                    System.out.print("=".repeat(progress) + ">" + " ".repeat(50 - progress) + "]");
                    System.out.printf(" %d%%\r", (progress * 2));
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
            System.out.printf("Compression ratio: %.1f%%\n", 
                (1 - (double)compressedSize / originalSize) * 100);
            System.out.printf("Space saved:      %s\n", formatBytes(bytesSaved));
            System.out.printf("Time taken:       %d ms\n", (endTime - startTime));
            System.out.println("Output file:      " + destPath);
            
            totalOperations++;
            totalBytesSaved += bytesSaved;
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during compression: " + e.getMessage());
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
        
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            System.out.println("❌ Error: File '" + sourcePath + "' not found!");
            return;
        }
        
        String defaultName = sourceFile.getName().replace(".gz", "");
        System.out.printf("Enter destination file path [%s]: ", defaultName);
        String destPath = scanner.nextLine().trim();
        
        if (destPath.isEmpty()) {
            destPath = defaultName;
        }
        
        System.out.println(getCurrentTime() + " Starting decompression...");
        
        try {
            long startTime = System.currentTimeMillis();
            long compressedSize = sourceFile.length();
            
            try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(sourcePath));
                 FileOutputStream fos = new FileOutputStream(destPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                int animationState = 0;
                
                System.out.print("Progress: [");
                while ((bytesRead = gzis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Simple animation since we don't know the decompressed size in advance
                    animationState = (animationState + 1) % 50;
                    System.out.print("=".repeat(animationState) + ">" + " ".repeat(50 - animationState) + "]");
                    System.out.print(" Decompressing...\r");
                    
                    // Small delay for animation visibility
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            File decompressedFile = new File(destPath);
            long decompressedSize = decompressedFile.length();
            
            System.out.println("\n✅ " + getCurrentTime() + " Decompression completed!");
            System.out.println("═".repeat(50));
            System.out.printf("Compressed size:   %s\n", formatBytes(compressedSize));
            System.out.printf("Decompressed size: %s\n", formatBytes(decompressedSize));
            System.out.printf("Size difference:   %s\n", formatBytes(decompressedSize - compressedSize));
            System.out.printf("Time taken:        %d ms\n", (endTime - startTime));
            System.out.println("Output file:       " + destPath);
            
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
        
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            System.out.println("❌ Error: File or directory '" + sourcePath + "' not found!");
            return;
        }
        
        long originalSize = calculateTotalSize(sourceFile);
        
        String defaultName = sourceFile.getName() + ".zip";
        System.out.printf("Enter destination ZIP file path [%s]: ", defaultName);
        String destPath = scanner.nextLine().trim();
        
        if (destPath.isEmpty()) {
            destPath = defaultName;
        }
        
        if (!destPath.toLowerCase().endsWith(".zip")) {
            destPath += ".zip";
        }
        
        System.out.println(getCurrentTime() + " Starting ZIP compression...");
        
        try {
            long startTime = System.currentTimeMillis();
            
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
            System.out.printf("Compression ratio: %.1f%%\n", 
                (1 - (double)compressedSize / originalSize) * 100);
            System.out.printf("Space saved:      %s\n", formatBytes(bytesSaved));
            System.out.printf("Time taken:       %d ms\n", (endTime - startTime));
            System.out.println("Output file:      " + destPath);
            
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
        
        File zipFile = new File(zipFilePath);
        if (!zipFile.exists() || !zipFile.isFile()) {
            System.out.println("❌ Error: ZIP file '" + zipFilePath + "' not found!");
            return;
        }
        
        long compressedSize = zipFile.length();
        
        System.out.print("Enter destination directory: ");
        String destDirectory = scanner.nextLine().trim();
        
        if (destDirectory.isEmpty()) {
            destDirectory = "extracted_" + System.currentTimeMillis();
        }
        
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        
        System.out.println(getCurrentTime() + " Starting ZIP decompression...");
        
        try {
            long startTime = System.currentTimeMillis();
            int fileCount = 0;
            long totalExtractedSize = 0;
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
                ZipEntry zipEntry = zis.getNextEntry();
                
                System.out.print("Progress: [");
                int totalEntries = countZipEntries(zipFilePath);
                int processedEntries = 0;
                
                while (zipEntry != null) {
                    String filePath = destDirectory + File.separator + zipEntry.getName();
                    
                    if (!zipEntry.isDirectory()) {
                        long fileSize = extractFile(zis, filePath);
                        totalExtractedSize += fileSize;
                        System.out.printf("📄 Extracted: %s (%,d bytes)%n", zipEntry.getName(), fileSize);
                        fileCount++;
                    } else {
                        File dir = new File(filePath);
                        dir.mkdirs();
                        System.out.println("📁 Created directory: " + zipEntry.getName());
                    }
                    
                    processedEntries++;
                    int progress = (int) ((processedEntries * 50) / totalEntries);
                    System.out.print("=".repeat(progress) + ">" + " ".repeat(50 - progress) + "]");
                    System.out.printf(" %d%%\r", (progress * 2));
                    
                    zis.closeEntry();
                    zipEntry = zis.getNextEntry();
                }
            }
            
            long endTime = System.currentTimeMillis();
            
            System.out.println("\n✅ " + getCurrentTime() + " ZIP decompression completed!");
            System.out.println("═".repeat(50));
            System.out.printf("Compressed size:   %s\n", formatBytes(compressedSize));
            System.out.printf("Extracted size:    %s\n", formatBytes(totalExtractedSize));
            System.out.printf("Size difference:   %s\n", formatBytes(totalExtractedSize - compressedSize));
            System.out.printf("Time taken:        %d ms\n", (endTime - startTime));
            System.out.printf("Files extracted:   %d\n", fileCount);
            System.out.println("Output directory:  " + destDirectory);
            
            totalOperations++;
            
        } catch (IOException e) {
            System.out.println("❌ " + getCurrentTime() + " Error during ZIP decompression: " + e.getMessage());
        }
    }
    
    private static int countZipEntries(String zipFilePath) throws IOException {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            while (zis.getNextEntry() != null) {
                count++;
            }
        }
        return count;
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
    
    private static long extractFile(ZipInputStream zis, String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        
        long fileSize = 0;
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = zis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize += bytesRead;
            }
        }
        
        return fileSize;
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