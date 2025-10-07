import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.zip.*;

public class CompressTool {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Statistics tracking
    private static int totalOperations = 0;
    private static long totalBytesSaved = 0;
    private static int filesCompressed = 0;
    private static int filesDecompressed = 0;
    
    // Current operation files
    private static File currentCompressFile = null;
    private static File currentDecompressFile = null;
    
    // Output files
    private static File currentCompressOutput = null;
    private static File currentDecompressOutput = null;
    
    // Output information
    private static String currentOutputFormat = "-";
    private static String currentOutputLocation = "-";
    private static String currentEstimatedSize = "-";
    private static String currentCompressionRatio = "-";
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        displayWelcomeBanner();
        loadStatistics();
        
        while (true) {
            displayMainMenu();
            
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("❌ Invalid input. Please enter a number between 1-8.");
                continue;
            }
            
            switch (choice) {
                case 1:
                    handleFileSelection(scanner, "compress");
                    break;
                case 2:
                    handleFileSelection(scanner, "decompression");
                    break;
                case 3:
                    compressGZIP();
                    break;
                case 4:
                    decompressGZIP();
                    break;
                case 5:
                    compressZIP();
                    break;
                case 6:
                    decompressZIP();
                    break;
                case 7:
                    displayStatistics();
                    break;
                case 8:
                    System.out.println("\n" + getCurrentTime() + " Thank you for using the Advanced File Compression Tool!");
                    displayExitBanner();
                    saveStatistics();
                    scanner.close();
                    return;
                default:
                    System.out.println("❌ Invalid option. Please choose 1-8.");
            }
            
