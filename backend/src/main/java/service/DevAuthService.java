package service;

import dao.DevAuthDAO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class DevAuthService {
    private final DevAuthDAO devAuthDAO = new DevAuthDAO();

    public boolean isDevMode() {
        String value = System.getenv("BOOKMATE_DEV_MODE");
        if (value == null || value.isBlank()) {
            value = System.getProperty("bookmate.dev.mode");
        }
        return Boolean.parseBoolean(value);
    }

    public long findOrCreateDevMember() {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);

            try {
                Long memberId = devAuthDAO.selectDevMemberId(connection);
                if (memberId == null) {
                    memberId = devAuthDAO.insertDevMember(connection);
                }
                connection.commit();
                return memberId;
            } catch (SQLException exception) {
                rollback(connection);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("개발용 로그인 준비 중 오류가 발생했습니다.", exception);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 원래 발생한 예외를 유지합니다.
        }
    }
}
