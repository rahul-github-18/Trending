import { useState, useEffect, useRef, useTransition } from "react";
import {
  Search as SearchIcon,
  X,
  UserPlus,
  UserCheck,
  Heart,
  MessageCircle,
  Users,
  Sparkles,
  Loader2,
} from "lucide-react";
import { getThreads, likeThread, unlikeThread } from "../api/ThreadApi";
import {
  searchUsers,
  getCurrentUser,
  followUser,
  unfollowUser,
  getFollowingStatus,
} from "../api/UserApi";
import { getComments } from "../api/CommentApi";
import CommentSection from "../components/CommentSection";
import type { User } from "../types/User";
import type { Thread } from "../types/Thread";

type SearchTab = "users" | "threads";

export default function Search() {
  const [activeTab, setActiveTab] = useState<SearchTab>("users");
  const [query, setQuery] = useState("");
  const [, startTransition] = useTransition();

  // Current logged-in user
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [currentUsername, setCurrentUsername] = useState("");

  // User search results
  const [users, setUsers] = useState<User[]>([]);
  const [userFollowStates, setUserFollowStates] = useState<Record<number, boolean>>({});
  const [loadingUsers, setLoadingUsers] = useState(false);

  // Threads search results
  const [allThreads, setAllThreads] = useState<Thread[]>([]);
  const [matchedThreads, setMatchedThreads] = useState<Thread[]>([]);
  const [loadingThreads, setLoadingThreads] = useState(false);
  const [openCommentThreadIds, setOpenCommentThreadIds] = useState<Set<number>>(
    new Set()
  );
  const [commentCounts, setCommentCounts] = useState<Record<number, number>>({});

  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Initial load: current user info only (no recommendations!)
  useEffect(() => {
    getCurrentUser()
      .then((me) => {
        setCurrentUserId(me.id);
        setCurrentUsername(me.username);
      })
      .catch((err) => console.error("Could not fetch current user:", err));
  }, []);

  // Pre-fetch threads for instant client-side thread search
  useEffect(() => {
    if (activeTab === "threads" && allThreads.length === 0) {
      setLoadingThreads(true);
      getThreads()
        .then((data) => {
          const storedLiked: number[] = JSON.parse(
            localStorage.getItem("liked_threads") || "[]"
          );
          setAllThreads(
            data.map((t) => ({
              ...t,
              liked: storedLiked.includes(t.id),
            }))
          );
        })
        .finally(() => setLoadingThreads(false));
    }
  }, [activeTab, allThreads.length]);

  // Real-time character-by-character user search with debounce
  useEffect(() => {
    if (activeTab !== "users") return;

    const trimmed = query.trim();

    if (!trimmed) {
      setUsers([]);
      setLoadingUsers(false);
      return;
    }

    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    setLoadingUsers(true);

    debounceTimerRef.current = setTimeout(async () => {
      try {
        const results = await searchUsers(trimmed);
        startTransition(() => {
          setUsers(results);
        });

        // Check follow status for each found user
        const statusMap: Record<number, boolean> = {};
        await Promise.all(
          results.map(async (u) => {
            if (u.id === currentUserId) return;
            try {
              const status = await getFollowingStatus(u.id);
              statusMap[u.id] = status;
            } catch {
              statusMap[u.id] = false;
            }
          })
        );
        setUserFollowStates((prev) => ({ ...prev, ...statusMap }));
      } catch (err) {
        console.error("Search users error:", err);
      } finally {
        setLoadingUsers(false);
      }
    }, 220);

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [query, activeTab, currentUserId]);

  // Thread search filtering
  useEffect(() => {
    if (activeTab !== "threads") return;
    const q = query.trim().toLowerCase();
    if (!q) {
      setMatchedThreads([]);
      return;
    }
      const filtered = allThreads.filter(
        (t) =>
          t.content.toLowerCase().includes(q) ||
          t.username.toLowerCase().includes(q)
      );
      setMatchedThreads(filtered);

      if (filtered.length > 0) {
        Promise.all(
          filtered.map((t) =>
            getComments(t.id)
              .then((res) => ({ id: t.id, count: res.length }))
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
      }
  }, [query, activeTab, allThreads]);

  // Toggle follow
  const handleToggleFollow = async (targetUserId: number) => {
    const isFollowing = !!userFollowStates[targetUserId];

    setUserFollowStates((prev) => ({
      ...prev,
      [targetUserId]: !isFollowing,
    }));

    try {
      if (isFollowing) {
        await unfollowUser(targetUserId);
      } else {
        await followUser(targetUserId);
      }
    } catch (err) {
      console.error("Failed to update follow status:", err);
      setUserFollowStates((prev) => ({
        ...prev,
        [targetUserId]: isFollowing,
      }));
    }
  };

  // Like thread in search
  const handleLikeThread = async (thread: Thread) => {
    try {
      const result = thread.liked
        ? await unlikeThread(thread.id)
        : await likeThread(thread.id);

      const storedLiked: number[] = JSON.parse(
        localStorage.getItem("liked_threads") || "[]"
      );
      const updatedLiked = result.liked
        ? Array.from(new Set([...storedLiked, thread.id]))
        : storedLiked.filter((id) => id !== thread.id);
      localStorage.setItem("liked_threads", JSON.stringify(updatedLiked));

      setMatchedThreads((prev) =>
        prev.map((item) =>
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

  const toggleComments = (threadId: number) => {
    setOpenCommentThreadIds((prev) => {
      const next = new Set(prev);
      if (next.has(threadId)) next.delete(threadId);
      else next.add(threadId);
      return next;
    });
  };

  return (
    <section className="search-page">
      {/* Search Header */}
      <div className="search-header">
        <h1>Search</h1>
        <p className="search-subtitle">
          Type to find people or discover relevant posts
        </p>
      </div>

      {/* Tabs Switcher */}
      <div className="search-tabs">
        <button
          type="button"
          className={`search-tab ${activeTab === "users" ? "active" : ""}`}
          onClick={() => setActiveTab("users")}
        >
          <Users size={16} />
          <span>People</span>
        </button>
        <button
          type="button"
          className={`search-tab ${activeTab === "threads" ? "active" : ""}`}
          onClick={() => setActiveTab("threads")}
        >
          <Sparkles size={16} />
          <span>Posts</span>
        </button>
      </div>

      {/* Search Input */}
      <div className="search-bar-wrapper">
        <SearchIcon className="search-icon" size={20} />
        <input
          type="text"
          className="search-input"
          placeholder={
            activeTab === "users"
              ? "Type a letter or username to search..."
              : "Search posts by keyword..."
          }
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          autoFocus
        />
        {query && (
          <button
            type="button"
            className="clear-search-btn"
            onClick={() => setQuery("")}
            title="Clear"
          >
            <X size={16} />
          </button>
        )}
      </div>

      {/* People Search Results */}
      {activeTab === "users" && (
        <div className="search-results">
          {loadingUsers ? (
            <div className="search-loading">
              <Loader2 size={20} className="spin-icon" />
              <span>Finding people matching &ldquo;{query}&rdquo;...</span>
            </div>
          ) : !query.trim() ? (
            <div className="search-empty">
              <SearchIcon size={32} className="empty-search-icon" />
              <p>Find people</p>
              <span>Type a character or username above to start searching</span>
            </div>
          ) : users.length === 0 ? (
            <div className="search-empty">
              <p>No users found matching &ldquo;{query}&rdquo;</p>
              <span>Try typing a different character or name</span>
            </div>
          ) : (
            <div>
              <div className="search-section-label">
                Matching Users ({users.length})
              </div>

              <div className="user-results-list">
                {users.map((user) => {
                  const isMe = user.id === currentUserId;
                  const isFollowing = !!userFollowStates[user.id];

                  return (
                    <div key={user.id} className="person-card">
                      <div className="person-info">
                        <div className="person-avatar">
                          {user.profilePicture ? (
                            <img src={user.profilePicture} alt={user.username} />
                          ) : (
                            <span>{user.username.charAt(0).toUpperCase()}</span>
                          )}
                        </div>
                        <div className="person-details">
                          <div className="person-name-row">
                            <strong>{user.username}</strong>
                            {isMe && <span className="you-pill">You</span>}
                          </div>
                          {user.bio ? (
                            <p className="person-bio">{user.bio}</p>
                          ) : (
                            <p className="person-email">{user.email}</p>
                          )}
                        </div>
                      </div>

                      {!isMe && (
                        <button
                          type="button"
                          className={`follow-toggle-btn ${
                            isFollowing ? "following" : ""
                          }`}
                          onClick={() => handleToggleFollow(user.id)}
                        >
                          {isFollowing ? (
                            <>
                              <UserCheck size={14} />
                              <span>Following</span>
                            </>
                          ) : (
                            <>
                              <UserPlus size={14} />
                              <span>Follow</span>
                            </>
                          )}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Threads Search Results */}
      {activeTab === "threads" && (
        <div className="search-results">
          {loadingThreads ? (
            <div className="search-loading">
              <Loader2 size={20} className="spin-icon" />
              <span>Loading posts...</span>
            </div>
          ) : !query.trim() ? (
            <div className="search-empty">
              <SearchIcon size={32} className="empty-search-icon" />
              <p>Search posts</p>
              <span>Type keywords above to find matching conversations</span>
            </div>
          ) : matchedThreads.length === 0 ? (
            <div className="search-empty">
              <p>No posts found matching &ldquo;{query}&rdquo;</p>
              <span>Try searching for something else</span>
            </div>
          ) : (
            <div className="thread-list">
              <div className="search-section-label">
                Found {matchedThreads.length} matching posts
              </div>
              {matchedThreads.map((thread) => {
                const isCommentsOpen = openCommentThreadIds.has(thread.id);
                return (
                  <article className="thread-card" key={thread.id}>
                    <div className="thread-avatar-col">
                      <div className="author-avatar">
                        <span>{thread.username.charAt(0).toUpperCase()}</span>
                      </div>
                    </div>

                    <div className="thread-body-col">
                      <div className="thread-header">
                        <div className="thread-meta">
                          <strong>{thread.username}</strong>
                          <span className="thread-time">
                            {new Date(thread.createdAt).toLocaleDateString()}
                          </span>
                        </div>
                      </div>

                      <p className="thread-content">{thread.content}</p>

                      <div className="thread-actions">
                        <button
                          className={`thread-action-btn like-btn ${
                            thread.liked ? "liked" : ""
                          }`}
                          type="button"
                          onClick={() => handleLikeThread(thread)}
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

                        <button
                          className={`thread-action-btn comment-toggle-btn ${
                            isCommentsOpen ? "active" : ""
                          }`}
                          type="button"
                          onClick={() => toggleComments(thread.id)}
                        >
                          <MessageCircle size={18} />
                          <span className="action-count">
                            {commentCounts[thread.id] ?? 0}
                          </span>
                        </button>
                      </div>

                      {isCommentsOpen && (
                        <CommentSection
                          threadId={thread.id}
                          currentUsername={currentUsername}
                          onCommentCountChange={(c) =>
                            setCommentCounts((prev) => ({
                              ...prev,
                              [thread.id]: c,
                            }))
                          }
                        />
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
