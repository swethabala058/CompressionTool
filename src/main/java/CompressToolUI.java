import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.*;

public class CompressToolUI extends Application {

    private File compressFile;
    private File decompressFile;

    private Label compressFileLabel = new Label("No file selected");
    private Label decompressFileLabel = new Label("No file selected");
    private TextArea statusArea = new TextArea();
    private ProgressBar compressProgress = new ProgressBar(0);
    private ProgressBar decompressProgress = new ProgressBar(0);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Compression and Decompression Tool - JavaFX");

        TabPane tabPane = new TabPane();

        Tab compressTab = new Tab("Compress");
        compressTab.setContent(createCompressPane());
        compressTab.setClosable(false);

        Tab decompressTab = new Tab("Decompress");
        decompressTab.setContent(createDecompressPane());
        decompressTab.setClosable(false);

        tabPane.getTabs().addAll(compressTab, decompressTab);

        VBox root = new VBox(10, tabPane, createStatusPane());
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 400);
        // scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); // Commented out as style.css does not exist
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Pane createCompressPane() {
        Button chooseFileBtn = new Button("📁 Choose File to Compress");
        chooseFileBtn.setTooltip(new Tooltip("Click to select a file for compression or drag and drop a file here"));
        chooseFileBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Compress");
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                compressFile = selectedFile;
                compressFileLabel.setText(selectedFile.getAbsolutePath());
                appendStatus("Selected file for compression: " + selectedFile.getName());
            }
        });

        Button compressBtn = new Button("🗜️ Compress (GZIP)");
        compressBtn.setTooltip(new Tooltip("Compress the selected file using GZIP"));
        compressBtn.setOnAction(e -> {
            if (compressFile == null) {
                appendStatus("Please select a file to compress.");
                return;
            }
            compressProgress.setProgress(0);
            try {
                File outputFile = new File(compressFile.getParent(), compressFile.getName() + ".gz");
                compressGZIP(compressFile, outputFile);
                appendStatus("Compression completed: " + outputFile.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Compression Completed");
                alert.setContentText("File compressed successfully to " + outputFile.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException ex) {
                appendStatus("Error during compression: " + ex.getMessage());
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Compression Failed");
                errorAlert.setContentText(ex.getMessage());
                errorAlert.showAndWait();
            }
        });

        VBox vbox = new VBox(10, chooseFileBtn, compressFileLabel, compressBtn, compressProgress);
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.setPadding(new Insets(10));

        // Drag and drop
        vbox.setOnDragOver(event -> {
            if (event.getGestureSource() != vbox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        vbox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                compressFile = db.getFiles().get(0);
                compressFileLabel.setText(compressFile.getAbsolutePath());
                appendStatus("Selected file for compression: " + compressFile.getName());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return vbox;
    }

    private Pane createDecompressPane() {
        Button chooseFileBtn = new Button("📁 Choose File to Decompress (.gz)");
        chooseFileBtn.setTooltip(new Tooltip("Click to select a .gz file for decompression or drag and drop a file here"));
        chooseFileBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Decompress");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("GZIP files (*.gz)", "*.gz");
            fileChooser.getExtensionFilters().add(extFilter);
            File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                decompressFile = selectedFile;
                decompressFileLabel.setText(selectedFile.getAbsolutePath());
                appendStatus("Selected file for decompression: " + selectedFile.getName());
            }
        });

        Button decompressBtn = new Button("📂 Decompress (GZIP)");
        decompressBtn.setTooltip(new Tooltip("Decompress the selected .gz file"));
        decompressBtn.setOnAction(e -> {
            if (decompressFile == null) {
                appendStatus("Please select a file to decompress.");
                return;
            }
            decompressProgress.setProgress(0);
            try {
                String outputName = decompressFile.getName().replaceAll("\\.gz$", "");
                File outputFile = new File(decompressFile.getParent(), outputName);
                decompressGZIP(decompressFile, outputFile);
                appendStatus("Decompression completed: " + outputFile.getAbsolutePath());
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Decompression Completed");
                alert.setContentText("File decompressed successfully to " + outputFile.getAbsolutePath());
                alert.showAndWait();
            } catch (IOException ex) {
                appendStatus("Error during decompression: " + ex.getMessage());
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Error");
                errorAlert.setHeaderText("Decompression Failed");
                errorAlert.setContentText(ex.getMessage());
                errorAlert.showAndWait();
            }
        });

        VBox vbox = new VBox(10, chooseFileBtn, decompressFileLabel, decompressBtn, decompressProgress);
        vbox.setAlignment(Pos.CENTER_LEFT);
        vbox.setPadding(new Insets(10));

        // Drag and drop
        vbox.setOnDragOver(event -> {
            if (event.getGestureSource() != vbox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        vbox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                decompressFile = db.getFiles().get(0);
                decompressFileLabel.setText(decompressFile.getAbsolutePath());
                appendStatus("Selected file for decompression: " + decompressFile.getName());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return vbox;
    }

    private Pane createStatusPane() {
        statusArea.setEditable(false);
        statusArea.setWrapText(true);
        statusArea.setPrefHeight(150);
        statusArea.setPromptText("Status messages will appear here...");
        VBox vbox = new VBox(new Label("Status:"), statusArea);
        vbox.setPadding(new Insets(10, 0, 0, 0));
        return vbox;
    }

    private void appendStatus(String message) {
        statusArea.appendText(message + "\n");
    }

    private void compressGZIP(File sourceFile, File destFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(destFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            long fileSize = sourceFile.length();

            while ((bytesRead = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                updateProgress(totalRead, fileSize, "Compressing");
            }
        }
    }

    private void decompressGZIP(File sourceFile, File destFile) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(sourceFile));
             FileOutputStream fos = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalRead = 0;
            long fileSize = sourceFile.length();

            while ((bytesRead = gzis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                updateProgress(totalRead, fileSize, "Decompressing");
            }
        }
    }

    private void updateProgress(long current, long total, String action) {
        int percent = (int) ((current * 100) / total);
        String progressMessage = String.format("%s... %d%%", action, percent);
        // Update status area on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            if (statusArea.getText().contains(progressMessage)) {
                // Already showing this progress, do nothing
            } else {
                appendStatus(progressMessage);
            }
        });
    }
}
