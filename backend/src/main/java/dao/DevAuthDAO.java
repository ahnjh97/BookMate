package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DevAuthDAO {
    private static final String DEV_LOGIN_ID = "bookmate_dev";

    public Long selectDevMemberId(Connection connection) throws SQLException {
        String sql = "SELECT member_id FROM MEMBER WHERE login_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DEV_LOGIN_ID);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("member_id") : null;
            }
        }
    }

    public long insertDevMember(Connection connection) throws SQLException {
        String sql = """
                INSERT INTO MEMBER (
                    member_id,
                    login_id,
                    password,
                    nickname,
                    email,
                    role
                ) VALUES (
                    SEQ_MEMBER.NEXTVAL,
                    ?,
                    ?,
                    ?,
                    ?,
                    'USER'
                )
                """;
        String[] generatedColumns = {"MEMBER_ID"};

        try (PreparedStatement statement = connection.prepareStatement(sql, generatedColumns)) {
            statement.setString(1, DEV_LOGIN_ID);
            statement.setString(2, "DEV_ONLY_NOT_A_REAL_PASSWORD");
            statement.setString(3, "개발회원");
            statement.setString(4, "bookmate-dev@local.test");

            if (statement.executeUpdate() == 0) {
                throw new SQLException("개발용 회원 생성에 실패했습니다.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }
        }

        throw new SQLException("개발용 회원 번호를 가져오지 못했습니다.");
    }
}
