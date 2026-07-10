import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import '../style/ListsPage.css';

// "My Lists", "Featured Lists" (newest public lists), and "Popular This
// Week" (public lists with the most shows, since there's no real likes/
// view-tracking system yet) are all real data from the Lists backend.
// "Recently Liked" and "Crew Picks" have no backend behind them at all (no
// concept of likes or curated picks exists) - those two stay fixed
// placeholder content purely to show the intended layout, mirroring the
// Letterboxd Lists page. The "Upgrade to Pro" ad banner from the reference
// screenshot was intentionally skipped since it's monetization-specific
// and not relevant to this app.
//
// Crew Picks posters: each pick lists its real show titles in `shows`, and
// we look up a poster for each title via the existing /api/shows/search
// (TMDB TV search) endpoint, the same one the show-search-to-add feature
// uses. Clicking a Crew Pick navigates to CrewPickDetailPage, a full-page
// view mirroring ListDetailPage's layout (no real backend list behind it).
//
// Clicking a "My Lists" card no longer opens an inline panel - it navigates
// to a full ListDetailPage (/lists/:id), the same page used for anyone's
// list. Editing (name/description/visibility/films/delete) lives on a
// separate ListEditPage (/lists/:id/edit), reached via the pencil icon on
// the detail page when you're the owner.

const LISTS_BASE_URL = 'https://localhost:8443/api/lists';
const SHOWS_BASE_URL = 'https://localhost:8443/api/shows';
const POSTER_BASE = 'https://image.tmdb.org/t/p/w300';

const RECENTLY_LIKED = [
    { title: "2000's", creator: 'gabi', films: 31, likes: 480, comments: 2, desc: "iconic 2000's girly films" },
    { title: 'favorites', creator: 'lisraa', films: 30, likes: 4 },
    { title: 'Giant Insects & Naked Ladies!', creator: 'Funktual', films: 26, likes: 1 },
    { title: "2000's chick flicks", creator: 'paden19', films: 150, likes: '2.5K', comments: 7, desc: "literally every 2000's chick flick you can think of and ones you don't even know about" },
];

export const CREW_PICKS = [
    {
        title: 'Certified By Nikolozi',
        creator: 'Nikolozi',
        films: 6,
        shows: [
            'The Spectacular Spider-Man',
            'Over the Garden Wall',
            'Gintama',
            'Ted Lasso',
            'Brooklyn Nine-Nine',
            'The Walking Dead',
        ],
    },
    {
        title: "Kekno's Gems",
        creator: 'Kekno',
        films: 5,
        shows: ['The Office', 'How I Met Your Mother', 'Gilmore Girls', 'The Bear', 'Ted Lasso'],
    },
    {
        title: 'Essential Crime TV',
        creator: 'Official Crew',
        films: 17,
        shows: [
            'Hannibal',
            'Dark',
            'Band of Brothers',
            'Mare of Easttown',
            'Sherlock',
            'Narcos',
            'Breaking Bad',
            'Sons of Anarchy',
            'Chernobyl',
            'The Mentalist',
            'Criminal Minds',
            'White Collar',
            'Fargo',
            'Angels in America',
            'Sharp Objects',
            'True Detective',
            'Mindhunter',
        ],
    },
];

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

function HeartIcon() {
    return (
        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.8 1-1a5.5 5.5 0 0 0 0-7.8z" />
        </svg>
    );
}

