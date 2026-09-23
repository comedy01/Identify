package dev.identify.info;

public final class HorseStats {
    private static final double BLOCKS_PER_SECOND_PER_SPEED = 43.17D;

    private HorseStats() {
    }

    public static double blocksPerSecond(double movementSpeed) {
        return movementSpeed * BLOCKS_PER_SECOND_PER_SPEED;
    }

    public static double jumpHeight(double jumpStrength) {
        double s = jumpStrength;
        return -0.1817584952D * s * s * s + 3.689713992D * s * s + 2.128599134D * s - 0.343930367D;
    }
}