            System.out.println("\n" + "═".repeat(60));
            System.out.print("Press Enter to continue...");
            scanner.nextLine();
        }
    }
    
    private static void displayWelcomeBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                                    ║");
        System.out.println("║              🚀 ADVANCED FILE COMPRESSION TOOL                    ║");
        System.out.println("║                  Console Edition v2.1                             ║");
        System.out.println("║                                                                    ║");
        System.out.println("║           📦 GZIP & ZIP Compression/Decompression                 ║");
        System.out.println("║           📊 Detailed Statistics & Progress Tracking              ║");
        System.out.println("║           📁 File Information & Enhanced Logging                  ║");
        System.out.println("║                                                                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
        System.out.println("Version 2.1 | " + LocalDate.now().getYear() + " | Java " + System.getProperty("java.version"));
        System.out.println();
    }
    
    private static void displayExitBanner() {
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         OPERATION SUMMARY                         ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.printf("║ 📁 Total Operations:    %-40d ║\n", totalOperations);
        System.out.printf("║ 💾 Total Bytes Saved:   %-40s ║\n", formatBytes(totalBytesSaved));
        System.out.printf("║ 🗜️  Files Compressed:   %-40d ║\n", filesCompressed);
        System.out.printf("║ 📤 Files Decompressed:  %-40d ║\n", filesDecompressed);
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
    }
    
    private static void displayMainMenu() {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("📋 MAIN MENU (" + getCurrentTime() + ")");
        System.out.println("═".repeat(60));
        System.out.println("1. 📁 Select File for Compression");
        System.out.println("2. 📦 Select Archive for Decompression");
        System.out.println("3. 🗜️  Compress to GZIP");
        System.out.println("4. 📤 Decompress GZIP");
        System.out.println("5. 📦 Create ZIP Archive");
        System.out.println("6. 📂 Extract ZIP Archive");
        System.out.println("7. 📊 View Detailed Statistics");
        System.out.println("8. 🚪 Exit");
        System.out.println("═".repeat(60));
        
        // Display current selections
        if (currentCompressFile != null) {
            System.out.println("📄 Selected for compression: " + currentCompressFile.getName() + 
                             " (" + formatBytes(currentCompressFile.length()) + ")");
        } else {
            System.out.println("📄 No file selected for compression");
        }
        
        if (currentDecompressFile != null) {
            System.out.println("📦 Selected for decompression: " + currentDecompressFile.getName() + 
                             " (" + formatBytes(currentDecompressFile.length()) + ")");
        } else {
            System.out.println("📦 No file selected for decompression");
        }
        
        // Display output files if they exist
        if (currentCompressOutput != null && currentCompressOutput.exists()) {
            System.out.println("\n🗜️  COMPRESSION OUTPUT:");
            System.out.println("   📄 File: " + currentCompressOutput.getName());
            System.out.println("   💾 Size: " + formatBytes(currentCompressOutput.length()));
            System.out.println("   📈 Ratio: " + currentCompressionRatio);
            System.out.println("   📍 Location: " + currentCompressOutput.getParent());
        }
        
        if (currentDecompressOutput != null && currentDecompressOutput.exists()) {
            System.out.println("\n📤 DECOMPRESSION OUTPUT:");
            if (currentDecompressOutput.isDirectory()) {
                File[] files = currentDecompressOutput.listFiles();
                int itemCount = files != null ? files.length : 0;
                System.out.println("   📁 Directory: " + currentDecompressOutput.getName());
                System.out.println("   📊 Items: " + itemCount + " files/folders");
            } else {
                System.out.println("   📄 File: " + currentDecompressOutput.getName());
                System.out.println("   💾 Size: " + formatBytes(currentDecompressOutput.length()));
            }
            System.out.println("   📍 Location: " + currentDecompressOutput.getParent());
        }
        System.out.println("═".repeat(60));
        System.out.print("Choose an option (1-8): ");
    }
    
    private static void handleFileSelection(Scanner scanner, String type) {
        System.out.println("\n" + "═".repeat(60));
        if (type.equals("compress")) {
            System.out.println("📄 SELECT FILE FOR COMPRESSION");
        } else {
            System.out.println("📦 SELECT ARCHIVE FOR DECOMPRESSION");
        }
        System.out.println("═".repeat(60));
        
        // Show current directory files
        listFilesInDirectory();
        
        System.out.print("Enter file path (or 'back' to return): ");
        String filePath = scanner.nextLine().trim();
        
        if (filePath.equalsIgnoreCase("back")) {
            return;
        }
        
        if (filePath.isEmpty()) {
            System.out.println("❌ No file path provided.");
            return;
        }
        
        // Handle relative paths
        File file = new File(filePath);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), filePath);
        }
        
        System.out.println("🔍 Looking for: " + file.getAbsolutePath());
        
        if (!file.exists()) {
            System.out.println("❌ Error: File '" + file.getAbsolutePath() + "' not found!");
            return;
        }
        
        if (type.equals("compress")) {
            currentCompressFile = file;
            // Reset output information when new file is selected
            resetOutputInfo();
            displayFileInfo(file, "compression");
        } else {
            // Validate archive type
            if (!isValidArchiveType(file)) {
                System.out.println("❌ Invalid archive type. Please select .gz or .zip files for decompression.");
                return;
            }
            currentDecompressFile = file;
            // Reset output information when new file is selected
            resetOutputInfo();
            displayFileInfo(file, "decompression");
        }
        
        System.out.println("✅ File selected successfully!");
    }
    
    private static void resetOutputInfo() {
        currentOutputFormat = "-";
        currentOutputLocation = "-";
        currentEstimatedSize = "-";
        currentCompressionRatio = "-";
        currentCompressOutput = null;
        currentDecompressOutput = null;
    }
    
    private static void displayFileInfo(File file, String operationType) {
        System.out.println("\n📋 FILE INFORMATION");
        System.out.println("═".repeat(60));
        System.out.println("📄 File Name: " + file.getName());
        System.out.println("💾 File Size: " + formatBytes(file.length()));
        System.out.println("📁 File Type: " + (file.isDirectory() ? "Directory" : getFileExtension(file)));
        System.out.println("⏰ Last Modified: " + Instant.ofEpochMilli(file.lastModified())
                .atZone(ZoneId.systemDefault())
                .format(DATE_FORMATTER));
        System.out.println("📍 File Path: " + file.getAbsolutePath());
        
        if (operationType.equals("compression")) {
            System.out.println("\n📤 COMPRESSION OUTPUT INFORMATION");
            System.out.println("═".repeat(40));
            System.out.println("📦 Output Format: " + currentOutputFormat);
            System.out.println("📁 Output Location: " + currentOutputLocation);
            System.out.println("💡 Estimated Size: " + currentEstimatedSize);
            System.out.println("📈 Compression Ratio: " + currentCompressionRatio);
        } else {
            System.out.println("\n📂 EXTRACTION OUTPUT INFORMATION");
            System.out.println("═".repeat(40));
            System.out.println("📦 Output Format: " + currentOutputFormat);
            System.out.println("📁 Output Location: " + currentOutputLocation);
            System.out.println("💡 Estimated Size: " + currentEstimatedSize);
        }
    }
    
    private static void updateOutputInfo(File outputFile, long originalSize, long outputSize, String operationType) {
        currentOutputLocation = outputFile.getParent();
        
        if (operationType.equals("compression")) {
            currentOutputFormat = outputFile.getName().toLowerCase().endsWith(".zip") ? "ZIP Archive" : "GZIP File";
            currentEstimatedSize = formatBytes(outputSize);
            if (originalSize > 0) {
                double ratio = (1 - (double) outputSize / originalSize) * 100;
                currentCompressionRatio = String.format("%.1f%%", ratio);
            } else {
                currentCompressionRatio = "N/A";
            }
        } else {
            currentOutputFormat = outputFile.isDirectory() ? "Extracted Files Directory" : "Decompressed File";
            currentEstimatedSize = formatBytes(outputSize);
            currentCompressionRatio = "N/A";
        }
    }
    
    private static boolean isValidArchiveType(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".gz") || name.endsWith(".zip");
    }
    
    private static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return file.isDirectory() ? "Directory" : "File";
        }
        return name.substring(lastIndexOf).toUpperCase() + " File";
    }
    
    private static void compressGZIP() {
        if (currentCompressFile == null) {
            System.out.println("❌ No file selected for compression. Please select a file first.");
            return;
        }
        
        if (currentCompressFile.isDirectory()) {
            System.out.println("❌ GZIP compression only works with single files, not folders.");
            System.out.println("💡 Please use ZIP compression for folders.");
            return;
        }
        
        System.out.println("\n🎯 GZIP COMPRESSION");
        System.out.println("═".repeat(60));
        
        try {
            String sourceDir = currentCompressFile.getParent();
            String fileName = currentCompressFile.getName();
            String destPath = sourceDir + File.separator + fileName + ".gz";
            
            // Handle naming conflicts
            File destFile = new File(destPath);
            int counter = 1;
            while (destFile.exists()) {
                String nameWithoutExt = fileName.contains(".") ? 
                    fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                destPath = sourceDir + File.separator + nameWithoutExt + "_" + counter + ".gz";
                destFile = new File(destPath);
                counter++;
            }
            
            System.out.println("📄 Source: " + currentCompressFile.getName());
            System.out.println("💾 Output: " + new File(destPath).getName());
            System.out.println("⏰ " + getCurrentTime() + " Starting compression...");
            
            long startTime = System.currentTimeMillis();
            long originalSize = currentCompressFile.length();
            
            try (FileInputStream fis = new FileInputStream(currentCompressFile);
                 FileOutputStream fos = new FileOutputStream(destPath);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                System.out.print("📊 Progress: [");
                int lastProgress = -1;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    gzos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    int progress = (int) ((totalBytesRead * 50) / originalSize);
                    if (progress != lastProgress) {
                        System.out.print("=".repeat(progress) + ">" + " ".repeat(50 - progress) + "]");
                        System.out.printf(" %d%%\r", Math.min(100, (progress * 2)));
                        lastProgress = progress;
                    }
                }
            }
            
            long endTime = System.currentTimeMillis();
            File compressedFile = new File(destPath);
            long compressedSize = compressedFile.length();
            long bytesSaved = originalSize - compressedSize;
            
            // Update output information
            updateOutputInfo(compressedFile, originalSize, compressedSize, "compression");
            
            // Store the output file reference
            currentCompressOutput = compressedFile;
            
            System.out.println("\n✅ " + getCurrentTime() + " Compression completed!");
            System.out.println("═".repeat(60));
            System.out.printf("📊 Original size:    %s\n", formatBytes(originalSize));
            System.out.printf("📊 Compressed size:  %s\n", formatBytes(compressedSize));
            System.out.printf("📈 Compression ratio: %.1f%%\n", (1 - (double)compressedSize / originalSize) * 100);
            System.out.printf("💾 Space saved:      %s\n", formatBytes(bytesSaved));
            System.out.printf("⏱️  Time taken:       %d ms\n", (endTime - startTime));
            
            // Display UPDATED file information with output details
            System.out.println("\n📋 UPDATED FILE INFORMATION");
            System.out.println("═".repeat(60));
            displayFileInfo(currentCompressFile, "compression");
            
            // Update statistics
            totalOperations++;
            filesCompressed++;
            totalBytesSaved += bytesSaved;
            saveStatistics();
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during compression: " + e.getMessage());
        }
    }
    
    private static void decompressGZIP() {
        if (currentDecompressFile == null) {
            System.out.println("❌ No file selected for decompression. Please select a file first.");
            return;
        }
        
        if (!currentDecompressFile.getName().toLowerCase().endsWith(".gz")) {
            System.out.println("❌ Please select a .gz file for GZIP decompression.");
            return;
        }
        
        System.out.println("\n🎯 GZIP DECOMPRESSION");
        System.out.println("═".repeat(60));
        
        try {
            String sourceDir = currentDecompressFile.getParent();
            String fileName = currentDecompressFile.getName();
            String destPath;
            
            if (fileName.toLowerCase().endsWith(".gz")) {
                String baseName = fileName.substring(0, fileName.length() - 3);
                destPath = sourceDir + File.separator + baseName + "_decompressed";
            } else {
                destPath = sourceDir + File.separator + fileName + "_decompressed";
            }
            
            // Handle naming conflicts
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
            
            System.out.println("📦 Source: " + currentDecompressFile.getName());
            System.out.println("💾 Output: " + new File(destPath).getName());
            System.out.println("⏰ " + getCurrentTime() + " Starting decompression...");
            
            long startTime = System.currentTimeMillis();
            long compressedSize = currentDecompressFile.length();
            long decompressedSize = 0;
            
            // Ensure parent directory exists
            File outputFile = new File(destPath);
            File parentDir = outputFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(currentDecompressFile));
                 FileOutputStream fos = new FileOutputStream(destPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                System.out.print("📊 Progress: [");
                while ((bytesRead = gzis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    decompressedSize += bytesRead;
                    System.out.print("▓");
                }
                fos.flush();
                fos.getFD().sync();
            }
            
            long endTime = System.currentTimeMillis();
            
            System.out.println("]");
            
            // Verify file creation
            File finalOutputFile = new File(destPath);
            if (!finalOutputFile.exists() || finalOutputFile.length() == 0) {
                throw new IOException("Output file was not created properly");
            }
            
            // Update output information
            updateOutputInfo(finalOutputFile, compressedSize, decompressedSize, "decompression");
            
            // Store the output file reference
            currentDecompressOutput = finalOutputFile;
            
            System.out.println("✅ " + getCurrentTime() + " Decompression completed!");
            System.out.println("═".repeat(60));
            System.out.printf("📊 Compressed size:   %s\n", formatBytes(compressedSize));
            System.out.printf("📊 Decompressed size: %s\n", formatBytes(decompressedSize));
            System.out.printf("📈 Size difference:   %s\n", formatBytes(decompressedSize - compressedSize));
            System.out.printf("⏱️  Time taken:        %d ms\n", (endTime - startTime));
            
            // Display UPDATED file information with output details
            System.out.println("\n📋 UPDATED FILE INFORMATION");
            System.out.println("═".repeat(60));
            displayFileInfo(currentDecompressFile, "decompression");
            
            // Update statistics
            totalOperations++;
            filesDecompressed++;
            saveStatistics();
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during decompression: " + e.getMessage());
        }
    }
    
    private static void compressZIP() {
        if (currentCompressFile == null) {
            System.out.println("❌ No file/folder selected for compression. Please select a file first.");
            return;
        }
        
        System.out.println("\n🎯 ZIP COMPRESSION");
        System.out.println("═".repeat(60));
        
        try {
            String sourceDir = currentCompressFile.getParent();
            String fileName = currentCompressFile.getName();
            String baseName = currentCompressFile.isDirectory() ? fileName :
                    fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            String destPath = sourceDir + File.separator + baseName + ".zip";
            
            // Handle naming conflicts
            File destFile = new File(destPath);
            int counter = 1;
            while (destFile.exists()) {
                destPath = sourceDir + File.separator + baseName + "_" + counter + ".zip";
                destFile = new File(destPath);
                counter++;
            }
            
            long originalSize = calculateTotalSize(currentCompressFile);
            
            System.out.println("📄 Source: " + currentCompressFile.getName());
            System.out.println("💾 Output: " + new File(destPath).getName());
            System.out.println("⏰ " + getCurrentTime() + " Starting ZIP compression...");
            
            long startTime = System.currentTimeMillis();
            
            try (FileOutputStream fos = new FileOutputStream(destPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                
                if (currentCompressFile.isDirectory()) {
                    System.out.println("📁 Zipping directory: " + currentCompressFile.getName());
                    zipDirectory(currentCompressFile, currentCompressFile.getName(), zos);
                } else {
                    System.out.println("📄 Zipping file: " + currentCompressFile.getName());
                    addFileToZip(currentCompressFile, currentCompressFile.getName(), zos);
                }
            }
            
            long endTime = System.currentTimeMillis();
            File compressedFile = new File(destPath);
            long compressedSize = compressedFile.length();
            long bytesSaved = originalSize - compressedSize;
            
            // Update output information
            updateOutputInfo(compressedFile, originalSize, compressedSize, "compression");
            
            // Store the output file reference
            currentCompressOutput = compressedFile;
            
            System.out.println("✅ " + getCurrentTime() + " ZIP compression completed!");
            System.out.println("═".repeat(60));
            System.out.printf("📊 Original size:    %s\n", formatBytes(originalSize));
            System.out.printf("📊 Compressed size:  %s\n", formatBytes(compressedSize));
            System.out.printf("📈 Compression ratio: %.1f%%\n", (1 - (double)compressedSize / originalSize) * 100);
            System.out.printf("💾 Space saved:      %s\n", formatBytes(bytesSaved));
            System.out.printf("⏱️  Time taken:       %d ms\n", (endTime - startTime));
            
            // Display UPDATED file information with output details
            System.out.println("\n📋 UPDATED FILE INFORMATION");
            System.out.println("═".repeat(60));
            displayFileInfo(currentCompressFile, "compression");
            
            // Update statistics
            totalOperations++;
            filesCompressed++;
            totalBytesSaved += bytesSaved;
            saveStatistics();
            
        } catch (IOException e) {
            System.out.println("❌ " + getCurrentTime() + " Error during ZIP compression: " + e.getMessage());
        }
    }
    
    private static void decompressZIP() {
        if (currentDecompressFile == null) {
            System.out.println("❌ No file selected for decompression. Please select a file first.");
            return;
        }
        
        if (!currentDecompressFile.getName().toLowerCase().endsWith(".zip")) {
            System.out.println("❌ Please select a .zip file for ZIP decompression.");
            return;
        }
        
        System.out.println("\n🎯 ZIP DECOMPRESSION");
        System.out.println("═".repeat(60));
        
        try {
            String zipDir = currentDecompressFile.getParent();
            String zipFileName = currentDecompressFile.getName();
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
            
            long compressedSize = currentDecompressFile.length();
            
            System.out.println("📦 ZIP file: " + currentDecompressFile.getName());
            System.out.println("💾 Extract to: " + new File(destDirectory).getName());
            
            // Create destination directory
            if (!destDir.mkdirs()) {
                System.out.println("❌ Error: Could not create destination directory: " + destDirectory);
                return;
            }
            
            System.out.println("⏰ " + getCurrentTime() + " Starting ZIP decompression...");
            
            long startTime = System.currentTimeMillis();
            int fileCount = 0;
            int dirCount = 0;
            long totalExtractedSize = 0;
            
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(currentDecompressFile))) {
                ZipEntry zipEntry;
                
                System.out.print("📊 Extracting: ");
                
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
                            fos.getFD().sync();
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
            
            // Update output information
            updateOutputInfo(destDir, compressedSize, totalExtractedSize, "decompression");
            
            // Store the output directory reference
            currentDecompressOutput = destDir;
            
            System.out.println("✅ " + getCurrentTime() + " ZIP decompression completed!");
            System.out.println("═".repeat(60));
            System.out.printf("📊 Compressed size:   %s\n", formatBytes(compressedSize));
            System.out.printf("📊 Extracted size:    %s\n", formatBytes(totalExtractedSize));
            System.out.printf("📁 Files extracted:   %d\n", fileCount);
            System.out.printf("📁 Directories:       %d\n", dirCount);
            System.out.printf("⏱️  Time taken:        %d ms\n", (endTime - startTime));
            
            // Display UPDATED file information with output details
            System.out.println("\n📋 UPDATED FILE INFORMATION");
            System.out.println("═".repeat(60));
            displayFileInfo(currentDecompressFile, "decompression");
            
            // Update statistics
            totalOperations++;
            filesDecompressed++;
            saveStatistics();
            
        } catch (IOException e) {
            System.out.println("\n❌ " + getCurrentTime() + " Error during ZIP decompression: " + e.getMessage());
        }
    }
    
    private static void displayStatistics() {
        System.out.println("\n📈 COMPRESSION STATISTICS");
        System.out.println("═".repeat(60));
        System.out.printf("📁 Total Operations:    %d\n", totalOperations);
        System.out.printf("💾 Total Bytes Saved:   %s\n", formatBytes(totalBytesSaved));
        System.out.printf("🗜️  Files Compressed:   %d\n", filesCompressed);
        System.out.printf("📤 Files Decompressed:  %d\n", filesDecompressed);
        System.out.printf("⚡ Current Directory:   %s\n", System.getProperty("user.dir"));
        System.out.printf("💻 Available Memory:    %s\n", formatBytes(Runtime.getRuntime().freeMemory()));
        System.out.printf("🔢 Java Version:        %s\n", System.getProperty("java.version"));
        
        // Display recent output files
        if (currentCompressOutput != null && currentCompressOutput.exists()) {
            System.out.println("\n📦 Last Compression Output:");
            System.out.printf("   File: %s (%s)\n", currentCompressOutput.getName(), 
                             formatBytes(currentCompressOutput.length()));
            System.out.printf("   Ratio: %s\n", currentCompressionRatio);
        }
        
        if (currentDecompressOutput != null && currentDecompressOutput.exists()) {
            System.out.println("📤 Last Decompression Output:");
            if (currentDecompressOutput.isDirectory()) {
                File[] files = currentDecompressOutput.listFiles();
                int itemCount = files != null ? files.length : 0;
                System.out.printf("   Directory: %s (%d items)\n", currentDecompressOutput.getName(), itemCount);
            } else {
                System.out.printf("   File: %s (%s)\n", currentDecompressOutput.getName(), 
                                 formatBytes(currentDecompressOutput.length()));
            }
        }
        
        System.out.println("═".repeat(60));
    }
    
    private static void listFilesInDirectory() {
        System.out.println("\n📁 FILES IN CURRENT DIRECTORY");
        System.out.println("═".repeat(60));
        
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
        System.out.println("═".repeat(60));
        System.out.printf("Total: %d items\n", files.length);
    }
    
    // Helper methods for ZIP operations
    private static void zipDirectory(File directory, String baseName, ZipOutputStream zos) throws IOException {
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
    
    private static void addFileToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
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
    
    private static String getCurrentTime() {
        return "[" + LocalTime.now().format(TIME_FORMATTER) + "]";
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private static void saveStatistics() {
        Properties props = new Properties();
        props.setProperty("totalOperations", String.valueOf(totalOperations));
        props.setProperty("totalBytesSaved", String.valueOf(totalBytesSaved));
        props.setProperty("filesCompressed", String.valueOf(filesCompressed));
        props.setProperty("filesDecompressed", String.valueOf(filesDecompressed));
        
        try (FileOutputStream fos = new FileOutputStream("compression_stats.properties")) {
            props.store(fos, "Compression Tool Statistics");
        } catch (IOException e) {
            System.out.println("⚠️  Could not save statistics: " + e.getMessage());
        }
    }
    
    private static void loadStatistics() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("compression_stats.properties")) {
            props.load(fis);
            totalOperations = Integer.parseInt(props.getProperty("totalOperations", "0"));
            totalBytesSaved = Long.parseLong(props.getProperty("totalBytesSaved", "0"));
            filesCompressed = Integer.parseInt(props.getProperty("filesCompressed", "0"));
            filesDecompressed = Integer.parseInt(props.getProperty("filesDecompressed", "0"));
        } catch (IOException e) {
            // No previous statistics found, starting fresh
        }
    }
}