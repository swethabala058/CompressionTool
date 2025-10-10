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
