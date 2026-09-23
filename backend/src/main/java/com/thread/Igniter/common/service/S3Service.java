package com.thread.Igniter.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String endpointUrl;

    public String uploadProfilePicture(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "profile.jpg";
        String extension = ".jpg";
        int lastDot = originalName.lastIndexOf(".");
        if (lastDot != -1) {
            extension = originalName.substring(lastDot);
        }

        String key = "profiles/" + UUID.randomUUID() + extension;

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "image/jpeg";
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(
                putRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

        String cleanEndpoint = endpointUrl.replaceAll("/+$", "");
        return cleanEndpoint + "/" + bucketName + "/" + key;
    }

    public void deleteFileByUrl(String fileUrl) {
        if (fileUrl == null || !fileUrl.contains(bucketName)) {
            return;
        }

        try {
            // Find key starting after "/<bucketName>/"
            String marker = "/" + bucketName + "/";
            int idx = fileUrl.indexOf(marker);
            if (idx != -1) {
                String key = fileUrl.substring(idx + marker.length());
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteRequest);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old S3 file {}: {}", fileUrl, e.getMessage());
        }
    }
}
