package com.example.demo.AWS_S3.domain;

import com.example.demo.AWS_S3.dto.MediaUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class S3MultipartService {
    private static final Logger logger = LoggerFactory.getLogger(S3MultipartService.class);
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    @Autowired
    private S3Client s3Client;
    @Autowired
    private S3Presigner s3Presigner;

    private final long PART_SIZE = 50 * 1024 * 1024; // 50MB por parte
    private final long MIN_MULTIPART_SIZE = 100 * 1024 * 1024; // 100MB mínimo para multipart

    public String generateSecureDownloadUrl(String fileName, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            logger.debug("URL firmada generada para: {} (expira en: {})", fileName, expiration);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            logger.error("Error generando URL firmada para {}: {}", fileName, e.getMessage());
            throw new RuntimeException("Error generando URL de descarga", e);
        }
    }

    private String getSecureFileUrl(String fileName) {
        // En lugar de URL pública, generar URL firmada
        return generateSecureDownloadUrl(fileName, Duration.ofDays(7)); // Expira en 7 días
    }

    public MediaUploadResponse uploadMediaFile(MultipartFile file, String folder) {
        long startTime = System.currentTimeMillis();
        validateFile(file);
        String fileName = generateFileName(file, folder);
        long fileSize = file.getSize();
        try {
            String fileUrl;
            if (fileSize > MIN_MULTIPART_SIZE) {
                logger.info("Usando multipart upload para archivo: {} ({}MB)", fileName, fileSize / (1024 * 1024));
                fileUrl = uploadWithMultipart(file, fileName);
            } else {
                logger.info("Usando upload simple para archivo: {} ({}MB)", fileName, fileSize / (1024 * 1024));
                fileUrl = uploadSimple(file, fileName);
            }
            long uploadTime = System.currentTimeMillis() - startTime;
            return MediaUploadResponse.builder()
                    .success(true)
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .uploadMethod(fileSize > MIN_MULTIPART_SIZE ? "multipart" : "simple")
                    .uploadTimeMs(uploadTime)
                    .build();

        } catch (Exception e) {
            logger.error("Error subiendo archivo {}: {}", fileName, e.getMessage());
            return MediaUploadResponse.builder()
                    .success(false)
                    .errorMessage("Error interno: " + e.getMessage())
                    .fileName(fileName)
                    .fileSize(fileSize)
                    .build();
        }
    }

    private String uploadWithMultipart(MultipartFile file, String fileName) throws IOException {
        String uploadId = null;
        try (InputStream inputStream = file.getInputStream()) {
            // 1. Iniciar multipart upload
            CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .metadata(java.util.Map.of(
                            "uploaded-by", "spring-app",
                            "upload-timestamp", String.valueOf(System.currentTimeMillis()),
                            "original-name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
                    ))
                    .build();
            CreateMultipartUploadResponse createMultipartUploadResponse = s3Client.createMultipartUpload(createMultipartUploadRequest);
            uploadId = createMultipartUploadResponse.uploadId();
            // 2. Calcular partes
            long fileSize = file.getSize();
            long partCount = (fileSize + PART_SIZE - 1) / PART_SIZE; // Redondear hacia arriba
            List<CompletedPart> completedParts = new ArrayList<>();
            // 3. Subir partes
            for (int partNumber = 1; partNumber <= partCount; partNumber++) {
                long startPos = (partNumber - 1) * PART_SIZE;
                long curPartSize = Math.min(PART_SIZE, fileSize - startPos);

                // Crear datos para esta parte
                byte[] partData = new byte[(int) curPartSize];
                int bytesRead = inputStream.read(partData);

                if (bytesRead != curPartSize) {
                    throw new IOException("Error leyendo datos del archivo");
                }
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(fileName)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(
                        uploadPartRequest,
                        RequestBody.fromBytes(partData)
                );
                CompletedPart completedPart = CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build();
                completedParts.add(completedPart);
                double progress = ((double) partNumber / partCount) * 100;
                logger.info("Progreso upload: {:.1f}% (parte {}/{})", progress, partNumber, partCount);
            }

            // 4. Completar multipart upload
            CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .uploadId(uploadId)
                    .multipartUpload(completedMultipartUpload)
                    .build();

            s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            logger.info("Multipart upload completado exitosamente: {}", fileName);
            return getSecureFileUrl(fileName);

        } catch (Exception e) {
            // Limpiar multipart upload fallido
            if (uploadId != null) {
                try {
                    AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .uploadId(uploadId)
                            .build();
                    s3Client.abortMultipartUpload(abortRequest);
                    logger.warn("Multipart upload abortado para: {}", fileName);
                } catch (Exception abortEx) {
                    logger.error("Error abortando multipart upload: {}", abortEx.getMessage());
                }
            }
            throw new RuntimeException("Error en multipart upload: " + e.getMessage(), e);
        }
    }

    private String uploadSimple(MultipartFile file, String fileName) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(java.util.Map.of(
                            "uploaded-by", "spring-app",
                            "upload-timestamp", String.valueOf(System.currentTimeMillis()),
                            "original-name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown"
                    ))
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
            return getSecureFileUrl(fileName);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (fileExists(file.getOriginalFilename())) {
            throw new IllegalArgumentException("El archivo ya existe");
        }
        long maxSize = 4L * 1024 * 1024 * 1024; // 4GB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Archivo demasiado grande. Máximo: 4GB");
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("video/") && !contentType.startsWith("audio/")) && !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Tipo de archivo no válido. Solo video y audio");
        }
        // Validar extensión
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidExtension(originalFilename)) {
            throw new IllegalArgumentException("Extensión de archivo no válida");
        }
    }

    private boolean isValidExtension(String filename) {
        String[] validExtensions = {
                // Video
                ".mp4", ".avi", ".mov", ".wmv", ".flv", ".webm", ".mkv", ".m4v", ".mpg", ".mpeg",
                ".m2v", ".ts", ".m2ts", ".3gp", ".3g2", ".mxf", ".ogv", ".vob",
                // Audio
                ".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a", ".aiff", ".aif", ".alac",
                ".wma", ".amr", ".opus", ".ac3", ".caf", ".mid", ".midi", ".dsf", ".dff",
                // Subtítulos / listas
                ".srt", ".vtt", ".ass", ".ssa", ".sub", ".idx", ".m3u", ".m3u8", ".pls", ".cue", ".mpd",
                // Imágenes
                ".jpg", ".jpeg", ".png", ".gif", ".webp", ".tiff", ".tif", ".bmp", ".svg", ".heic", ".heif", ".avif",
                // RAW de cámara
                ".cr2", ".nef", ".arw", ".rw2", ".orf", ".dng"
        };
        String lowerFilename = filename.toLowerCase();
        return Arrays.stream(validExtensions)
                .anyMatch(lowerFilename::endsWith);
    }

    private String generateFileName(MultipartFile file, String folder) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalName = file.getOriginalFilename();
        if (originalName == null) originalName = "unknown_file";
        String cleanName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return folder + "/" + timestamp + "_" + cleanName;
    }


    // Method para verificar si un archivo existe
    public boolean fileExists(String fileName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Error verificando existencia del archivo: {}", e.getMessage());
            return false;
        }
    }


    public boolean deleteObject(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true; // S3 no devuelve 404 si no existe; si no lanzó excepción asumimos OK
        } catch (NoSuchKeyException e) {
            // El objeto no existe; lo tratamos como idempotente
            return false;
        }
    }

    public String determineFolder(String contentType) {
        if (contentType == null) return "media";
        if (contentType.startsWith("video/")) {
            return "videos";
        } else if (contentType.startsWith("audio/")) {
            return "audios";
        } else {
            return "media";
        }
    }
}