function PosterStrip({ size = 'lg', count = 5 }) {
    return (
        <div className={`lp-poster-strip lp-poster-strip-${size}`}>
            {Array.from({ length: count }).map((_, i) => (
                <div key={i} className={`lp-poster-strip-item lp-poster-strip-item-${size}`} />
            ))}
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

// ჩასწორდა დეკლარაცია: დაემატა გამოტოვებული პარამეტრები პროპსებში
function FeaturedListCard({ list, posterPaths, itemCount, onOpen, liked, likeCount, onLike, showLike }) {
    const creator = list.ownerUsername || 'unknown';
    const initial = creator.charAt(0).toUpperCase();
    const hasAvatar = !!list.ownerProfilePicture;
    const avatarSrc = hasAvatar ? `data:image/jpeg;base64,${list.ownerProfilePicture}` : null;

    return (
        <div className="lp-card" onClick={() => onOpen(list.id)} style={{ cursor: 'pointer' }}>
            <RealPosterStrip posterPaths={posterPaths} size="card" />
            <div className="lp-card-title">{list.name}</div>
            <div className="lp-card-meta" style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    {hasAvatar ? (
                        <img
                            src={avatarSrc}
                            alt={creator}
                            style={{ width: '18px', height: '18px', borderRadius: '50%', objectFit: 'cover', border: '1px solid rgba(255,255,255,0.1)' }}
                        />
                    ) : (
                        <div
                            className="lp-avatar"
                            style={{
                                width: '18px',
                                height: '18px',
                                backgroundColor: colorForName(creator),
                                fontSize: '9px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: '50%',
                                color: '#fff',
                                fontWeight: 'bold'
                            }}
                        >
                            {initial}
                        </div>
                    )}
                    <span className="lp-creator-name" style={{ fontSize: '12px', color: '#cbd5e1' }}>{creator}</span>
                </div>
                <span className="lp-stat" style={{ fontSize: '12px', color: '#9ab3c8' }}>
                    {itemCount} {itemCount === 1 ? 'show' : 'shows'}
                </span>
            </div>
            <div className="lp-card-stats">
                <span className="lp-stat">{itemCount} {itemCount === 1 ? 'show' : 'shows'}</span>
                {showLike && (
                    <button
                        type="button"
                        className={`lp-card-like-btn${liked ? ' lp-card-like-btn-active' : ''}`}
                        onClick={(e) => {
                            e.stopPropagation(); // ხელს უშლის ბარათზე დაჭერის ივენთის გააქტიურებას
                            onLike(e);
                        }}
                        title={liked ? 'Unlike this list' : 'Like this list'}
                    >
                        ♥{likeCount > 0 ? ` ${likeCount}` : ''}
                    </button>
                )}
            </div>
        </div>
    );
}

export default function ListsPage() {
    const navigate = useNavigate();

    const [lists, setLists] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [itemsByListId, setItemsByListId] = useState({});

    const [featuredLists, setFeaturedLists] = useState([]);
    const [featuredLoading, setFeaturedLoading] = useState(true);

    const [popularLists, setPopularLists] = useState([]);
    const [popularLoading, setPopularLoading] = useState(true);

    const [showInfoCache, setShowInfoCache] = useState({});
    const requestedShowIds = useRef(new Set());

    const [recentlyLiked, setRecentlyLiked] = useState([]);
    const [likeCountOverride, setLikeCountOverride] = useState({});

    const [crewPosterCache, setCrewPosterCache] = useState({});
    const requestedCrewTitles = useRef(new Set());

    const [showCreateForm, setShowCreateForm] = useState(false);
    const [newListName, setNewListName] = useState('');
    const [newListDescription, setNewListDescription] = useState('');
    const [newListPublic, setNewListPublic] = useState(true);
    const [formError, setFormError] = useState('');

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
    }, [token]);

    const ensureCrewPoster = useCallback((title) => {
        if (!title || requestedCrewTitles.current.has(title)) return;
        requestedCrewTitles.current.add(title);
        fetch(`${SHOWS_BASE_URL}/search?query=${encodeURIComponent(title)}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                const poster = data?.results?.[0]?.poster_path || null;
                setCrewPosterCache(prev => ({ ...prev, [title]: poster }));
            })
            .catch(() => {});
    }, [token]);

    useEffect(() => {
        CREW_PICKS.forEach((pick) => (pick.shows || []).forEach(ensureCrewPoster));
    }, [ensureCrewPoster]);

    const loadItemsFor = useCallback((listId) => {
        return fetch(`${LISTS_BASE_URL}/${listId}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                const items = data?.items || [];
                setItemsByListId(prev => ({ ...prev, [listId]: items }));
                items.forEach(item => ensureShowInfo(item.showId));
            });
    }, [token, ensureShowInfo]);

    const loadLists = useCallback(() => {
        if (!username) {
            setLoading(false);
            return;
        }
        setLoading(true);
        setError('');
        fetch(`${LISTS_BASE_URL}?actingUsername=${username}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : []))
            .then(data => {
                const ls = data || [];
                setLists(ls);
                ls.forEach(l => loadItemsFor(l.id));
            })
            .catch(() => setError('Could not load your lists.'))
            .finally(() => setLoading(false));
    }, [username, loadItemsFor, token]);

    useEffect(() => {
        loadLists();
    }, [loadLists]);

    const loadFeaturedLists = useCallback(() => {
        setFeaturedLoading(true);
        fetch(`${LISTS_BASE_URL}/public`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : []))
            .then(data => {
                const fl = data || [];
                setFeaturedLists(fl);
                fl.forEach(l => loadItemsFor(l.id));
            })
            .catch(() => setFeaturedLists([]))
            .finally(() => setFeaturedLoading(false));
    }, [token, loadItemsFor]);

    useEffect(() => {
        loadFeaturedLists();
    }, [loadFeaturedLists]);

    const loadPopularLists = useCallback(() => {
        setPopularLoading(true);
        fetch(`${LISTS_BASE_URL}/public/popular`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : []))
            .then(data => {
                const pl = data || [];
                setPopularLists(pl);
                pl.forEach(l => loadItemsFor(l.id));
            })
            .catch(() => setPopularLists([]))
            .finally(() => setPopularLoading(false));
    }, [token, loadItemsFor]);

    useEffect(() => {
        loadPopularLists();
    }, [loadPopularLists]);

    useEffect(() => {
        if (!username) return;
        fetch(`${LISTS_BASE_URL}/liked?username=${username}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : []))
            .then(data => {
                const rl = data || [];
                setRecentlyLiked(rl);
                rl.forEach(l => loadItemsFor(l.id));
            })
            .catch(() => setRecentlyLiked([]));
    }, [username, loadItemsFor, token]);

    const handleCreateList = () => {
        if (!username) return;
        if (!newListName.trim()) {
            setFormError('List name cannot be empty.');
            return;
        }
        setFormError('');
        const params = new URLSearchParams({
            actingUsername: username,
            name: newListName.trim(),
            description: newListDescription,
            isPublic: String(newListPublic),
        });
        fetch(`${LISTS_BASE_URL}?${params.toString()}`, { method: 'POST', headers: authHeaders })
            .then(res => (res.ok ? res.json() : res.text().then(msg => Promise.reject(msg))))
            .then((created) => {
                setNewListName('');
                setNewListDescription('');
                setNewListPublic(true);
                setShowCreateForm(false);
                navigate(`/lists/${created.id}/edit`);
            })
            .catch((msg) => setFormError(typeof msg === 'string' ? msg : 'Could not create list.'));
    };

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
            const enriched = { ...list, likeCount: currentCount + 1, likedByMe: true };
            setRecentlyLiked(prev => [enriched, ...prev]);
            setLikeCountOverride(prev => ({ ...prev, [list.id]: currentCount + 1 }));
            if (!itemsByListId[list.id]) loadItemsFor(list.id);
            fetch(`${LISTS_BASE_URL}/${list.id}/like?username=${username}`, {
                method: 'POST', headers: authHeaders,
            }).catch(() => {
                setRecentlyLiked(prev => prev.filter(l => l.id !== list.id));
                setLikeCountOverride(prev => ({ ...prev, [list.id]: currentCount }));
            });
        }
    };

    return (
        <div className="lp-page">
            <main className="lp-main">
                <p className="lp-tagline">Collect, curate, and share. Lists are the perfect way to group TV shows.</p>
                <div className="lp-cta-wrap">
                    <button className="lp-start-btn" onClick={() => setShowCreateForm(v => !v)}>
                        Start your own list
                    </button>
                </div>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">My Lists</span>
                        {username && lists.length > 0 && (
                            <button className="lp-view-all-btn" onClick={() => navigate('/lists/all/mine')}>
                                View All
                            </button>
                        )}
                    </div>

                    {!username && (
                        <p className="lp-section-empty">Log in to create and manage your own lists.</p>
                    )}

                    {username && showCreateForm && (
                        <div className="lp-create-form">
                            <input
                                className="lp-add-input"
                                placeholder="List name"
                                value={newListName}
                                onChange={(e) => setNewListName(e.target.value)}
                            />
                            <input
                                className="lp-add-input"
                                placeholder="Description (optional)"
                                value={newListDescription}
                                onChange={(e) => setNewListDescription(e.target.value)}
                            />
                            <label className="lp-checkbox-label">
                                <input
                                    type="checkbox"
                                    checked={newListPublic}
                                    onChange={(e) => setNewListPublic(e.target.checked)}
                                />
                                Public
                            </label>
                            <button className="lp-add-btn" onClick={handleCreateList}>Create</button>
                            {formError && <p className="lp-panel-message">{formError}</p>}
                        </div>
                    )}

                    {username && loading && <p className="lp-section-empty">Loading your lists...</p>}
                    {username && error && <p className="lp-panel-message">{error}</p>}

                    {username && !loading && !error && lists.length === 0 && (
                        <p className="lp-section-empty">You haven't created any lists yet.</p>
                    )}

                    {username && lists.length > 0 && (
                        <div className="lp-grid-3">
                            {lists.slice(0, 3).map((list) => {
                                const items = itemsByListId[list.id] || [];
                                const posterPaths = items
                                    .map(item => showInfoCache[item.showId]?.poster_path)
                                    .filter(Boolean);
                                return (
                                    <MyListCard
                                        key={list.id}
                                        list={list}
                                        posterPaths={posterPaths}
                                        itemCount={items.length}
                                        onOpen={(id) => navigate(`/lists/${id}`)}
                                    />
                                );
                            })}
                        </div>
                    )}
                </section>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">Featured Lists</span>
                        {featuredLists.length > 0 && (
                            <button className="lp-view-all-btn" onClick={() => navigate('/lists/all/featured')}>
                                View All
                            </button>
                        )}
                    </div>
                    {featuredLoading && <p className="lp-section-empty">Loading featured lists...</p>}
                    {!featuredLoading && featuredLists.length === 0 && (
                        <p className="lp-section-empty">No public lists yet — be the first to make one.</p>
                    )}
                    {!featuredLoading && featuredLists.length > 0 && (
                        <div className="lp-grid-3">
                            {featuredLists.slice(0, 3).map((list) => {
                                const items = itemsByListId[list.id] || [];
                                const posterPaths = items
                                    .map(item => showInfoCache[item.showId]?.poster_path)
                                    .filter(Boolean);
                                return (
                                    <FeaturedListCard
                                        key={list.id}
                                        list={list}
                                        posterPaths={posterPaths}
                                        itemCount={items.length}
                                        onOpen={(id) => navigate(`/lists/${id}`)}
                                        liked={likedSet.has(list.id)}
                                        likeCount={likeCountOverride[list.id] ?? list.likeCount ?? 0}
                                        onLike={(e) => handleCardLike(list, e)}
                                        showLike={!!username}
                                    />
                                );
                            })}
                        </div>
                    )}
                </section>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">Popular This Week</span>
                        {popularLists.length > 0 && (
                            <button className="lp-view-all-btn" onClick={() => navigate('/lists/all/popular')}>
                                View All
                            </button>
                        )}
                    </div>
                    {popularLoading && <p className="lp-section-empty">Loading popular lists...</p>}
                    {!popularLoading && popularLists.length === 0 && (
                        <p className="lp-section-empty">No public lists yet — be the first to make one.</p>
                    )}
                    {!popularLoading && popularLists.length > 0 && (
                        <div className="lp-grid-3">
                            {popularLists.slice(0, 3).map((list) => {
                                const items = itemsByListId[list.id] || [];
                                const posterPaths = items
                                    .map(item => showInfoCache[item.showId]?.poster_path)
                                    .filter(Boolean);
                                return (
                                    <FeaturedListCard
                                        key={list.id}
                                        list={list}
                                        posterPaths={posterPaths}
                                        itemCount={items.length}
                                        onOpen={(id) => navigate(`/lists/${id}`)}
                                        liked={likedSet.has(list.id)}
                                        likeCount={likeCountOverride[list.id] ?? list.likeCount ?? 0}
                                        onLike={(e) => handleCardLike(list, e)}
                                        showLike={!!username}
                                    />
                                );
                            })}
                        </div>
                    )}
                </section>

                <div className="lp-two-col">
                    <section className="lp-section lp-recently-liked">
                        <div className="lp-section-header">
                            <span className="lp-kicker">Recently Liked</span>
                        </div>
                        <div className="lp-liked-list">
                            {/* 🟢 თუ იუზერი შესულია და აქვს დალაიქებული სიები */}
                            {username && recentlyLiked.length > 0 ? (
                                recentlyLiked.map((item, i) => (
                                    <div key={i} className="lp-liked-row">
                                        <RealPosterStrip
                                            posterPaths={(itemsByListId[item.id] || [])
                                                .map(item => showInfoCache[item.showId]?.poster_path)
                                                .filter(Boolean)}
                                            size="sm"
                                        />
                                        <div className="lp-liked-content">
                                            <div className="lp-liked-title">{item.name}</div>
                                            <div className="lp-liked-meta">
                                                <MiniAvatar name={item.ownerUsername} size={18} />
                                                <span className="lp-creator-name">{item.ownerUsername}</span>
                                                <span className="lp-stat">
                            {(itemsByListId[item.id]?.length || 0)} shows
                        </span>
                                                <span className="lp-stat">
                            <HeartIcon /> {item.likeCount ?? 0}
                        </span>
                                            </div>
                                            {item.description && (
                                                <p className="lp-liked-desc">{item.description}</p>
                                            )}
                                        </div>
                                    </div>
                                ))
                            ) : (
                                /* 🟢 თუ არაფერი აქვს დალაიქებული ან იუზერი საერთოდ არ არის ავტორიზებული */
                                <p className="lp-section-empty" style={{ padding: '20px 0', color: '#64748b' }}>
                                    {username ? "No liked lists yet." : "Log in to see your liked lists."}
                                </p>
                            )}
                        </div>
                    </section>

                    <section className="lp-section lp-crew-picks">
                        <div className="lp-section-header">
                            <span className="lp-kicker">Crew Picks</span>
                        </div>
                        <div className="lp-crew-picks-list">
                            {CREW_PICKS.map((item, i) => {
                                const posterPaths = (item.shows || [])
                                    .map(s => crewPosterCache[s])
                                    .filter(Boolean);
                                return (
                                    <div
                                        key={i}
                                        className="lp-crew-pick-item lp-crew-pick-clickable"
                                        onClick={() => navigate(`/crew-picks/${i}`)}
                                    >
                                        <RealPosterStrip posterPaths={posterPaths} size="sm" />
                                        <div className="lp-crew-pick-title">{item.title}</div>
                                        {item.creator && (
                                            <div className="lp-crew-pick-meta">
                                                <MiniAvatar name={item.creator} size={18} />
                                                <span className="lp-creator-name">{item.creator}</span>
                                                {item.films !== undefined && <span className="lp-stat">{item.films} TV shows</span>}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </section>
                </div>
            </main>
        </div>
    );
}