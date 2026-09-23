package com.thread.Igniter.thread.util;

import com.thread.Igniter.thread.dto.Cursor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

public class CursorUtil {

    public static String encode(Cursor cursor) {

        String value = cursor.getCreatedAt() + "_" + cursor.getId();

        return Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursor) {

        String decoded = new String(
                Base64.getDecoder().decode(cursor),
                StandardCharsets.UTF_8
        );

        String[] parts = decoded.split("_");

        LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
        Long id = Long.parseLong(parts[1]);

        return new Cursor(id,createdAt);
    }
}