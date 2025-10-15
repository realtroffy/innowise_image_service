package com.innowise.imageservice.service;

import com.innowise.imageservice.config.S3Properties;
import com.innowise.imageservice.exception.ImageFileOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    public static final String FILE_UPLOAD_EXCEPTION_MESSAGE = "";
    public static final String URL_FORMAT = "%s/%s/%s";

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public String uploadImage(MultipartFile file, String filename) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(filename)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("File uploaded successfully: {}", filename);
            return generateImageUrl(filename);
        } catch (Exception e) {
            throw new ImageFileOperationException(FILE_UPLOAD_EXCEPTION_MESSAGE + e.getMessage(), e);
        }
    }

    private String generateImageUrl(String filename) {
        return String.format(URL_FORMAT,
                s3Properties.getPublicUrl(),
                s3Properties.getBucketName(),
                filename);
    }

    public void deleteFile(String filename) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(filename)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", filename);

        } catch (Exception e) {
            throw new ImageFileOperationException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
}
