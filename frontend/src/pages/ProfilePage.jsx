import React, { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import '../style/ProfilePage.css';

const FRIENDS_BASE_URL = 'https://localhost:8443/api/friends';

// "Films" = number of shows the user has fully watched (COMPLETED),
// pulled live from the /api/tracking/films-count endpoint.

// No "favorite movies/actors" feature exists on the backend yet, so this
// tab is entirely fixed placeholder shapes/counts until that data exists.
const FAVORITE_MOVIES_COUNT = 6;
const FAVORITE_ACTORS_COUNT = 6;

// Diary is now backed by real logged watch-history (watchDate) from the
// /api/tracking/diary endpoint. Sort/filter controls are wired to that data.

// No "likes" feature exists on the backend yet either, so this tab is
// genuinely empty (no fabricated rows) — just the filter chrome plus an
// empty state, matching the real "no data yet" situation.
const LIKES_FILTERS = ['Rating', 'Decade', 'Genre', 'Service', 'Sort by When Liked'];
const LIKES_SUBTABS = ['TV Shows', 'Reviews', 'Lists'];

// Watchlist is backed by the real "PLAN_TO_WATCH" show status (set from the
// star icon on a show/details page). Backend already returns it newest-added
// first, so there's no separate sort/filter UI for this view.
const POSTER_BASE = 'https://image.tmdb.org/t/p/w342';

// Network tab reuses the real friends/requests/suggestions backend (same
// endpoints as the old standalone Members/Friends page) so we don't need a
// separate page for it. Film/review counts per friend are still placeholders
// (no watch/review feature aggregation exists yet).
const NETWORK_FILM_COUNT = 128;
const NETWORK_REVIEW_COUNT = 34;
const NETWORK_POSTER_COUNT = 4;

const AVATAR_COLORS = ['#00b4a2', '#e85d75', '#f2b134', '#5b8def', '#9b59b6', '#2ecc71'];

function colorForName(name) {
    if (!name) return AVATAR_COLORS[0];
    const code = name.charCodeAt(0) || 0;
    return AVATAR_COLORS[code % AVATAR_COLORS.length];
}

function Avatar({ name, size = 44 }) {
    const initial = name ? name.charAt(0).toUpperCase() : '?';
    return (
        <div
            className="pp-avatar"
            style={{ width: size, height: size, backgroundColor: colorForName(name), fontSize: size * 0.38 }}
        >
            {initial}
        </div>
    );
}

function HeartIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.8 1-1a5.5 5.5 0 0 0 0-7.8z" />
        </svg>
    );
}

// Filter dropdown that looks like the original plain "LABEL ▾" span,
// but opens a real menu. value === '' means "All".
function DiaryDropdown({ label, value, options, onChange }) {
    const [open, setOpen] = useState(false);
    return (
        <div className="pp-diary-dd">
            <span
                className="pp-diary-filter"
                style={{ cursor: 'pointer', color: value ? '#00b4a2' : undefined }}
                onClick={() => setOpen(o => !o)}
            >
                {value || label} ▾
            </span>
            {open && (
                <>
                    <div
                        style={{ position: 'fixed', inset: 0, zIndex: 40 }}
                        onClick={() => setOpen(false)}
                    />
                    <div className="pp-diary-dd-menu">
                        <div
                            className={`pp-diary-dd-item ${value === '' ? 'pp-diary-dd-item-active' : ''}`}
                            onClick={() => { onChange(''); setOpen(false); }}
                        >
                            All
                        </div>
                        {options.map(opt => (
                            <div
                                key={opt}
                                className={`pp-diary-dd-item ${value === opt ? 'pp-diary-dd-item-active' : ''}`}
                                onClick={() => { onChange(opt); setOpen(false); }}
                            >
                                {opt}
                            </div>
                        ))}
                    </div>
                </>
            )}
        </div>
    );
}

