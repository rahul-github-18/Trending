import { useEffect, useState, useRef, useCallback } from "react";
import { Heart, MessageCircle, Loader2, ArrowDown } from "lucide-react";

import {
  getFeedCursorPage,
  createThread,
  updateThread,
  deleteThread,
  likeThread,
  unlikeThread,
} from "../api/ThreadApi";

import { getCurrentUser } from "../api/UserApi";
import { getComments } from "../api/CommentApi";
import CommentSection from "../components/CommentSection";
import type { Thread } from "../types/Thread";

function formatRelativeTime(dateStr: string) {
  try {
    const date = new Date(dateStr);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return "Just now";
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

const PAGE_SIZE = 10;

function Threads() {
  const [threads, setThreads] = useState<Thread[]>([]);
  const [currentUsername, setCurrentUsername] = useState("");
  const [content, setContent] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [creating, setCreating] = useState(false);

  // Cursor pagination state
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);

  // Thread editing
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editingContent, setEditingContent] = useState("");

  // Comment section toggle per thread
  const [openCommentThreadIds, setOpenCommentThreadIds] = useState<Set<number>>(
    new Set()
  );
  const [commentCounts, setCommentCounts] = useState<Record<number, number>>({});

  // Intersection observer sentinel for infinite scroll
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  const getStoredLikes = (uname: string): number[] => {
    try {
      const key = uname ? `liked_threads_${uname}` : "liked_threads";
      return JSON.parse(localStorage.getItem(key) || "[]");
    } catch {
      return [];
    }
  };

  const saveStoredLikes = (uname: string, ids: number[]) => {
    const key = uname ? `liked_threads_${uname}` : "liked_threads";
    localStorage.setItem(key, JSON.stringify(ids));
  };

  const syncCommentCounts = (content: Thread[]) => {
    if (!content || content.length === 0) return;

    Promise.all(
      content.map((t) =>
        getComments(t.id)
          .then((data) => ({ id: t.id, count: data.length }))
          .catch(() => ({ id: t.id, count: 0 }))
      )
    ).then((results) => {
      setCommentCounts((prev) => {
        const next = { ...prev };
        results.forEach((r) => {
          next[r.id] = r.count;
        });
        return next;
      });
    });
  };

  // Fetch initial feed (page 0 / initial cursor)
  const loadInitialFeed = async (uname: string) => {
    setLoading(true);
    try {
      const pageData = await getFeedCursorPage(null, PAGE_SIZE);
      const storedLiked = getStoredLikes(uname);

      setThreads(
        pageData.content.map((thread) => ({
          ...thread,
          liked: (thread.likeCount > 0) && storedLiked.includes(thread.id),
        }))
      );
      syncCommentCounts(pageData.content);
      setCursor(pageData.nextCursor);
      setHasNext(pageData.hasNext);
    } catch (error) {
      console.error("Failed to load feed threads:", error);
    } finally {
      setLoading(false);
    }
  };

  // Load next cursor page
  const loadNextPage = useCallback(async () => {
    if (loadingMore || !hasNext || !cursor || loading) return;

    setLoadingMore(true);

    try {
      const pageData = await getFeedCursorPage(cursor, PAGE_SIZE);
      const storedLiked = getStoredLikes(currentUsername);

      const newThreads = pageData.content.map((thread) => ({
        ...thread,
        liked: (thread.likeCount > 0) && storedLiked.includes(thread.id),
      }));

      setThreads((prev) => {
        // Prevent duplicates
        const existingIds = new Set(prev.map((t) => t.id));
        const filtered = newThreads.filter((t) => !existingIds.has(t.id));
        return [...prev, ...filtered];
      });

      syncCommentCounts(pageData.content);
      setCursor(pageData.nextCursor);
      setHasNext(pageData.hasNext);
    } catch (error) {
      console.error("Failed to load next cursor page of feed:", error);
    } finally {
      setLoadingMore(false);
    }
  }, [loadingMore, hasNext, cursor, loading, currentUsername]);

  useEffect(() => {
    getCurrentUser()
      .then((user) => {
        setCurrentUsername(user.username);
        loadInitialFeed(user.username);
      })
      .catch((err) => {
        console.error("Failed to load current user:", err);
        loadInitialFeed("");
      });
  }, []);

  // Set up intersection observer for infinite scroll
  useEffect(() => {
    if (!hasNext || loading || loadingMore) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadNextPage();
        }
      },
      { rootMargin: "200px" }
    );

    const target = sentinelRef.current;
    if (target) observer.observe(target);

    return () => {
      if (target) observer.unobserve(target);
    };
  }, [hasNext, loading, loadingMore, loadNextPage]);

  // Like / Unlike thread
  const handleLike = async (thread: Thread) => {
    try {
      const result = thread.liked
        ? await unlikeThread(thread.id)
        : await likeThread(thread.id);

      const storedLiked = getStoredLikes(currentUsername);
      const updatedLiked = result.liked
        ? Array.from(new Set([...storedLiked, thread.id]))
        : storedLiked.filter((id) => id !== thread.id);
      saveStoredLikes(currentUsername, updatedLiked);

      setThreads((previousThreads) =>
        previousThreads.map((item) =>
          item.id === thread.id
            ? {
                ...item,
                liked: result.liked,
                likeCount: result.likeCount,
              }
            : item
        )
      );
    } catch (error) {
      console.error("Failed to like/unlike thread:", error);
    }
  };

  // Create thread
  const handleCreate = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!content.trim() || creating) return;

    setCreating(true);
    try {
      const newThread = await createThread({
        content: content.trim(),
      });

      setThreads((previousThreads) => [
        {
          ...newThread,
          likeCount: newThread.likeCount ?? 0,
          liked: false,
        },
        ...previousThreads,
      ]);
      setCommentCounts((prev) => ({ ...prev, [newThread.id]: 0 }));

      setContent("");
    } catch (error) {
      console.error("Failed to create thread:", error);
    } finally {
      setCreating(false);
    }
  };

  const handleEditStart = (thread: Thread) => {
    setEditingId(thread.id);
    setEditingContent(thread.content);
  };

  const handleEditCancel = () => {
    setEditingId(null);
    setEditingContent("");
  };

  const handleUpdate = async (event: React.FormEvent, id: number) => {
    event.preventDefault();
    if (!editingContent.trim()) return;

    try {
      const updatedThread = await updateThread(id, {
        content: editingContent.trim(),
      });

      setThreads((previousThreads) =>
        previousThreads.map((thread) =>
          thread.id === id
            ? {
                ...updatedThread,
                likeCount: thread.likeCount,
                liked: thread.liked,
              }
            : thread
        )
      );

      handleEditCancel();
    } catch (error) {
      console.error("Failed to update thread:", error);
    }
  };

  const handleDelete = async (id: number) => {
    const confirmed = window.confirm(
      "Are you sure you want to delete this thread?"
    );
    if (!confirmed) return;

    try {
      await deleteThread(id);
      setThreads((previousThreads) =>
        previousThreads.filter((thread) => thread.id !== id)
      );
    } catch (error: any) {
      console.error("Failed to delete thread:", error);
      alert(
        error.response?.data?.message || "Failed to delete thread. Please try again."
      );
    }
  };

  const toggleComments = (threadId: number) => {
    setOpenCommentThreadIds((prev) => {
      const next = new Set(prev);
      if (next.has(threadId)) {
        next.delete(threadId);
      } else {
        next.add(threadId);
      }
      return next;
    });
  };

  const handleCommentCountUpdate = (threadId: number, count: number) => {
    setCommentCounts((prev) => ({
      ...prev,
      [threadId]: count,
    }));
  };

  return (
    <section className="threads-page">
      {/* Create Thread Composer */}
      <form className="create-thread" onSubmit={handleCreate}>
        <div className="create-thread-body">
          <div className="author-avatar user-avatar">
            <span>
              {currentUsername ? currentUsername.charAt(0).toUpperCase() : "U"}
            </span>
          </div>

          <div className="create-thread-input-col">
            <div className="create-thread-author">
              <strong>{currentUsername || "You"}</strong>
            </div>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="What's new? Share a thought..."
              rows={2}
            />
          </div>
        </div>

        <div className="create-thread-footer">
          <span className="create-thread-hint">Anyone can reply</span>
          <button
            type="submit"
            disabled={!content.trim() || creating}
            className="post-submit-btn"
          >
            {creating ? "Posting..." : "Post"}
          </button>
        </div>
      </form>

      {/* Threads Feed List */}
      <div className="thread-list">
        {loading ? (
          <div className="feed-loading-state">
            <Loader2 size={24} className="spin-icon" />
            <p>Loading your feed...</p>
          </div>
        ) : threads.length === 0 ? (
          <div className="feed-empty-state">
            <p>Your feed is empty.</p>
            <span>
              Follow other users from Search or post your first thread above!
            </span>
          </div>
        ) : (
          threads.map((thread) => {
            const isCommentsOpen = openCommentThreadIds.has(thread.id);

            return (
              <article className="thread-card" key={thread.id}>
                <div className="thread-avatar-col">
                  <div className="author-avatar">
                    <span>{thread.username.charAt(0).toUpperCase()}</span>
                  </div>
                </div>

                <div className="thread-body-col">
                  {/* Thread Header */}
                  <div className="thread-header">
                    <div className="thread-meta">
                      <strong>{thread.username}</strong>
                      <span className="thread-time">
                        {formatRelativeTime(thread.createdAt)}
                      </span>
                    </div>

                    {thread.username === currentUsername && (
                      <div className="thread-owner-actions">
                        <button
                          type="button"
                          className="text-action-btn"
                          onClick={() => handleEditStart(thread)}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="text-action-btn delete-btn"
                          onClick={() => handleDelete(thread.id)}
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </div>

                  {/* Editing Form */}
                  {editingId === thread.id ? (
                    <form
                      className="edit-thread-form"
                      onSubmit={(event) => handleUpdate(event, thread.id)}
                    >
                      <textarea
                        value={editingContent}
                        onChange={(event) =>
                          setEditingContent(event.target.value)
                        }
                      />

                      <div className="thread-actions">
                        <button type="submit" className="save-btn">
                          Save
                        </button>
                        <button
                          type="button"
                          className="cancel-btn"
                          onClick={handleEditCancel}
                        >
                          Cancel
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      {/* Thread Content */}
                      <p className="thread-content">{thread.content}</p>

                      {/* Thread Actions Row */}
                      <div className="thread-actions">
                        {/* Like Button */}
                        <button
                          className={`thread-action-btn like-btn ${
                            thread.liked ? "liked" : ""
                          }`}
                          type="button"
                          onClick={() => handleLike(thread)}
                          title="Like"
                        >
                          <Heart
                            size={18}
                            fill={thread.liked ? "#ef4444" : "none"}
                            color={thread.liked ? "#ef4444" : "currentColor"}
                          />
                          <span className="action-count">
                            {thread.likeCount ?? 0}
                          </span>
                        </button>

                        {/* Comment Button */}
                        <button
                          className={`thread-action-btn comment-toggle-btn ${
                            isCommentsOpen ? "active" : ""
                          }`}
                          type="button"
                          title="Reply"
                          onClick={() => toggleComments(thread.id)}
                        >
                          <MessageCircle size={18} />
                          <span className="action-count">
                            {commentCounts[thread.id] ?? 0}
                          </span>
                        </button>
                      </div>

                      {/* Inline Expandable Comment Section */}
                      {isCommentsOpen && (
                        <CommentSection
                          threadId={thread.id}
                          currentUsername={currentUsername}
                          onCommentCountChange={(c) =>
                            handleCommentCountUpdate(thread.id, c)
                          }
                        />
                      )}
                    </>
                  )}
                </div>
              </article>
            );
          })
        )}
      </div>

      {/* Infinite Scroll Sentinel & Load More UI */}
      {!loading && threads.length > 0 && (
        <div className="feed-pagination-section">
          {hasNext ? (
            <div className="pagination-action-box">
              <div ref={sentinelRef} className="scroll-sentinel" />
              <button
                type="button"
                className="load-more-btn"
                onClick={loadNextPage}
                disabled={loadingMore}
              >
                {loadingMore ? (
                  <>
                    <Loader2 size={16} className="spin-icon" />
                    <span>Loading more threads...</span>
                  </>
                ) : (
                  <>
                    <ArrowDown size={16} />
                    <span>Load more threads</span>
                  </>
                )}
              </button>
            </div>
          ) : (
            <div className="all-caught-up">
              <span>You&apos;re all caught up!</span>
            </div>
          )}
        </div>
      )}
    </section>
  );
}

export default Threads;