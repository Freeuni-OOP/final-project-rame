import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import '../style/ListEditPage.css';

// Full-page edit form for a list you own, modeled on Letterboxd's Edit List
// page but scoped to only what the backend actually supports: name,
// description, public/private, adding/removing shows via search, and
// deleting the list. Tags, per-film notes, ranked-list ordering, and import
// are intentionally left out since there's no backend support for them yet.

const LISTS_BASE_URL = 'https://localhost:8443/api/lists';
const SHOWS_BASE_URL = 'https://localhost:8443/api/shows';
const MINI_POSTER_BASE = 'https://image.tmdb.org/t/p/w92';

export default function ListEditPage() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [list, setList] = useState(null);
    const [items, setItems] = useState([]);
    const [showInfoCache, setShowInfoCache] = useState({});
    const requestedShowIds = useRef(new Set());

    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [notFoundOrForbidden, setNotFoundOrForbidden] = useState(false);

    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [isPublic, setIsPublic] = useState(true);

    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState('');
    const [saveMessage, setSaveMessage] = useState('');

    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [searchLoading, setSearchLoading] = useState(false);
    const searchDebounceRef = useRef(null);

    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState('');

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

    const loadList = useCallback(() => {
        setLoading(true);
        setLoadError('');
        fetch(`${LISTS_BASE_URL}/${id}`, { headers: authHeaders })
            .then(r => (r.ok ? r.json() : Promise.reject('not found')))
            .then(data => {
                setList(data);
                setName(data.name || '');
                setDescription(data.description || '');
                setIsPublic(!!data.isPublic);
                const sortedItems = (data.items || []).slice().sort((a, b) => a.position - b.position);
                setItems(sortedItems);
                sortedItems.forEach(item => ensureShowInfo(item.showId));
            })
            .catch(() => setLoadError('This list could not be found.'))
            .finally(() => setLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    useEffect(() => {
        loadList();
    }, [loadList]);

    useEffect(() => {
        if (list && username && list.ownerUsername !== username) {
            setNotFoundOrForbidden(true);
        }
    }, [list, username]);

    const handleSave = async () => {
        if (!name.trim()) {
            setSaveError('List name cannot be empty.');
            return;
        }
        setSaving(true);
        setSaveError('');
        setSaveMessage('');

        const renameParams = new URLSearchParams({
            actingUsername: username,
            name: name.trim(),
            description: description || '',
        });

        try {
            const renameRes = await fetch(`${LISTS_BASE_URL}/${id}?${renameParams.toString()}`, {
                method: 'PUT',
                headers: authHeaders,
            });
            if (!renameRes.ok) {
                throw new Error(await renameRes.text());
            }

            const visibilityRes = await fetch(
                `${LISTS_BASE_URL}/${id}/visibility?actingUsername=${username}&isPublic=${isPublic}`,
                { method: 'PUT', headers: authHeaders }
            );
            if (!visibilityRes.ok) {
                throw new Error(await visibilityRes.text());
            }

            // Both updates went through - head back to the full list page
            // instead of leaving the user sitting on the edit form.
            navigate(`/lists/${id}`);
        } catch (err) {
            setSaveError(typeof err?.message === 'string' && err.message ? err.message : 'Could not save changes.');
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        setDeleting(true);
        setDeleteError('');
        try {
            const res = await fetch(`${LISTS_BASE_URL}/${id}?actingUsername=${username}`, {
                method: 'DELETE',
                headers: authHeaders,
            });
            if (!res.ok) {
                throw new Error(await res.text());
            }
            navigate('/lists');
        } catch (err) {
            setDeleteError(typeof err?.message === 'string' && err.message ? err.message : 'Could not delete this list.');
            setDeleting(false);
        }
    };

    const handleSearchChange = (value) => {
        setSearchQuery(value);
        if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);

        if (!value.trim()) {
            setSearchResults([]);
            setSearchLoading(false);
            return;
        }

        setSearchLoading(true);
        searchDebounceRef.current = setTimeout(() => {
            fetch(`${SHOWS_BASE_URL}/search?query=${encodeURIComponent(value.trim())}&page=1`, { headers: authHeaders })
                .then(r => (r.ok ? r.json() : { results: [] }))
                .then(data => setSearchResults((data.results || []).slice(0, 6)))
                .catch(() => setSearchResults([]))
                .finally(() => setSearchLoading(false));
        }, 350);
    };

    const handleAddShow = (showId) => {
        if (!showId) return;
        fetch(`${LISTS_BASE_URL}/${id}/shows?actingUsername=${username}&showId=${showId}`, {
            method: 'POST',
            headers: authHeaders,
        })
            .then(res => (res.ok ? null : res.text().then(msg => Promise.reject(msg))))
            .then(() => {
                setSearchQuery('');
                setSearchResults([]);
                loadList();
            })
            .catch(() => { /* show stays silent in this first pass */ });
    };

    const handleRemoveShow = (showId) => {
        fetch(`${LISTS_BASE_URL}/${id}/shows/${showId}?actingUsername=${username}`, {
            method: 'DELETE',
            headers: authHeaders,
        }).then(() => loadList());
    };

    if (loading) {
        return (
            <div className="lep-page">
                <main className="lep-main">
                    <p className="lep-empty">Loading list...</p>
                </main>
            </div>
        );
    }

    if (loadError || !list) {
        return (
            <div className="lep-page">
                <main className="lep-main">
                    <p className="lep-empty">{loadError || 'This list could not be found.'}</p>
                    <Link to="/lists" className="lep-back-link">&larr; Back to Lists</Link>
                </main>
            </div>
        );
    }

    if (notFoundOrForbidden) {
        return (
            <div className="lep-page">
                <main className="lep-main">
                    <p className="lep-empty">You can only edit your own lists.</p>
                    <Link to={`/lists/${id}`} className="lep-back-link">&larr; Back to list</Link>
                </main>
            </div>
        );
    }

    return (
        <div className="lep-page">
            <main className="lep-main">
                <Link to={`/lists/${id}`} className="lep-back-link">&larr; Back to list</Link>

                <h1 className="lep-page-title">Edit List</h1>

                <div className="lep-field">
                    <label className="lep-label">Name</label>
                    <input
                        className="lep-input"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                    />
                </div>

                <div className="lep-field">
                    <label className="lep-label">Who can view this list?</label>
                    <select
                        className="lep-input"
                        value={isPublic ? 'public' : 'private'}
                        onChange={(e) => setIsPublic(e.target.value === 'public')}
                    >
                        <option value="public">Anyone — Public list</option>
                        <option value="private">Only me — Private list</option>
                    </select>
                </div>

                <div className="lep-field">
                    <label className="lep-label">Description</label>
                    <textarea
                        className="lep-textarea"
                        rows={4}
                        value={description}
                        onChange={(e) => setDescription(e.target.value)}
                        placeholder="Say something about this list..."
                    />
                </div>

                <div className="lep-field">
                    <label className="lep-label">Add a show</label>
                    <div className="lep-add-show-search">
                        <input
                            className="lep-input"
                            placeholder="Search for a show to add..."
                            value={searchQuery}
                            onChange={(e) => handleSearchChange(e.target.value)}
                        />
                        {searchLoading && <p className="lep-empty">Searching...</p>}
                        {searchResults.length > 0 && (
                            <ul className="lep-search-results">
                                {searchResults.map((show) => (
                                    <li key={show.id} className="lep-search-result-row" onClick={() => handleAddShow(show.id)}>
                                        {show.poster_path ? (
                                            <img src={`${MINI_POSTER_BASE}${show.poster_path}`} alt="" className="lep-mini-poster" />
                                        ) : (
                                            <div className="lep-mini-poster lep-mini-poster-placeholder" />
                                        )}
                                        <div className="lep-search-result-info">
                                            <span>{show.name || show.title}</span>
                                            {show.first_air_date && <span className="lep-stat">{show.first_air_date.slice(0, 4)}</span>}
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>

                <div className="lep-field">
                    <label className="lep-label">Shows in this list ({items.length})</label>
                    {items.length === 0 && <p className="lep-empty">No shows in this list yet.</p>}
                    {items.length > 0 && (
                        <ul className="lep-items">
                            {items.map((item, i) => {
                                const info = showInfoCache[item.showId];
                                return (
                                    <li key={item.id} className="lep-item-row">
                                        <span className="lep-item-number">{i + 1}</span>
                                        {info?.poster_path ? (
                                            <img src={`${MINI_POSTER_BASE}${info.poster_path}`} alt="" className="lep-mini-poster" />
                                        ) : (
                                            <div className="lep-mini-poster lep-mini-poster-placeholder" />
                                        )}
                                        <span className="lep-item-title">{info?.name || `Show #${item.showId}`}</span>
                                        <button className="lep-item-remove" onClick={() => handleRemoveShow(item.showId)}>
                                            Remove
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                {saveError && <p className="lep-error">{saveError}</p>}
                {saveMessage && <p className="lep-success">{saveMessage}</p>}

                <div className="lep-actions">
                    <button className="lep-delete-btn" onClick={() => setShowDeleteConfirm(true)}>
                        Delete
                    </button>
                    <div className="lep-actions-right">
                        <button className="lep-view-btn" onClick={() => navigate(`/lists/${id}`)}>
                            View List
                        </button>
                        <button className="lep-save-btn" onClick={handleSave} disabled={saving}>
                            {saving ? 'Saving...' : 'Save'}
                        </button>
                    </div>
                </div>

                {showDeleteConfirm && (
                    <div className="lep-modal-backdrop" onClick={() => setShowDeleteConfirm(false)}>
                        <div className="lep-confirm-box" onClick={(e) => e.stopPropagation()}>
                            <p>Delete "{list.name}"? This can't be undone.</p>
                            {deleteError && <p className="lep-error">{deleteError}</p>}
                            <div className="lep-confirm-actions">
                                <button className="lep-cancel-btn" onClick={() => setShowDeleteConfirm(false)} disabled={deleting}>
                                    Cancel
                                </button>
                                <button className="lep-delete-btn" onClick={handleDelete} disabled={deleting}>
                                    {deleting ? 'Deleting...' : 'Delete list'}
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}
