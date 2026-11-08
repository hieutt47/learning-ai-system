package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import vn.edu.learningai.dto.response.CategoryResponse;
import vn.edu.learningai.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
