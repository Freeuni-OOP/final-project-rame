import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import ShowsPage from './ShowsPage.jsx';
import '../style/HomePage.css';

const FEED_URL = 'https://localhost:8443/api/reviews/feed';
const LIKE_URL = 'https://localhost:8443/api/reviews/like';
const POSTER_BASE = 'https://image.tmdb.org/t/p/w185';

const AVATAR_COLORS = ['#00b09b', '#3b82f6', '#f59e0b', '#e85d75', '#7ca873', '#a855f7'];

const getAvatarColor = (name) => {
    if (!name) return AVATAR_COLORS[0];
    let sum = 0;
    for (let i = 0; i < name.length; i++) sum += name.charCodeAt(i);
    return AVATAR_COLORS[sum % AVATAR_COLORS.length];
};

export default function HomePage() {
    const navigate = useNavigate();

    const [feed, setFeed] = useState([]);
    const [error, setError] = useState(null);

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const username = parseJwt(token)?.sub;

    useEffect(() => {
        if (!username) return;

        const fetchFeed = async () => {
            try {
                const response = await fetch(`${FEED_URL}?username=${username}&limit=30`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (!response.ok) throw new Error(`Failed to load feed (Status: ${response.status})`);

                const text = await response.text();
                setFeed(text ? JSON.parse(text) : []);
                setError(null);
            } catch (e) {
                console.error('Error loading feed:', e);
                setError('Could not load your feed. Please try again.');
            }
        };

        fetchFeed();
    }, [username, token]);

    const handleLike = (post) => {
        if (!username || !post.reviewId) return;

        fetch(`${LIKE_URL}?username=${username}&reviewType=${post.reviewType}&reviewId=${post.reviewId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(res => res.ok ? res.json() : null)
            .then(data => {
                if (!data) return;
                setFeed(prev => prev.map(p =>
                    (p.reviewId === post.reviewId && p.reviewType === post.reviewType)
                        ? { ...p, likeCount: data.likeCount, likedByMe: data.liked }
                        : p
                ));
            })
            .catch(err => console.error('Failed to toggle like:', err));
    };

    const renderCard = (post) => {
        const avatarSrc = post.profilePicture ? `data:image/jpeg;base64,${post.profilePicture}` : null;
        const initial = post.username ? post.username.charAt(0).toUpperCase() : '?';

        const episodeBadge = post.seasonNumber != null && post.episodeNumber != null
            ? `S${post.seasonNumber} E${post.episodeNumber}`
            : null;

        return (
            <article className="friend-card" key={`${post.reviewType}-${post.reviewId}`}>
                <header className="friend-card-head">
                    <div
                        className="friend-avatar"
                        onClick={() => navigate(`/profile/${post.username}`)}
                        title={`View ${post.username}'s profile`}
                        style={{ backgroundColor: avatarSrc ? 'transparent' : getAvatarColor(post.username) }}
                    >
                        {avatarSrc ? <img src={avatarSrc} alt={post.username} /> : initial}
                    </div>
                    <span
                        className="friend-author"
                        onClick={() => navigate(`/profile/${post.username}`)}
                    >
                        {post.username}
                    </span>
                </header>

                <div className="friend-card-body">
                    <div
                        className="friend-poster"
                        onClick={() => navigate(`/shows/${post.showId}`)}
                        title={post.showName || 'View show'}
                    >
                        {post.posterPath
                            ? <img src={`${POSTER_BASE}${post.posterPath}`} alt={post.showName || 'Poster'} />
                            : <div className="friend-poster-empty">No poster</div>}
                    </div>

                    <div className="friend-card-info">
                        <span
                            className="friend-show-name"
                            onClick={() => navigate(`/shows/${post.showId}`)}
                        >
                            {post.showName || `Show #${post.showId}`}
                        </span>

                        <div className="friend-tags">
                            {post.rating != null && post.rating > 0 && (
                                <span className="feed-stars">{'★'.repeat(post.rating)}</span>
                            )}
                            {post.liked && <span className="feed-heart">{'♥'}</span>}
                            {episodeBadge && <span className="feed-badge">{episodeBadge}</span>}
                        </div>
                    </div>
                </div>

                {post.review && <p className="friend-review">{post.review}</p>}

                <footer className="friend-card-foot">
                    <button
                        type="button"
                        className={`feed-like-button${post.likedByMe ? ' liked' : ''}`}
                        onClick={() => handleLike(post)}
                        title={post.likedByMe ? 'Unlike this review' : 'Like this review'}
                    >
                        <span className="feed-like-heart">{post.likedByMe ? '♥' : '♡'}</span>
                        <span className="feed-like-count">{post.likeCount || 0}</span>
                    </button>
                </footer>
            </article>
        );
    };

    // ზემოთ — მეგობრების რივიუების ჰორიზონტალური პანელი (მხოლოდ თუ პოსტებია),
    // ქვემოთ — მთავარი გვერდის საწყისი სტილი (trending სერიალები) უცვლელად.
    const hasFeed = !!username && !error && feed.length > 0;

    return (
        <div className="home-page">
            {hasFeed && (
                <section className="friends-strip">
                    <div className="friends-strip-head">
                        <h2 className="friends-strip-title">From your friends</h2>
                        <button
                            type="button"
                            className="friends-strip-more"
                            onClick={() => navigate('/reviews')}
                        >
                            All reviews →
                        </button>
                    </div>
                    <div className="friends-scroll">
                        {feed.map(renderCard)}
                    </div>
                </section>
            )}
            <ShowsPage />
        </div>
    );
}
