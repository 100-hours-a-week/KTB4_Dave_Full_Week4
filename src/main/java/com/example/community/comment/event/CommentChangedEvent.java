package com.example.community.comment.event;

public sealed interface CommentChangedEvent {
    record Created(long postNum, Long parentNum)
            implements CommentChangedEvent {
    }

    record Updated(long commentNum) implements CommentChangedEvent {
    }

    record Deleted(long commentNum, Long parentNum)
            implements CommentChangedEvent {
    }
}
