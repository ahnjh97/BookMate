package service;

import dao.tier.TierListDAO;
import dao.tier.TierStatsDAO;
import dao.tier.TierTemplateDAO;

import java.util.List;
import java.util.Map;

public class TierService {
    private final TierTemplateDAO templateDAO = new TierTemplateDAO();
    private final TierListDAO listDAO = new TierListDAO();
    private final TierStatsDAO statsDAO = new TierStatsDAO();

    public List<Map<String, Object>> findTemplates(String keyword, boolean includePending) {
        return templateDAO.findTemplates(keyword, includePending);
    }

    public List<Map<String, Object>> findTemplates(
            String keyword, boolean includePending, Long memberId, Long bookId) {
        return templateDAO.findTemplates(keyword, includePending, memberId, bookId);
    }

    public Map<String, Object> findTemplate(long templateId) {
        return templateDAO.findTemplate(templateId);
    }

    public long requestTemplate(
            long memberId, String title, String description, String category, List<Long> bookIds) {
        return templateDAO.requestTemplate(memberId, title, description, category, bookIds);
    }

    public Map<String, Object> findLatestTierList(long memberId, long templateId) {
        return listDAO.findLatestTierList(memberId, templateId);
    }

    public Map<String, Object> findTierList(long tierListId) {
        return listDAO.findTierList(tierListId);
    }

    public long saveTierList(long memberId, long templateId, String description,
                             boolean publishToCommunity, List<Placement> placements) {
        List<TierListDAO.Placement> daoPlacements = placements == null ? null : placements.stream()
                .map(value -> value == null ? null : new TierListDAO.Placement(value.bookId(), value.grade()))
                .toList();
        return listDAO.saveTierList(
                memberId, templateId, description, publishToCommunity, daoPlacements);
    }

    public void reviewTemplate(
            long templateId, long adminId, boolean approved, String reason) {
        templateDAO.reviewTemplate(templateId, adminId, approved, reason);
    }

    public Map<String, Object> findTemplateStats(long templateId) {
        return statsDAO.findTemplateStats(templateId);
    }

    public record Placement(long bookId, String grade) {}
}
