package vn.edu.learningai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.learningai.entity.CourseMember;
import vn.edu.learningai.entity.CourseMemberId;

public interface CourseMemberRepository extends JpaRepository<CourseMember, CourseMemberId> {
    boolean existsById_CourseIdAndId_StudentId(Long courseId, Long studentId);
}
