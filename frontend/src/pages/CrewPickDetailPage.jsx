import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { CREW_PICKS } from './ListsPage.jsx';
import '../style/ListDetailPage.css';

// Full-page view of a single Crew Pick, reached by clicking a pick on the
// Lists page - mirrors ListDetailPage's layout exactly so it feels like a
// real list page, but there's no backend list behind a Crew Pick (it's
// hardcoded decoration), so this resolves each show title to a real TMDB
// id/poster via /api/shows/search instead of fetching a list by id.

const SHOWS_BASE_URL = 'https://localhost:8443/api/shows';
const POSTER_BASE = 'https://image.tmdb.org/t/p/w342';

export default function CrewPickDetailPage() {
    const { index } = useParams();
    const navigate = useNavigate();
    const pick = CREW_PICKS[Number(index)];

    const [showInfoCache, setShowInfoCache] = useState({});
    const requestedTitles = useRef(new Set());

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;
    const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

    const ensureShowInfo = useCallback((title) => {
        if (!title || requestedTitles.current.has(title)) return;
        requestedTitles.current.add(title);
        fetch(`${SHOWS_BASE_URL}/search?query=${encodeURIComponent(title)}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : null))
            .then(data => {
                const result = data?.results?.[0];
                setShowInfoCache(prev => ({
                    ...prev,
                    [title]: {
                        id: result?.id || null,
                        name: result?.name || title,
                        poster_path: result?.poster_path || null,
                        year: (result?.first_air_date || '').slice(0, 4),
                    },
                }));
            })
            .catch(() => {});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [token]);

    useEffect(() => {
        (pick?.shows || []).forEach(ensureShowInfo);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [index]);

    if (!pick) {
        return (
            <div className="ldp-page">
                <main className="ldp-main">
                    <p className="ldp-empty">This pick could not be found.</p>
                    <Link to="/lists" className="ldp-back-link">&larr; Back to Lists</Link>
                </main>
            </div>
        );
    }

    const shows = pick.shows || [];

    return (
        <div className="ldp-page">
            <main className="ldp-main">
                <Link to="/lists" className="ldp-back-link">&larr; Back to Lists</Link>

                <div className="ldp-header">
                    <div className="ldp-header-top">
                        <h1 className="ldp-title">{pick.title}</h1>
                    </div>
                    <div className="ldp-meta">
                        <span className="ldp-pill">Public</span>
                        <span className="ldp-stat">A list by <span className="ldp-owner">{pick.creator}</span></span>
                        <span className="ldp-stat">{shows.length} {shows.length === 1 ? 'show' : 'shows'}</span>
                    </div>
                </div>

                {shows.length === 0 && (
                    <p className="ldp-empty">This list doesn't have any shows yet.</p>
                )}

                {shows.length > 0 && (
                    <div className="ldp-grid">
                        {shows.map((title, i) => {
                            const info = showInfoCache[title];
                            return (
                                <div key={title} className="ldp-grid-item">
                                    <span className="ldp-grid-number">{i + 1}</span>
                                    {info?.poster_path ? (
                                        <img
                                            src={`${POSTER_BASE}${info.poster_path}`}
                                            alt={info?.name || title}
                                            className="ldp-poster"
                                            onClick={() => info?.id && navigate(`/shows/${info.id}`)}
                                        />
                                    ) : (
                                        <div
                                            className="ldp-poster ldp-poster-placeholder"
                                            onClick={() => info?.id && navigate(`/shows/${info.id}`)}
                                        />
                                    )}
                                    <div className="ldp-grid-caption">
                                        <span className="ldp-grid-title">{info?.name || title}</span>
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
