package com.guessmarket.engine.util;

import java.util.List;
import java.util.ArrayList;

public final class LMSRCalculator {
    private LMSRCalculator() {}

    public static double calculateCost(double b, List<Integer> quantities) {
        if (b <= 0) {
            throw new IllegalArgumentException("b must be > 0.");
        }
        double maxQ = quantities.stream().mapToDouble(q -> q).max().orElse(0.0);
        double sumExp = 0.0;
        for (int q : quantities) {
            sumExp += Math.exp((q - maxQ) / b);
        }
        return b * (Math.log(sumExp) + (maxQ / b));
    }

    public static double calculatePurchaseCost(double b, List<Integer> currentQuantities, int targetIndex, int sharesToBuy) {
        double costOld = calculateCost(b, currentQuantities);
        List<Integer> futureQuantities = new ArrayList<>(currentQuantities);
        futureQuantities.set(targetIndex, futureQuantities.get(targetIndex) + sharesToBuy);
        return calculateCost(b, futureQuantities) - costOld;
    }
}