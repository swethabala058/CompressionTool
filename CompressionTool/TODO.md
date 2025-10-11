# TODO: Integrate Spring Boot Backend

## Tasks
- [x] Update pom.xml to add Spring Boot dependencies
- [x] Create CompressionToolApplication.java (Spring Boot main class)
- [x] Create CompressionService.java (extract compression logic)
- [x] Create CompressionController.java (REST endpoints)
- [x] Modify CompressTool.java to use CompressionService
- [x] Refactor compressZIP to use service
- [x] Refactor decompressZIP to use service
- [x] Remove helper methods from CompressTool.java
- [x] Test REST endpoints (GET endpoint verified, POST endpoints attempted)
- [x] Run Spring Boot application
- [x] Verify UI still works (batch file runs UI successfully)
- [x] Update run_compression_tool.bat to include backend startup
- [x] Add PostgreSQL JDBC driver and Spring Data JPA dependencies to pom.xml
- [x] Create application.properties with JDBC URL configuration
- [x] Test database connection

## UI-Backend Integration for Database Persistence
- [ ] Update run_compression_tool.bat to keep backend terminal open (cmd /k)
- [ ] Refactor CompressToolUI.java: Add HttpClient imports and remove local compression/decompression methods
- [ ] Refactor CompressToolUI.java: Update handleGzipCompression to POST to /api/compression/compress/gzip and handle response
- [ ] Refactor CompressToolUI.java: Update handleZipCompression to POST to /api/compression/compress/zip and handle response
- [ ] Refactor CompressToolUI.java: Update handleGzipDecompression to POST to /api/compression/decompress/gzip and handle response
- [ ] Refactor CompressToolUI.java: Update handleZipDecompression to POST to /api/compression/decompress/zip and handle response
- [ ] Refactor CompressToolUI.java: Simplify progress tracking (indeterminate during API calls)
- [ ] Refactor CompressToolUI.java: Update statistics based on API response metadata
- [ ] Rebuild and test: Run batch file, perform operations in UI, verify data in pgAdmin
