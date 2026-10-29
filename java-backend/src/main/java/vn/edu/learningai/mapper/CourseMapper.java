package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.learningai.dto.response.CourseOwnerResponse;
import vn.edu.learningai.entity.Course;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "teacherId", source = "teacher.id")
    CourseOwnerResponse toCourseOwnerResponse(Course course);

    List<CourseOwnerResponse> toListCourseOwnerResponse(List<Course> courses);
}
