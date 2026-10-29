package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import vn.edu.learningai.dto.response.CategoryResponse;
import vn.edu.learningai.entity.Category;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toListCategoryResponse(List<Category> categories);
}
