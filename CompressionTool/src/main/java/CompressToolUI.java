import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.*;

import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.function.Consumer;

public class CompressToolUI extends Application {

    private static final String BACKEND_URL = "http://localhost:8081";

    private void performApiOperation(String endpoint, File inputFile, boolean isDirectory, String path, Consumer<File> onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                URL url = new URL(BACKEND_URL + endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");

                String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
                try (OutputStream os = conn.getOutputStream()) {
                    if (isDirectory && path != null) {
                        os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                        os.write(("Content-Disposition: form-data; name=\"path\"\r\n\r\n").getBytes("UTF-8"));
                        os.write((path + "\r\n").getBytes("UTF-8"));
                        os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                        os.write(("Content-Disposition: form-data; name=\"isDirectory\"\r\n\r\n").getBytes("UTF-8"));
                        os.write(("true\r\n").getBytes("UTF-8"));
                    } else if (inputFile != null) {
                        os.write(("--" + boundary + "\r\n").getBytes("UTF-8"));
                        os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + inputFile.getName() + "\"\r\n").getBytes("UTF-8"));
                        os.write(("Content-Type: " + Files.probeContentType(inputFile.toPath()) + "\r\n\r\n").getBytes("UTF-8"));
                        try (InputStream is = new FileInputStream(inputFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                        os.write("\r\n".getBytes("UTF-8"));
                    }
                    os.write(("--" + boundary + "--\r\n").getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String contentDisposition = conn.getHeaderField("Content-Disposition");
                    String filename = "output";
                    if (contentDisposition != null && contentDisposition.contains("filename=")) {
                        filename = contentDisposition.split("filename=")[1].replace("\"", "");
                    }
                    File parentDir = inputFile != null ? inputFile.getParentFile() : new File(".");
                    String baseName = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;
                    String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
                    File outputFile = getUniqueOutputFile(parentDir, baseName, extension);

                    try (InputStream is = conn.getInputStream();
                         FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    onSuccess.accept(outputFile);
                } else {
                    String error = readStream(conn.getErrorStream());
                    onError.accept("API Error: " + responseCode + " - " + error);
                }
            } catch (Exception e) {
                onError.accept("Network Error: " + e.getMessage());
            }
        }).start();
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, bytesRead));
        }
        return sb.toString();
    }

    private File compressFile;
    private File decompressFile;
    private TabPane tabPane;

    // UI Components
    private Label compressFileLabel = new Label("No file selected");
    private Label decompressFileLabel = new Label("No file selected");
    private TextArea statusArea = new TextArea();
    private ProgressBar compressProgress = new ProgressBar(0);
    private ProgressBar decompressProgress = new ProgressBar(0);
    private Label compressProgressLabel = new Label("0%");
    private Label decompressProgressLabel = new Label("0%");

    // Statistics
    private AtomicInteger totalOperations = new AtomicInteger(0);
    private AtomicLong totalBytesSaved = new AtomicLong(0);
    private AtomicInteger filesCompressed = new AtomicInteger(0);
    private AtomicInteger filesDecompressed = new AtomicInteger(0);

    // Enhanced file info labels for compress tab
    private Label compressFileNameLabel = new Label("File Name: -");
    private Label compressFileSizeLabel = new Label("File Size: -");
    private Label compressFilePathLabel = new Label("File Path: -");
    private Label compressFileTypeLabel = new Label("File Type: -");
    private Label compressLastModifiedLabel = new Label("Last Modified: -");

    // Enhanced file info labels for decompress tab
    private Label decompressFileNameLabel = new Label("File Name: -");
    private Label decompressFileSizeLabel = new Label("File Size: -");
    private Label decompressFilePathLabel = new Label("File Path: -");
    private Label decompressFileTypeLabel = new Label("File Type: -");
    private Label decompressLastModifiedLabel = new Label("Last Modified: -");

    // Output info labels for compress tab
    private Label compressOutputFormatLabel;
    private Label compressOutputLocationLabel;
    private Label compressEstimatedSizeLabel;
    private Label compressCompressionRatioLabel;

    // Output info labels for decompress tab
    private Label decompressOutputFormatLabel;
    private Label decompressOutputLocationLabel;
    private Label decompressEstimatedSizeLabel;
    private Label decompressCompressionRatioLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🚀 Advanced File Compression Tool");

        // Apply modern styling
        compressProgress.setStyle("-fx-accent: #4CAF50;");
        decompressProgress.setStyle("-fx-accent: #FF9800;");
        statusArea.setStyle("-fx-control-inner-background: #f8f9fa; -fx-border-color: #dee2e6;");

        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white;");

        Tab compressTab = createCompressTab();
        Tab decompressTab = createDecompressTab();
        Tab statsTab = createStatisticsTab();

        tabPane.getTabs().addAll(compressTab, decompressTab, statsTab);

        VBox root = new VBox(10, createHeader(), tabPane, createStatusPane());
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f5f7fa, #c3cfe2);");

        Scene scene = new Scene(root, 900, 700);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();

        // Load statistics from file
        loadStatistics();
    }

    private VBox createHeader() {
        Label titleLabel = new Label("🔧 Advanced File Compression Tool");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        Label subtitleLabel = new Label("Compress and decompress files with ease - GZIP & ZIP formats supported");
        subtitleLabel.setFont(Font.font("System", 14));
        subtitleLabel.setTextFill(Color.web("#7f8c8d"));

        VBox header = new VBox(5, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 10, 0));
        return header;
    }

    private Tab createCompressTab() {
        Tab tab = new Tab("📦 Compress");
        tab.setClosable(false);

        ScrollPane scrollPane = new ScrollPane(createCompressPane());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        tab.setContent(scrollPane);
        return tab;
    }

    private Tab createDecompressTab() {
        Tab tab = new Tab("📤 Decompress");
        tab.setClosable(false);

        ScrollPane scrollPane = new ScrollPane(createDecompressPane());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        tab.setContent(scrollPane);
        return tab;
    }

    private Tab createStatisticsTab() {
        Tab tab = new Tab("📊 Statistics");
        tab.setClosable(false);

        ScrollPane scrollPane = new ScrollPane(createStatisticsPane());
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        tab.setContent(scrollPane);

        return tab;
    }

    private VBox createStatisticsPane() {
        VBox statsPane = new VBox(20);
        statsPane.setPadding(new Insets(25));
        statsPane.setAlignment(Pos.TOP_CENTER);
        statsPane.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label statsTitle = new Label("Compression Statistics");
        statsTitle.setFont(Font.font("System", FontWeight.BOLD, 22));
        statsTitle.setTextFill(Color.web("#2c3e50"));

        // Stats cards in a grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(20);
        statsGrid.setAlignment(Pos.CENTER);

        // Create dynamic stat cards
        VBox totalOpsCard = createStatCard("📁 Total Operations",
                String.valueOf(totalOperations.get()),
                "All compression and decompression operations");

        VBox spaceSavedCard = createStatCard("💾 Total Space Saved",
                formatFileSize(totalBytesSaved.get()),
                "Bytes saved through compression");

        VBox filesCompressedCard = createStatCard("🗜️ Files Compressed",
                String.valueOf(filesCompressed.get()),
                "Successful compression operations");

        VBox filesDecompressedCard = createStatCard("📤 Files Decompressed",
                String.valueOf(filesDecompressed.get()),
                "Successful decompression operations");

        VBox currentDirCard = createStatCard("⚡ Current Directory",
                System.getProperty("user.dir"),
                "Working directory");

        VBox availableMemoryCard = createStatCard("💻 Available Memory",
                formatFileSize(Runtime.getRuntime().freeMemory()),
                "JVM free memory");

        // Row 1
        statsGrid.add(totalOpsCard, 0, 0);
        statsGrid.add(spaceSavedCard, 1, 0);

        // Row 2
        statsGrid.add(filesCompressedCard, 0, 1);
        statsGrid.add(filesDecompressedCard, 1, 1);

        // Row 3
        statsGrid.add(currentDirCard, 0, 2);
        statsGrid.add(availableMemoryCard, 1, 2);

        // Control buttons
        HBox controlBox = new HBox(20);
        controlBox.setAlignment(Pos.CENTER);

        Button refreshStatsBtn = createStyledButton("🔄 Refresh Statistics", "#3498db");
        refreshStatsBtn.setOnAction(e -> refreshStatistics());

        Button resetStatsBtn = createStyledButton("🔄 Reset Statistics", "#e74c3c");
        resetStatsBtn.setOnAction(e -> resetStatistics());

        controlBox.getChildren().addAll(refreshStatsBtn, resetStatsBtn);

        statsPane.getChildren().addAll(statsTitle, statsGrid, controlBox);
        return statsPane;
    }

    private VBox createStatCard(String title, String value, String description) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 20; -fx-alignment: center-left;");
        card.setPrefWidth(250);
        card.setMinHeight(120);

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.web("#7f8c8d"));

        Label valueLabel = new Label(value);
        valueLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        valueLabel.setTextFill(Color.web("#2c3e50"));
        valueLabel.setWrapText(true);

        Label descLabel = new Label(description);
        descLabel.setFont(Font.font("System", 11));
        descLabel.setTextFill(Color.web("#95a5a6"));
        descLabel.setWrapText(true);

        card.getChildren().addAll(titleLabel, valueLabel, descLabel);
        return card;
    }

    private void refreshStatistics() {
        Platform.runLater(() -> {
            // Update all statistic cards in the statistics tab
            Tab statsTab = tabPane.getTabs().get(2);
            ScrollPane scrollPane = (ScrollPane) statsTab.getContent();
            VBox statsPane = (VBox) scrollPane.getContent();
            GridPane statsGrid = (GridPane) statsPane.getChildren().get(1);

            // Update each card
            for (javafx.scene.Node node : statsGrid.getChildren()) {
                if (node instanceof VBox) {
                    VBox card = (VBox) node;
                    Label titleLabel = (Label) card.getChildren().get(0);
                    Label valueLabel = (Label) card.getChildren().get(1);

                    switch (titleLabel.getText()) {
                        case "📁 Total Operations":
                            valueLabel.setText(String.valueOf(totalOperations.get()));
                            break;
                        case "💾 Total Space Saved":
                            valueLabel.setText(formatFileSize(totalBytesSaved.get()));
                            break;
                        case "🗜️ Files Compressed":
                            valueLabel.setText(String.valueOf(filesCompressed.get()));
                            break;
                        case "📤 Files Decompressed":
                            valueLabel.setText(String.valueOf(filesDecompressed.get()));
                            break;
                        case "⚡ Current Directory":
                            valueLabel.setText(System.getProperty("user.dir"));
                            break;
                        case "💻 Available Memory":
                            valueLabel.setText(formatFileSize(Runtime.getRuntime().freeMemory()));
                            break;
                    }
                }
            }
        });
        appendStatus("📊 Statistics refreshed");
    }

    private void resetStatistics() {
        totalOperations.set(0);
        totalBytesSaved.set(0);
        filesCompressed.set(0);
        filesDecompressed.set(0);
        appendStatus("🔄 Statistics reset");
        saveStatistics();
        refreshStatistics();
    }

    private VBox createCompressPane() {
        VBox mainVBox = new VBox(20);
        mainVBox.setPadding(new Insets(25));
        mainVBox.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title
        Label titleLabel = new Label("File Compression");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        // Compression type selection
        VBox typeBox = new VBox(10);
        Label typeLabel = new Label("Select Compression Type:");
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        ToggleGroup compressionGroup = new ToggleGroup();
        RadioButton gzipRadio = new RadioButton("🎯 GZIP Compression (Single File)");
        RadioButton zipRadio = new RadioButton("📦 ZIP Archive (Multiple Files/Folders)");
        gzipRadio.setToggleGroup(compressionGroup);
        zipRadio.setToggleGroup(compressionGroup);
        gzipRadio.setSelected(true);

        VBox radioBox = new VBox(8, gzipRadio, zipRadio);
        radioBox.setPadding(new Insets(15));
        radioBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #f8f9fa;");

        typeBox.getChildren().addAll(typeLabel, radioBox);

        // File selection section
        VBox fileSelectionBox = new VBox(15);
        Label selectLabel = new Label("Select Source:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox buttonBox = new HBox(15);
        Button chooseFileBtn = createStyledButton("📁 Choose File/Folder", "#3498db");
        chooseFileBtn.setTooltip(new Tooltip("Click to select a file or folder for compression\nor drag and drop files directly here"));

        Button clearSelectionBtn = createStyledButton("🗑️ Clear Selection", "#e74c3c");
        clearSelectionBtn.setOnAction(e -> clearCompressSelection());

        buttonBox.getChildren().addAll(chooseFileBtn, clearSelectionBtn);

        // Selected File Information Section
        VBox selectedFileInfoBox = createSelectedFileInfoSection("compress");
        
        // Compression Output Information Section
        VBox outputInfoBox = createOutputInfoSection("compress");

        // Compression buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);

        Button compressGzipBtn = createStyledButton("🗜️ Compress to GZIP", "#27ae60");
        compressGzipBtn.setTooltip(new Tooltip("Compress selected file using GZIP format"));

        Button compressZipBtn = createStyledButton("📦 Create ZIP Archive", "#2980b9");
        compressZipBtn.setTooltip(new Tooltip("Create ZIP archive from selected files/folders"));

        actionBox.getChildren().addAll(compressGzipBtn, compressZipBtn);

        // Progress section
        VBox progressBox = new VBox(8);
        Label progressLabel = new Label("Compression Progress:");
        progressLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox progressBarBox = new HBox(10);
        progressBarBox.setAlignment(Pos.CENTER_LEFT);
        compressProgress.setPrefWidth(400);
        progressBarBox.getChildren().addAll(compressProgress, compressProgressLabel);

        progressBox.getChildren().addAll(progressLabel, progressBarBox);

        // Add all to main layout
        fileSelectionBox.getChildren().addAll(selectLabel, buttonBox);
        mainVBox.getChildren().addAll(
                titleLabel, typeBox, new Separator(),
                fileSelectionBox, selectedFileInfoBox, outputInfoBox, new Separator(),
                actionBox, progressBox
        );

        // Event Handlers
        chooseFileBtn.setOnAction(e -> handleFileSelection(gzipRadio.isSelected()));

        compressGzipBtn.setOnAction(e -> handleGzipCompression());

        compressZipBtn.setOnAction(e -> handleZipCompression());

        // Drag and drop functionality
        setupDragAndDrop(mainVBox, "compress");

        return mainVBox;
    }

    private VBox createDecompressPane() {
        VBox mainVBox = new VBox(20);
        mainVBox.setPadding(new Insets(25));
        mainVBox.setStyle("-fx-background-color: white; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title
        Label titleLabel = new Label("File Decompression");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        titleLabel.setTextFill(Color.web("#2c3e50"));

        // Archive type selection
        VBox typeBox = new VBox(10);
        Label typeLabel = new Label("Select Archive Type:");
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        ToggleGroup decompressionGroup = new ToggleGroup();
        RadioButton gzipRadio = new RadioButton("📄 GZIP File (.gz)");
        RadioButton zipRadio = new RadioButton("📁 ZIP Archive (.zip)");
        gzipRadio.setToggleGroup(decompressionGroup);
        zipRadio.setToggleGroup(decompressionGroup);
        gzipRadio.setSelected(true);

        VBox radioBox = new VBox(8, gzipRadio, zipRadio);
        radioBox.setPadding(new Insets(15));
        radioBox.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-background-color: #f8f9fa;");

        typeBox.getChildren().addAll(typeLabel, radioBox);

        // File selection section
        VBox fileSelectionBox = new VBox(15);
        Label selectLabel = new Label("Select Archive File:");
        selectLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox buttonBox = new HBox(15);
        Button chooseFileBtn = createStyledButton("📁 Choose Archive", "#3498db");
        chooseFileBtn.setTooltip(new Tooltip("Click to select a .gz or .zip file for decompression\nor drag and drop files directly here"));

        Button clearSelectionBtn = createStyledButton("🗑️ Clear Selection", "#e74c3c");
        clearSelectionBtn.setOnAction(e -> clearDecompressSelection());

        buttonBox.getChildren().addAll(chooseFileBtn, clearSelectionBtn);

        // Selected Archive Information Section
        VBox selectedFileInfoBox = createSelectedFileInfoSection("decompress");
        
        // Extraction Output Information Section
        VBox outputInfoBox = createOutputInfoSection("decompress");

        // Decompression buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);

        Button decompressGzipBtn = createStyledButton("📤 Extract GZIP", "#f39c12");
        decompressGzipBtn.setTooltip(new Tooltip("Decompress selected .gz file"));

        Button decompressZipBtn = createStyledButton("📂 Extract ZIP", "#8e44ad");
        decompressZipBtn.setTooltip(new Tooltip("Extract contents from ZIP archive"));

        actionBox.getChildren().addAll(decompressGzipBtn, decompressZipBtn);

        // Progress section
        VBox progressBox = new VBox(8);
        Label progressLabel = new Label("Extraction Progress:");
        progressLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox progressBarBox = new HBox(10);
        progressBarBox.setAlignment(Pos.CENTER_LEFT);
        decompressProgress.setPrefWidth(400);
        progressBarBox.getChildren().addAll(decompressProgress, decompressProgressLabel);

        progressBox.getChildren().addAll(progressLabel, progressBarBox);

        // Add all to main layout
        fileSelectionBox.getChildren().addAll(selectLabel, buttonBox);
        mainVBox.getChildren().addAll(
                titleLabel, typeBox, new Separator(),
                fileSelectionBox, selectedFileInfoBox, outputInfoBox, new Separator(),
                actionBox, progressBox
        );

        // Event Handlers
        chooseFileBtn.setOnAction(e -> handleArchiveSelection(gzipRadio.isSelected()));

        decompressGzipBtn.setOnAction(e -> handleGzipDecompression());

        decompressZipBtn.setOnAction(e -> handleZipDecompression());

        // Drag and drop functionality
        setupDragAndDrop(mainVBox, "decompress");

        return mainVBox;
    }

    private VBox createSelectedFileInfoSection(String type) {
        VBox fileInfoBox = new VBox(10);
        fileInfoBox.setStyle("-fx-border-color: #bdc3c7; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 15; -fx-background-color: #f8f9fa;");
        
        String title = type.equals("compress") ? "📄 Selected File Information" : "📦 Selected Archive Information";
        Label fileInfoLabel = new Label(title);
        fileInfoLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        fileInfoLabel.setTextFill(Color.web("#2c3e50"));

        VBox fileDetails = new VBox(8);
        
        if (type.equals("compress")) {
            // Initialize labels with default values for compression
            compressFileNameLabel.setText("File Name: -");
            compressFileSizeLabel.setText("File Size: -");
            compressFilePathLabel.setText("File Path: -");
            compressFileTypeLabel.setText("File Type: -");
            compressLastModifiedLabel.setText("Last Modified: -");
            
            // Style the labels
            compressFileNameLabel.setStyle("-fx-font-weight: bold;");
            compressFileSizeLabel.setStyle("-fx-font-weight: bold;");
            compressFilePathLabel.setWrapText(true);
            compressFileTypeLabel.setStyle("-fx-font-weight: bold;");
            compressLastModifiedLabel.setStyle("-fx-font-weight: bold;");

            fileDetails.getChildren().addAll(
                compressFileNameLabel, 
                compressFileSizeLabel, 
                compressFileTypeLabel,
                compressLastModifiedLabel,
                compressFilePathLabel
            );
        } else {
            // Initialize labels with default values for decompression
            decompressFileNameLabel.setText("File Name: -");
            decompressFileSizeLabel.setText("File Size: -");
            decompressFilePathLabel.setText("File Path: -");
            decompressFileTypeLabel.setText("File Type: -");
            decompressLastModifiedLabel.setText("Last Modified: -");
            
            // Style the labels
            decompressFileNameLabel.setStyle("-fx-font-weight: bold;");
            decompressFileSizeLabel.setStyle("-fx-font-weight: bold;");
            decompressFilePathLabel.setWrapText(true);
            decompressFileTypeLabel.setStyle("-fx-font-weight: bold;");
            decompressLastModifiedLabel.setStyle("-fx-font-weight: bold;");

            fileDetails.getChildren().addAll(
                decompressFileNameLabel, 
                decompressFileSizeLabel, 
                decompressFileTypeLabel,
                decompressLastModifiedLabel,
                decompressFilePathLabel
            );
        }
        
        fileInfoBox.getChildren().addAll(fileInfoLabel, new Separator(), fileDetails);
        return fileInfoBox;
    }

    private VBox createOutputInfoSection(String type) {
        VBox outputInfoBox = new VBox(10);
        outputInfoBox.setStyle("-fx-border-color: #27ae60; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 15; -fx-background-color: #e8f5e8;");
        
        String title = type.equals("compress") ? "📤 Compression Output Information" : "📂 Extraction Output Information";
        Label outputInfoLabel = new Label(title);
        outputInfoLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        outputInfoLabel.setTextFill(Color.web("#27ae60"));

        VBox outputDetails = new VBox(8);
        
        // Create output information labels
        if (type.equals("compress")) {
            compressOutputFormatLabel = new Label("Output Format: GZIP/ZIP");
            compressOutputLocationLabel = new Label("Output Location: Original file directory");
            compressEstimatedSizeLabel = new Label("Estimated Size: -");
            compressCompressionRatioLabel = new Label("Compression Ratio: -");
            
            // Style the labels
            compressOutputFormatLabel.setStyle("-fx-font-weight: bold;");
            compressOutputLocationLabel.setStyle("-fx-font-weight: bold;");
            compressEstimatedSizeLabel.setStyle("-fx-font-weight: bold;");
            compressCompressionRatioLabel.setStyle("-fx-font-weight: bold;");
            compressOutputLocationLabel.setWrapText(true);

            outputDetails.getChildren().addAll(
                compressOutputFormatLabel,
                compressOutputLocationLabel,
                compressEstimatedSizeLabel,
                compressCompressionRatioLabel
            );
        } else {
            decompressOutputFormatLabel = new Label("Output Format: Extracted Files");
            decompressOutputLocationLabel = new Label("Output Location: Original file directory");
            decompressEstimatedSizeLabel = new Label("Estimated Size: -");
            decompressCompressionRatioLabel = new Label("Compression Ratio: -");
            
            // Style the labels
            decompressOutputFormatLabel.setStyle("-fx-font-weight: bold;");
            decompressOutputLocationLabel.setStyle("-fx-font-weight: bold;");
            decompressEstimatedSizeLabel.setStyle("-fx-font-weight: bold;");
            decompressCompressionRatioLabel.setStyle("-fx-font-weight: bold;");
            decompressOutputLocationLabel.setWrapText(true);

            outputDetails.getChildren().addAll(
                decompressOutputFormatLabel,
                decompressOutputLocationLabel,
                decompressEstimatedSizeLabel,
                decompressCompressionRatioLabel
            );
        }
        
        outputInfoBox.getChildren().addAll(outputInfoLabel, new Separator(), outputDetails);
        return outputInfoBox;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle(String.format(
                "-fx-font-size: 14px; -fx-padding: 12 25; -fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold;",
                color
        ));
        button.setOnMouseEntered(e -> button.setStyle(String.format(
                "-fx-font-size: 14px; -fx-padding: 12 25; -fx-background-color: derive(%s, 20%%); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold;",
                color
        )));
        button.setOnMouseExited(e -> button.setStyle(String.format(
                "-fx-font-size: 14px; -fx-padding: 12 25; -fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold;",
                color
        )));
        return button;
    }

    private void handleFileSelection(boolean isGzip) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File or Folder to Compress");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        File selectedFile;
        if (isGzip) {
            selectedFile = fileChooser.showOpenDialog(null);
        } else {
            FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter("All Files", "*.*");
            fileChooser.getExtensionFilters().add(allFilter);
            selectedFile = fileChooser.showOpenDialog(null);
        }

        if (selectedFile != null) {
            if (isGzip && selectedFile.isDirectory()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Selection",
                        "GZIP compression only works with single files. Please select a file, not a folder.");
                return;
            }
            if (selectedFile.length() == 0) {
                showAlert(Alert.AlertType.WARNING, "Invalid File", "The selected file is empty.");
                return;
            }
            if (selectedFile.length() > 1024L * 1024 * 1024 * 10) { // 10GB limit
                showAlert(Alert.AlertType.WARNING, "File Too Large",
                        "The selected file is too large (over 10GB).");
                return;
            }
            setCompressFile(selectedFile);
        }
    }

    private void handleArchiveSelection(boolean isGzip) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Archive File to Decompress");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        if (isGzip) {
            FileChooser.ExtensionFilter gzipFilter = new FileChooser.ExtensionFilter("GZIP files (*.gz)", "*.gz");
            fileChooser.getExtensionFilters().add(gzipFilter);
            fileChooser.setSelectedExtensionFilter(gzipFilter);
        } else {
            FileChooser.ExtensionFilter zipFilter = new FileChooser.ExtensionFilter("ZIP files (*.zip)", "*.zip");
            fileChooser.getExtensionFilters().add(zipFilter);
            fileChooser.setSelectedExtensionFilter(zipFilter);
        }

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            if (selectedFile.length() == 0) {
                showAlert(Alert.AlertType.WARNING, "Invalid File", "The selected file is empty.");
                return;
            }
            if (selectedFile.length() > 1024L * 1024 * 1024 * 10) { // 10GB limit
                showAlert(Alert.AlertType.WARNING, "File Too Large",
                        "The selected file is too large (over 10GB).");
                return;
            }
            setDecompressFile(selectedFile);
        }
    }

    private void handleGzipCompression() {
        if (compressFile == null) {
            showAlert(Alert.AlertType.WARNING, "No File Selected", "Please select a file to compress.");
            return;
        }
        if (compressFile.isDirectory()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Selection", "GZIP compression only works with single files, not folders.");
            return;
        }

        compressProgress.setProgress(0);
        compressProgressLabel.setText("0%");

        compressProgress.setProgress(-1); // indeterminate
        compressProgressLabel.setText("Processing...");

        performApiOperation("/api/compression/compress/gzip", compressFile, false, null, outputFile -> {
            long originalSize = compressFile.length();
            long compressedSize = outputFile.length();
            long bytesSaved = originalSize - compressedSize;

            Platform.runLater(() -> {
                updateOutputInfo("compress", outputFile, originalSize, compressedSize);
                appendStatus("✅ File compressed successfully: " + outputFile.getName());
                totalOperations.incrementAndGet();
                filesCompressed.incrementAndGet();
                totalBytesSaved.addAndGet(bytesSaved);
                saveStatistics();
                refreshStatistics();
                compressProgress.setProgress(1);
                compressProgressLabel.setText("100%");
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "File compressed successfully!\n\n" +
                                "Original: " + compressFile.getName() + " (" + formatFileSize(originalSize) + ")\n" +
                                "Compressed: " + outputFile.getName() + " (" + formatFileSize(compressedSize) + ")\n" +
                                "Space saved: " + formatFileSize(bytesSaved) + "\n" +
                                "Location: " + outputFile.getParent());
            });
        }, error -> {
            Platform.runLater(() -> {
                appendStatus("❌ Compression failed: " + error);
                compressProgress.setProgress(0);
                compressProgressLabel.setText("0%");
                showAlert(Alert.AlertType.ERROR, "Compression Failed", error);
            });
        });
    }

    private void handleZipCompression() {
        if (compressFile == null) {
            showAlert(Alert.AlertType.WARNING, "No File/Folder Selected", "Please select a file or folder to compress.");
            return;
        }

        compressProgress.setProgress(0);
        compressProgressLabel.setText("0%");

        compressProgress.setProgress(-1); // indeterminate
        compressProgressLabel.setText("Processing...");

        if (compressFile.isDirectory()) {
            performApiOperation("/api/compression/compress/zip", null, true, compressFile.getAbsolutePath(), outputFile -> {
                long originalSize = calculateTotalSize(compressFile);
                long compressedSize = outputFile.length();
                long bytesSaved = originalSize - compressedSize;

                Platform.runLater(() -> {
                    updateOutputInfo("compress", outputFile, originalSize, compressedSize);
                    appendStatus("✅ ZIP archive created successfully: " + outputFile.getName());
                    totalOperations.incrementAndGet();
                    filesCompressed.incrementAndGet();
                    totalBytesSaved.addAndGet(bytesSaved);
                    saveStatistics();
                    refreshStatistics();
                    compressProgress.setProgress(1);
                    compressProgressLabel.setText("100%");
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "ZIP archive created successfully!\n\n" +
                                    "Source: " + compressFile.getName() + " (" + formatFileSize(originalSize) + ")\n" +
                                    "Archive: " + outputFile.getName() + " (" + formatFileSize(compressedSize) + ")\n" +
                                    "Space saved: " + formatFileSize(bytesSaved) + "\n" +
                                    "Location: " + outputFile.getParent());
                });
            }, error -> {
                Platform.runLater(() -> {
                    appendStatus("❌ ZIP compression failed: " + error);
                    compressProgress.setProgress(0);
                    compressProgressLabel.setText("0%");
                    showAlert(Alert.AlertType.ERROR, "ZIP Compression Failed", error);
                });
            });
        } else {
            performApiOperation("/api/compression/compress/zip", compressFile, false, null, outputFile -> {
                long originalSize = compressFile.length();
                long compressedSize = outputFile.length();
                long bytesSaved = originalSize - compressedSize;

                Platform.runLater(() -> {
                    updateOutputInfo("compress", outputFile, originalSize, compressedSize);
                    appendStatus("✅ ZIP archive created successfully: " + outputFile.getName());
                    totalOperations.incrementAndGet();
                    filesCompressed.incrementAndGet();
                    totalBytesSaved.addAndGet(bytesSaved);
                    saveStatistics();
                    refreshStatistics();
                    compressProgress.setProgress(1);
                    compressProgressLabel.setText("100%");
                    showAlert(Alert.AlertType.INFORMATION, "Success",
                            "ZIP archive created successfully!\n\n" +
                                    "Source: " + compressFile.getName() + " (" + formatFileSize(originalSize) + ")\n" +
                                    "Archive: " + outputFile.getName() + " (" + formatFileSize(compressedSize) + ")\n" +
                                    "Space saved: " + formatFileSize(bytesSaved) + "\n" +
                                    "Location: " + outputFile.getParent());
                });
            }, error -> {
                Platform.runLater(() -> {
                    appendStatus("❌ ZIP compression failed: " + error);
                    compressProgress.setProgress(0);
                    compressProgressLabel.setText("0%");
                    showAlert(Alert.AlertType.ERROR, "ZIP Compression Failed", error);
                });
            });
        }
    }

    private void handleGzipDecompression() {
        if (decompressFile == null) {
            showAlert(Alert.AlertType.WARNING, "No File Selected", "Please select a .gz file to decompress.");
            return;
        }
        if (!decompressFile.getName().toLowerCase().endsWith(".gz")) {
            showAlert(Alert.AlertType.WARNING, "Invalid File", "Please select a valid .gz file for decompression.");
            return;
        }
        try {
            if (!isValidGzipFile(decompressFile)) {
                showAlert(Alert.AlertType.WARNING, "Invalid File", "The selected file is not a valid GZIP file.");
                return;
            }
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Validation Failed", "Error validating GZIP file: " + ex.getMessage());
            return;
        }

        decompressProgress.setProgress(0);
        decompressProgressLabel.setText("0%");

        decompressProgress.setProgress(-1); // indeterminate
        decompressProgressLabel.setText("Processing...");

        performApiOperation("/api/compression/decompress/gzip", decompressFile, false, null, outputFile -> {
            long compressedSize = decompressFile.length();
            long decompressedSize = outputFile.length();

            Platform.runLater(() -> {
                updateOutputInfo("decompress", outputFile, compressedSize, decompressedSize);
                appendStatus("✅ File decompressed successfully: " + outputFile.getName());
                totalOperations.incrementAndGet();
                filesDecompressed.incrementAndGet();
                saveStatistics();
                refreshStatistics();
                decompressProgress.setProgress(1);
                decompressProgressLabel.setText("100%");
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "File decompressed successfully!\n\n" +
                                "Archive: " + decompressFile.getName() + " (" + formatFileSize(compressedSize) + ")\n" +
                                "Extracted: " + outputFile.getName() + " (" + formatFileSize(decompressedSize) + ")\n" +
                                "Location: " + outputFile.getParent() + "\n" +
                                "Full path: " + outputFile.getAbsolutePath());
            });
        }, error -> {
            Platform.runLater(() -> {
                appendStatus("❌ Decompression failed: " + error);
                decompressProgress.setProgress(0);
                decompressProgressLabel.setText("0%");
                showAlert(Alert.AlertType.ERROR, "Decompression Failed", error);
            });
        });
    }

    private void handleZipDecompression() {
        if (decompressFile == null) {
            showAlert(Alert.AlertType.WARNING, "No File Selected", "Please select a .zip file to extract.");
            return;
        }
        if (!decompressFile.getName().toLowerCase().endsWith(".zip")) {
            showAlert(Alert.AlertType.WARNING, "Invalid File", "Please select a valid .zip file for extraction.");
            return;
        }

        decompressProgress.setProgress(0);
        decompressProgressLabel.setText("0%");

        decompressProgress.setProgress(-1); // indeterminate
        decompressProgressLabel.setText("Processing...");

        performApiOperation("/api/compression/decompress/zip", decompressFile, false, null, outputFile -> {
            long compressedSize = decompressFile.length();
            long decompressedSize = outputFile.length(); // Note: this is the size of the zipped extracted, not the extracted size

            Platform.runLater(() -> {
                updateOutputInfo("decompress", outputFile, compressedSize, decompressedSize);
                appendStatus("✅ ZIP archive extracted successfully: " + outputFile.getName());
                totalOperations.incrementAndGet();
                filesDecompressed.incrementAndGet();
                saveStatistics();
                refreshStatistics();
                decompressProgress.setProgress(1);
                decompressProgressLabel.setText("100%");
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "ZIP archive extracted successfully!\n\n" +
                                "Archive: " + decompressFile.getName() + " (" + formatFileSize(compressedSize) + ")\n" +
                                "Extracted to: " + outputFile.getName() + " (zipped)\n" +
                                "Location: " + outputFile.getParent());
            });
        }, error -> {
            Platform.runLater(() -> {
                appendStatus("❌ ZIP extraction failed: " + error);
                decompressProgress.setProgress(0);
                decompressProgressLabel.setText("0%");
                showAlert(Alert.AlertType.ERROR, "ZIP Extraction Failed", error);
            });
        });
    }

    private void setupDragAndDrop(Pane pane, String type) {
        pane.setUserData(type);

        pane.setOnDragOver(event -> {
            if (event.getGestureSource() != pane && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                File droppedFile = db.getFiles().get(0);
                if ("compress".equals(type)) {
                    // Check if ZIP compression is selected
                    VBox typeBox = (VBox) pane.getChildren().get(1);
                    VBox radioBox = (VBox) typeBox.getChildren().get(1);
                    RadioButton zipRadio = (RadioButton) radioBox.getChildren().get(1);
                    boolean isZip = zipRadio.isSelected();

                    if (isZip || droppedFile.isFile()) {
                        if (droppedFile.length() == 0) {
                            appendStatus("❌ Dropped file is empty: " + droppedFile.getName());
                        } else if (droppedFile.length() > 1024L * 1024 * 1024 * 10) {
                            appendStatus("❌ Dropped file is too large (over 10GB): " + droppedFile.getName());
                        } else {
                            setCompressFile(droppedFile);
                            success = true;
                            appendStatus("📁 File selected for compression: " + droppedFile.getName());
                        }
                    } else {
                        appendStatus("❌ Dropped folder not allowed for GZIP compression");
                    }
                } else {
                    if (droppedFile.length() == 0) {
                        appendStatus("❌ Dropped file is empty: " + droppedFile.getName());
                    } else if (droppedFile.length() > 1024L * 1024 * 1024 * 10) {
                        appendStatus("❌ Dropped file is too large (over 10GB): " + droppedFile.getName());
                    } else {
                        setDecompressFile(droppedFile);
                        success = true;
                        appendStatus("📁 File selected for decompression: " + droppedFile.getName());
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private VBox createStatusPane() {
        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        statusArea.setPrefHeight(250); // Increased from 150 to 250
        statusArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px; " +
                "-fx-control-inner-background: #2c3e50; -fx-text-fill: #ecf0f1; " +
                "-fx-border-color: #34495e; -fx-border-radius: 5;");

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = createStyledButton("🗑️ Clear Log", "#e74c3c");
        clearBtn.setOnAction(e -> statusArea.clear());

        Button exportBtn = createStyledButton("💾 Export Log", "#3498db");
        exportBtn.setOnAction(e -> exportLog());

        toolbar.getChildren().addAll(clearBtn, exportBtn);

        VBox vbox = new VBox(8, new Label("📋 Operation Log:"), statusArea, toolbar);
        vbox.setPadding(new Insets(10, 0, 0, 0));
        return vbox;
    }

    private void exportLog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Log File");
        fileChooser.setInitialFileName("compression_log_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".txt");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.write(statusArea.getText());
                appendStatus("✅ Log exported to: " + file.getAbsolutePath());
            } catch (IOException e) {
                appendStatus("❌ Failed to export log: " + e.getMessage());
            }
        }
    }

    private void setCompressFile(File file) {
        compressFile = file;
        Platform.runLater(() -> {
            compressFileNameLabel.setText("File Name: " + file.getName());
            compressFileSizeLabel.setText("File Size: " + formatFileSize(file.length()));
            compressFileTypeLabel.setText("File Type: " + (file.isDirectory() ? "Directory" : getFileExtension(file)));
            compressLastModifiedLabel.setText("Last Modified: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified())));
            compressFilePathLabel.setText("File Path: " + file.getAbsolutePath());
            appendStatus("📄 File selected for compression: " + file.getName() + " (" + formatFileSize(file.length()) + ")");
        });
    }

    private void setDecompressFile(File file) {
        decompressFile = file;
        Platform.runLater(() -> {
            decompressFileNameLabel.setText("File Name: " + file.getName());
            decompressFileSizeLabel.setText("File Size: " + formatFileSize(file.length()));
            decompressFileTypeLabel.setText("File Type: " + getFileExtension(file));
            decompressLastModifiedLabel.setText("Last Modified: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified())));
            decompressFilePathLabel.setText("File Path: " + file.getAbsolutePath());
            appendStatus("📦 File selected for decompression: " + file.getName() + " (" + formatFileSize(file.length()) + ")");
        });
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return file.isDirectory() ? "Directory" : "File";
        }
        return name.substring(lastIndexOf).toUpperCase() + " File";
    }

    private void clearCompressSelection() {
        compressFile = null;
        Platform.runLater(() -> {
            compressFileNameLabel.setText("File Name: -");
            compressFileSizeLabel.setText("File Size: -");
            compressFileTypeLabel.setText("File Type: -");
            compressLastModifiedLabel.setText("Last Modified: -");
            compressFilePathLabel.setText("File Path: -");
            appendStatus("🗑️ Compression selection cleared");
        });
    }

    private void clearDecompressSelection() {
        decompressFile = null;
        Platform.runLater(() -> {
            decompressFileNameLabel.setText("File Name: -");
            decompressFileSizeLabel.setText("File Size: -");
            decompressFileTypeLabel.setText("File Type: -");
            decompressLastModifiedLabel.setText("Last Modified: -");
            decompressFilePathLabel.setText("File Path: -");
            appendStatus("🗑️ Decompression selection cleared");
        });
    }

    private void appendStatus(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        Platform.runLater(() -> {
            statusArea.appendText("[" + timestamp + "] " + message + "\n");
            statusArea.selectPositionCaret(statusArea.getLength());
            statusArea.deselect();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private File getUniqueOutputFile(File parentDir, String baseName, String extension) {
        File outputFile = new File(parentDir, baseName + extension);
        int counter = 1;
        while (outputFile.exists()) {
            outputFile = new File(parentDir, baseName + "_" + counter + extension);
            counter++;
        }
        return outputFile;
    }

    private boolean isValidGzipFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[2];
            if (fis.read(header) != 2) return false;
            return header[0] == (byte) 0x1F && header[1] == (byte) 0x8B;
        }
    }



















    private long calculateTotalSize(File file) {
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

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private void saveStatistics() {
        Properties props = new Properties();
        props.setProperty("totalOperations", String.valueOf(totalOperations.get()));
        props.setProperty("totalBytesSaved", String.valueOf(totalBytesSaved.get()));
        props.setProperty("filesCompressed", String.valueOf(filesCompressed.get()));
        props.setProperty("filesDecompressed", String.valueOf(filesDecompressed.get()));

        try (FileOutputStream fos = new FileOutputStream("compression_stats.properties")) {
            props.store(fos, "Compression Tool Statistics");
        } catch (IOException e) {
            appendStatus("❌ Failed to save statistics: " + e.getMessage());
        }
    }

    private void loadStatistics() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("compression_stats.properties")) {
            props.load(fis);
            totalOperations.set(Integer.parseInt(props.getProperty("totalOperations", "0")));
            totalBytesSaved.set(Long.parseLong(props.getProperty("totalBytesSaved", "0")));
            filesCompressed.set(Integer.parseInt(props.getProperty("filesCompressed", "0")));
            filesDecompressed.set(Integer.parseInt(props.getProperty("filesDecompressed", "0")));
        } catch (IOException e) {
            // No previous statistics found, starting fresh
        }
    }

    private void updateOutputInfo(String type, File outputFile, long originalSize, long outputSize) {
        Platform.runLater(() -> {
            if (type.equals("compress")) {
                compressOutputLocationLabel.setText("Output Location: " + outputFile.getParent());
                compressOutputFormatLabel.setText("Output Format: " + (outputFile.getName().endsWith(".zip") ? "ZIP Archive" : "GZIP File"));
                compressEstimatedSizeLabel.setText("Estimated Size: " + formatFileSize(outputSize));
                if (originalSize > 0) {
                    double ratio = (1 - (double) outputSize / originalSize) * 100;
                    compressCompressionRatioLabel.setText("Compression Ratio: " + String.format("%.1f%%", ratio));
                } else {
                    compressCompressionRatioLabel.setText("Compression Ratio: N/A");
                }
            } else {
                decompressOutputLocationLabel.setText("Output Location: " + outputFile.getParent());
                decompressOutputFormatLabel.setText("Output Format: " + (outputFile.isDirectory() ? "Extracted Files Directory" : "Decompressed File"));
                decompressEstimatedSizeLabel.setText("Estimated Size: " + formatFileSize(outputSize));
                decompressCompressionRatioLabel.setText("Compression Ratio: N/A");
            }
        });
    }
}