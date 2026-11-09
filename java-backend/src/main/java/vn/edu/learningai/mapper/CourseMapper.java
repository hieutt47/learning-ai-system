package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.learningai.dto.response.CourseOwnerResponse;
import vn.edu.learningai.entity.Course;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    /**
     * Counts are filled by {@code CourseOwnerAssembler}, not here: they need two
     * extra queries, and a mapper that hits repositories is hard to reason about.
     */
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "teacherId", source = "teacher.id")
    @Mapping(target = "teacherName", source = "teacher.fullName")
    @Mapping(target = "lessonCount", ignore = true)
    @Mapping(target = "totalStudents", ignore = true)
    CourseOwnerResponse toCourseOwnerResponse(Course course);

    List<CourseOwnerResponse> toListCourseOwnerResponse(List<Course> courses);
}
