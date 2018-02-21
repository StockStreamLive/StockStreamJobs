package application;

public class Config {
    public static final int REFRESH_INTERVAL_MINUTES = Integer.valueOf(System.getenv("REFRESH_INTERVAL_MINUTES"));
    public static final int QUICK_REFRESH_INTERVAL_SECONDS = Integer.valueOf(System.getenv("QUICK_REFRESH_INTERVAL_SECONDS"));

    public static final double MARGIN_ADJUSTMENT = Double.valueOf(System.getenv().getOrDefault("MARGIN_ADJUSTMENT", "0.0"));
}
