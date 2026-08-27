package service;

import dao.HomeDAO;
import dto.HomeSummaryDTO;
import util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class HomeService {
    private final HomeDAO homeDAO = new HomeDAO();

    public HomeSummaryDTO findSummary() {
        try (Connection connection = DBUtil.getConnection()) {
            return homeDAO.selectSummary(connection);
        } catch (SQLException exception) {
            throw new RuntimeException("홈 통계를 조회하는 중 오류가 발생했습니다.", exception);
        }
    }
}
