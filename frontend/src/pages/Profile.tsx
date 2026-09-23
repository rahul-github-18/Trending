import { useEffect, useState, useRef, useCallback } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import {
  X,
  UserCheck,
  UserPlus,
  Heart,
  MessageCircle,
  Camera,
  Loader2,
  Trash2,
  Edit3,
  ArrowLeft,
  ArrowDown,
} from "lucide-react";

import {
  getCurrentUser,
  updateCurrentUser,
  uploadProfilePicture,
  getFollowersCount,
  getFollowingCount,
  getFollowers,
  getFollowing,
  followUser,
  unfollowUser,
  searchUsers,
  getFollowingStatus,
} from "../api/UserApi";

import {
  getThreadsPage,
  getAllThreadsList,
  updateThread,
  deleteThread,
  likeThread,
  unlikeThread,
} from "../api/ThreadApi";

import { getComments } from "../api/CommentApi";
import CommentSection from "../components/CommentSection";
import type { User } from "../types/User";
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

type ProfileTab = "threads" | "liked";
const ITEMS_PER_PAGE = 10;

export default function Profile() {
  const [user, setUser] = useState<User | null>(null);
  const [isEditing, setIsEditing] = useState(false);

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [bio, setBio] = useState("");
  const [savingProfile, setSavingProfile] = useState(false);

  // Photo upload states
  const [previewPhotoUrl, setPreviewPhotoUrl] = useState<string | null>(null);
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const [photoError, setPhotoError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);

  // Tabs: Threads vs Liked
  const [activeTab, setActiveTab] = useState<ProfileTab>("threads");

  // Threads lists
  const [myThreads, setMyThreads] = useState<Thread[]>([]);
  const [likedThreads, setLikedThreads] = useState<Thread[]>([]);
  const [threadsLoading, setThreadsLoading] = useState(false);

  // Offset pagination state for threads (GET /api/threads)
  const [threadsPage, setThreadsPage] = useState(0);
  const [hasMoreThreads, setHasMoreThreads] = useState(false);
  const [totalThreads, setTotalThreads] = useState(0);
  const [loadingMore, setLoadingMore] = useState(false);
  const sentinelRef = useRef<HTMLDivElement | null>(null);
  const threadsSectionRef = useRef<HTMLDivElement | null>(null);

  // Modal for Followers / Following list
  const [modalType, setModalType] = useState<"followers" | "following" | null>(null);
  const [modalUsers, setModalUsers] = useState<User[]>([]);
  const [followingSet, setFollowingSet] = useState<Set<number>>(new Set());
  const [modalLoading, setModalLoading] = useState(false);

  // Thread editing
  const [editingThreadId, setEditingThreadId] = useState<number | null>(null);
  const [editingThreadContent, setEditingThreadContent] = useState("");

  // Comment section toggle per thread
  const [openCommentThreadIds, setOpenCommentThreadIds] = useState<Set<number>>(
    new Set()
  );
  const [commentCounts, setCommentCounts] = useState<Record<number, number>>({});

  const syncCommentCounts = (threadsList: Thread[]) => {
    if (!threadsList || threadsList.length === 0) return;
    Promise.all(
      threadsList.map((t) =>
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
  };

  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const viewUsername = searchParams.get("username");
  const [isTargetUser, setIsTargetUser] = useState(false);
  const [isFollowingTarget, setIsFollowingTarget] = useState(false);

  // Fetch initial page (page 0) of threads for this user from GET /api/threads
  const loadInitialThreads = async (uname: string) => {
    setThreadsLoading(true);
    try {
      const pageData = await getThreadsPage(0, ITEMS_PER_PAGE, uname);
      const targetUname = uname.trim().toLowerCase();

      const userOnly = pageData.content.filter(
        (t) => t.username?.trim().toLowerCase() === targetUname
      );

      const storageKey = `liked_threads_${uname}`;
      const storedLiked: number[] = JSON.parse(
        localStorage.getItem(storageKey) || "[]"
      );

      const mapped = userOnly.map((t) => ({
        ...t,
        liked: storedLiked.includes(t.id),
      }));

      setMyThreads(mapped);
      setThreadsPage(0);
      setHasMoreThreads(!pageData.last);
      setTotalThreads(pageData.totalElements || mapped.length);
      syncCommentCounts(mapped);
    } catch (err) {
      console.error("Failed to load initial threads:", err);
    } finally {
      setThreadsLoading(false);
    }
  };

  // Load next page of threads for this user (infinite scroll + load more)
  const loadNextThreadsPage = useCallback(async () => {
    if (loadingMore || !hasMoreThreads || threadsLoading) return;

    setLoadingMore(true);
    try {
      const currentUname = (isTargetUser && viewUsername ? viewUsername : (username || user?.username || "")).trim();
      const targetUname = currentUname.toLowerCase();
      const nextPage = threadsPage + 1;

      const pageData = await getThreadsPage(nextPage, ITEMS_PER_PAGE, currentUname);

      const userOnly = pageData.content.filter(
        (t) => t.username?.trim().toLowerCase() === targetUname
      );

      const storageKey = `liked_threads_${currentUname}`;
      const storedLiked: number[] = JSON.parse(
        localStorage.getItem(storageKey) || "[]"
      );

      const newThreads = userOnly.map((t) => ({
        ...t,
        liked: storedLiked.includes(t.id),
      }));

      setMyThreads((prev) => {
        const existingIds = new Set(prev.map((t) => t.id));
        const filtered = newThreads.filter((t) => !existingIds.has(t.id));
        return [...prev, ...filtered];
      });

      setThreadsPage(nextPage);
      setHasMoreThreads(!pageData.last);
      if (pageData.totalElements) {
        setTotalThreads(pageData.totalElements);
      }
      syncCommentCounts(newThreads);
    } catch (err) {
      console.error("Failed to load next page of threads:", err);
    } finally {
      setLoadingMore(false);
    }
  }, [loadingMore, hasMoreThreads, threadsLoading, threadsPage, isTargetUser, viewUsername, username, user]);

  // Set up intersection observer for infinite scroll
  useEffect(() => {
    if (activeTab !== "threads" || !hasMoreThreads || threadsLoading || loadingMore) return;

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          loadNextThreadsPage();
        }
      },
      { rootMargin: "200px" }
    );

    const target = sentinelRef.current;
    if (target) {
      observer.observe(target);
    }

    return () => {
      if (target) {
        observer.unobserve(target);
      }
    };
  }, [activeTab, hasMoreThreads, threadsLoading, loadingMore, loadNextThreadsPage]);

  const loadProfile = async () => {
    try {
      const currentUser = await getCurrentUser();

      // Check if viewing another user's profile
      if (
        viewUsername &&
        viewUsername.trim().toLowerCase() !== currentUser.username.toLowerCase()
      ) {
        setIsTargetUser(true);

        const searchMatches = await searchUsers(viewUsername.trim());
        const target = searchMatches.find(
          (u) => u.username.toLowerCase() === viewUsername.trim().toLowerCase()
        );

        if (target) {
          setUser(target);
          setUsername(target.username);
          setEmail(target.email);
          setBio(target.bio || "");

          const [followers, following, isFollowing] =
            await Promise.all([
              getFollowersCount(target.id).catch(() => 0),
              getFollowingCount(target.id).catch(() => 0),
              getFollowingStatus(target.id).catch(() => false),
            ]);

          setFollowersCount(followers);
          setFollowingCount(following);
          setIsFollowingTarget(isFollowing);
          setLikedThreads([]);

          await loadInitialThreads(target.username);
          return;
        }
      }

      // Default: load logged in user's profile
      setIsTargetUser(false);
      const data = currentUser;

      const [followers, following, myFollowing, allSystemThreads] =
        await Promise.all([
          getFollowersCount(data.id).catch(() => 0),
          getFollowingCount(data.id).catch(() => 0),
          getFollowing(data.id).catch(() => []),
          getAllThreadsList().catch(() => []),
        ]);

      setUser(data);
      setUsername(data.username);
      setEmail(data.email);
      setBio(data.bio || "");

      setFollowersCount(followers);
      setFollowingCount(following);
      setFollowingSet(new Set(myFollowing.map((u) => u.id)));

      const storageKey = `liked_threads_${data.username}`;
      const storedLiked: number[] = JSON.parse(
        localStorage.getItem(storageKey) || "[]"
      );

      // Clean up any stale IDs from storedLiked where likeCount is 0
      const activeLikedIds = new Set(
        allSystemThreads
          .filter((t) => storedLiked.includes(t.id) && t.likeCount > 0)
          .map((t) => t.id)
      );
      localStorage.setItem(storageKey, JSON.stringify(Array.from(activeLikedIds)));

      // Map liked threads
      const mappedLiked = allSystemThreads
        .filter((t) => activeLikedIds.has(t.id))
        .map((t) => ({
          ...t,
          liked: true,
        }));
      setLikedThreads(mappedLiked);
      syncCommentCounts(mappedLiked);

      // Load initial threads using GET /api/threads offset pagination
      await loadInitialThreads(data.username);
    } catch (error) {
      console.error("Failed to load profile:", error);
      setThreadsLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, [viewUsername]);

  const handleToggleFollowTarget = async () => {
    if (!user) return;
    try {
      if (isFollowingTarget) {
        await unfollowUser(user.id);
        setIsFollowingTarget(false);
        setFollowersCount((prev) => Math.max(0, prev - 1));
      } else {
        await followUser(user.id);
        setIsFollowingTarget(true);
        setFollowersCount((prev) => prev + 1);
      }
    } catch (err) {
      console.error("Failed to toggle follow:", err);
    }
  };

  // Like / Unlike thread
  const handleToggleLike = async (thread: Thread) => {
    if (!user) return;
    try {
      const result = thread.liked
        ? await unlikeThread(thread.id)
        : await likeThread(thread.id);

      const storageKey = `liked_threads_${user.username}`;
      const storedLiked: number[] = JSON.parse(
        localStorage.getItem(storageKey) || "[]"
      );

      const updatedLiked = result.liked
        ? Array.from(new Set([...storedLiked, thread.id]))
        : storedLiked.filter((id) => id !== thread.id);
      localStorage.setItem(storageKey, JSON.stringify(updatedLiked));

      // Update myThreads
      setMyThreads((prev) =>
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

      // Update likedThreads
      setLikedThreads((prev) => {
        if (result.liked) {
          const exists = prev.some((item) => item.id === thread.id);
          if (exists) {
            return prev.map((item) =>
              item.id === thread.id
                ? { ...item, liked: true, likeCount: result.likeCount }
                : item
            );
          }
          return [
            {
              ...thread,
              liked: true,
              likeCount: result.likeCount,
            },
            ...prev,
          ];
        } else {
          return prev.filter((item) => item.id !== thread.id);
        }
      });
    } catch (error) {
      console.error("Failed to like/unlike thread:", error);
    }
  };

  // Delete user's thread
  const handleDeleteThread = async (id: number) => {
    const confirmed = window.confirm(
      "Are you sure you want to delete this thread?"
    );
    if (!confirmed) return;

    try {
      await deleteThread(id);
      setMyThreads((prev) => prev.filter((t) => t.id !== id));
      setLikedThreads((prev) => prev.filter((t) => t.id !== id));
      setTotalThreads((prev) => Math.max(0, prev - 1));
    } catch (error: any) {
      console.error("Failed to delete thread:", error);
      alert(error.response?.data?.message || "Failed to delete thread.");
    }
  };

  const handleEditThreadStart = (thread: Thread) => {
    setEditingThreadId(thread.id);
    setEditingThreadContent(thread.content);
  };

  const handleEditThreadCancel = () => {
    setEditingThreadId(null);
    setEditingThreadContent("");
  };

  const handleUpdateThread = async (e: React.FormEvent, id: number) => {
    e.preventDefault();
    if (!editingThreadContent.trim()) return;

    try {
      const updated = await updateThread(id, {
        content: editingThreadContent.trim(),
      });

      setMyThreads((prev) =>
        prev.map((t) =>
          t.id === id
            ? {
              ...updated,
              likeCount: t.likeCount,
              liked: t.liked,
            }
            : t
        )
      );
      handleEditThreadCancel();
    } catch (error) {
      console.error("Failed to update thread:", error);
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

  const openFollowModal = async (type: "followers" | "following") => {
    if (!user) return;
    setModalType(type);
    setModalLoading(true);

    try {
      const list =
        type === "followers"
          ? await getFollowers(user.id)
          : await getFollowing(user.id);
      setModalUsers(list);

      const myFollowing = await getFollowing(user.id).catch(() => []);
      setFollowingSet(new Set(myFollowing.map((u) => u.id)));
    } catch (error) {
      console.error(`Failed to load ${type}:`, error);
    } finally {
      setModalLoading(false);
    }
  };

  const handleToggleFollowInModal = async (targetUserId: number) => {
    if (!user) return;
    const isCurrentlyFollowing = followingSet.has(targetUserId);

    try {
      if (isCurrentlyFollowing) {
        await unfollowUser(targetUserId);
        setFollowingSet((prev) => {
          const next = new Set(prev);
          next.delete(targetUserId);
          return next;
        });
      } else {
        await followUser(targetUserId);
        setFollowingSet((prev) => new Set(prev).add(targetUserId));
      }

      const [newFollowers, newFollowing] = await Promise.all([
        getFollowersCount(user.id),
        getFollowingCount(user.id),
      ]);
      setFollowersCount(newFollowers);
      setFollowingCount(newFollowing);
    } catch (error: any) {
      console.error("Failed to toggle follow:", error);
      alert(error.response?.data?.message || "Failed to update follow status.");
    }
  };

  const handleUpdate = async (event: React.FormEvent) => {
    event.preventDefault();
    setSavingProfile(true);

    try {
      const updatedUser = await updateCurrentUser({
        username,
        email,
        bio,
      });

      setUser(updatedUser);
      setIsEditing(false);
    } catch (error) {
      console.error("Failed to update profile:", error);
    } finally {
      setSavingProfile(false);
    }
  };

  const handleProfilePictureChange = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setPhotoError("Please select an image file (PNG, JPG, WebP, etc.).");
      return;
    }

    // Immediately display preview in UI
    const objectUrl = URL.createObjectURL(file);
    setPreviewPhotoUrl(objectUrl);
    setPhotoError(null);
    setUploadingPhoto(true);

    try {
      const updatedUser = await uploadProfilePicture(file);
      setUser(updatedUser);
      setPreviewPhotoUrl(null);
      URL.revokeObjectURL(objectUrl);
    } catch (error: any) {
      console.error("Failed to upload profile picture:", error);
      setPhotoError(
        error.response?.data?.message ||
        error.message ||
        "Failed to upload photo. Please check your connection and try again."
      );
    } finally {
      setUploadingPhoto(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

  const handleCancel = () => {
    if (!user) return;
    setUsername(user.username);
    setEmail(user.email);
    setBio(user.bio || "");
    setPreviewPhotoUrl(null);
    setPhotoError(null);
    setIsEditing(false);
  };

  const currentThreads = activeTab === "threads" ? myThreads : likedThreads;

  if (!user) {
    return (
      <section className="profile">
        <div className="profile-loading-state">
          <Loader2 size={24} className="spin-icon" />
          <p>Loading profile...</p>
        </div>
      </section>
    );
  }

  return (
    <section className="profile">
      {/* Profile Header Card */}
      <div className="profile-header-card">
        {isTargetUser && (
          <div className="profile-back-row">
            <button
              type="button"
              className="profile-back-btn"
              onClick={() => navigate(-1)}
            >
              <ArrowLeft size={16} />
              <span>Back</span>
            </button>
          </div>
        )}

        <div className="profile-header-top">
          <div className="profile-text-info">
            <h1 className="profile-username">{user.username}</h1>
            {user.bio ? (
              <p className="profile-bio">{user.bio}</p>
            ) : (
              <p className="profile-bio-empty">No bio added yet.</p>
            )}

            <div className="profile-stats">
              <button
                type="button"
                className="stat-btn"
                onClick={() => openFollowModal("followers")}
              >
                <strong>{followersCount}</strong> Followers
              </button>

              <button
                type="button"
                className="stat-btn"
                onClick={() => openFollowModal("following")}
              >
                <strong>{followingCount}</strong> Following
              </button>
            </div>
          </div>

          <div className="profile-avatar-wrapper">
            <div className="profile-avatar-large">
              {previewPhotoUrl || user.profilePicture ? (
                <img
                  src={previewPhotoUrl || user.profilePicture!}
                  alt={user.username}
                />
              ) : (
                <span>{user.username.charAt(0).toUpperCase()}</span>
              )}
            </div>
          </div>
        </div>

        {/* Edit profile button or follow toggle */}
        {isTargetUser ? (
          <div className="profile-action-row">
            <button
              type="button"
              className={`follow-toggle-btn ${isFollowingTarget ? "following" : ""
                }`}
              onClick={handleToggleFollowTarget}
            >
              {isFollowingTarget ? (
                <>
                  <UserCheck size={15} />
                  <span>Following</span>
                </>
              ) : (
                <>
                  <UserPlus size={15} />
                  <span>Follow</span>
                </>
              )}
            </button>
            <span className="profile-email-badge">{user.email}</span>
          </div>
        ) : !isEditing ? (
          <div className="profile-action-row">
            <button
              type="button"
              className="edit-profile-btn"
              onClick={() => setIsEditing(true)}
            >
              <Edit3 size={15} />
              <span>Edit profile</span>
            </button>
            <span className="profile-email-badge">{user.email}</span>
          </div>
        ) : (
          <form className="edit-profile-form" onSubmit={handleUpdate}>
            <div className="edit-photo-row">
              <div className="profile-avatar-preview">
                {previewPhotoUrl || user.profilePicture ? (
                  <img
                    src={previewPhotoUrl || user.profilePicture!}
                    alt={user.username}
                  />
                ) : (
                  <span>{user.username.charAt(0).toUpperCase()}</span>
                )}
                {uploadingPhoto && (
                  <div className="avatar-upload-spinner">
                    <Loader2 size={18} className="spin-icon" />
                  </div>
                )}
              </div>
              <button
                type="button"
                className="change-photo-btn"
                onClick={() => fileInputRef.current?.click()}
                disabled={uploadingPhoto}
              >
                {uploadingPhoto ? (
                  <>
                    <Loader2 size={15} className="spin-icon" />
                    <span>Uploading...</span>
                  </>
                ) : (
                  <>
                    <Camera size={15} />
                    <span>Change photo</span>
                  </>
                )}
              </button>
              <input
                ref={fileInputRef}
                id="profile-picture"
                type="file"
                accept="image/*"
                onChange={handleProfilePictureChange}
                style={{ display: "none" }}
              />
            </div>

            {photoError && (
              <div className="photo-upload-error">
                {photoError}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="username">Username</label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="bio">Bio</label>
              <textarea
                id="bio"
                rows={3}
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Tell the world about yourself..."
              />
            </div>

            <div className="edit-actions">
              <button
                type="submit"
                className="save-btn"
                disabled={savingProfile}
              >
                {savingProfile ? "Saving..." : "Save changes"}
              </button>
              <button
                type="button"
                className="cancel-btn"
                onClick={handleCancel}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </div>

      {/* User's Threads & Liked Section */}
      <div className="profile-threads-section" ref={threadsSectionRef}>
        <div className="profile-tabs-header">
          <button
            type="button"
            className={`profile-tab-item ${activeTab === "threads" ? "active" : ""}`}
            onClick={() => setActiveTab("threads")}
          >
            <span>Threads</span>
            <span className="pill-count">{totalThreads || myThreads.length}</span>
          </button>

          {!isTargetUser && (
            <button
              type="button"
              className={`profile-tab-item ${activeTab === "liked" ? "active" : ""}`}
              onClick={() => setActiveTab("liked")}
            >
              <Heart
                size={14}
                fill={activeTab === "liked" ? "#ef4444" : "none"}
                color={activeTab === "liked" ? "#ef4444" : "currentColor"}
              />
              <span>Liked</span>
              <span className="pill-count">{likedThreads.length}</span>
            </button>
          )}
        </div>

        {threadsLoading ? (
          <div className="profile-loading-state">
            <Loader2 size={20} className="spin-icon" />
            <p>Loading {activeTab === "threads" ? "threads" : "liked posts"}...</p>
          </div>
        ) : currentThreads.length === 0 ? (
          <div className="profile-empty-threads">
            <p>
              {activeTab === "threads"
                ? "You haven't posted any threads yet."
                : "No liked threads yet."}
            </p>
            <span>
              {activeTab === "threads"
                ? "Your posts will show up here for your followers to see."
                : "Heart threads on your feed to find them here!"}
            </span>
          </div>
        ) : (
          <>
            <div className="thread-list">
              {currentThreads.map((thread) => {
                const isCommentsOpen = openCommentThreadIds.has(thread.id);
                const isOwner = thread.username === user.username;

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
                            {formatRelativeTime(thread.createdAt)}
                          </span>
                        </div>

                        {isOwner && (
                          <div className="thread-owner-actions">
                            <button
                              type="button"
                              className="text-action-btn"
                              onClick={() => handleEditThreadStart(thread)}
                            >
                              Edit
                            </button>
                            <button
                              type="button"
                              className="text-action-btn delete-btn"
                              onClick={() => handleDeleteThread(thread.id)}
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        )}
                      </div>

                      {editingThreadId === thread.id ? (
                        <form
                          className="edit-thread-form"
                          onSubmit={(e) => handleUpdateThread(e, thread.id)}
                        >
                          <textarea
                            value={editingThreadContent}
                            onChange={(e) =>
                              setEditingThreadContent(e.target.value)
                            }
                          />
                          <div className="thread-actions">
                            <button type="submit" className="save-btn">
                              Save
                            </button>
                            <button
                              type="button"
                              className="cancel-btn"
                              onClick={handleEditThreadCancel}
                            >
                              Cancel
                            </button>
                          </div>
                        </form>
                      ) : (
                        <>
                          <p className="thread-content">{thread.content}</p>

                          <div className="thread-actions">
                            <button
                              className={`thread-action-btn like-btn ${thread.liked ? "liked" : ""
                                }`}
                              type="button"
                              onClick={() => handleToggleLike(thread)}
                              title={thread.liked ? "Unlike" : "Like"}
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
                              className={`thread-action-btn comment-toggle-btn ${isCommentsOpen ? "active" : ""
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
                              currentUsername={user.username}
                              onCommentCountChange={(c) =>
                                setCommentCounts((prev) => ({
                                  ...prev,
                                  [thread.id]: c,
                                }))
                              }
                            />
                          )}
                        </>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>

            {/* Continuous pagination & Load more */}
            {activeTab === "threads" && myThreads.length > 0 && (
              <div className="feed-pagination-section">
                {hasMoreThreads ? (
                  <div className="pagination-action-box">
                    <div ref={sentinelRef} className="scroll-sentinel" />
                    <button
                      type="button"
                      className="load-more-btn"
                      onClick={loadNextThreadsPage}
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
                    <span>You&apos;re all caught up • {totalThreads || myThreads.length} posts</span>
                  </div>
                )}
              </div>
            )}
          </>
        )}
      </div>

      {/* Followers / Following Modal */}
      {modalType && (
        <div className="modal-backdrop" onClick={() => setModalType(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>
                {modalType === "followers" ? "Followers" : "Following"}
              </h3>
              <button
                type="button"
                className="modal-close-btn"
                onClick={() => setModalType(null)}
              >
                <X size={18} />
              </button>
            </div>

            <div className="modal-body">
              {modalLoading ? (
                <div className="modal-loading-state">
                  <Loader2 size={18} className="spin-icon" />
                  <span>Loading {modalType}...</span>
                </div>
              ) : modalUsers.length === 0 ? (
                <p className="modal-empty">
                  No {modalType === "followers" ? "followers yet" : "one followed yet"}
                </p>
              ) : (
                <div className="modal-user-list">
                  {modalUsers.map((person) => {
                    const isFollowing = followingSet.has(person.id);
                    const isMe = person.id === user.id;

                    return (
                      <div key={person.id} className="modal-user-card">
                        <div className="modal-user-info">
                          <div className="person-avatar">
                            {person.profilePicture ? (
                              <img
                                src={person.profilePicture}
                                alt={person.username}
                              />
                            ) : (
                              <span>
                                {person.username.charAt(0).toUpperCase()}
                              </span>
                            )}
                          </div>
                          <div>
                            <strong>{person.username}</strong>
                            {person.bio && (
                              <p className="modal-user-bio">{person.bio}</p>
                            )}
                          </div>
                        </div>

                        {!isMe && (
                          <button
                            type="button"
                            className={`follow-toggle-btn ${isFollowing ? "following" : ""
                              }`}
                            onClick={() => handleToggleFollowInModal(person.id)}
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
              )}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}