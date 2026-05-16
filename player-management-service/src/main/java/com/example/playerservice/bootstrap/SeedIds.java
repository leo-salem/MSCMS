package com.example.playerservice.bootstrap;

public final class SeedIds {
    private SeedIds() {}

    public static final String SPORT_MANAGER  = "00000000-0000-0000-0000-000000000010";

    public static final String PLAYER_1_STRIKER = "00000000-0000-0000-0000-000000000101";
    public static final String PLAYER_2_MID     = "00000000-0000-0000-0000-000000000102";
    public static final String PLAYER_3_DEF     = "00000000-0000-0000-0000-000000000103";
    public static final String PLAYER_4_GK      = "00000000-0000-0000-0000-000000000104";
    public static final String PLAYER_5_BBALL   = "00000000-0000-0000-0000-000000000105";
    public static final String PLAYER_6_TENNIS  = "00000000-0000-0000-0000-000000000106";

    // Matches user-management user_profiles insert order (admin=1, then 16 demo users)
    // Player rows are inserted last in UserDataSeeder → players get ids 12..17
    public static final long PLAYER_1_ID = 12L;
    public static final long PLAYER_2_ID = 13L;
    public static final long PLAYER_3_ID = 14L;
    public static final long PLAYER_4_ID = 15L;
    public static final long PLAYER_5_ID = 16L;
    public static final long PLAYER_6_ID = 17L;
}
