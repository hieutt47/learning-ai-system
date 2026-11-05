package vn.edu.learningai.security;

/** ID người dùng đang gọi API. Khi auth (JWT) merge vào develop, cài đặt bằng SecurityHelper và bỏ stub. */
public interface CurrentUserProvider {

    /** @throws vn.edu.learningai.exception.ApiException UNAUTHENTICATED nếu chưa đăng nhập */
    Long currentUserId();
}
