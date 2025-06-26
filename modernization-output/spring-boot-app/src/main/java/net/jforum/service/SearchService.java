package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.PostDto;
import net.jforum.dto.SearchRequestDto;
import net.jforum.dto.SearchResultDto;
import net.jforum.entity.Post;
import net.jforum.entity.SearchIndex;
import net.jforum.entity.SearchWord;
import net.jforum.repository.PostRepository;
import net.jforum.repository.SearchIndexRepository;
import net.jforum.repository.SearchWordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchWordRepository searchWordRepository;
    private final SearchIndexRepository searchIndexRepository;
    private final PostRepository postRepository;
    private final PostService postService;

    @Transactional(readOnly = true)
    public SearchResultDto search(SearchRequestDto searchRequest, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Basic search implementation - in a real application, you'd use a proper search engine
        // like Elasticsearch or Lucene
        Page<Post> posts;
        
        if (searchRequest.getUserId() != null) {
            // Search by user
            posts = postRepository.findByUserId(searchRequest.getUserId(), pageable);
        } else if (searchRequest.getKeywords() != null && !searchRequest.getKeywords().isBlank()) {
            // Search by keywords
            // This is a simplified implementation - in a real app, you'd use a search engine
            // or a more sophisticated database search
            String keywords = "%" + searchRequest.getKeywords().toLowerCase() + "%";
            posts = postRepository.findAll(pageable);
            // Filter posts containing keywords (this is inefficient and just for demonstration)
            posts = posts.filter(post -> post.getContent().toLowerCase().contains(searchRequest.getKeywords().toLowerCase()));
        } else {
            // Default to recent posts
            posts = postRepository.findAll(pageable);
        }
        
        // Map posts to DTOs
        List<PostDto> postDtos = posts.getContent().stream()
                .map(post -> postService.getPostById(post.getId()))
                .collect(Collectors.toList());
        
        return SearchResultDto.builder()
                .posts(postDtos)
                .totalResults((int) posts.getTotalElements())
                .currentPage(page)
                .totalPages(posts.getTotalPages())
                .build();
    }

    @Transactional
    public void indexPost(Post post) {
        // This is a simplified implementation - in a real app, you'd use a search engine
        
        // Extract words from content
        String content = post.getContent();
        String[] words = content.toLowerCase().split("\\W+");
        
        for (String word : words) {
            if (word.length() < 3) {
                continue; // Skip short words
            }
            
            // Find or create search word
            SearchWord searchWord = searchWordRepository.findByWord(word)
                    .orElseGet(() -> {
                        SearchWord newWord = new SearchWord();
                        newWord.setWord(word);
                        return searchWordRepository.save(newWord);
                    });
            
            // Create search index entry
            SearchIndex index = SearchIndex.builder()
                    .word(searchWord)
                    .post(post)
                    .topic(post.getTopic())
                    .forum(post.getForum())
                    .relevance(1.0f) // Simple relevance score
                    .build();
            
            searchIndexRepository.save(index);
        }
    }

    @Transactional
    public void reindexAllPosts() {
        // Delete all existing index entries
        searchIndexRepository.deleteAll();
        
        // Get all posts and index them
        List<Post> allPosts = postRepository.findAll();
        for (Post post : allPosts) {
            indexPost(post);
        }
    }
}