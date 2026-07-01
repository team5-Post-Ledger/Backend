package com.fairpilot.core.media;

import com.fairpilot.core.common.BusinessException;
import com.fairpilot.core.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAssetService {

    private final MediaAssetRepository mediaAssetRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * 파일 업로드
     * 1. 파일 유효성 검증 (타입, 크기)
     * 2. S3 업로드
     * 3. media_asset 레코드 저장
     */
    @Transactional
    public MediaAssetResponse upload(MultipartFile file,
                                     MediaUploadRequest req,
                                     Long uploadedBy) {
        // 파일 유효성 검증
        validateFile(file);

        // S3 키 생성: {ownerType}/{ownerId}/{UUID}.{확장자}
        String ext = getExtension(file.getOriginalFilename());
        String s3Key = req.ownerType().name().toLowerCase() + "/"
                + req.ownerId() + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);

        // S3 업로드
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("S3 업로드 완료: bucket={}, key={}", bucket, s3Key);

        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.UPSTREAM_ERROR,
                    "파일 업로드에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        // 미디어 타입 판별
        MediaType mediaType = isVideo(file.getContentType())
                ? MediaType.VIDEO : MediaType.IMAGE;

        // DB 저장
        MediaAsset asset = MediaAsset.builder()
                .ownerType(req.ownerType())
                .ownerId(req.ownerId())
                .mediaType(mediaType)
                .s3Key(s3Key)
                .s3Bucket(bucket)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .displayOrder(req.displayOrder())
                .isThumbnail(req.isThumbnail())
                .uploadedBy(uploadedBy)
                .build();

        mediaAssetRepository.save(asset);

        String url = buildUrl(s3Key);
        log.info("미디어 업로드 완료: id={}, ownerType={}, ownerId={}",
                asset.getId(), req.ownerType(), req.ownerId());

        return MediaAssetResponse.of(asset, url);
    }

    /**
     * owner별 미디어 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MediaAssetResponse> findByOwner(OwnerType ownerType, Long ownerId) {
        return mediaAssetRepository
                .findByOwnerTypeAndOwnerIdOrderByDisplayOrderAsc(ownerType, ownerId)
                .stream()
                .map(a -> MediaAssetResponse.of(a, buildUrl(a.getS3Key())))
                .collect(Collectors.toList());
    }

    /**
     * 미디어 삭제 (Soft Delete + S3 삭제)
     */
    @Transactional
    public void delete(Long mediaId) {
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "미디어를 찾을 수 없습니다."));

        // S3 삭제
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(asset.getS3Bucket())
                    .key(asset.getS3Key())
                    .build());
            log.info("S3 삭제 완료: key={}", asset.getS3Key());
        } catch (Exception e) {
            log.error("S3 삭제 실패: key={}, error={}", asset.getS3Key(), e.getMessage());
            // S3 삭제 실패해도 DB soft delete는 진행
        }

        mediaAssetRepository.delete(asset);
        log.info("미디어 삭제 완료: id={}", mediaId);
    }

    // ── private 유틸 ────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "파일이 없습니다.");
        }

        // 허용 MIME 타입
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "이미지 또는 영상 파일만 업로드 가능합니다.");
        }

        // 최대 파일 크기: 이미지 10MB, 영상 500MB
        long maxSize = contentType.startsWith("video/")
                ? 500L * 1024 * 1024
                : 10L * 1024 * 1024;

        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "파일 크기 초과: " + (contentType.startsWith("video/") ? "500MB" : "10MB"));
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isVideo(String contentType) {
        return contentType != null && contentType.startsWith("video/");
    }

    private String buildUrl(String s3Key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + s3Key;
    }
}