package com.example.community.post.event;

public sealed interface PostChangedEvent {
    long postNum();

    record Updated(long postNum) implements PostChangedEvent {
    }

    record Removed(long postNum) implements PostChangedEvent {
    }
}
