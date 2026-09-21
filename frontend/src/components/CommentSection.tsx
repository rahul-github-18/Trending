import { useEffect, useState, useRef } from "react";
import {
  Trash2,
  SendHorizontal,
  Loader2,
  Reply,
  X,
  CornerDownRight,
} from "lucide-react";
import { getComments, createComment, deleteComment } from "../api/CommentApi";
import type { Comment } from "../types/Comment";

interface CommentSectionProps {
  threadId: number;
  currentUsername: string;
  onCommentCountChange?: (count: number) => void;
  highlightCommentUsername?: string;
}

interface ReplyingToState {
  parentId: number;
  targetUsername: string;
}

function formatRelativeTime(dateStr: string) {
  try {
    const date = new Date(dateStr);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return "just now";
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) return `${diffInMinutes}m`;
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}h`;
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays}d`;

    return date.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
    });
  } catch {
    return dateStr;
  }
}

export default function CommentSection({
  threadId,
  currentUsername,
  onCommentCountChange,
  highlightCommentUsername,
}: CommentSectionProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [commentText, setCommentText] = useState("");
  const [error, setError] = useState<string | null>(null);

  // Replying state
  const [replyingTo, setReplyingTo] = useState<ReplyingToState | null>(null);
  const [replyText, setReplyText] = useState("");
  const [submittingReply, setSubmittingReply] = useState(false);
  const replyInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let isMounted = true;

    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await getComments(threadId);
        if (isMounted) {
          setComments(data);
          onCommentCountChange?.(data.length);
        }
      } catch (err) {
        console.error("Failed to load comments:", err);
        if (isMounted) setError("Could not load comments.");
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    load();

    return () => {
      isMounted = false;
    };
  }, [threadId]);

  useEffect(() => {
    if (replyingTo && replyInputRef.current) {
      replyInputRef.current.focus();
    }
  }, [replyingTo]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = commentText.trim();
    if (!trimmed || submitting) return;

    setSubmitting(true);
    setError(null);

    try {
      const newComment = await createComment(threadId, trimmed);
      const updated = [...comments, newComment];
      setComments(updated);
      setCommentText("");
      onCommentCountChange?.(updated.length);
    } catch (err: any) {
      console.error("Failed to post comment:", err);
      setError(
        err.response?.data?.message ||
        "Failed to post comment. Please try again."
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleStartReply = (parentId: number, targetUsername: string) => {
    setReplyingTo({ parentId, targetUsername });
    setReplyText(`@${targetUsername} `);
  };

  const handleCancelReply = () => {
    setReplyingTo(null);
    setReplyText("");
  };

  const handleReplySubmit = async (
    e: React.FormEvent,
    parentId: number
  ) => {
    e.preventDefault();
    const trimmed = replyText.trim();
    if (!trimmed || submittingReply) return;

    setSubmittingReply(true);
    setError(null);

    try {
      const newReply = await createComment(threadId, trimmed, parentId);
      const updated = [...comments, newReply];
      setComments(updated);
      setReplyText("");
      setReplyingTo(null);
      onCommentCountChange?.(updated.length);
    } catch (err: any) {
      console.error("Failed to post reply:", err);
      setError(
        err.response?.data?.message || "Failed to post reply. Please try again."
      );
    } finally {
      setSubmittingReply(false);
    }
  };

  const handleDelete = async (commentId: number) => {
    try {
      await deleteComment(commentId);
      // When deleting a parent comment, backend cascades to delete its replies as well
      const updated = comments.filter(
        (c) => c.id !== commentId && c.parentCommentId !== commentId
      );
      setComments(updated);
      if (replyingTo && replyingTo.parentId === commentId) {
        setReplyingTo(null);
        setReplyText("");
      }
      onCommentCountChange?.(updated.length);
    } catch (err) {
      console.error("Failed to delete comment:", err);
      alert("Failed to delete comment.");
    }
  };

  // Group comments: Root comments vs replies
  const rootComments = comments.filter((c) => !c.parentCommentId);
  const knownParentIds = new Set(rootComments.map((c) => c.id));
  const orphanReplies = comments.filter(
    (c) => c.parentCommentId && !knownParentIds.has(c.parentCommentId)
  );
  const displayRoots = [...rootComments, ...orphanReplies];

  const repliesByParentId = comments.reduce<Record<number, Comment[]>>(
    (acc, c) => {
      if (c.parentCommentId && knownParentIds.has(c.parentCommentId)) {
        if (!acc[c.parentCommentId]) {
          acc[c.parentCommentId] = [];
        }
        acc[c.parentCommentId].push(c);
      }
      return acc;
    },
    {}
  );

  return (
    <div className="comments-wrapper">
      {/* Top Thread Comment Composer */}
      <form className="comment-composer" onSubmit={handleSubmit}>
        <div className="comment-avatar-small">
          {currentUsername ? currentUsername.charAt(0).toUpperCase() : "U"}
        </div>
        <input
          type="text"
          className="comment-input"
          placeholder="Reply to this thread..."
          value={commentText}
          onChange={(e) => setCommentText(e.target.value)}
          disabled={submitting}
        />
        <button
          type="submit"
          className="comment-submit-btn"
          disabled={!commentText.trim() || submitting}
          title="Post comment"
        >
          {submitting ? (
            <Loader2 size={16} className="spin-icon" />
          ) : (
            <SendHorizontal size={16} />
          )}
        </button>
      </form>

      {error && <p className="comment-error">{error}</p>}

      {/* Comment List */}
      <div className="comment-list">
        {loading ? (
          <div className="comment-loading">
            <Loader2 size={16} className="spin-icon" />
            <span>Loading replies...</span>
          </div>
        ) : comments.length === 0 ? (
          <p className="comment-empty">No replies yet. Start the conversation!</p>
        ) : (
          displayRoots.map((comment) => {
            const isHighlighted =
              highlightCommentUsername &&
              comment.username?.toLowerCase() ===
              highlightCommentUsername.toLowerCase();
            const replies = repliesByParentId[comment.id] || [];
            const isReplyingThis = replyingTo?.parentId === comment.id;

            return (
              <div key={comment.id} className="comment-thread-group">
                {/* Parent Comment */}
                <div
                  className={`comment-item ${isHighlighted ? "comment-highlighted" : ""
                    }`}
                >
                  <div className="comment-avatar-small">
                    {comment.username
                      ? comment.username.charAt(0).toUpperCase()
                      : "U"}
                  </div>
                  <div className="comment-content-area">
                    <div className="comment-header">
                      <span className="comment-author">{comment.username}</span>
                      {isHighlighted && (
                        <span className="comment-highlight-pill">
                          From notification
                        </span>
                      )}
                      <span className="comment-time">
                        {formatRelativeTime(comment.createdAt)}
                      </span>
                      {comment.username === currentUsername && (
                        <button
                          type="button"
                          className="comment-delete-btn"
                          onClick={() => handleDelete(comment.id)}
                          title="Delete comment"
                        >
                          <Trash2 size={13} />
                        </button>
                      )}
                    </div>
                    <p className="comment-text">{comment.content}</p>

                    <div className="comment-footer-actions">
                      <button
                        type="button"
                        className="comment-reply-action-btn"
                        onClick={() =>
                          handleStartReply(comment.id, comment.username)
                        }
                      >
                        <Reply size={12} />
                        <span>Reply</span>
                      </button>
                      {replies.length > 0 && (
                        <span className="comment-replies-count-tag">
                          {replies.length}{" "}
                          {replies.length === 1 ? "reply" : "replies"}
                        </span>
                      )}
                    </div>
                  </div>
                </div>

                {/* Nested Replies & Inline Composer Container */}
                {(replies.length > 0 || isReplyingThis) && (
                  <div className="comment-replies-container">
                    <div className="comment-thread-line" />
                    <div className="comment-replies-list">
                      {replies.map((reply) => {
                        const isReplyHighlighted =
                          highlightCommentUsername &&
                          reply.username?.toLowerCase() ===
                          highlightCommentUsername.toLowerCase();

                        return (
                          <div
                            key={reply.id}
                            className={`comment-item comment-reply-item ${isReplyHighlighted ? "comment-highlighted" : ""
                              }`}
                          >
                            <div className="comment-avatar-small reply-avatar">
                              {reply.username
                                ? reply.username.charAt(0).toUpperCase()
                                : "U"}
                            </div>
                            <div className="comment-content-area">
                              <div className="comment-header">
                                <span className="comment-author">
                                  {reply.username}
                                </span>
                                {isReplyHighlighted && (
                                  <span className="comment-highlight-pill">
                                    From notification
                                  </span>
                                )}
                                <span className="comment-time">
                                  {formatRelativeTime(reply.createdAt)}
                                </span>
                                {reply.username === currentUsername && (
                                  <button
                                    type="button"
                                    className="comment-delete-btn"
                                    onClick={() => handleDelete(reply.id)}
                                    title="Delete reply"
                                  >
                                    <Trash2 size={13} />
                                  </button>
                                )}
                              </div>
                              <p className="comment-text">{reply.content}</p>

                              <div className="comment-footer-actions">
                                <button
                                  type="button"
                                  className="comment-reply-action-btn"
                                  onClick={() =>
                                    handleStartReply(comment.id, reply.username)
                                  }
                                >
                                  <Reply size={12} />
                                  <span>Reply</span>
                                </button>
                              </div>
                            </div>
                          </div>
                        );
                      })}

                      {/* Inline Reply Composer */}
                      {isReplyingThis && (
                        <div className="comment-reply-composer-wrapper">
                          <div className="comment-replying-badge">
                            <CornerDownRight size={12} />
                            <span>
                              Replying to <strong>@{replyingTo.targetUsername}</strong>
                            </span>
                            <button
                              type="button"
                              className="comment-cancel-reply-btn"
                              onClick={handleCancelReply}
                              title="Cancel reply"
                            >
                              <X size={12} />
                            </button>
                          </div>
                          <form
                            className="comment-composer comment-inline-reply-composer"
                            onSubmit={(e) => handleReplySubmit(e, comment.id)}
                          >
                            <div className="comment-avatar-small reply-avatar">
                              {currentUsername
                                ? currentUsername.charAt(0).toUpperCase()
                                : "U"}
                            </div>
                            <input
                              ref={replyInputRef}
                              type="text"
                              className="comment-input"
                              placeholder={`Reply to @${replyingTo.targetUsername}...`}
                              value={replyText}
                              onChange={(e) => setReplyText(e.target.value)}
                              disabled={submittingReply}
                            />
                            <button
                              type="submit"
                              className="comment-submit-btn"
                              disabled={!replyText.trim() || submittingReply}
                              title="Send reply"
                            >
                              {submittingReply ? (
                                <Loader2 size={14} className="spin-icon" />
                              ) : (
                                <SendHorizontal size={14} />
                              )}
                            </button>
                          </form>
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
