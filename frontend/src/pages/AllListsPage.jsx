import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import '../style/ListsPage.css';

// Full view of "My Lists", "Featured Lists", or "Popular This Week" -
// reached via the "View All" buttons on ListsPage. Those sections only ever
// show a single row there, so this page is where you see everything.

const LISTS_BASE_URL = 'https://localhost:8443/api/lists';
const SHOWS_BASE_URL = 'https://localhost:8443/api/shows';
const POSTER_BASE = 'https://image.tmdb.org/t/p/w300';

const AVATAR_COLORS = ['#00b4a2', '#e85d75', '#f2b134', '#5b8def', '#9b59b6', '#2ecc71'];

function colorForName(name) {
    if (!name) return AVATAR_COLORS[0];
    const code = name.charCodeAt(0) || 0;
    return AVATAR_COLORS[code % AVATAR_COLORS.length];
}

function MiniAvatar({ name, size = 22 }) {
    const initial = name ? name.trim().charAt(0).toUpperCase() : '?';
    return (
        <div
            className="lp-avatar"
            style={{ width: size, height: size, backgroundColor: colorForName(name), fontSize: size * 0.5 }}
        >
            {initial}
        </div>
    );
}

function RealPosterStrip({ posterPaths = [], size = 'lg' }) {
    const slots = posterPaths.length > 0 ? posterPaths.slice(0, 5) : [null];
    return (
        <div className={`lp-poster-strip lp-poster-strip-${size}`}>
            {slots.map((path, i) => (
                path ? (
                    <img
                        key={i}
                        src={`${POSTER_BASE}${path}`}
                        alt=""
                        className={`lp-poster-strip-item lp-poster-strip-item-${size} lp-poster-img`}
                    />
                ) : (
                    <div key={i} className={`lp-poster-strip-item lp-poster-strip-item-${size}`} />
                )
            ))}
        </div>
    );
}

function MyListCard({ list, posterPaths, itemCount, onOpen }) {
    return (
        <div className="lp-card lp-my-list-card-v2" onClick={() => onOpen(list.id)}>
            <RealPosterStrip posterPaths={posterPaths} size="card" />
            <div className="lp-card-title">{list.name}</div>
            <div className="lp-card-meta">
                <span className="lp-pill">{list.isPublic ? 'Public' : 'Private'}</span>
                <span className="lp-stat">{itemCount} {itemCount === 1 ? 'show' : 'shows'}</span>
            </div>
        </div>
    );
}

function FeaturedListCard({ list, posterPaths, itemCount, onOpen, liked, onLike, showLike }) {
    return (
        <div className="lp-card" onClick={() => onOpen(list.id)}>
            <RealPosterStrip posterPaths={posterPaths} size="lg" />
            <div className="lp-card-title">{list.name}</div>
            <div className="lp-card-meta">
                <MiniAvatar name={list.ownerUsername} />
                <span>
                    Created by <span className="lp-creator-name">{list.ownerUsername}</span>
                </span>
            </div>
            <div className="lp-card-stats">
                <span className="lp-stat">{itemCount} {itemCount === 1 ? 'show' : 'shows'}</span>
                {showLike && (
                    <button
                        type="button"
                        className={`lp-card-like-btn${liked ? ' lp-card-like-btn-active' : ''}`}
                        onClick={onLike}
                        title={liked ? 'Unlike this list' : 'Like this list'}
                    >
                        ♥
                    </button>
                )}
            </div>
        </div>
    );
}

