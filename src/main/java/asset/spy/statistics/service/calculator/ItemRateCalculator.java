package asset.spy.statistics.service.calculator;

import asset.spy.statistics.service.entity.ProductItemStatusEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class ItemRateCalculator implements StatisticsCalculator<ProductItemStatusEntity, Double> {

    private static final double EMPTY_RATE = 0.0;
    private static final double PERCENT_COEFFICIENT = 100.0;

    @Override
    public Map<String, Double> calculateGrouped(List<ProductItemStatusEntity> data,
                                                Predicate<ProductItemStatusEntity> filter,
                                                Function<ProductItemStatusEntity, String> keyMapper) {
        return calculateRate(data, filter, keyMapper);
    }

    @Override
    public Double calculateOverall(List<ProductItemStatusEntity> data,
                                   Predicate<ProductItemStatusEntity> filter) {
        return calculateOverallRate(data, filter);
    }

    private Map<String, Double> calculateRate(
            List<ProductItemStatusEntity> statuses,
            Predicate<ProductItemStatusEntity> filter,
            Function<ProductItemStatusEntity, String> keyMapper) {
        Map<String, Set<UUID>> total = new HashMap<>();
        Map<String, Set<UUID>> matching = new HashMap<>();

        for (ProductItemStatusEntity status : statuses) {
            if (status.getProductItem() != null) {
                UUID itemId = status.getProductItem().getId();
                String groupKey = keyMapper.apply(status);

                total.computeIfAbsent(groupKey, k -> new HashSet<>()).add(itemId);
                if (filter.test(status)) {
                    matching.computeIfAbsent(groupKey, k -> new HashSet<>()).add(itemId);
                }
            }
        }
        return total.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> calculatePercentage(e.getValue(), matching.getOrDefault(e.getKey(), Set.of()))
                ));
    }

    private double calculateOverallRate(
            List<ProductItemStatusEntity> statuses,
            Predicate<ProductItemStatusEntity> filter) {
        Set<UUID> allItems = new HashSet<>();
        Set<UUID> matchedItems = new HashSet<>();

        for (ProductItemStatusEntity status : statuses) {
            if (status.getProductItem() == null) continue;
            UUID id = status.getProductItem().getId();
            allItems.add(id);
            if (filter.test(status)) {
                matchedItems.add(id);
            }
        }
        return calculatePercentage(allItems, matchedItems);
    }

    private double calculatePercentage(Set<UUID> total, Set<UUID> matched) {
        if (total.isEmpty()) return EMPTY_RATE;
        return (double) matched.size() / total.size() * PERCENT_COEFFICIENT;
    }
}
