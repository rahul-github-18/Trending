package com.thread.Igniter.thread.dto;

import lombok.Data;

import java.util.List;

@Data
public class CursorPageResponse<T> {
    private List<T> content;
    private String nextCursor;
    private boolean hasNext;
}