// Edit an existing diary entry (rating / review / like / rewatch).
// Saves via the same /api/log upsert, keeping watchDate so it stays in the diary.
function DiaryEditModal({ entry, username, token, onClose, onSaved }) {
    const [rating, setRating] = useState(entry.rating || 0);
    const [hover, setHover] = useState(0);
    const [review, setReview] = useState(entry.review || '');
    const [liked, setLiked] = useState(!!entry.liked);
    const [rewatch, setRewatch] = useState(!!entry.rewatch);
    const [saving, setSaving] = useState(false);

    const epLabel = (entry.seasonNumber != null && entry.episodeNumber != null)
        ? ` S${entry.seasonNumber}E${entry.episodeNumber}` : '';

    const save = () => {
        setSaving(true);
        const payload = {
            username,
            showId: entry.showId,
            rating,
            review,
            liked,
            wholeShow: false, // status-ს არ ვცვლით, მხოლოდ არსებულ ჩანაწერს ვანახლებთ
            seasonNumber: entry.seasonNumber ?? null,
            episodeNumber: entry.episodeNumber ?? null,
            rewatch,
            watchDate: entry.watchDate, // თარიღი რჩება, რომ diary-ში დარჩეს
        };
        fetch('https://localhost:8443/api/log', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
            body: JSON.stringify(payload),
        })
            .then(r => { if (!r.ok) throw new Error('save failed'); onSaved(); onClose(); })
            .catch(() => alert('Could not save review.'))
            .finally(() => setSaving(false));
    };

    return (
        <div className="pp-modal-overlay" onClick={onClose}>
            <div className="pp-modal" onClick={e => e.stopPropagation()}>
                <div className="pp-modal-header">
                    <h3 className="pp-modal-title">{entry.title}{epLabel}</h3>
                    <button className="pp-modal-close" onClick={onClose} aria-label="Close">✕</button>
                </div>

                <div className="pp-modal-stars">
                    {[1, 2, 3, 4, 5].map(s => (
                        <span
                            key={s}
                            onClick={() => setRating(s === rating ? 0 : s)}
                            onMouseEnter={() => setHover(s)}
                            onMouseLeave={() => setHover(0)}
                            style={{ cursor: 'pointer', fontSize: '28px', color: s <= (hover || rating) ? '#f2b134' : '#3a4450' }}
                        >
                            ★
                        </span>
                    ))}
                </div>

                <textarea
                    className="pp-modal-textarea"
                    value={review}
                    onChange={e => setReview(e.target.value)}
                    placeholder="Write your review..."
                />

                <div className="pp-modal-toggles">
                    <label className="pp-modal-check">
                        <input type="checkbox" checked={liked} onChange={e => setLiked(e.target.checked)} /> Liked
                    </label>
                    <label className="pp-modal-check">
                        <input type="checkbox" checked={rewatch} onChange={e => setRewatch(e.target.checked)} /> Rewatch
                    </label>
                </div>

                <div className="pp-modal-footer">
                    <button className="pp-modal-save" onClick={save} disabled={saving}>
                        {saving ? 'Saving...' : 'Save'}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default function ProfilePage() {
    const navigate = useNavigate();
    const { username: routeUsername } = useParams();
    const [friendsCount, setFriendsCount] = useState(0);
    const [filmsCount, setFilmsCount] = useState(0);
    const [loading, setLoading] = useState(true);
    const [activeTab, setActiveTab] = useState('profile');
    const [likesSubTab, setLikesSubTab] = useState('TV Shows');
    const [activities, setActivities] = useState([]);

    const [friends, setFriends] = useState([]);
    const [pending, setPending] = useState([]);
    const [sent, setSent] = useState([]);
    const [suggestions, setSuggestions] = useState([]);
    const [recommendations, setRecommendations] = useState([]);
    const [requestsTab, setRequestsTab] = useState('pending');
    const [addUsername, setAddUsername] = useState('');
    const [panelMessage, setPanelMessage] = useState('');

    const [watchlistShowIds, setWatchlistShowIds] = useState([]);
    const [watchlistInfo, setWatchlistInfo] = useState({});
    const [friendPosterIds, setFriendPosterIds] = useState({}); // username -> [showId,...] (Network tab-ის რეალური ფოტოებისთვის)
    const [likedShows, setLikedShows] = useState([]); // Likes tab — [{showId, rating}], ბოლო-პირველი
    const [likesSort, setLikesSort] = useState('liked-desc'); // liked-desc|liked-asc|rating-desc|rating-asc
    const [likesDecade, setLikesDecade] = useState('');
    const [likesGenre, setLikesGenre] = useState('');

    // Diary — რეალური, დათარიღებული ჩანაწერები + სორტი/ფილტრები
    const [diaryEntries, setDiaryEntries] = useState([]);
    const [diarySort, setDiarySort] = useState('date-desc'); // date-desc | date-asc | rating-desc | rating-asc
    const [diaryYear, setDiaryYear] = useState('');
    const [diaryDecade, setDiaryDecade] = useState('');
    const [diaryGenre, setDiaryGenre] = useState('');
    const [editingEntry, setEditingEntry] = useState(null); // Diary-ს რომელ ჩანაწერს ვასწორებთ

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const currentUsername = decodedToken?.sub;          // who is logged in
    const username = routeUsername || currentUsername;  // whose profile is open
    const isOwnProfile = !routeUsername || routeUsername === currentUsername;
    const authHeaders = { Authorization: `Bearer ${token}` };


const loadAll = () => {
        if (!username) return;
        setLoading(true);

        const authHeaders = { Authorization: `Bearer ${token}` };

        // საჯარო მონაცემი (ნებისმიერი იუზერის): მეგობრები + watchlist + diary +
        // "People you may know" — ეს ყოველთვის შემხედველის (currentUsername) შემოთავაზებაა,
        // ამიტომ ჩანს ნებისმიერი პროფილის Network ტაბზეც, არა მხოლოდ საკუთარზე.
        const publicCalls = [
            fetch(`${FRIENDS_BASE_URL}?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`https://localhost:8443/api/tracking/watchlist?username=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
         
            fetch(`https://localhost:8443/api/tracking/diary?username=${username}&viewer=${currentUsername || ''}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            currentUsername
                ? fetch(`${FRIENDS_BASE_URL}/suggestions?actingUsername=${currentUsername}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : []))
                : Promise.resolve([]),
            fetch(`https://localhost:8443/api/tracking/activity?username=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`https://localhost:8443/api/tracking/likes?username=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`https://localhost:8443/api/tracking/films-count?username=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : 0)),
        ];

        // Private data (own profile only) — მოთხოვნები/რეკომენდაციები მხოლოდ საკუთარია
        const privateCalls = isOwnProfile ? [
            fetch(`${FRIENDS_BASE_URL}/pending?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`${FRIENDS_BASE_URL}/sent?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`https://localhost:8443/api/tracking/recommendations?username=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
        ] : [];

        Promise.all([...publicCalls, ...privateCalls])
            .then(([
                friendsList, 
                watchlistIds, 
                diaryList, 
                suggestionslist = [], 
                activityList = [], 
                likedIds = [], 
                filmsCountVal = 0,
                pendingList = [], 
                sentlist = [], 
                recsList = []
            ]) => {

                setFriends(friendsList || []);
                setFriendsCount((friendsList || []).length);
                setFilmsCount(Number(filmsCountVal) || 0);
                setWatchlistShowIds(watchlistIds || []);
                (watchlistIds || []).forEach(ensureWatchlistShowInfo);

                setLikedShows(likedIds || []);
                (likedIds || []).forEach(it => ensureWatchlistShowInfo(it.showId));

                setDiaryEntries(diaryList || []);
                (diaryList || []).forEach(e => ensureWatchlistShowInfo(e.showId));

                setSuggestions(suggestionslist || []);
                setActivities((activityList || []).sort((a, b) => b.id - a.id));

                if (isOwnProfile) {
                    setPending(pendingList || []);
                    setSent(sentlist || []);
                    setRecommendations(recsList || []);
                } else {
                    setPending([]);
                    setSent([]);
                    setRecommendations([]);
                }
            })
            .catch(err => console.error("Error loading profile data:", err))
            .finally(() => setLoading(false));
    };


    // Mirrors ListDetailPage's per-show info fetch: the watchlist endpoint
    // only returns raw showIds, so poster/title come from a per-id lookup,
    // cached so repeat renders/tab switches don't refetch.
    const requestedWatchlistIds = useRef(new Set());
    const ensureWatchlistShowInfo = useCallback((showId) => {
        if (!showId || requestedWatchlistIds.current.has(showId)) return;
        requestedWatchlistIds.current.add(showId);
        fetch(`https://localhost:8443/api/shows/${showId}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                setWatchlistInfo(prev => ({
                    ...prev,
                    [showId]: {
                        name: data?.name || data?.title || `Show #${showId}`,
                        poster_path: data?.poster_path || null,
                        year: (data?.first_air_date || data?.release_date || '').slice(0, 4),
                        genres: (data?.genres || []).map(g => g.name),
                    },
                }));
            })
            .catch(() => {});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token]);

    // Network tab: მეგობრის ბოლოს ნანახი შოუების პოსტერები (Diary-დან, უახლესი პირველი)
    const requestedFriendPosters = useRef(new Set());
    const ensureFriendPosters = useCallback((friendUsername) => {
        if (!friendUsername || requestedFriendPosters.current.has(friendUsername)) return;
        requestedFriendPosters.current.add(friendUsername);
        fetch(`https://localhost:8443/api/tracking/diary?username=${friendUsername}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : []))
            .then(data => {
                const ids = [...new Set((data || []).map(e => e.showId))].slice(0, NETWORK_POSTER_COUNT);
                setFriendPosterIds(prev => ({ ...prev, [friendUsername]: ids }));
                ids.forEach(ensureWatchlistShowInfo);
            })
            .catch(() => {});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token, ensureWatchlistShowInfo]);

    useEffect(() => {
        friends.forEach(ensureFriendPosters);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [friends]);

    const handleRemoveFromWatchlist = (showId) => {
        if (!username) return;
        fetch(`https://localhost:8443/api/tracking/show-status?username=${username}&showId=${showId}`, {
            method: 'POST',
            headers: authHeaders,
        })
            .then(r => {
                if (!r.ok) throw new Error('Could not remove from watchlist.');
                setWatchlistShowIds(prev => prev.filter(id => id !== showId));
            })
            .catch(err => console.error('Error removing from watchlist:', err));
    };

    // ახალ პროფილზე გადასვლისას (network-დან სხვისი პროფილის გახსნისას):
    // 1) ყოველთვის "Profile" ტაბით დავიწყოთ; 2) ძველი პროფილის მონაცემი გავასუფთაოთ,
    // რომ ახალის ჩატვირთვამდე წამით არ გამოჩნდეს.
    useEffect(() => {
        setActiveTab('profile');
        setActivities([]);
        setFriends([]);
        setFriendsCount(0);
        setWatchlistShowIds([]);
        setDiaryEntries([]);
        setPending([]);
        setSent([]);
        setRecommendations([]);
    }, [routeUsername]);

    useEffect(() => {
        if (!username) {
            setLoading(false);
            return;
        }
        loadAll();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [username]);

    const handleSendRequest = (targetUsername) => {
        const recipient = (targetUsername || addUsername).trim();
        if (!recipient || !currentUsername) return;
        setPanelMessage('');
        // ყოველთვის შემხედველის (currentUsername) სახელით იგზავნება — თუნდაც
        // "People you may know" სხვისი პროფილიდან იყოს გახსნილი
        fetch(`${FRIENDS_BASE_URL}/request?actingUsername=${currentUsername}&targetUsername=${recipient}`, {
            method: 'POST',
            headers: authHeaders,
        })
            .then(r => {
                if (!r.ok) throw new Error('Could not send request.');
                setAddUsername('');
                loadAll();
            })
            .catch(() => setPanelMessage('Could not send that request.'));
    };

    const handleAccept = (requestId) => {
        fetch(`${FRIENDS_BASE_URL}/${requestId}/accept?actingUsername=${username}`, {
            method: 'POST',
            headers: authHeaders,
        }).then(() => loadAll());
    };

    const handleDecline = (requestId) => {
        fetch(`${FRIENDS_BASE_URL}/${requestId}/decline?actingUsername=${username}`, {
            method: 'POST',
            headers: authHeaders,
        }).then(() => loadAll());
    };

    // ── Diary: მონაცემს ვამდიდრებთ TMDB info-თი, ვფილტრავთ და ვასორტირებთ ──
    const MONTHS_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const decadeOf = (releasedYear) => (releasedYear ? `${Math.floor(Number(releasedYear) / 10) * 10}s` : null);
    // "2026-06-27" → "27 Jun 2026"
    const formatDiaryDate = (wd) => {
        if (!wd || wd.length < 10) return '';
        const day = parseInt(wd.slice(8, 10), 10);
        const mon = MONTHS_SHORT[parseInt(wd.slice(5, 7), 10) - 1] || '';
        return `${day} ${mon} ${wd.slice(0, 4)}`;
    };

    const diaryDisplay = diaryEntries.map(e => {
        const info = watchlistInfo[e.showId] || {};
        return {
            ...e,
            title: info.name || `Show #${e.showId}`,
            poster_path: info.poster_path || null,
            released: info.year || '',
            genres: info.genres || [],
        };
    });

    const diaryYears = [...new Set(diaryDisplay.map(d => (d.watchDate || '').slice(0, 4)).filter(Boolean))].sort().reverse();
    const diaryDecades = [...new Set(diaryDisplay.map(d => decadeOf(d.released)).filter(Boolean))].sort().reverse();
    const diaryGenres = [...new Set(diaryDisplay.flatMap(d => d.genres))].sort();

    const visibleDiary = diaryDisplay
        .filter(d => !diaryYear || (d.watchDate || '').slice(0, 4) === diaryYear)
        .filter(d => !diaryDecade || decadeOf(d.released) === diaryDecade)
        .filter(d => !diaryGenre || d.genres.includes(diaryGenre))
        .sort((a, b) => {
            if (diarySort === 'date-asc') return (a.watchDate || '').localeCompare(b.watchDate || '');
            if (diarySort === 'rating-desc') return (b.rating || 0) - (a.rating || 0);
            if (diarySort === 'rating-asc') return (a.rating || 0) - (b.rating || 0);
            return (b.watchDate || '').localeCompare(a.watchDate || ''); // date-desc (default)
        });

    // სხვისი diary-ჩანაწერის (რევიუს) ლაიქის toggle
    const handleDiaryReviewLike = (entry) => {
        if (!currentUsername || !entry.reviewId) return;
        fetch(`https://localhost:8443/api/reviews/like?username=${currentUsername}&reviewType=${entry.reviewType}&reviewId=${entry.reviewId}`, {
            method: 'POST',
            headers: authHeaders,
        })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                if (!data) return;
                setDiaryEntries(prev => prev.map(e =>
                    (e.reviewId === entry.reviewId && e.reviewType === entry.reviewType)
                        ? { ...e, likeCount: data.likeCount, likedByMe: data.liked }
                        : e
                ));
            })
            .catch(err => console.error("Diary review like failed:", err));
    };
    // ── Likes: მოწონებულ შოუებს ვამდიდრებთ TMDB info-თი, ვფილტრავთ და ვასორტირებთ ──
    const likesDisplay = likedShows.map((it, idx) => {
        const info = watchlistInfo[it.showId] || {};
        return {
            showId: it.showId,
            rating: it.rating || 0,
            likedOrder: idx, // 0 = ბოლოს მოწონებული (backend-ის id DESC რიგი)
            poster_path: info.poster_path || null,
            name: info.name || '',
            released: info.year || '',
            genres: info.genres || [],
        };
    });

    const likesDecades = [...new Set(likesDisplay.map(d => decadeOf(d.released)).filter(Boolean))].sort().reverse();
    const likesGenres = [...new Set(likesDisplay.flatMap(d => d.genres))].sort();

    const visibleLikes = likesDisplay
        .filter(d => !likesDecade || decadeOf(d.released) === likesDecade)
        .filter(d => !likesGenre || d.genres.includes(likesGenre))
        .sort((a, b) => {
            if (likesSort === 'liked-asc') return b.likedOrder - a.likedOrder;
            if (likesSort === 'rating-desc') return b.rating - a.rating;
            if (likesSort === 'rating-asc') return a.rating - b.rating;
            return a.likedOrder - b.likedOrder; // liked-desc (ბოლოს მოწონებული პირველი) — default
        });

    if (!username) {
        return (
            <div className="pp-page">
                <main className="pp-main">
                    <p className="pp-empty-state">Log in to view your profile.</p>
                </main>
            </div>
        );
    }

    return (
        <div className="pp-page">
            <main className="pp-main">
                <section className="pp-header-section">
                    <div className="pp-header-left">
                        <Avatar name={username} size={110} />
                        <div className="pp-header-info">
                            <h1 className="pp-display-name">{username}</h1>
                            {isOwnProfile && (
                                <div className="pp-header-actions">
                                    <button className="pp-edit-btn">Edit Profile</button>
                                    <button className="pp-more-btn" aria-label="More options">&hellip;</button>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="pp-stats">
                        <div className="pp-stat-item">
                            <span className="pp-stat-num">{PLACEHOLDER_FILMS_COUNT}</span>
                            <span className="pp-stat-label">TV Shows</span>
                        </div>
                        <div className="pp-stat-item">
                            <span className="pp-stat-num">{loading ? '–' : friendsCount}</span>
                            <span className="pp-stat-label">Friends</span>
                        </div>
                    </div>
                </section>

                <nav className="pp-tabs">
                    <span
                        className={`pp-tab ${activeTab === 'profile' ? 'pp-tab-active' : ''}`}
                        onClick={() => setActiveTab('profile')}
                    >
                        Profile
                    </span>
                    <span
                        className={`pp-tab ${activeTab === 'favorites' ? 'pp-tab-active' : ''}`}
                        onClick={() => setActiveTab('favorites')}
                    >
                        Favorites
                    </span>
                    <span
                        className={`pp-tab ${activeTab === 'diary' ? 'pp-tab-active' : ''}`}
                        onClick={() => setActiveTab('diary')}
                    >
                        Diary
                    </span>
                    <span
                        className={`pp-tab ${activeTab === 'likes' ? 'pp-tab-active' : ''}`}
                        onClick={() => setActiveTab('likes')}
                    >
                        Likes
                    </span>
                    <span
                        className={`pp-tab ${activeTab === 'watchlist' ? 'pp-tab-active' : ''}`}
                        onClick={() => setActiveTab('watchlist')}
                    >
                        Watchlist
                    </span>
                    <span
                        className={`pp-tab ${activeTab === 'network' ? 'pp-tab-active' : ''}`}
                        onClick={() => setActiveTab('network')}
                    >
                        Network
                    </span>
                </nav>

                {activeTab === 'profile' ? (
                    <div className="pp-body">
                        <div className="pp-body-left">
                            <section className="pp-block">
                                <div className="pp-block-header">
                                    <span className="pp-block-title">Favorite TV Shows</span>
                                </div>
                                <p className="pp-favorite-empty">
                                    Don&apos;t forget to select your <strong>favorite TV shows</strong>!
                                </p>
                            </section>

                            {isOwnProfile && (
                                <section className="pp-block">
                                    <div className="pp-block-header">
                                        <span className="pp-block-title">Recommended by Friends</span>
                                    </div>
                                    {recommendations.length === 0 ? (
                                        <p className="pp-favorite-empty">No recommendations from friends yet.</p>
                                    ) : (
                                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '12px', marginTop: '12px' }}>
                                            {recommendations.map(rec => (
                                                <div
                                                    key={rec.id}
                                                    onClick={() => navigate(`/shows/${rec.showId}`)}
                                                    style={{
                                                        background: 'rgba(255, 255, 255, 0.03)',
                                                        border: '1px solid rgba(0, 255, 213, 0.1)',
                                                        borderRadius: '8px',
                                                        padding: '12px',
                                                        cursor: 'pointer',
                                                        transition: 'all 0.2s ease-in-out'
                                                    }}
                                                    onMouseEnter={(e) => {
                                                        e.currentTarget.style.borderColor = '#00ffd5';
                                                        e.currentTarget.style.background = 'rgba(0, 255, 213, 0.03)';
                                                    }}
                                                    onMouseLeave={(e) => {
                                                        e.currentTarget.style.borderColor = 'rgba(0, 255, 213, 0.1)';
                                                        e.currentTarget.style.background = 'rgba(255, 255, 255, 0.03)';
                                                    }}
                                                >
                                                <span style={{ fontSize: '11px', color: '#8b949e', display: 'block', marginBottom: '4px' }}>
                                                    From: <strong style={{ color: '#fff' }}>{rec.senderUsername}</strong>
                                                </span>
                                                    <h4 style={{ margin: '0 0 6px 0', fontSize: '14px', color: '#00ffd5', fontWeight: '500' }}>
                                                        {rec.showName}
                                                    </h4>
                                                    {rec.comment && (
                                                        <div
                                                            style={{
                                                                maxHeight: '54px',
                                                                overflowY: 'auto',
                                                                margin: '4px 0 0 0',
                                                                paddingLeft: '6px',
                                                                borderLeft: '2px solid rgba(0, 255, 213, 0.4)',
                                                            }}
                                                            className="rec-comment-scroll"
                                                        >
                                                            <p
                                                                style={{
                                                                    margin: '0',
                                                                    fontSize: '11px',
                                                                    color: '#c9d1d9',
                                                                    fontStyle: 'italic',
                                                                    wordBreak: 'break-word',
                                                                    overflowWrap: 'break-word',
                                                                    lineHeight: '18px'
                                                                }}
                                                            >
                                                                "{rec.comment}"
                                                            </p>
                                                        </div>
                                                    )}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </section>
                            )}

                            <section className="pp-block">
                                <div className="pp-block-header">
                                    <span className="pp-block-title">Recent Activity</span>
                                    <span className="pp-block-all" onClick={() => setActiveTab('diary')} style={{ cursor: 'pointer' }}>All</span>
                                </div>
                                <div className="pp-activity-grid">
                                    {(() => {
                                        if (!activities || activities.length === 0) {
                                            return <p className="pp-favorite-empty" style={{ gridColumn: '1/-1' }}>No recent activity.</p>;
                                        }

                                        const mergedMap = new Map();

                                        activities.forEach((act) => {
                                            const id = act.showId;
                                            const detailText = act.detail || "";
                                            const currentAction = act.actionType || "";

                                            if (!mergedMap.has(id)) {
                                                const isStatusAction = ['WATCHING', 'COMPLETED', 'DROPPED', 'PLAN_TO_WATCH'].includes(currentAction);

                                                mergedMap.set(id, {
                                                    id: act.id,
                                                    showId: id,
                                                    showName: act.showName,
                                                    posterPath: act.posterPath,
                                                    rating: act.rating || (detailText.includes("Rated") ? parseInt(detailText.match(/Rated (\d)/)?.[1] || 0, 10) : 0),
                                                    isLiked: currentAction === 'LIKED' || detailText.toLowerCase().includes('favorite') || detailText.includes('❤️'),
                                                    status: isStatusAction ? currentAction : ""
                                                });
                                            }

                                            const currentObj = mergedMap.get(id);

                                            if (!currentObj.posterPath && act.posterPath) {
                                                currentObj.posterPath = act.posterPath;
                                            }

                                            if (currentAction === 'LIKED' || detailText.toLowerCase().includes('favorite') || detailText.includes('❤️')) {
                                                currentObj.isLiked = true;
                                            }

                                            if (act.rating && !currentObj.rating) {
                                                currentObj.rating = act.rating;
                                            } else if (detailText.includes("Rated") && !currentObj.rating) {
                                                currentObj.rating = parseInt(detailText.match(/Rated (\d)/)?.[1] || 0, 10);
                                            }

                                            let incomingStatus = "";
                                            if (currentAction === 'COMPLETED' || detailText.includes('COMPLETED') || detailText.includes('Completed')) {
                                                incomingStatus = "COMPLETED";
                                            } else if (currentAction === 'WATCHING' || detailText.includes('WATCHING') || detailText.includes('Watching')) {
                                                incomingStatus = "WATCHING";
                                            } else if (currentAction === 'DROPPED' || detailText.includes('DROPPED') || detailText.includes('Dropped')) {
                                                incomingStatus = "DROPPED";
                                            } else if (currentAction === 'PLAN_TO_WATCH' || detailText.includes('PLAN_TO_WATCH') || detailText.includes('Plan')) {
                                                incomingStatus = "PLAN_TO_WATCH";
                                            }

                                            if (incomingStatus) {
                                                if (!currentObj.status) {
                                                    currentObj.status = incomingStatus;
                                                }
                                            }
                                        });

                                        const uniqueActivities = Array.from(mergedMap.values()).slice(0, 4);

                                        return uniqueActivities.map((act) => {
                                            const posterUrl = act.posterPath
                                                ? `https://image.tmdb.org/t/p/w300${act.posterPath}`
                                                : null;

                                            let statusLabel = "";
                                            let statusClass = "";

                                            if (act.status === 'COMPLETED') {
                                                statusLabel = "✔ Watched";
                                                statusClass = "status-completed";
                                            } else if (act.status === 'WATCHING') {
                                                statusLabel = "👁 Watching";
                                                statusClass = "status-watching";
                                            } else if (act.status === 'DROPPED') {
                                                statusLabel = "✕ Dropped";
                                                statusClass = "status-dropped";
                                            } else if (act.status === 'PLAN_TO_WATCH') {
                                                statusLabel = "⏳ Plan";
                                                statusClass = "status-plan";
                                            }

                                            return (
                                                <div key={act.id} className="pp-activity-poster" onClick={() => navigate(`/shows/${act.showId}`)}>
                                                    <div className="pp-poster-placeholder-lg">
                                                        {posterUrl ? (
                                                            <img src={posterUrl} alt={act.showName} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                        ) : (
                                                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '8px', textAlign: 'center', fontSize: '11px', color: '#9aabbc', fontWeight: 'bold', height: '100%' }}>
                                                                {act.showName}
                                                            </div>
                                                        )}

                                                        <div className="pp-poster-title-hover">
                                                            {act.showName}
                                                        </div>
                                                    </div>

                                                    <div className="pp-poster-footer-info" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px', marginTop: '10px', width: '100%' }}>
                                                        {statusLabel && (
                                                            <div className="status-row" style={{ display: 'flex', justifyContent: 'center', width: '100%' }}>
                                                                <span className={`pp-status-badge ${statusClass}`}>
                                                                    {statusLabel}
                                                                </span>
                                                            </div>
                                                        )}

                                                        {(act.isLiked || act.rating > 0) && (
                                                            <div className="meta-row" style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: '8px', width: '100%', minHeight: '20px' }}>
                                                                {act.rating > 0 && (
                                                                    <span className="pp-poster-stars" style={{ color: '#ffb400', letterSpacing: '1px' }}>
                                                                        {'★'.repeat(Math.round(act.rating))}
                                                                    </span>
                                                                )}

                                                                {act.isLiked && (
                                                                    <span className="pp-poster-like" style={{ display: 'inline-flex', alignItems: 'center' }}>❤️</span>
                                                                )}
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            );
                                        });
                                    })()}
                                </div>
                            </section>
                        </div>

                        {isOwnProfile && (
                            <div className="pp-body-right">
                                <section className="pp-block">
                                    <div className="pp-block-header">
                                        <span className="pp-block-title">Activity</span>
                                    </div>
                                    <div className="pp-activity-feed">
                                        {activities.length === 0 ? (
                                            <p className="pp-favorite-empty">No logged interactions yet.</p>
                                        ) : (
                                            activities.slice(0, 15).map((act) => {
                                                const dateObj = new Date(act.createdAt);
                                                const timeLabel = dateObj.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

                                                return (
                                                    <div key={act.id} className="pp-activity-row" onClick={() => navigate(`/shows/${act.showId}`)} style={{ cursor: 'pointer' }}>
                                                        <span className="pp-activity-dot" style={{ backgroundColor: act.actionType === 'LIKED' ? '#ff3a44' : '#00ffd5' }} />
                                                        <span className="pp-activity-text">
                                                        You <strong>{act.actionType.toLowerCase().replace('_', ' ')}</strong> <span style={{ color: '#00ffd5' }}>{act.showName}</span> ({act.detail})
                                                    </span>
                                                        <span className="pp-activity-time">{timeLabel}</span>
                                                    </div>
                                                );
                                            })
                                        )}
                                    </div>
                                </section>
                            </div>
                        )}
                    </div>
                ) : activeTab === 'favorites' ? (
                    <div className="pp-favorites">
                        <div className="pp-favorites-header">
                            <h2 className="pp-favorites-title">Favorites</h2>
                            <div className="pp-favorites-actions">
                                <button className="pp-fav-icon-btn" aria-label="Download">⬇</button>
                                <button className="pp-fav-icon-btn" aria-label="Share">↗</button>
                                <button className="pp-fav-all-btn">All Favorites</button>
                            </div>
                        </div>

                        <section className="pp-fav-section">
                            <div className="pp-fav-section-title">Movies ({FAVORITE_MOVIES_COUNT})</div>
                            <div className="pp-fav-grid">
                                {Array.from({ length: FAVORITE_MOVIES_COUNT }).map((_, i) => (
                                    <div key={i} className="pp-fav-poster" />
                                ))}
                            </div>
                        </section>

                        <section className="pp-fav-section">
                            <div className="pp-fav-section-title">Actors ({FAVORITE_ACTORS_COUNT})</div>
                            <div className="pp-fav-grid">
                                {Array.from({ length: FAVORITE_ACTORS_COUNT }).map((_, i) => (
                                    <div key={i} className="pp-fav-actor" />
                                ))}
                            </div>
                        </section>
                    </div>
                ) : activeTab === 'diary' ? (
                    <div className="pp-diary">
                        <div className="pp-diary-header">
                            <h2 className="pp-diary-title">Diary</h2>
                            <div className="pp-diary-filters">
                                <span
                                    className="pp-diary-filter"
                                    style={{ cursor: 'pointer', color: diarySort.startsWith('rating') ? '#00b4a2' : undefined }}
                                    onClick={() => setDiarySort(diarySort === 'rating-desc' ? 'rating-asc' : 'rating-desc')}
                                >
                                    Rating {diarySort === 'rating-asc' ? '▴' : '▾'}
                                </span>
                                <DiaryDropdown label="Year" value={diaryYear} options={diaryYears} onChange={setDiaryYear} />
                                <DiaryDropdown label="Decade" value={diaryDecade} options={diaryDecades} onChange={setDiaryDecade} />
                                <DiaryDropdown label="Genre" value={diaryGenre} options={diaryGenres} onChange={setDiaryGenre} />
                                <span
                                    className="pp-diary-filter"
                                    style={{ cursor: 'pointer', color: diarySort.startsWith('date') ? '#00b4a2' : undefined }}
                                    onClick={() => setDiarySort(diarySort === 'date-desc' ? 'date-asc' : 'date-desc')}
                                >
                                    Sort by Watched Date {diarySort === 'date-asc' ? '▴' : '▾'}
                                </span>
                            </div>
                        </div>

                        {visibleDiary.length === 0 ? (
                            <div className="pp-likes-empty-box">
                                <p className="pp-likes-empty-text">
                                    {diaryEntries.length === 0 ? 'No diary entries yet' : 'Nothing matches these filters'}
                                </p>
                            </div>
                        ) : (
                            <div className="pp-diary-table">
                                <div className="pp-diary-row pp-diary-row-head">
                                    <span className="pp-diary-col-date">Date</span>
                                    <span className="pp-diary-col-film">Show</span>
                                    <span className="pp-diary-col-released">Released</span>
                                    <span className="pp-diary-col-rating">Rating</span>
                                    <span className="pp-diary-col-like">Like</span>
                                    <span className="pp-diary-col-rewatch">Rewatch</span>
                                    <span className="pp-diary-col-review">Review</span>
                                    <span className="pp-diary-col-edit">{isOwnProfile ? 'Edit' : 'Like'}</span>
                                </div>

                                {visibleDiary.map((entry, i) => {
                                    const wd = entry.watchDate || '';
                                    const rating = entry.rating || 0;
                                    const epLabel = (entry.seasonNumber != null && entry.episodeNumber != null)
                                        ? ` S${entry.seasonNumber}E${entry.episodeNumber}` : '';
                                    return (
                                        <div key={`${entry.showId}-${entry.seasonNumber}-${entry.episodeNumber}-${wd}-${i}`} className="pp-diary-row">
                                            <span className="pp-diary-col-date">{formatDiaryDate(wd)}</span>
                                            <span className="pp-diary-col-film pp-diary-film-cell">
                                                {entry.poster_path ? (
                                                    <img
                                                        src={`${POSTER_BASE}${entry.poster_path}`}
                                                        alt={entry.title}
                                                        className="pp-diary-poster"
                                                        style={{ cursor: 'pointer', objectFit: 'cover' }}
                                                        onClick={() => navigate(`/shows/${entry.showId}`)}
                                                    />
                                                ) : (
                                                    <span className="pp-diary-poster" />
                                                )}
                                                <span
                                                    className="pp-diary-film-title"
                                                    style={{ cursor: 'pointer' }}
                                                    onClick={() => navigate(`/shows/${entry.showId}`)}
                                                >
                                                    {entry.title}{epLabel}
                                                </span>
                                            </span>
                                            <span className="pp-diary-col-released">{entry.released}</span>
                                            <span className="pp-diary-col-rating pp-diary-stars">
                                                <span className="pp-diary-stars-filled">{'★'.repeat(rating)}</span>
                                                <span className="pp-diary-stars-empty">{'★'.repeat(5 - rating)}</span>
                                            </span>
                                            <span className="pp-diary-col-like">
                                                {entry.liked && (
                                                    <span className="pp-diary-heart pp-diary-heart-filled">
                                                        <HeartIcon />
                                                    </span>
                                                )}
                                            </span>
                                            <span className="pp-diary-col-rewatch">{entry.rewatch ? '↻' : ''}</span>
                                            <span className="pp-diary-col-review" title={entry.review || ''}>
                                                {entry.review || ''}
                                            </span>
                                            <span className="pp-diary-col-edit">
                                                {isOwnProfile ? (
                                                    <span
                                                        className="pp-diary-edit-icon"
                                                        style={{ cursor: 'pointer' }}
                                                        title="Edit review"
                                                        onClick={() => setEditingEntry(entry)}
                                                    >
                                                        ✎
                                                    </span>
                                                ) : (
                                                    <span
                                                        className="pp-diary-like-cell"
                                                        title={entry.likedByMe ? 'Unlike' : 'Like this review'}
                                                    >
                                                        <span
                                                            className="pp-diary-like-heart"
                                                            style={{ cursor: 'pointer', color: entry.likedByMe ? '#e85d75' : '#5f758a' }}
                                                            onClick={() => handleDiaryReviewLike(entry)}
                                                        >
                                                            {entry.likedByMe ? '♥' : '♡'}
                                                        </span>
                                                        <span className="pp-diary-like-count">{entry.likeCount || 0}</span>
                                                    </span>
                                                )}
                                            </span>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                ) : activeTab === 'likes' ? (
                    <div className="pp-likes">
                        <div className="pp-likes-subtabs">
                            {LIKES_SUBTABS.map((t) => (
                                <span
                                    key={t}
                                    className={`pp-likes-subtab ${likesSubTab === t ? 'pp-likes-subtab-active' : ''}`}
                                    onClick={() => setLikesSubTab(t)}
                                >
                                    {t}
                                </span>
                            ))}
                        </div>

                        {likesSubTab === 'Films' && (
                            <div className="pp-likes-filters-row">
                                <div className="pp-likes-filters">
                                    <span
                                        className="pp-diary-filter"
                                        style={{ cursor: 'pointer', color: likesSort.startsWith('rating') ? '#00b4a2' : undefined }}
                                        onClick={() => setLikesSort(likesSort === 'rating-desc' ? 'rating-asc' : 'rating-desc')}
                                    >
                                        Rating {likesSort === 'rating-asc' ? '▴' : '▾'}
                                    </span>
                                    <DiaryDropdown label="Decade" value={likesDecade} options={likesDecades} onChange={setLikesDecade} />
                                    <DiaryDropdown label="Genre" value={likesGenre} options={likesGenres} onChange={setLikesGenre} />
                                    <span
                                        className="pp-diary-filter"
                                        style={{ cursor: 'pointer', color: likesSort.startsWith('liked') ? '#00b4a2' : undefined }}
                                        onClick={() => setLikesSort(likesSort === 'liked-desc' ? 'liked-asc' : 'liked-desc')}
                                    >
                                        Sort by When Liked {likesSort === 'liked-asc' ? '▴' : '▾'}
                                    </span>
                                </div>
                            </div>
                        )}

                        {likesSubTab === 'Films' ? (
                            visibleLikes.length === 0 ? (
                                <div className="pp-likes-empty-box">
                                    <p className="pp-likes-empty-text">
                                        {likedShows.length === 0 ? 'No liked films yet' : 'Nothing matches these filters'}
                                    </p>
                                </div>
                            ) : (
                                <div className="pp-watchlist-poster-grid">
                                    {visibleLikes.map(({ showId }) => {
                                        const info = watchlistInfo[showId];
                                        return (
                                            <div key={showId} className="pp-watchlist-poster-item">
                                                {info?.poster_path ? (
                                                    <img
                                                        src={`${POSTER_BASE}${info.poster_path}`}
                                                        alt={info?.name || ''}
                                                        className="pp-watchlist-poster"
                                                        onClick={() => navigate(`/shows/${showId}`)}
                                                    />
                                                ) : (
                                                    <div
                                                        className="pp-watchlist-poster pp-watchlist-poster-placeholder"
                                                        onClick={() => navigate(`/shows/${showId}`)}
                                                    />
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            )
                        ) : (
                            <div className="pp-likes-empty-box">
                                <p className="pp-likes-empty-text">No {likesSubTab.toLowerCase()} yet</p>
                            </div>
                        )}
                    </div>
                ) : activeTab === 'watchlist' ? (
                    <div className="pp-watchlist-solo">
                        <div className="pp-watchlist-title">You want to see {watchlistShowIds.length} TV show{watchlistShowIds.length === 1 ? '' : 's'}</div>

                        {watchlistShowIds.length === 0 ? (
                            <div className="pp-likes-empty-box">
                                <p className="pp-likes-empty-text">No TV shows yet</p>
                            </div>
                        ) : (
                            <div className="pp-watchlist-poster-grid">
                                {watchlistShowIds.map((showId) => {
                                    const info = watchlistInfo[showId];
                                    return (
                                        <div key={showId} className="pp-watchlist-poster-item">
                                            {info?.poster_path ? (
                                                <img
                                                    src={`${POSTER_BASE}${info.poster_path}`}
                                                    alt={info?.name || ''}
                                                    className="pp-watchlist-poster"
                                                    onClick={() => navigate(`/shows/${showId}`)}
                                                />
                                            ) : (
                                                <div
                                                    className="pp-watchlist-poster pp-watchlist-poster-placeholder"
                                                    onClick={() => navigate(`/shows/${showId}`)}
                                                />
                                            )}
                                            {isOwnProfile && (
                                                <button
                                                    type="button"
                                                    className="pp-watchlist-remove-btn"
                                                    title="Remove from watchlist"
                                                    onClick={() => handleRemoveFromWatchlist(showId)}
                                                >
                                                    ✕
                                                </button>
                                            )}
                                            <div className="pp-watchlist-poster-caption">
                                                <span className="pp-watchlist-poster-title">{info?.name || `Show #${showId}`}</span>
                                                {info?.year && <span className="pp-watchlist-poster-year">{info.year}</span>}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="pp-network">
                        <div className="pp-network-row">
                            <section className="pp-block pp-network-col">
                                <div className="pp-kicker">{isOwnProfile ? 'Your Network' : `${username}'s Network`}</div>
                                {friends.length === 0 ? (
                                    <div className="pp-section-empty">
                                        {isOwnProfile ? "You haven't added any friends yet." : `${username} hasn't added any friends yet.`}
                                    </div>
                                ) : (
                                    <div className="pp-network-scroll">
                                        {friends.map((name) => {
                                            const posterIds = friendPosterIds[name] || [];
                                            return (
                                                <div key={name} className="pp-network-card">
                                                    <div
                                                        className="pp-network-card-top"
                                                        style={{ cursor: 'pointer' }}
                                                        onClick={() => navigate(`/profile/${name}`)}
                                                        title={`View ${name}'s profile`}
                                                    >
                                                        <Avatar name={name} size={64} />
                                                        <span className="pp-network-name">{name}</span>
                                                        <span className="pp-network-stats">
                                                            {NETWORK_FILM_COUNT} TV shows &bull; {NETWORK_REVIEW_COUNT} reviews
                                                        </span>
                                                    </div>
                                                    <div className="pp-poster-row">
                                                        {Array.from({ length: NETWORK_POSTER_COUNT }).map((_, i) => {
                                                            const showId = posterIds[i];
                                                            const info = showId ? watchlistInfo[showId] : null;
                                                            return info?.poster_path ? (
                                                                <img
                                                                    key={i}
                                                                    src={`${POSTER_BASE}${info.poster_path}`}
                                                                    alt={info?.name || ''}
                                                                    className="pp-poster-real"
                                                                    onClick={() => navigate(`/shows/${showId}`)}
                                                                />
                                                            ) : (
                                                                <div key={i} className="pp-poster-placeholder" />
                                                            );
                                                        })}
                                                    </div>
                                                </div>
                                            );
                                        })}
                                    </div>
                                )}
                            </section>

                            {isOwnProfile && (
                                <section className="pp-block pp-requests-col">
                                    <div className="pp-kicker">Requests</div>
                                    <div className="pp-requests-tabs">
                                        <button
                                            type="button"
                                            className={`pp-tab-btn ${requestsTab === 'pending' ? 'pp-tab-btn-active' : ''}`}
                                            onClick={() => setRequestsTab('pending')}
                                        >
                                            Pending ({pending.length})
                                        </button>
                                        <button
                                            type="button"
                                            className={`pp-tab-btn ${requestsTab === 'sent' ? 'pp-tab-btn-active' : ''}`}
                                            onClick={() => setRequestsTab('sent')}
                                        >
                                            Sent ({sent.length})
                                        </button>
                                    </div>

                                    <form
                                        className="pp-add-form"
                                        onSubmit={(e) => { e.preventDefault(); handleSendRequest(); }}
                                    >
                                        <input
                                            className="pp-add-input"
                                            placeholder="Add by username"
                                            value={addUsername}
                                            onChange={(e) => setAddUsername(e.target.value)}
                                        />
                                        <button type="submit" className="pp-add-btn">Send</button>
                                    </form>

                                    {panelMessage && <div className="pp-panel-message">{panelMessage}</div>}

                                    <div className="pp-requests-list">
                                        {requestsTab === 'pending' ? (
                                            pending.length === 0 ? (
                                                <div className="pp-panel-empty">No incoming requests.</div>
                                            ) : (
                                                pending.map((req) => (
                                                    <div key={req.id} className="pp-panel-row">
                                                        <Avatar name={req.requesterUsername} size={28} />
                                                        <span className="pp-panel-row-name">{req.requesterUsername}</span>
                                                        <div className="pp-panel-row-actions">
                                                            <button className="pp-mini-btn pp-mini-accept" onClick={() => handleAccept(req.id)}>Accept</button>
                                                            <button className="pp-mini-btn pp-mini-decline" onClick={() => handleDecline(req.id)}>Decline</button>
                                                        </div>
                                                    </div>
                                                ))
                                            )
                                        ) : sent.length === 0 ? (
                                            <div className="pp-panel-empty">No sent requests.</div>
                                        ) : (
                                            sent.map((req) => (
                                                <div key={req.id} className="pp-panel-row">
                                                    <Avatar name={req.recipientUsername} size={28} />
                                                    <span className="pp-panel-row-name">{req.recipientUsername}</span>
                                                    <span className="pp-pill">awaiting</span>
                                                </div>
                                            ))
                                        )}
                                    </div>
                                </section>
                            )}
                        </div>

                        {currentUsername && (
                            <section className="pp-block">
                                <div className="pp-kicker">People you may know</div>
                                {suggestions.length === 0 ? (
                                    <div className="pp-section-empty">No suggestions right now.</div>
                                ) : (
                                    <div className="pp-suggestions-list">
                                        {suggestions.map((s) => (
                                            <div key={s.username} className="pp-suggestion-row">
                                                <div
                                                    className="pp-suggestion-clickable"
                                                    style={{ cursor: 'pointer' }}
                                                    onClick={() => navigate(`/profile/${s.username}`)}
                                                    title={`View ${s.username}'s profile`}
                                                >
                                                    <Avatar name={s.username} size={44} />
                                                    <div className="pp-suggestion-info">
                                                        <span className="pp-suggestion-name">{s.username}</span>
                                                        <span className="pp-suggestion-sub">
                                                        {s.mutualFriendCount} mutual friend{s.mutualFriendCount === 1 ? '' : 's'}
                                                    </span>
                                                    </div>
                                                </div>
                                                <button className="pp-add-circle-btn" onClick={() => handleSendRequest(s.username)}>+</button>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </section>
                        )}
                    </div>
                )}
            </main>

            {editingEntry && isOwnProfile && (
                <DiaryEditModal
                    entry={editingEntry}
                    username={currentUsername}
                    token={token}
                    onClose={() => setEditingEntry(null)}
                    onSaved={loadAll}
                />
            )}
        </div>
    );
}
