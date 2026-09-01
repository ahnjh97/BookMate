package service;

import dao.ideal.IdealRunDAO;
import dao.ideal.IdealStatsDAO;
import dao.ideal.IdealTemplateDAO;

import java.util.List;
import java.util.Map;

public class IdealService {
    private final IdealTemplateDAO templateDAO = new IdealTemplateDAO();
    private final IdealRunDAO runDAO = new IdealRunDAO();
    private final IdealStatsDAO statsDAO = new IdealStatsDAO();

    public List<Map<String, Object>> findTemplates(String keyword, Long memberId) {
        return templateDAO.findTemplates(keyword, memberId);
    }

    public List<Map<String, Object>> findTemplates(String keyword, Long memberId, Long bookId) {
        return templateDAO.findTemplates(keyword, memberId, bookId);
    }

    public List<Map<String, Object>> findTemplates(
            String keyword, Long memberId, Long bookId, boolean includePending) {
        return templateDAO.findTemplates(keyword, memberId, bookId, includePending);
    }

    public Map<String, Object> findTemplate(long id) {
        return templateDAO.findTemplate(id);
    }

    public Map<String, Object> findTemplate(long id, Long memberId) {
        return templateDAO.findTemplate(id, memberId);
    }

    public long createTemplate(
            long memberId, String title, String description, String category, List<Long> ids) {
        return templateDAO.createTemplate(memberId, title, description, category, ids);
    }

    public void reviewTemplate(
            long templateId, long adminId, boolean approved, String reason) {
        templateDAO.reviewTemplate(templateId, adminId, approved, reason);
    }

    public long saveRun(long memberId, long templateId, int size, List<Match> matches) {
        List<IdealRunDAO.Match> daoMatches = matches == null ? null : matches.stream()
                .map(value -> value == null ? null : new IdealRunDAO.Match(
                        value.roundSize(), value.matchOrder(), value.leftBookId(),
                        value.rightBookId(), value.winnerBookId()))
                .toList();
        return runDAO.saveRun(memberId, templateId, size, daoMatches);
    }

    public Map<String, Object> result(long runId) {
        return runDAO.result(runId);
    }

    public Map<String, Object> result(long runId, Long viewerId) {
        return runDAO.result(runId, viewerId);
    }

    public long publishResult(long runId, long memberId, String postContent) {
        return runDAO.publishResult(runId, memberId, postContent);
    }

    public Map<String, Object> stats(long templateId) {
        return statsDAO.stats(templateId);
    }

    public record Match(
            int roundSize, int matchOrder, long leftBookId, long rightBookId, long winnerBookId) {}
}
