import React, { useState, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import '../style/AddToListButton.css';

const VISIBLE_CAP = 5; // keep the modal a predictable size; search reveals the rest

// Lets a logged-in user add the current show to one or more of their own lists.
export default function AddToListButton({ showId, showName }) {
    const [isOpen, setIsOpen] = useState(false);
    const [lists, setLists] = useState([]);
    const [loading, setLoading] = useState(false);
    const [tab, setTab] = useState('public'); // 'public' | 'private'
    const [query, setQuery] = useState('');
    const [selectedIds, setSelectedIds] = useState(new Set());
    const [creatingNew, setCreatingNew] = useState(false);
    const [newListName, setNewListName] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [resultMessage, setResultMessage] = useState('');
    const navigate = useNavigate();

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const currentUsername = decodedToken?.sub;

    const fetchLists = () => {
        if (!currentUsername || !token) return;
        setLoading(true);
        fetch(`https://localhost:8443/api/lists?actingUsername=${currentUsername}`, {
            headers: { Authorization: `Bearer ${token}` }
        })
            .then(res => res.ok ? res.json() : [])
            .then(data => setLists(data || []))
            .catch(err => console.error('Error fetching lists:', err))
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        if (isOpen) fetchLists();
    }, [isOpen]);

    const closeModal = () => {
        setIsOpen(false);
        setQuery('');
        setSelectedIds(new Set());
        setCreatingNew(false);
        setNewListName('');
        setResultMessage('');
    };

    const tabLists = useMemo(
        () => lists.filter(l => (tab === 'public' ? l.isPublic : !l.isPublic)),
        [lists, tab]
    );

    const displayedLists = useMemo(() => {
        const q = query.trim().toLowerCase();
        if (!q) return tabLists.slice(0, VISIBLE_CAP);
        return tabLists.filter(l => l.name.toLowerCase().includes(q));
    }, [tabLists, query]);

    const toggleSelected = (listId) => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (next.has(listId)) next.delete(listId);
            else next.add(listId);
            return next;
        });
    };

    const handleCreateList = () => {
        const name = newListName.trim();
        if (!name || !currentUsername) return;
        fetch(`https://localhost:8443/api/lists?actingUsername=${currentUsername}&name=${encodeURIComponent(name)}&isPublic=${tab === 'public'}`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${token}` }
        })
            .then(async (res) => {
                if (!res.ok) {
                    const msg = await res.text().catch(() => '');
                    throw new Error(msg || 'Failed to create list.');
                }
                return res.json();
            })
            .then(newList => {
                // Prepend so the new list shows up immediately even when the
                // tab already has VISIBLE_CAP lists (displayedLists only shows
                // the first VISIBLE_CAP when there's no search query).
                setLists(prev => [newList, ...prev]);
                setSelectedIds(prev => new Set(prev).add(newList.id));
                setNewListName('');
                setCreatingNew(false);
            })
            .catch(err => {
                console.error('Error creating list:', err);
                setResultMessage(err.message || 'Could not create list.');
            });
    };

    const handleAdd = async () => {
        if (selectedIds.size === 0) {
            closeModal();
            return;
        }
        setSubmitting(true);
        const ids = Array.from(selectedIds);
        const results = await Promise.all(ids.map(listId =>
            fetch(`https://localhost:8443/api/lists/${listId}/shows?actingUsername=${currentUsername}&showId=${showId}`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${token}` }
            }).then(async (res) => ({ listId, ok: res.ok, message: res.ok ? null : await res.text() }))
        ));

        const failed = results.filter(r => !r.ok);
        setSubmitting(false);

        if (failed.length === 0) {
            setResultMessage(`Added to ${ids.length} list${ids.length > 1 ? 's' : ''}.`);
            setTimeout(closeModal, 900);
        } else {
            setResultMessage(
                failed.map(f => {
                    const list = lists.find(l => l.id === f.listId);
                    return `${list?.name || 'List'}: ${f.message?.includes('already in the list') ? 'already in list' : 'failed'}`;
                }).join(' · ')
            );
        }
    };

    if (!currentUsername) return null;

    return (
        <>
            <button className="design-btn list-btn" onClick={() => setIsOpen(true)}>
                Add to list
            </button>

            {isOpen && createPortal(
                <div className="atl-overlay" onClick={closeModal}>
                    <div className="atl-modal" onClick={(e) => e.stopPropagation()}>
                        <div className="atl-header">
                            <h3>Add '{showName}' to lists</h3>
                            <button className="atl-close" onClick={closeModal}>✕</button>
                        </div>

                        <div className="atl-tabs">
                            <button
                                className={`atl-tab ${tab === 'public' ? 'active' : ''}`}
                                onClick={() => { setTab('public'); setQuery(''); }}
                            >
                                Public
                            </button>
                            <button
                                className={`atl-tab ${tab === 'private' ? 'active' : ''}`}
                                onClick={() => { setTab('private'); setQuery(''); }}
                            >
                                Private
                            </button>
                        </div>

                        <div className="atl-toolbar">
                            {creatingNew ? (
                                <div className="atl-new-list-form">
                                    <input
                                        className="atl-new-list-input"
                                        autoFocus
                                        placeholder="List name..."
                                        value={newListName}
                                        onChange={(e) => setNewListName(e.target.value)}
                                        onKeyDown={(e) => {
                                            if (e.key === 'Enter') handleCreateList();
                                            if (e.key === 'Escape') { setCreatingNew(false); setNewListName(''); }
                                        }}
                                        onBlur={() => { if (!newListName.trim()) setCreatingNew(false); }}
                                    />
                                    <button
                                        type="button"
                                        className="atl-new-list-confirm"
                                        title="Create list"
                                        onMouseDown={(e) => e.preventDefault()}
                                        onClick={handleCreateList}
                                    >
                                        ✓
                                    </button>
                                </div>
                            ) : (
                                <span className="atl-new-list-link" onClick={() => setCreatingNew(true)}>
                                    + New list...
                                </span>
                            )}

                            <input
                                className="atl-search-input"
                                placeholder="Type to search 🔍"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                            />
                        </div>

                        <div className="atl-list">
                            {loading && <div className="atl-empty">Loading your lists...</div>}

                            {!loading && displayedLists.length === 0 && (
                                <div className="atl-empty">
                                    {tabLists.length === 0
                                        ? `You have no ${tab} lists.`
                                        : 'No lists match your search.'}
                                </div>
                            )}

                            {!loading && displayedLists.map(list => {
                                const selected = selectedIds.has(list.id);
                                return (
                                    <div
                                        key={list.id}
                                        className={`atl-row ${selected ? 'selected' : ''}`}
                                        onClick={() => toggleSelected(list.id)}
                                    >
                                        <span className={`atl-checkbox ${selected ? 'checked' : ''}`}>
                                            {selected ? '✓' : ''}
                                        </span>
                                        <span className="atl-list-name">{list.name}</span>
                                    </div>
                                );
                            })}
                        </div>

                        <div className="atl-footer">
                            <span className="atl-result-message">{resultMessage}</span>
                            <button className="atl-add-confirm-btn" onClick={handleAdd} disabled={submitting}>
                                {submitting ? 'Adding...' : 'Add'}
                            </button>
                        </div>
                    </div>
                </div>,
                document.body
            )}
        </>
    );
}
