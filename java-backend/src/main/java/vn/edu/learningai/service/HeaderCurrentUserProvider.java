//package vn.edu.learningai.service;
//
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import vn.edu.learningai.exception.ApiException;
//import vn.edu.learningai.exception.ErrorCode;
//
//@Component
//@RequiredArgsConstructor
//public class HeaderCurrentUserProvider implements CurrentUserProvider {
//    private final HttpServletRequest request;
//
//    @Override
//    public Long currentUserId() {
//        String rawUserId = request.getHeader("X-User-Id");
//        try {
//            long userId = Long.parseLong(rawUserId);
//            if (userId < 1) {
//                throw new NumberFormatException();
//            }
//            return userId;
//        } catch (RuntimeException exception) {
//            throw new ApiException(ErrorCode.UNAUTHENTICATED);
//        }
//    }
//}
