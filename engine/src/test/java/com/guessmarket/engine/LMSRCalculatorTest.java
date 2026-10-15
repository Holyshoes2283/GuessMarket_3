package com.guessmarket.engine;

import com.guessmarket.engine.util.LMSRCalculator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class LMSRCalculatorTest {

    @Test
    public void calculateCost_ThrowsException_WhenLiquidityIsZeroOrLess() {
        double invalidLiquidityZero = 0.0;
        double invalidLiquidityNegative = -10.0;
        List<Integer> currentShares = List.of(100, 100);

        
        assertThrows(IllegalArgumentException.class, () -> {
            LMSRCalculator.calculatePurchaseCost(invalidLiquidityZero, currentShares, 0, 10);
        }, "Should throw IllegalArgumentException when liquidity is zero.");

        assertThrows(IllegalArgumentException.class, () -> {
            LMSRCalculator.calculatePurchaseCost(invalidLiquidityNegative, currentShares, 0, 10);
        }, "Should throw IllegalArgumentException when liquidity is negative.");
    }

    @Test
    public void calculatePurchaseCost_MathematicalPrecision_MatchesExpected() {
        double liquidity = 100.0;
        List<Integer> currentShares = List.of(50, 50);
        int targetIndex = 0;
        int sharesToBuy = 10;

        
        double expectedCost = 5.124947951362543;
        double tolerance = 0.0001;

        double actualCost = LMSRCalculator.calculatePurchaseCost(liquidity, currentShares, targetIndex, sharesToBuy);

        assertEquals(expectedCost, actualCost, tolerance,
                "The calculated LMSR cost does not match the expected precision from Appendix A.");
    }

    @Test
    public void calculateCost_ExtremelyHighShareCounts_NoOverflow() {
        double liquidity = 500.0;
        List<Integer> currentShares = List.of(1000000, 1000000);
        int targetIndex = 0;
        int sharesToBuy = 10000;

        assertDoesNotThrow(() -> {
            double cost = LMSRCalculator.calculatePurchaseCost(liquidity, currentShares, targetIndex, sharesToBuy);
            assertTrue(Double.isFinite(cost), "The calculated cost should be a finite number, not Infinity or NaN.");
            assertTrue(cost > 0, "The cost of purchasing shares must be greater than zero.");
        }, "Extremely high share counts caused an unexpected exception (likely floating-point overflow).");
    }
}