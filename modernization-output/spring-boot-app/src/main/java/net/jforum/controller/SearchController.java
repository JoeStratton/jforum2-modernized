package net.jforum.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.jforum.dto.ApiResponseDto;
import net.jforum.dto.SearchRequestDto;
import net.jforum.dto.SearchResultDto;
import net.jforum.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Search API")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Search for content")
    public ResponseEntity<SearchResultDto> search(
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long forumId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        SearchRequestDto searchRequest = SearchRequestDto.builder()
                .keywords(keywords)
                .userId(userId)
                .forumId(forumId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .sortBy(sortBy)
                .build();
        
        return ResponseEntity.ok(searchService.search(searchRequest, page, size));
    }

    @PostMapping("/reindex")
    @Operation(
            summary = "Reindex all posts",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Void>> reindexAllPosts() {
        searchService.reindexAllPosts();
        return ResponseEntity.ok(ApiResponseDto.success("All posts have been reindexed"));
    }
}