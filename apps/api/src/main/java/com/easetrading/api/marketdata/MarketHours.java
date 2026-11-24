package com.easetrading.api.marketdata;

import java.time.*;

/**
 * NSE/BSE regular session: Monday–Friday, 9:15–15:30 IST.
 * Exchange holidays aren't modelled yet — on a holiday we simply poll and get
 * unchanged prices.
 */
public final class MarketHours {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final LocalTime OPEN = LocalTime.of(9, 15);
    private static final LocalTime CLOSE = LocalTime.of(15, 30);

    private MarketHours() { }

    public static boolean isOpen(Instant at) {
        ZonedDateTime t = at.atZone(IST);
        DayOfWeek d = t.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) return false;
        LocalTime time = t.toLocalTime();
        return !time.isBefore(OPEN) && time.isBefore(CLOSE);
    }
}
