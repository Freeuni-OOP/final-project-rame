import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import '../style/ListDetailPage.css';

// Full-page view of a single list - reusable for any user's list, not just
// the current user's own (the GET /api/lists/{id} backend endpoint isn't
// owner-scoped). Shows the full description and every show in a numbered
// grid (numbered using the item's stored "position", which the backend
// already tracks end-to-end). If the logged-in user owns this list, a
// pencil icon links through to the edit page.

const LISTS_BASE_URL = 'https://localhost:8443/api/lists';
const SHOWS_BASE_URL = 'https://localhost:8443/api/shows';
const POSTER_BASE = 'https://image.tmdb.org/t/p/w342';

function PencilIcon() {
    return (
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z" />
        </svg>
    );
}

export default function ListDetailPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [list, setList] = useState(null);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const [showInfoCache, setShowInfoCache] = useState({});
    const requestedShowIds = useRef(new Set());

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const username = decodedToken?.sub;
    const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

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
                        year: (data?.first_air_date || data?.release_date || '').slice(0, 4),
                    },
                }));
            })
            .catch(() => {});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token]);

    useEffect(() => {
        setLoading(true);
        setError('');
        fetch(`${LISTS_BASE_URL}/${id}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : Promise.reject('not found')))
            .then(data => {
                setList(data);
                const sortedItems = (data.items || []).slice().sort((a, b) => a.position - b.position);
                setItems(sortedItems);
                sortedItems.forEach(item => ensureShowInfo(item.showId));
            })
            .catch(() => setError('This list could not be found.'))
            .finally(() => setLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const isOwner = username && list?.ownerUsername === username;

    if (loading) {
        return (
            <div className="ldp-page">
                <main className="ldp-main">
                    <p className="ldp-empty">Loading list...</p>
                </main>
            </div>
        );
    }

    if (error || !list) {
        return (
            <div className="ldp-page">
                <main className="ldp-main">
                    <p className="ldp-empty">{error || 'This list could not be found.'}</p>
                    <Link to="/lists" className="ldp-back-link">&larr; Back to Lists</Link>
                </main>
            </div>
        );
    }

    return (
        <div className="ldp-page">
            <main className="ldp-main">
                <Link to="/lists" className="ldp-back-link">&larr; Back to Lists</Link>

                <div className="ldp-header">
                    <div className="ldp-header-top">
                        <h1 className="ldp-title">{list.name}</h1>
                        {isOwner && (
                            <button
                                className="ldp-edit-btn"
                                onClick={() => navigate(`/lists/${id}/edit`)}
                                aria-label="Edit list"
                                title="Edit list"
                            >
                                <PencilIcon />
                            </button>
                        )}
                    </div>
                    <div className="ldp-meta">
                        <span className="ldp-pill">{list.isPublic ? 'Public' : 'Private'}</span>
                        <span className="ldp-stat">A list by <span className="ldp-owner">{list.ownerUsername}</span></span>
                        <span className="ldp-stat">{items.length} {items.length === 1 ? 'show' : 'shows'}</span>
                    </div>
                    {list.description && <p className="ldp-description">{list.description}</p>}
                </div>

                {items.length === 0 && (
                    <p className="ldp-empty">This list doesn't have any shows yet.</p>
                )}

                {items.length > 0 && (
                    <div className="ldp-grid">
                        {items.map((item, i) => {
                            const info = showInfoCache[item.showId];
                            return (
                                <div key={item.id} className="ldp-grid-item">
                                    <span className="ldp-grid-number">{i + 1}</span>
                                    {info?.poster_path ? (
                                        <img
                                            src={`${POSTER_BASE}${info.poster_path}`}
                                            alt={info?.name || ''}
                                            className="ldp-poster"
                                            onClick={() => navigate(`/shows/${item.showId}`)}
                                        />
                                    ) : (
                                        <div
                                            className="ldp-poster ldp-poster-placeholder"
                                            onClick={() => navigate(`/shows/${item.showId}`)}
                                        />
                                    )}
                                    <div className="ldp-grid-caption">
                                        <span className="ldp-grid-title">{info?.name || `Show #${item.showId}`}</span>
                                        {info?.year && <span className="ldp-grid-year">{info.year}</span>}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </main>
        </div>
    );
}
