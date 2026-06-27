import React, { useState, useEffect, useCallback } from 'react';
import '../style/FriendsPage.css';

const FRIENDS_BASE_URL = 'https://localhost:8443/api/friends';

// Fixed placeholder stats. The backend has no concept of "films watched" or
// "reviews written" yet (that lives in a different feature entirely), so
// these are deliberately hardcoded shapes/values until that data exists.
const PLACEHOLDER_FILM_COUNT = 128;
const PLACEHOLDER_REVIEW_COUNT = 34;
const PLACEHOLDER_POSTER_COUNT = 4;
const PLACEHOLDER_STATS = { eye: 1240, grid: 18, heart: 326 };

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
            className="fp-avatar"
            style={{ width: size, height: size, backgroundColor: colorForName(name), fontSize: size * 0.42 }}
        >
            {initial}
        </div>
    );
}

function EyeIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
        </svg>
    );
}

function GridIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="3" width="7" height="7" />
            <rect x="14" y="3" width="7" height="7" />
            <rect x="3" y="14" width="7" height="7" />
            <rect x="14" y="14" width="7" height="7" />
        </svg>
    );
}

function HeartIcon() {
    return (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.8 1-1a5.5 5.5 0 0 0 0-7.8z" />
        </svg>
    );
}

export default function FriendsPage() {
    const [friends, setFriends] = useState([]);
    const [pending, setPending] = useState([]);
    const [sent, setSent] = useState([]);
    const [suggestions, setSuggestions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [requestsTab, setRequestsTab] = useState('pending');
    const [addUsername, setAddUsername] = useState('');
    const [panelMessage, setPanelMessage] = useState('');

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const username = decodedToken?.sub;
    const authHeaders = { Authorization: `Bearer ${token}` };

    const loadAll = useCallback(() => {
        if (!username) {
            setLoading(false);
            return;
        }
        setLoading(true);
        setError('');
        Promise.all([
            fetch(`${FRIENDS_BASE_URL}?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`${FRIENDS_BASE_URL}/pending?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`${FRIENDS_BASE_URL}/sent?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
            fetch(`${FRIENDS_BASE_URL}/suggestions?actingUsername=${username}`, { headers: authHeaders }).then(r => (r.ok ? r.json() : [])),
        ])
            .then(([friendsRes, pendingRes, sentRes, suggestionsRes]) => {
                setFriends(friendsRes || []);
                setPending(pendingRes || []);
                setSent(sentRes || []);
                setSuggestions(suggestionsRes || []);
            })
            .catch(() => setError('Could not load friends data.'))
            .finally(() => setLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [username]);

    useEffect(() => {
        loadAll();
    }, [loadAll]);

    const handleSendRequest = (targetUsername) => {
        if (!username || !targetUsername) return;
        setPanelMessage('');
        fetch(`${FRIENDS_BASE_URL}/request?actingUsername=${username}&targetUsername=${targetUsername}`, {
            method: 'POST',
            headers: authHeaders,
        })
            .then(res => (res.ok ? res.json() : res.text().then(msg => Promise.reject(msg))))
            .then(() => {
                setAddUsername('');
                loadAll();
            })
            .catch((msg) => setPanelMessage(typeof msg === 'string' ? msg : 'Could not send request.'));
    };

    const handleAccept = (id) => {
        fetch(`${FRIENDS_BASE_URL}/${id}/accept?actingUsername=${username}`, { method: 'POST', headers: authHeaders })
            .then(() => loadAll());
    };

    const handleDecline = (id) => {
        fetch(`${FRIENDS_BASE_URL}/${id}/decline?actingUsername=${username}`, { method: 'POST', headers: authHeaders })
            .then(() => loadAll());
    };

    return (
        <div className="fp-page">
            <p className="fp-tagline">Film lovers, critics and friends — find popular members.</p>

            <main className="fp-main">
                {!username ? (
                    <p className="fp-empty-state">Log in to see your friends.</p>
                ) : loading ? (
                    <p className="fp-empty-state">Loading...</p>
                ) : error ? (
                    <p className="fp-empty-state fp-error">{error}</p>
                ) : (
                    <>
                        <div className="fp-top-row">
                            <section className="fp-section fp-network-col">
                                <div className="fp-kicker">Your Network</div>
                                {friends.length === 0 ? (
                                    <div className="fp-section-empty">You haven't added any friends yet.</div>
                                ) : (
                                    <div className="fp-network-scroll">
                                        {friends.map(name => (
                                            <div key={name} className="fp-network-card">
                                                <div className="fp-network-card-top">
                                                    <Avatar name={name} size={64} />
                                                    <span className="fp-network-name">{name}</span>
                                                    <span className="fp-network-stats">
                                                        {PLACEHOLDER_FILM_COUNT} films &bull; {PLACEHOLDER_REVIEW_COUNT} reviews
                                                    </span>
                                                </div>
                                                <div className="fp-poster-row">
                                                    {Array.from({ length: PLACEHOLDER_POSTER_COUNT }).map((_, i) => (
                                                        <div key={i} className="fp-poster-placeholder" />
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </section>

                            <section className="fp-section fp-requests-col">
                                <div className="fp-kicker">Requests</div>

                                <div className="fp-requests-tabs">
                                    <button
                                        className={`fp-tab-btn ${requestsTab === 'pending' ? 'fp-tab-active' : ''}`}
                                        onClick={() => setRequestsTab('pending')}
                                    >
                                        Pending ({pending.length})
                                    </button>
                                    <button
                                        className={`fp-tab-btn ${requestsTab === 'sent' ? 'fp-tab-active' : ''}`}
                                        onClick={() => setRequestsTab('sent')}
                                    >
                                        Sent ({sent.length})
                                    </button>
                                </div>

                                <form
                                    className="fp-add-form"
                                    onSubmit={(e) => { e.preventDefault(); handleSendRequest(addUsername.trim()); }}
                                >
                                    <input
                                        className="fp-add-input"
                                        type="text"
                                        placeholder="Add by username"
                                        value={addUsername}
                                        onChange={(e) => setAddUsername(e.target.value)}
                                    />
                                    <button type="submit" className="fp-add-btn">Send</button>
                                </form>
                                {panelMessage && <div className="fp-panel-message">{panelMessage}</div>}

                                <div className="fp-requests-list">
                                    {requestsTab === 'pending' ? (
                                        pending.length === 0 ? (
                                            <div className="fp-panel-empty">No incoming requests.</div>
                                        ) : (
                                            pending.map(req => (
                                                <div key={req.id} className="fp-panel-row">
                                                    <Avatar name={req.requesterUsername} size={28} />
                                                    <span className="fp-panel-row-name">{req.requesterUsername}</span>
                                                    <div className="fp-panel-row-actions">
                                                        <button className="fp-mini-btn fp-mini-accept" onClick={() => handleAccept(req.id)}>Accept</button>
                                                        <button className="fp-mini-btn fp-mini-decline" onClick={() => handleDecline(req.id)}>Decline</button>
                                                    </div>
                                                </div>
                                            ))
                                        )
                                    ) : (
                                        sent.length === 0 ? (
                                            <div className="fp-panel-empty">No sent requests.</div>
                                        ) : (
                                            sent.map(req => (
                                                <div key={req.id} className="fp-panel-row">
                                                    <Avatar name={req.recipientUsername} size={28} />
                                                    <span className="fp-panel-row-name">{req.recipientUsername}</span>
                                                    <span className="fp-pill">awaiting</span>
                                                </div>
                                            ))
                                        )
                                    )}
                                </div>
                            </section>
                        </div>

                        <section className="fp-section">
                            <div className="fp-kicker">People you may know</div>
                            {suggestions.length === 0 ? (
                                <div className="fp-section-empty">No suggestions right now.</div>
                            ) : (
                                <div className="fp-suggestions-list">
                                    {suggestions.map(s => (
                                        <div key={s.username} className="fp-suggestion-row">
                                            <Avatar name={s.username} size={44} />
                                            <div className="fp-suggestion-info">
                                                <span className="fp-suggestion-name">{s.username}</span>
                                                <span className="fp-suggestion-sub">
                                                    {s.mutualFriendCount} mutual friend{s.mutualFriendCount === 1 ? '' : 's'}
                                                </span>
                                            </div>
                                            <div className="fp-suggestion-stats">
                                                <span className="fp-stat"><EyeIcon /> {PLACEHOLDER_STATS.eye}</span>
                                                <span className="fp-stat"><GridIcon /> {PLACEHOLDER_STATS.grid}</span>
                                                <span className="fp-stat"><HeartIcon /> {PLACEHOLDER_STATS.heart}</span>
                                            </div>
                                            <button
                                                className="fp-add-circle-btn"
                                                onClick={() => handleSendRequest(s.username)}
                                                aria-label={`Add ${s.username}`}
                                            >
                                                +
                                            </button>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </section>
                    </>
                )}
            </main>
        </div>
    );
}
