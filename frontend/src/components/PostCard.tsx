import type { Post } from "../types/Post";
interface PostCardProps {
    post: Post;
}
function PostCard({ post }: PostCardProps) {
    return (
        <article>
            <strong>{post.username}</strong>
            <p>{post.content}</p>
            <div>
                <button>♡ {post.likes}</button>
                <button>💬 {post.comments}</button>
            </div>
        </article>
    );
}
export default PostCard;