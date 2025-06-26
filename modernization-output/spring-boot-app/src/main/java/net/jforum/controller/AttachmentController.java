package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.AttachmentDto;
import net.jforum.security.CurrentUser;
import net.jforum.service.AttachmentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Attachment management API")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/post/{postId}")
    @Operation(summary = "Get attachments by post ID")
    public ResponseEntity<List<AttachmentDto>> getAttachmentsByPostId(@PathVariable Long postId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByPostId(postId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get attachment details by ID")
    public ResponseEntity<AttachmentDto> getAttachmentById(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachmentById(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        try {
            // Get attachment details
            AttachmentDto attachment = attachmentService.getAttachmentById(id);
            
            // Increment download count
            attachmentService.incrementDownloadCount(id);
            
            // Load file as Resource
            Path filePath = Paths.get("uploads").resolve(attachment.getFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            // Check if file exists
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            // Set content disposition and content type
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFilename() + "\"")
                    .body(resource);
            
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/upload")
    @Operation(
            summary = "Upload an attachment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<AttachmentDto> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentUser currentUser) {
        AttachmentDto attachment = attachmentService.uploadAttachment(file, currentUser.getId());
        return ResponseEntity.ok(attachment);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete an attachment",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponseDto<Void>> deleteAttachment(
            @PathVariable Long id,
            @AuthenticationPrincipal CurrentUser currentUser) {
        attachmentService.deleteAttachment(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponseDto.success("Attachment deleted successfully"));
    }
}