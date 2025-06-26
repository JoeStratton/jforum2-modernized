package net.jforum.service;

import lombok.RequiredArgsConstructor;
import net.jforum.dto.CategoryDto;
import net.jforum.dto.CategoryWithForumsDto;
import net.jforum.entity.Category;
import net.jforum.exception.ConflictException;
import net.jforum.exception.ResourceNotFoundException;
import net.jforum.repository.CategoryRepository;
import net.jforum.repository.ForumRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ForumRepository forumRepository;
    private final ForumService forumService;

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryWithForumsDto> getAllCategoriesWithForums() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(category -> {
                    CategoryWithForumsDto dto = new CategoryWithForumsDto();
                    dto.setId(category.getId());
                    dto.setName(category.getName());
                    dto.setDisplayOrder(category.getDisplayOrder());
                    dto.setModerated(category.isModerated());
                    dto.setForums(forumService.getForumsByCategory(category.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToDto(category);
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        Category category = Category.builder()
                .name(categoryDto.getName())
                .displayOrder(categoryDto.getDisplayOrder())
                .moderated(categoryDto.isModerated())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToDto(savedCategory);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setName(categoryDto.getName());
        category.setDisplayOrder(categoryDto.getDisplayOrder());
        category.setModerated(categoryDto.isModerated());

        Category updatedCategory = categoryRepository.save(category);
        return mapToDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (!category.getForums().isEmpty()) {
            throw new ConflictException("Cannot delete category with forums. Remove forums first.");
        }

        categoryRepository.delete(category);
    }

    // Helper method to map Category entity to CategoryDto
    private CategoryDto mapToDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .displayOrder(category.getDisplayOrder())
                .moderated(category.isModerated())
                .build();
    }
}