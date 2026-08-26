package service;

import dao.PostDAO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class PostService {
    private final PostDAO postDAO = new PostDAO();

    public boolean deletePostByAdmin(long postId, String memberRole) {
        if (!"ADMIN".equals(memberRole)) {
            throw new SecurityException("관리자 권한이 필요합니다.");
        }

        try (Connection connection = DBUtil.getConnection()) {
            return postDAO.deletePostByAdmin(connection, postId) > 0;
        } catch (SQLException exception) {
            throw new RuntimeException("게시글 삭제 중 데이터베이스 오류가 발생했습니다.", exception);
        }
    }
}
