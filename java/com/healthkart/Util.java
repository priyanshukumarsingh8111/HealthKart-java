package com.healthkart;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.*;

/** Small helpers that mimic the loose typing the Node server relied on. */
public final class Util {
    private Util() {}

    private static final Random RNG = new Random();

    /** Build a LinkedHashMap from key/value pairs (null values allowed). */
    public static Map<String, Object> m(Object... kv) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }

    /** Non-empty string value or null (like JS truthiness for strings). */
    public static String s(Object o) {
        if (o == null) return null;
        String v = String.valueOf(o);
        return v.isEmpty() ? null : v;
    }

    public static String or(Object o, String fallback) {
        String v = s(o);
        return v == null ? fallback : v;
    }

    public static String lower(Object o) {
        String v = s(o);
        return v == null ? "" : v.toLowerCase();
    }

    public static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0d;
    }

    /** ISO timestamp with millisecond precision, e.g. 2026-10-11T10:15:30.123Z */
    public static String now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    }

    public static String today() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    public static int rand6() {
        return 100000 + RNG.nextInt(900000);
    }

    public static String randBase36(int len) {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(RNG.nextInt(chars.length())));
        return sb.toString();
    }

    public static String notifId() {
        return "notif_" + System.currentTimeMillis() + "_" + randBase36(6);
    }

    public static boolean has(String text, String... needles) {
        for (String n : needles) if (text.contains(n)) return true;
        return false;
    }

    public static Map<String, Object> body(Map<String, Object> raw) {
        return raw == null ? new LinkedHashMap<>() : raw;
    }

    /** Get (creating if absent) a top-level array in the db. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> list(Map<String, Object> db, String key) {
        Object o = db.get(key);
        if (!(o instanceof List)) {
            o = new ArrayList<Map<String, Object>>();
            db.put(key, o);
        }
        return (List<Map<String, Object>>) o;
    }

    /** Get (creating if absent) a top-level object in the db. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Map<String, Object> db, String key) {
        Object o = db.get(key);
        if (!(o instanceof Map)) {
            o = new LinkedHashMap<String, Object>();
            db.put(key, o);
        }
        return (Map<String, Object>) o;
    }

    /** YYYY-MM-DD -> is it a Sunday? Invalid dates return false (same as Node version). */
    public static boolean isSunday(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            return LocalDate.parse(dateStr).getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
        } catch (Exception e) {
            return false;
        }
    }
}
