package ru.nsu.marketplace.review.reply.reaction;

public interface ReplyReactionCounts {
    Long getReplyId();
    long getLikes();
    long getDislikes();
}
