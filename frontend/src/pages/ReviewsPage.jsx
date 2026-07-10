import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import '../style/ReviewsPage.css';

const FEED_URL = 'https://localhost:8443/api/reviews/feed';
const PUBLIC_URL = 'https://localhost:8443/api/reviews/public';
const LIKE_URL = 'https://localhost:8443/api/reviews/like';
const POSTER_BASE = 'https://image.tmdb.org/t/p/w185';

const AVATAR_COLORS = ['#00b09b', '#3b82f6', '#f59e0b', '#e85d75', '#7ca873', '#a855f7'];

const getAvatarColor = (name) => {
    if (!name) return AVATAR_COLORS[0];
    let sum = 0;
    for (let i = 0; i < name.length; i++) sum += name.charCodeAt(i);
    return AVATAR_COLORS[sum % AVATAR_COLORS.length];
};

export default function ReviewsPage() {
    const navigate = useNavigate();

    const [friends, setFriends] = useState([]);
    const [discover, setDiscover] = useState([]);
    const [sort, setSort] = useState('popular'); // 'popular' | 'newest'
    const [loadingDiscover, setLoadingDiscover] = useState(true);
    const [selectedPost, setSelectedPost] = useState(null); // გახსნილი რივიუს მოდალი

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const username = parseJwt(token)?.sub;

    const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

    // მეგობრების რივიუები (ზედა სექცია)
    useEffect(() => {
        if (!username) return;
        fetch(`${FEED_URL}?username=${username}&limit=30`, { headers: authHeaders })
            .then(res => res.ok ? res.text() : '')
            .then(text => setFriends(text ? JSON.parse(text) : []))
            .catch(err => console.error('Error loading friends reviews:', err));
    }, [username, token]);

    // საჯარო რივიუები — ფილტრის მიხედვით (ქვედა სექცია)
    const loadDiscover = useCallback(() => {
        setLoadingDiscover(true);
        const userParam = username ? `username=${username}&` : '';
        fetch(`${PUBLIC_URL}?${userParam}sort=${sort}&limit=40`, { headers: authHeaders })
            .then(res => res.ok ? res.text() : '')
            .then(text => setDiscover(text ? JSON.parse(text) : []))
            .catch(err => console.error('Error loading public reviews:', err))
            .finally(() => setLoadingDiscover(false));
    }, [username, token, sort]);

    useEffect(() => { loadDiscover(); }, [loadDiscover]);

    // Escape ხურავს რივიუს მოდალს
    useEffect(() => {
        if (!selectedPost) return;
        const onKey = (e) => { if (e.key === 'Escape') setSelectedPost(null); };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [selectedPost]);

    const handleLike = (post) => {
        if (!username || !post.reviewId) return;

        fetch(`${LIKE_URL}?username=${username}&reviewType=${post.reviewType}&reviewId=${post.reviewId}`, {
            method: 'POST',
            headers: authHeaders
        })
            .then(res => res.ok ? res.json() : null)
            .then(data => {
                if (!data) return;
                const patch = (list) => list.map(p =>
                    (p.reviewId === post.reviewId && p.reviewType === post.reviewType)
                        ? { ...p, likeCount: data.likeCount, likedByMe: data.liked }
                        : p
                );
                setFriends(patch);
                setDiscover(patch);
                setSelectedPost(prev =>
                    (prev && prev.reviewId === post.reviewId && prev.reviewType === post.reviewType)
                        ? { ...prev, likeCount: data.likeCount, likedByMe: data.liked }
                        : prev
                );
            })
            .catch(err => console.error('Failed to toggle like:', err));
    };

    const renderCard = (post) => {
        const avatarSrc = post.profilePicture ? `data:image/jpeg;base64,${post.profilePicture}` : null;
        const initial = post.username ? post.username.charAt(0).toUpperCase() : '?';
        const episodeBadge = post.seasonNumber != null && post.episodeNumber != null
            ? `S${post.seasonNumber} E${post.episodeNumber}`
            : null;

        const stop = (fn) => (e) => { e.stopPropagation(); fn(); };

        return (
            <article
                className="rev-card"
                key={`${post.reviewType}-${post.reviewId}`}
                onClick={() => setSelectedPost(post)}
                title="Open review"
            >
                <header className="rev-card-head">
                    <div
                        className="rev-avatar"
                        onClick={stop(() => navigate(`/profile/${post.username}`))}
                        title={`View ${post.username}'s profile`}
                        style={{ backgroundColor: avatarSrc ? 'transparent' : getAvatarColor(post.username) }}
                    >
                        {avatarSrc ? <img src={avatarSrc} alt={post.username} /> : initial}
                    </div>
                    <span className="rev-author" onClick={stop(() => navigate(`/profile/${post.username}`))}>
                        {post.username}
                    </span>
                </header>

                <div className="rev-card-body">
                    <div
                        className="rev-poster"
                        onClick={stop(() => navigate(`/shows/${post.showId}`))}
                        title={post.showName || 'View show'}
                    >
                        {post.posterPath
                            ? <img src={`${POSTER_BASE}${post.posterPath}`} alt={post.showName || 'Poster'} />
                            : <div className="rev-poster-empty">No poster</div>}
                    </div>

                    <div className="rev-info">
                        <span className="rev-show-name" onClick={stop(() => navigate(`/shows/${post.showId}`))}>
                            {post.showName || `Show #${post.showId}`}
                        </span>
                        <div className="rev-tags">
                            {post.rating != null && post.rating > 0 && (
                                <span className="rev-stars">{'★'.repeat(post.rating)}</span>
                            )}
                            {post.liked && <span className="rev-heart">{'♥'}</span>}
                            {episodeBadge && <span className="rev-badge">{episodeBadge}</span>}
                        </div>
                    </div>
                </div>

                {post.review && <p className="rev-review">{post.review}</p>}

                <footer className="rev-card-foot">
                    <button
                        type="button"
                        className={`rev-like-button${post.likedByMe ? ' liked' : ''}`}
                        onClick={stop(() => handleLike(post))}
                        title={post.likedByMe ? 'Unlike this review' : 'Like this review'}
                    >
                        <span className="rev-like-heart">{post.likedByMe ? '♥' : '♡'}</span>
                        <span className="rev-like-count">{post.likeCount || 0}</span>
                    </button>
                </footer>
            </article>
        );
    };

    const renderModal = () => {
        const post = selectedPost;
        if (!post) return null;

        const avatarSrc = post.profilePicture ? `data:image/jpeg;base64,${post.profilePicture}` : null;
        const initial = post.username ? post.username.charAt(0).toUpperCase() : '?';
        const episodeBadge = post.seasonNumber != null && post.episodeNumber != null
            ? `S${post.seasonNumber} E${post.episodeNumber}`
            : null;

        return (
            <div className="review-modal-overlay" onClick={() => setSelectedPost(null)}>
                <div className="review-modal" onClick={(e) => e.stopPropagation()}>
                    <button
                        type="button"
                        className="review-modal-close"
                        onClick={() => setSelectedPost(null)}
                        title="Close"
                    >
                        ×
                    </button>

                    <header className="review-modal-head">
                        <div
                            className="rev-avatar"
                            onClick={() => navigate(`/profile/${post.username}`)}
                            title={`View ${post.username}'s profile`}
                            style={{ backgroundColor: avatarSrc ? 'transparent' : getAvatarColor(post.username) }}
                        >
                            {avatarSrc ? <img src={avatarSrc} alt={post.username} /> : initial}
                        </div>
                        <span className="rev-author" onClick={() => navigate(`/profile/${post.username}`)}>
                            {post.username}
                        </span>
                    </header>

                    <div className="review-modal-body">
                        <div
                            className="review-modal-poster"
                            onClick={() => navigate(`/shows/${post.showId}`)}
                            title={post.showName || 'View show'}
                        >
                            {post.posterPath
                                ? <img src={`${POSTER_BASE}${post.posterPath}`} alt={post.showName || 'Poster'} />
                                : <div className="rev-poster-empty">No poster</div>}
                        </div>

                        <div className="review-modal-info">
                            <span className="review-modal-show" onClick={() => navigate(`/shows/${post.showId}`)}>
                                {post.showName || `Show #${post.showId}`}
                            </span>
                            <div className="rev-tags">
                                {post.rating != null && post.rating > 0 && (
                                    <span className="rev-stars">{'★'.repeat(post.rating)}</span>
                                )}
                                {post.liked && <span className="rev-heart">{'♥'}</span>}
                                {episodeBadge && <span className="rev-badge">{episodeBadge}</span>}
                                {post.rewatch && <span className="rev-badge">{'↻'} rewatch</span>}
                            </div>
                            {post.watchDate && <span className="review-modal-date">{post.watchDate}</span>}
                        </div>
                    </div>

                    {post.review
                        ? <p className="review-modal-text">{post.review}</p>
                        : <p className="review-modal-text review-modal-empty">No written review.</p>}

                    <footer className="review-modal-foot">
                        <button
                            type="button"
                            className={`rev-like-button${post.likedByMe ? ' liked' : ''}`}
                            onClick={() => handleLike(post)}
                            title={post.likedByMe ? 'Unlike this review' : 'Like this review'}
                        >
                            <span className="rev-like-heart">{post.likedByMe ? '♥' : '♡'}</span>
                            <span className="rev-like-count">{post.likeCount || 0}</span>
                        </button>
                    </footer>
                </div>
            </div>
        );
    };

    return (
        <div className="reviews-page">
            <div className="reviews-inner">
                <h1 className="reviews-title">Reviews</h1>

                {/* ზედა ნახევარი — მეგობრების რივიუები, გვერდულად ისქროლება */}
                <section className="reviews-section">
                    <h2 className="reviews-section-title">From your friends</h2>
                    {friends.length > 0 ? (
                        <div className="reviews-scroll">
                            {friends.map(renderCard)}
                        </div>
                    ) : (
                        <p className="reviews-empty">
                            {username
                                ? 'None of your friends have written a review yet.'
                                : 'Log in to see reviews from your friends.'}
                        </p>
                    )}
                </section>

                {/* ქვედა ნახევარი — საჯარო რივიუები, ფილტრით */}
                <section className="reviews-section">
                    <div className="reviews-section-head">
                        <h2 className="reviews-section-title">Discover</h2>
                        <div className="reviews-filter">
                            <button
                                className={sort === 'popular' ? 'active' : ''}
                                onClick={() => setSort('popular')}
                            >
                                Popular
                            </button>
                            <button
                                className={sort === 'newest' ? 'active' : ''}
                                onClick={() => setSort('newest')}
                            >
                                Newest
                            </button>
                        </div>
                    </div>

                    {loadingDiscover ? (
                        <p className="reviews-empty">Loading reviews…</p>
                    ) : discover.length > 0 ? (
                        <div className="reviews-grid">
                            {discover.map(renderCard)}
                        </div>
                    ) : (
                        <p className="reviews-empty">No reviews yet.</p>
                    )}
                </section>
            </div>
            {renderModal()}
        </div>
    );
}
