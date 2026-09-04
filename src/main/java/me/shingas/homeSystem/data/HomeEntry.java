package me.shingas.homeSystem.data;

public record HomeEntry (
    String world,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    String server
) {
    public HomeEntry(String world, double x, double y, double z, float yaw, float pitch) {
        this(world, x, y, z, yaw, pitch, null);
    }
}