export default function AllListsPage() {
    const { type } = useParams(); // "mine", "featured", or "popular"
    const navigate = useNavigate();
    const isMine = type === 'mine';
    const isPopular = type === 'popular';

    const [lists, setLists] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [itemsByListId, setItemsByListId] = useState({});
    const [showInfoCache, setShowInfoCache] = useState({});
    const requestedShowIds = useRef(new Set());

    const [recentlyLiked, setRecentlyLiked] = useState([]);
    const [likeCountOverride, setLikeCountOverride] = useState({});

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const username = decodedToken?.sub;
    const authHeaders = { Authorization: `Bearer ${token}` };

    const ensureShowInfo = useCallback((showId) => {
        if (!showId || requestedShowIds.current.has(showId)) return;
        requestedShowIds.current.add(showId);
        fetch(`${SHOWS_BASE_URL}/${showId}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                setShowInfoCache(prev => ({
                    ...prev,
                    [showId]: {
                        name: data?.name || data?.title || `Show #${showId}`,
                        poster_path: data?.poster_path || null,
                    },
                }));
            })
            .catch(() => {});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token]);

    const loadItemsFor = useCallback((listId) => {
        return fetch(`${LISTS_BASE_URL}/${listId}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                const items = data?.items || [];
                setItemsByListId(prev => ({ ...prev, [listId]: items }));
                items.forEach(item => ensureShowInfo(item.showId));
            });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token, ensureShowInfo]);

    const loadLists = useCallback(() => {
        setLoading(true);
        setError('');

        if (isMine) {
            if (!username) {
                setLoading(false);
                return;
            }
            fetch(`${LISTS_BASE_URL}?actingUsername=${username}`, { headers: authHeaders })
                .then(r => (r.ok ? r.json() : []))
                .then(data => {
                    const ls = data || [];
                    setLists(ls);
                    ls.forEach(l => loadItemsFor(l.id));
                })
                .catch(() => setError('Could not load your lists.'))
                .finally(() => setLoading(false));
        } else {
            const endpoint = isPopular ? `${LISTS_BASE_URL}/public/popular` : `${LISTS_BASE_URL}/public/all`;
            fetch(endpoint, { headers: authHeaders })
                .then(r => (r.ok ? r.json() : []))
                .then(data => {
                    const ls = data || [];
                    setLists(ls);
                    ls.forEach(l => loadItemsFor(l.id));
                })
                .catch(() => setError(isPopular ? 'Could not load popular lists.' : 'Could not load featured lists.'))
                .finally(() => setLoading(false));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isMine, isPopular, username]);

    useEffect(() => {
        loadLists();
    }, [loadLists]);

    // Fetch liked lists so we know which cards to show as active
    useEffect(() => {
        if (!username) return;
        fetch(`${LISTS_BASE_URL}/liked?username=${username}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : []))
            .then(data => setRecentlyLiked(data || []))
            .catch(() => {});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [username]);

    const likedSet = new Set(recentlyLiked.map(l => l.id));

    const handleCardLike = (list, e) => {
        e.stopPropagation();
        if (!username) return;

        const currentlyLiked = likedSet.has(list.id);
        const currentCount = likeCountOverride[list.id] !== undefined
            ? likeCountOverride[list.id]
            : (list.likeCount || 0);

        if (currentlyLiked) {
            setRecentlyLiked(prev => prev.filter(l => l.id !== list.id));
            setLikeCountOverride(prev => ({ ...prev, [list.id]: Math.max(0, currentCount - 1) }));
            fetch(`${LISTS_BASE_URL}/${list.id}/like?username=${username}`, {
                method: 'POST', headers: authHeaders,
            }).catch(() => {
                setRecentlyLiked(prev => [list, ...prev]);
                setLikeCountOverride(prev => ({ ...prev, [list.id]: currentCount }));
            });
        } else {
            setRecentlyLiked(prev => [{ ...list, likeCount: currentCount + 1 }, ...prev]);
            setLikeCountOverride(prev => ({ ...prev, [list.id]: currentCount + 1 }));
            fetch(`${LISTS_BASE_URL}/${list.id}/like?username=${username}`, {
                method: 'POST', headers: authHeaders,
            }).catch(() => {
                setRecentlyLiked(prev => prev.filter(l => l.id !== list.id));
                setLikeCountOverride(prev => ({ ...prev, [list.id]: currentCount }));
            });
        }
    };

    const title = isMine ? 'My Lists' : isPopular ? 'Popular This Week' : 'Featured Lists';

    return (
        <div className="lp-page">
            <main className="lp-main">
                <Link to="/lists" className="lp-back-link">&larr; Back to Lists</Link>
                <h1 className="lp-page-title">{title}</h1>

                {isMine && !username && (
                    <p className="lp-section-empty">Log in to see your lists.</p>
                )}

                {loading && <p className="lp-section-empty">Loading...</p>}
                {!loading && error && <p className="lp-panel-message">{error}</p>}

                {!loading && !error && (!isMine || username) && lists.length === 0 && (
                    <p className="lp-section-empty">
                        {isMine
                            ? "You haven't created any lists yet."
                            : 'No public lists yet — be the first to make one.'}
                    </p>
                )}

                {!loading && lists.length > 0 && (
                    <div className="lp-grid-3">
                        {lists.map((list) => {
                            const items = itemsByListId[list.id] || [];
                            const posterPaths = items
                                .map(item => showInfoCache[item.showId]?.poster_path)
                                .filter(Boolean);
                            if (isMine) {
                                return (
                                    <MyListCard
                                        key={list.id}
                                        list={list}
                                        posterPaths={posterPaths}
                                        itemCount={items.length}
                                        onOpen={(id) => navigate(`/lists/${id}`)}
                                    />
                                );
                            }
                            return (
                                <FeaturedListCard
                                    key={list.id}
                                    list={list}
                                    posterPaths={posterPaths}
                                    itemCount={items.length}
                                    onOpen={(id) => navigate(`/lists/${id}`)}
                                    liked={likedSet.has(list.id)}
                                    onLike={(e) => handleCardLike(list, e)}
                                    showLike={!!username}
                                />
                            );
                        })}
                    </div>
                )}
            </main>
        </div>
    );
}
