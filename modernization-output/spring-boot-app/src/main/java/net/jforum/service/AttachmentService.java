package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.AttachmentDto;
import net.jforum.entity.Attachment;
import net.jforum.entity.Post;
import net.jforum.entity.User;
import net.jforum.exception.BadRequestException;
import net.jforum.exception.ForbiddenException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.AttachmentRepository;
import net.jforum.repository.PostRepository;
import net.jforum.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    
    @Value("${attachment.upload-dir:uploads}")
    private String uploadDir;
    
    @Value("${attachment.max-size:5242880}") // 5MB default
    private long maxFileSize;
    
    @Value("${attachment.allowed-types:image/jpeg,image/png,image/gif,application/pdf,application/zip}")
    private List<String> allowedTypes;

    @Transactional(readOnly = true)
    public List<AttachmentDto> getAttachmentsByPostId(Long postId) {
        return attachmentRepository.findByPostId(postId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AttachmentDto getAttachmentById(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id));
        
        return mapToDto(attachment);
    }

    @Transactional
    public AttachmentDto uploadAttachment(MultipartFile file, Long userId) {
        // Validate file
        if (file.isEmpty()) {
            throw new BadRequestException("File cannot be empty");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new BadRequestException("File size exceeds maximum allowed size");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BadRequestException("File type not allowed");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String physicalFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Save file to disk
            Path filePath = uploadPath.resolve(physicalFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create attachment record
            Attachment attachment = Attachment.builder()
                    .filename(originalFilename)
                    .filesize(file.getSize())
                    .contentType(contentType)
                    .downloadCount(0)
                    .physicalFilename(physicalFilename)
                    .user(user)
                    .build();
            
            Attachment savedAttachment = attachmentRepository.save(attachment);
            return mapToDto(savedAttachment);
            
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    @Transactional
    public void associateAttachmentsWithPost(Long postId, List<Long> attachmentIds) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        
        for (Long attachmentId : attachmentIds) {
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));
            
            // Check if attachment belongs to the post author
            if (!attachment.getUser().getId().equals(post.getUser().getId())) {
                throw new ForbiddenException("You can only attach your own files");
            }
            
            // Associate attachment with post
            attachment.setPost(post);
            attachmentRepository.save(attachment);
        }
    }

    @Transactional
    public void deleteAttachment(Long id, Long userId) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id));
        
        // Check if user is the attachment owner
        if (!attachment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to delete this attachment");
        }
        
        // Delete file from disk
        try {
            Path filePath = Paths.get(uploadDir).resolve(attachment.getPhysicalFilename());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log error but continue with database deletion
        }
        
        // Delete from database
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteAttachmentsByPostId(Long postId) {
        List<Attachment> attachments = attachmentRepository.findByPostId(postId);
        
        // Delete files from disk
        for (Attachment attachment : attachments) {
            try {
                Path filePath = Paths.get(uploadDir).resolve(attachment.getPhysicalFilename());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but continue with database deletion
            }
        }
        
        // Delete from database
        attachmentRepository.deleteAll(attachments);
    }

    @Transactional
    public void incrementDownloadCount(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", id));
        
        attachment.setDownloadCount(attachment.getDownloadCount() + 1);
        attachmentRepository.save(attachment);
    }

    // Helper method to map Attachment entity to AttachmentDto
    private AttachmentDto mapToDto(Attachment attachment) {
        AttachmentDto dto = AttachmentDto.builder()
                .id(attachment.getId())
                .filename(attachment.getFilename())
                .filesize(attachment.getFilesize())
                .contentType(attachment.getContentType())
                .downloadCount(attachment.getDownloadCount())
                .userId(attachment.getUser().getId())
                .build();
        
        if (attachment.getPost() != null) {
            dto.setPostId(attachment.getPost().getId());
        }
        
        return dto;
    }
}