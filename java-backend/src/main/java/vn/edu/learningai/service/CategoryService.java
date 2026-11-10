package vn.edu.learningai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.learningai.dto.request.CreateCategoryRequest;
import vn.edu.learningai.dto.request.UpdateCategoryRequest;
import vn.edu.learningai.dto.response.CategoryResponse;
import vn.edu.learningai.dto.response.PageResponse;
import vn.edu.learningai.entity.Category;
import vn.edu.learningai.exception.ApiException;
import vn.edu.learningai.exception.ErrorCode;
import vn.edu.learningai.mapper.CategoryMapper;
import vn.edu.learningai.repository.CategoryRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final int MYSQL_FOREIGN_KEY_REFERENCE = 1451;

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(Pageable pageable) {
        Sort sort = normalizeSort(pageable.getSort());
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<Category> categories = categoryRepository.findAll(request);
        Page<CategoryResponse> responses = categories.map(category -> categoryMapper.toResponse(category));
        return PageResponse.from(responses);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByName(name)) {
            throw new ApiException(ErrorCode.INVALID_STATE, "Category name already exists");
        }
        Category category = Category.builder()
            .name(name)
            .description(request.getDescription())
            .build();
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CategoryResponse update(Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Name must not be blank");
            }
            if (categoryRepository.existsByNameAndIdNot(name, categoryId)) {
                throw new ApiException(ErrorCode.INVALID_STATE, "Category name already exists");
            }
            category.setName(name);
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Category not found"));
        try {
            categoryRepository.delete(category);
            categoryRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(
                ErrorCode.RESOURCE_IN_USE, "Category is used by a course"
            );
        }
    }

    private Sort normalizeSort(Sort requested) {
        if (requested.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt", "id");
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (Sort.Order order : requested) {
            String property = order.getProperty();
            if (property.equals("created_at") || property.equals("createdAt")) {
                property = "createdAt";
            } else if (property.equals("updated_at") || property.equals("updatedAt")) {
                property = "updatedAt";
            } else if (!property.equals("id") && !property.equals("name")) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Unsupported sort field");
            }
            orders.add(new Sort.Order(order.getDirection(), property));
        }
        Sort sort = Sort.by(orders);
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return sort;
    }
}
