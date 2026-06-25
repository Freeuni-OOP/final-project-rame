import React, { useState, useEffect } from 'react';
import { X, ArrowLeft, Heart, Star } from 'lucide-react';
import '../style/LogModal.css';

export default function LogModal({ onClose }) {
    const [step, setStep] = useState(1);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [selectedShow, setSelectedShow] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    // ფორმის სტეიტები
    const [watchDate, setWatchDate] = useState(new Date().toISOString().split('T')[0]);
    const [specifyDate, setSpecifyDate] = useState(false);
    const [rewatch, setRewatch] = useState(false);
    const [review, setReview] = useState('');
    const [rating, setRating] = useState(0);
    const [hoverRating, setHoverRating] = useState(0);
    const [isLiked, setIsLiked] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    // სეზონებისა და ეპიზოდების სტეიტები
    const [seasons, setSeasons] = useState([]);
    const [episodes, setEpisodes] = useState([]);
    const [selectedSeason, setSelectedSeason] = useState('');
    const [selectedEpisode, setSelectedEpisode] = useState('');

    // 🌟 "Whole TV Show" — ნიშნავს რომ მთელი სერიალი ნანახია (COMPLETED)
    const WHOLE_SHOW = 'WHOLE';

    const BASE_URL = 'https://localhost:8443/api/shows';
    const LOG_URL = 'https://localhost:8443/api/log';

    // 🔐 token + username
    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;
    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };
    const username = parseJwt(token)?.sub;

    useEffect(() => {
        if (searchQuery.trim().length > 0) {
            setIsLoading(true);
            fetch(`${BASE_URL}/search?query=${encodeURIComponent(searchQuery)}&page=1`)
                .then((res) => res.text())
                .then((textData) => {
                    if (textData) {
                        const parsedData = JSON.parse(textData);
                        setSearchResults(parsedData.results || []);
                    }
                    setIsLoading(false);
                })
                .catch((err) => {
                    console.error("Search error:", err);
                    setIsLoading(false);
                });
        } else {
            setSearchResults([]);
        }
    }, [searchQuery]);

    const handleSelectShow = (show) => {
        const isCustomEntry = typeof show === 'string';
        const showId = isCustomEntry ? null : show.id;

        setSeasons([]);
        setEpisodes([]);
        setSelectedSeason('');
        setSelectedEpisode('');

        setSelectedShow({
            id: showId,
            title: isCustomEntry ? show : (show.name || show.title),
            year: isCustomEntry ? "2026" : (show.first_air_date ? show.first_air_date.split('-')[0] : 'N/A'),
            poster: isCustomEntry ? "https://via.placeholder.com/120x180?text=Custom" : (show.poster_path ? `https://image.tmdb.org/t/p/w500${show.poster_path}` : "https://via.placeholder.com/120x180?text=No+Poster")
        });

        setStep(2);

        if (showId) {
            fetch(`${BASE_URL}/${showId}`)
                .then((res) => res.text())
                .then((textData) => {
                    if (textData) {
                        const parsedData = JSON.parse(textData);
                        if (parsedData && parsedData.seasons) {
                            setSeasons(parsedData.seasons);
                        }
                    }
                })
                .catch((err) => console.error("Error fetching show seasons:", err));
        }
    };

    const handleSeasonChange = (e) => {
        const value = e.target.value;
        setSelectedSeason(value);

        setSelectedEpisode('');
        setEpisodes([]);

        // "Whole TV Show" აირჩა — ეპიზოდები არ ვტვირთავთ
        if (value === WHOLE_SHOW) return;

        if (selectedShow?.id && value !== '') {
            fetch(`${BASE_URL}/${selectedShow.id}/season/${value}`)
                .then((res) => res.text())
                .then((textData) => {
                    if (textData) {
                        const parsedData = JSON.parse(textData);
                        if (parsedData && parsedData.episodes) {
                            setEpisodes(parsedData.episodes);
                        }
                    }
                })
                .catch((err) => console.error("Error fetching episodes:", err));
        }
    };

    const handleSave = (e) => {
        e.preventDefault();

        if (!username) {
            console.error("STOP: not logged in (username is empty)");
            alert("You must be logged in to save a log.");
            return;
        }
        if (!selectedShow?.id) {
            console.error("STOP: no show selected (custom entries can't be saved to DB)");
            alert("Please pick a real show from the search results.");
            return;
        }

        const isWholeShow = selectedSeason === WHOLE_SHOW;

        const payload = {
            username,
            showId: selectedShow.id,
            rating,
            review,
            liked: isLiked,
            // 🌟 თუ "Whole TV Show" — wholeShow=true, ხოლო სეზონი/ეპიზოდი null
            wholeShow: isWholeShow,
            seasonNumber: (!isWholeShow && selectedSeason) ? parseInt(selectedSeason, 10) : null,
            episodeNumber: (!isWholeShow && selectedEpisode) ? parseInt(selectedEpisode, 10) : null,
            // 🌟 ახალი: diary — rewatch + watchDate (მხოლოდ თუ "Add to your diary?" მონიშნულია)
            rewatch: specifyDate ? rewatch : false,
            watchDate: specifyDate ? watchDate : null
        };

        console.log("Saving log payload:", payload);
        setIsSaving(true);

        fetch(LOG_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(payload)
        })
            .then((res) => {
                console.log("Save response status:", res.status);
                if (!res.ok) throw new Error('Save failed: ' + res.status);
                onClose();
            })
            .catch((err) => {
                console.error("Error saving log:", err);
                alert("Save failed. Check console for details.");
            })
            .finally(() => setIsSaving(false));
    };

    const hasNormalSeasons = seasons && seasons.some(s => s.season_number > 0);
    const filteredSeasons = seasons ? seasons.filter(s => hasNormalSeasons ? s.season_number > 0 : true) : [];

    const isWholeShow = selectedSeason === WHOLE_SHOW;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className={`modal-container ${step === 2 ? 'wider-modal' : ''}`} onClick={(e) => e.stopPropagation()}>

                {/* STAGE 1: SEARCH */}
                {step === 1 && (
                    <div className="modal-step-1">
                        <div className="modal-header-bar">
                            <h3>Add to your shows...</h3>
                            <button className="close-icon-btn" onClick={onClose}><X size={20} /></button>
                        </div>
                        <div className="modal-body">
                            <input
                                type="text"
                                placeholder="Search for TV show..."
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                className="modal-search-input"
                                autoFocus
                            />
                            {searchQuery.trim().length > 0 && (
                                <div className="mock-dropdown-results">
                                    {isLoading && <div style={{ color: '#5f758a', padding: '0.75rem 1rem' }}>Searching...</div>}
                                    <div onClick={() => handleSelectShow(searchQuery)}>+ Log custom entry: "{searchQuery}"</div>
                                    {searchResults.map((show) => (
                                        <div key={show.id} onClick={() => handleSelectShow(show)}>
                                            {show.name || show.title} <span style={{ color: '#9aabbc', fontSize: '0.85rem' }}>({show.first_air_date ? show.first_air_date.split('-')[0] : 'N/A'})</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* STAGE 2: FORM DETAILS */}
                {step === 2 && (
                    <div className="modal-step-2">
                        <div className="modal-header-bar flex-between">
                            <button className="back-nav-btn" onClick={() => setStep(1)}>
                                <ArrowLeft size={14} /> BACK
                            </button>
                            <h3>I watched...</h3>
                            <button className="close-icon-btn" onClick={onClose}><X size={20} /></button>
                        </div>

                        <form onSubmit={handleSave} className="letterboxd-form-layout" style={{ overflow: 'visible' }}>

                            <div className="lb-top-row" style={{ overflow: 'visible' }}>
                                <div className="lb-poster-aside-column" style={{ position: 'relative', zIndex: 100, overflow: 'visible' }}>
                                    <div className="lb-poster-box">
                                        <img src={selectedShow?.poster} alt="Poster" />
                                    </div>

                                    {selectedShow?.id && (
                                        <div className="lb-tracking-selectors" style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                                            <div className="selector-field">
                                                <select
                                                    value={selectedSeason}
                                                    onChange={handleSeasonChange}
                                                    className="lb-dropdown-select"
                                                    style={{ width: '100%', cursor: 'pointer' }}
                                                >
                                                    <option value="">Select Season</option>
                                                    <option value={WHOLE_SHOW}>Whole TV Show</option>
                                                    {filteredSeasons.map((s) => (
                                                        <option key={s.id} value={s.season_number}>
                                                            {s.name || `Season ${s.season_number}`}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>

                                            {/* episode dropdown იმალება, თუ "Whole TV Show" აირჩა */}
                                            {!isWholeShow && (
                                                <div className="selector-field">
                                                    <select
                                                        value={selectedEpisode}
                                                        onChange={(e) => setSelectedEpisode(e.target.value)}
                                                        className="lb-dropdown-select"
                                                        disabled={selectedSeason === ''}
                                                        style={{ width: '100%', cursor: 'pointer' }}
                                                    >
                                                        <option value="">Select Episode</option>
                                                        {episodes && episodes.map((ep) => (
                                                            <option key={ep.id} value={ep.episode_number}>
                                                                {ep.episode_number}. {ep.name || `Episode ${ep.episode_number}`}
                                                            </option>
                                                        ))}
                                                    </select>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>

                                <div className="lb-meta-box">
                                    <div className="lb-title-heading">
                                        <h2>{selectedShow?.title}</h2>
                                        <span className="lb-year">{selectedShow?.year}</span>
                                    </div>

                                    <div className="lb-checkbox-group">
                                        <label className="lb-check-label">
                                            <input
                                                type="checkbox"
                                                checked={specifyDate}
                                                onChange={(e) => setSpecifyDate(e.target.checked)}
                                            />
                                            <span className="custom-checkbox"></span>
                                            <span className="text-span">
                                                {specifyDate ? "Watched on" : "Add to your diary?"}
                                            </span>
                                        </label>

                                        {specifyDate && (
                                            <input
                                                type="date"
                                                value={watchDate}
                                                onChange={(e) => setWatchDate(e.target.value)}
                                                className="lb-inline-datepicker"
                                            />
                                        )}

                                        {specifyDate && (
                                            <label className="lb-check-label">
                                                <input
                                                    type="checkbox"
                                                    checked={rewatch}
                                                    onChange={(e) => setRewatch(e.target.checked)}
                                                />
                                                <span className="custom-checkbox"></span>
                                                <span className="text-span">I've watched this before</span>
                                            </label>
                                        )}
                                    </div>

                                    <div className="lb-textarea-container">
                                        <textarea
                                            placeholder="Add a review..."
                                            value={review}
                                            onChange={(e) => setReview(e.target.value)}
                                            className="lb-review-field-small"
                                        />
                                    </div>
                                </div>
                            </div>

                            <div className="lb-controls-grid" style={{ position: 'relative', zIndex: 1 }}>
                                <div className="lb-rating-column">
                                    <label>Rating <span className="lb-sub-label text-right">{rating > 0 ? `${rating} out of 5` : '0 out of 5'}</span></label>
                                    <div className="lb-stars-row">
                                        {[1, 2, 3, 4, 5].map((starValue) => {
                                            const isHighlighted = starValue <= (hoverRating || rating);
                                            const starColor = isHighlighted ? "#ffb020" : "#445566";
                                            return (
                                                <button
                                                    key={starValue}
                                                    type="button"
                                                    className="lb-star-btn"
                                                    onClick={() => setRating(starValue === rating ? 0 : starValue)}
                                                    onMouseEnter={() => setHoverRating(starValue)}
                                                    onMouseLeave={() => setHoverRating(0)}
                                                >
                                                    <Star
                                                        size={24}
                                                        fill={isHighlighted ? "#ffb020" : "transparent"}
                                                        color={starColor}
                                                    />
                                                </button>
                                            );
                                        })}
                                    </div>
                                </div>

                                <div className="lb-like-column">
                                    <label>Like</label>
                                    <button
                                        type="button"
                                        className="lb-heart-toggle"
                                        onClick={() => setIsLiked(!isLiked)}
                                    >
                                        <Heart
                                            size={26}
                                            fill={isLiked ? "#ff3a44" : "transparent"}
                                            color={isLiked ? "#ff3a44" : "#445566"}
                                        />
                                    </button>
                                </div>
                            </div>

                            <div className="lb-modal-footer">
                                <button type="submit" className="lb-save-green-btn" disabled={isSaving}>
                                    {isSaving ? "Saving..." : "Save"}
                                </button>
                            </div>
                        </form>
                    </div>
                )}
            </div>
        </div>
    );
}
