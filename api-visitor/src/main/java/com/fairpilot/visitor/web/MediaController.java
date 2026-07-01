package com.fairpilot.visitor.web;

import com.fairpilot.core.auth.JwtProvider;
import com.fairpilot.core.common.ApiResponse;
import com.fairpilot.core.media.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaAssetService mediaAssetService;
    private final JwtProvider jwtProvider;

    /**
     * 파일 업로드
     * EXPO_ADMIN / PLATFORM_ADMIN / STAFF 전용
     * multipart/form-data 형식
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'PLATFORM_ADMIN', 'STAFF')")
    public ApiResponse<MediaAssetResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") @Valid MediaUploadRequest req,
            @RequestHeader("Authorization") String authorization) {

        Long uploadedBy = jwtProvider.extractUserId(authorization.replace("Bearer ", ""));
        return ApiResponse.ok(mediaAssetService.upload(file, req, uploadedBy));
    }

    /**
     * owner별 미디어 목록 조회 (공개)
     */
    @GetMapping
    public ApiResponse<List<MediaAssetResponse>> list(
            @RequestParam OwnerType ownerType,
            @RequestParam Long ownerId) {
        return ApiResponse.ok(mediaAssetService.findByOwner(ownerType, ownerId));
    }

    /**
     * 미디어 삭제
     * EXPO_ADMIN / PLATFORM_ADMIN 전용
     */
    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyRole('EXPO_ADMIN', 'PLATFORM_ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long mediaId) {
        mediaAssetService.delete(mediaId);
        return ApiResponse.ok(null);
    }
}