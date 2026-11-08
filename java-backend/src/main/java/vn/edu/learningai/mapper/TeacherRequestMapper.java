package vn.edu.learningai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.edu.learningai.dto.response.TeacherRequestResponse;
import vn.edu.learningai.entity.TeacherRequest;

@Mapper(componentModel = "spring")
public interface TeacherRequestMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "decidedBy", source = "decidedBy.id")
    TeacherRequestResponse toResponse(TeacherRequest teacherRequest);
}
