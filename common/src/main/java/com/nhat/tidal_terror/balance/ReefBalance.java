package com.nhat.tidal_terror.balance;
/** Equipment tuning shared by every Minecraft version and loader. */
public final class ReefBalance {
    public static final int BLEED_INTERVAL=40;
    public static final int SPEAR_DURABILITY=250;
    public static final double SPEAR_DAMAGE_BONUS=5;
    public static final double SPEAR_SPEED_BONUS=-2.9;
    public static int bleedingDuration(int level) { return 2*BLEED_INTERVAL+BLEED_INTERVAL*Math.max(0,Math.min(2,level)); }
    public static float bleedingDamage(int level,boolean fullArmor) { return (1+.5F*Math.max(0,Math.min(3,level)))*(fullArmor?.75F:1F); }
    private ReefBalance() {}
}
