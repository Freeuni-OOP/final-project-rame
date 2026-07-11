import React, { useState, useEffect, useRef } from 'react';
import '../style/ShowsPage.css';
import { useNavigate, useLocation } from 'react-router-dom';

const GENRES = [
    { id: 'all', name: 'All Genres' },
    { id: 10759, name: "Action & Adventure" },
    { id: 16, name: "Animation" },
    { id: 35, name: "Comedy" },
    { id: 80, name: "Crime" },
    { id: 18, name: "Drama" },
    { id: 10765, name: "Sci-Fi & Fantasy" }
];

export default function ShowsPage() {
    const location = useLocation();
    const navigate = useNavigate();

    const [trendingShowsList, setTrendingShowsList] = useState([]);
    const [allShows, setAllShows] = useState([]);
    const [loading, setLoading] = useState(false);
    const [gridTitle, setGridTitle] = useState('Explore TV Shows');
    const [selectedGenre, setSelectedGenre] = useState('all');

    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);

    const [activeStatus, setActiveStatus] = useState({});
    const [favorites, setFavorites] = useState({});

    const BASE_URL = 'https://localhost:8443/api/shows';
    const TRACKING_URL = 'https://localhost:8443/api/tracking';
    const ribbonRef = useRef(null);

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (token) => {
        if (!token) return null;
        try { return JSON.parse(atob(token.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const username = decodedToken?.sub;

    // Load trending ribbon content once on mount
    useEffect(() => {
        fetchRibbonTrending();
    }, []);

    // Sync the grid data with the URL query parameters automatically
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const queryParam = params.get('query') || '';
        const genreParam = params.get('genre') || 'all';
        const pageParam = parseInt(params.get('page') || '1', 10);

        setSelectedGenre(genreParam);

        let mode = 'trending';
        if (queryParam.trim()) mode = 'text';
        else if (genreParam !== 'all') mode = 'genre';

        fetchGridShows(pageParam, mode, queryParam, genreParam);
    }, [location.search]);

    // Track user statuses for all loaded shows
    useEffect(() => {
        if (!username) return;
        const uniqueShowIds = [...new Set([...allShows, ...trendingShowsList].map(s => s.id))];
        uniqueShowIds.forEach(id => {
            fetchShowStatusFromBackend(id);
        });
    }, [allShows, trendingShowsList, username]);

    const fetchShowStatusFromBackend = (showId) => {
        // 🟢 2. დაზღვევა: თუ ტოკენი არ არის, რექვესტი საერთოდ არ გაუშვა, რომ 403 არ აგდოს კოსოლში
        if (!token || !username) {
            // თუ ტოკენი არ არსებობს, შეგიძლია უბრალოდ გაჩერდე
            console.warn("No token found, skipping status fetch.");
            return;
        }

        fetch(`${TRACKING_URL}/get-status?username=${username}&showId=${showId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(res => {
                if (!res.ok) throw new Error(`Status HTTP error: ${res.status}`);
                return res.text();
            })
            .then(text => {
                const data = text ? JSON.parse(text) : null;
                if (data) {
                    setActiveStatus(prev => ({ ...prev, [showId]: data.status }));
                    setFavorites(prev => ({ ...prev, [showId]: data.favorite }));
                }
            })
            .catch(err => console.error("Error fetching show status:", err));
    };

    const fetchRibbonTrending = async () => {
        try {
            const response = await fetch(`${BASE_URL}/trending?page=1`);
            if (response.ok) {
                const text = await response.text();
                const data = text ? JSON.parse(text) : {};
                setTrendingShowsList(data.results || []);
            }
        } catch (error) {
            console.error("Error loading ribbon: ", error);
        }
    };

    const fetchGridShows = async (page = 1, currentMode, query, genre) => {
        setLoading(true);
        try {
            let url = `${BASE_URL}/trending?page=${page}`;
            if (currentMode === 'text' && query.trim()) {
                url = `${BASE_URL}/search?query=${encodeURIComponent(query)}&page=${page}`;
                setGridTitle(`Search Results for "${query}"`);
            } else if (currentMode === 'genre' && genre !== 'all') {
                url = `${BASE_URL}/genre?genreId=${genre}&page=${page}`;
                const genreName = GENRES.find(g => g.id.toString() === genre.toString())?.name;
                setGridTitle(`${genreName} TV Shows`);
            } else {
                setGridTitle('Explore TV Shows');
            }

            const headers = {};
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }

            const response = await fetch(url, { headers });

            if (!response.ok) throw new Error(`Failed to fetch data (Status: ${response.status})`);

            const text = await response.text();
            const data = text ? JSON.parse(text) : {};

            setAllShows(data.results || []);
            setCurrentPage(data.page || page);
            setTotalPages(data.total_pages > 500 ? 500 : (data.total_pages || 1));
        } catch (error) {
            console.error("Error fetching grid shows:", error);
            setAllShows([]);
        } finally {
            setLoading(false);
        }
    };

    const handleStatusUpdate = (show, statusName) => {
        const showId = show.id;
        const showName = show.name || show.title || "Unknown Show";
        // 🟢 ამოვიღოთ პოსტერის გზა უსაფრთხოდ
        const posterPath = show.poster_path || "";

        const currentStatus = activeStatus[showId];
        const newStatus = currentStatus === statusName ? null : statusName;
        setActiveStatus(prev => ({ ...prev, [showId]: newStatus }));

        // 🟢 URL-ის ბოლოში დავამატეთ: &posterPath=${encodeURIComponent(posterPath)}
        fetch(`${TRACKING_URL}/show-status?username=${username}&showId=${showId}&status=${newStatus !== null ? newStatus : ''}&showName=${encodeURIComponent(showName)}&posterPath=${encodeURIComponent(posterPath)}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then((res) => {
                if (!res.ok) throw new Error(`Failed to update status (Status: ${res.status})`);

                if (newStatus === 'COMPLETED') {
                    fetch(`${TRACKING_URL}/watch-all-episodes?username=${username}&showId=${showId}`, {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    }).catch(err => console.error(err));
                }
                else if (newStatus === null) {
                    fetch(`${TRACKING_URL}/unwatch-all-episodes?username=${username}&showId=${showId}`, {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    }).catch(err => console.error(err));
                }
            })
            .catch(err => {
                console.error("Request failed:", err);
                setActiveStatus(prev => ({ ...prev, [showId]: currentStatus }));
            });
    };

    const handleFavoriteToggle = (show) => {
        const showId = show.id;
        const showName = show.name || show.title || "Unknown Show";
        const posterPath = show.poster_path || ""; // 🟢 წამოვიღოთ პოსტერი
        const currentFav = !!favorites[showId];
        setFavorites(prev => ({ ...prev, [showId]: !currentFav }));

        // 🟢 URL-ის ბოლოში მივაწერეთ &posterPath=...
        fetch(`${TRACKING_URL}/toggle-favorite?username=${username}&showId=${showId}&showName=${encodeURIComponent(showName)}&posterPath=${encodeURIComponent(posterPath)}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        }).catch(err => console.error(err));
    };

    const handleGenreChange = (e) => {
        const newGenre = e.target.value;
        setSelectedGenre(newGenre);

        const params = new URLSearchParams(location.search);
        if (newGenre === 'all') {
            params.delete('genre');
        } else {
            params.set('genre', newGenre);
        }
        params.delete('query');
        params.delete('page');

        navigate({ search: params.toString() });
    };

    const handlePageChange = (newPage) => {
        const params = new URLSearchParams(location.search);
        params.set('page', String(newPage));
        navigate({ search: params.toString() });
        window.scrollTo(0, 0);
    };

    const renderShowCard = (show) => {
        const id = show.id;
        const isFavoriteShow = !!favorites[id];
        const currentShowStatus = activeStatus[id];

        let eyeClass = "action-icon eye-icon";
        if (currentShowStatus === 'WATCHING') eyeClass += " active-half";
        if (currentShowStatus === 'COMPLETED') eyeClass += " active-full";

        return (
            <div key={show.id} className="show-card" onClick={() => navigate(`/shows/${id}`)} style={{ cursor: 'pointer' }}>
                <div className="card-image-wrapper">
                    {show.poster_path ? (
                        <img src={`https://image.tmdb.org/t/p/w500${show.poster_path}`} alt={show.name} className="show-poster" />
                    ) : (
                        <div className="no-poster">No Image</div>
                    )}
                    <div className="rating-badge">★ {show.vote_average ? show.vote_average.toFixed(1) : 'N/A'}</div>
                </div>

                <div className="show-details-overlay">
                    <div className="text-meta">
                        <h3 className="overlay-show-title">{show.name}</h3>
                        <p className="show-date">{show.first_air_date ? show.first_air_date.split('-')[0] : 'N/A'}</p>
                    </div>

                    {username && (
                        <div className="letterboxd-actions" onClick={(e) => e.stopPropagation()}>
                            <button
                                className={eyeClass}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    // 🟢 id-ს ნაცვლად გადავეცით მთლიანი show ობიექტი
                                    if (!currentShowStatus) handleStatusUpdate(show, 'WATCHING');
                                    else if (currentShowStatus === 'WATCHING') handleStatusUpdate(show, 'COMPLETED');
                                    else handleStatusUpdate(show, null);
                                }}
                                title="Mark as Watched / Watching"
                            >
                                👁
                            </button>

                            <button
                                className={`action-icon heart-icon ${isFavoriteShow ? 'active' : ''}`}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleFavoriteToggle(show); // 🟢 id-ს ნაცვლად გადავეცით მთლიანი show
                                }}
                                title="Favorite"
                            >
                                {isFavoriteShow ? '❤️' : '♡'}
                            </button>

                            <button
                                className={`action-icon star-icon ${currentShowStatus === 'PLAN_TO_WATCH' ? 'active' : ''}`}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleStatusUpdate(show, 'PLAN_TO_WATCH'); // 🟢 id-ს ნაცვლად გადავეცით მთლიანი show
                                }}
                                title="Plan to Watch"
                            >
                                ★
                            </button>

                            <button
                                className={`action-icon drop-icon ${currentShowStatus === 'DROPPED' ? 'active' : ''}`}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleStatusUpdate(show, 'DROPPED'); // 🟢 id-ს ნაცვლად გადავეცით მთლიანი show
                                }}
                                title="Dropped"
                            >
                                ✕
                            </button>
                        </div>
                    )}
                </div>
            </div>
        );
    };

    return (
        <div className="shows-container">
            <div className="shows-page-filter-bar">
                <select value={selectedGenre} onChange={handleGenreChange} className="glass-select">
                    {GENRES.map(genre => (
                        <option key={genre.id} value={genre.id} className="dark-option">{genre.name}</option>
                    ))}
                </select>
            </div>

            {trendingShowsList.length > 0 && !location.search.includes('query') && !location.search.includes('genre=') && (
                <div className="ribbon-section">
                    <div className="ribbon-header">
                        <h2 className="section-title">Trending This Week</h2>
                        <div className="ribbon-arrows">
                            <button onClick={() => ribbonRef.current.scrollLeft -= 500} className="arrow-btn">⟨</button>
                            <button onClick={() => ribbonRef.current.scrollLeft += 500} className="arrow-btn">⟩</button>
                        </div>
                    </div>
                    <div className="ribbon-track" ref={ribbonRef}>
                        {trendingShowsList.map(show => renderShowCard(show))}
                    </div>
                </div>
            )}

            <div className="grid-section">
                <h2 className="section-title">{gridTitle}</h2>
                {loading ? (
                    <div className="loading"><div className="spinner"></div>Loading...</div>
                ) : (
                    <>
                        {allShows.length === 0 ? (
                            <div className="no-results">No shows found.</div>
                        ) : (
                            <div className="shows-grid">
                                {allShows.map(show => renderShowCard(show))}
                            </div>
                        )}
                        {totalPages > 1 && (
                            <div className="pagination-container">
                                <button onClick={() => handlePageChange(currentPage - 1)} disabled={currentPage === 1} className="pagination-button">⟨ Prev</button>
                                <span className="pagination-info">Page <strong className="neon-text">{currentPage}</strong> of {totalPages}</span>
                                <button onClick={() => handlePageChange(currentPage + 1)} disabled={currentPage === totalPages} className="pagination-button">Next ⟩</button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}