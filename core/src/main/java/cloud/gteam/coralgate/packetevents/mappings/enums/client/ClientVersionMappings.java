package cloud.gteam.coralgate.packetevents.mappings.enums.client;

import org.jetbrains.annotations.NotNull;

public enum ClientVersionMappings {

    V_1_7_2(4),
    V_1_7_10(5),

    V_1_8(47),

    V_1_9(107), V_1_9_1(108), V_1_9_2(109),
    /**
     * 1.9.3 or 1.9.4 as they have the same protocol version.
     */
    V_1_9_3(110),
    V_1_10(210),
    V_1_11(315),
    /**
     * 1.11.1 or 1.11.2 as they have the same protocol version.
     */
    V_1_11_1(316),
    V_1_12(335),
    V_1_12_1(338),
    V_1_12_2(340),

    V_1_13(393),
    V_1_13_1(401),
    V_1_13_2(404),

    V_1_14(477),
    V_1_14_1(480),
    V_1_14_2(485),
    V_1_14_3(490),
    V_1_14_4(498),

    V_1_15(573),
    V_1_15_1(575),
    V_1_15_2(578),

    V_1_16(735),
    V_1_16_1(736),
    V_1_16_2(751),
    V_1_16_3(753),
    /**
     * 1.16.4 or 1.16.5 as they have the same protocol version.
     */
    V_1_16_4(754),

    V_1_17(755),
    V_1_17_1(756),

    /**
     * 1.18 or 1.18.1 as they have the same protocol version.
     */
    V_1_18(757),
    V_1_18_2(758),

    V_1_19(759),
    V_1_19_1(760),
    V_1_19_3(761),
    V_1_19_4(762),
    /**
     * 1.20 and 1.20.1 have the same protocol version.
     */
    V_1_20(763),
    V_1_20_2(764),
    /**
     * 1.20.3 and 1.20.4 have the same protocol version.
     */
    V_1_20_3(765),
    /**
     * 1.20.5 and 1.20.6 have the same protocol version.
     */
    V_1_20_5(766),

    /**
     * 1.21 and 1.21.1 have the same protocol version.
     */
    V_1_21(767),
    /**
     * 1.21.2 and 1.21.3 have the same protocol version.
     */
    V_1_21_2(768),
    V_1_21_4(769),
    V_1_21_5(770),
    V_1_21_6(771),
    V_1_21_7(772),

    UNKNOWN(-1);

    private static final ClientVersionMappings[] VALUES = values();

    private final int protocolVersion;

    ClientVersionMappings(final int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    @NotNull
    public static ClientVersionMappings getById(final int protocolVersion) {

        for (final ClientVersionMappings forVersion : VALUES) {
            if (forVersion.protocolVersion == protocolVersion) return forVersion;
        }

        return UNKNOWN;

    }

}
