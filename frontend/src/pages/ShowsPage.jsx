import React, { useState, useEffect, useRef } from 'react';
import '../style/ShowsPage.css';
import { useNavigate } from 'react-router-dom';

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
    const [trendingShowsList, setTrendingShowsList] = useState([]);
    const [allShows, setAllShows] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedGenre, setSelectedGenre] = useState('all');
    const [loading, setLoading] = useState(false);
    const [gridTitle, setGridTitle] = useState('Explore TV Shows');

    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [searchMode, setSearchMode] = useState('trending');

    const navigate = useNavigate();

    // ბაზიდან წამოღებული რეალური სტატუსების სთეითები
    const [activeStatus, setActiveStatus] = useState({}); // ინახავს სტატუსს თითოეული შოუს ID-ისთვის
    const [favorites, setFavorites] = useState({});       // ინახავს true/false თითოეული შოუს ID-ისთვის

    const BASE_URL = 'https://localhost:8443/api/shows';
    const TRACKING_URL = 'https://localhost:8443/api/tracking';
    const ribbonRef = useRef(null);

    // 🔐 ტოკენის ამოღება და პარსინგი (ზუსტად როგორც Details გვერდზე)
    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (token) => {
        if (!token) return null;
        try {
            return JSON.parse(atob(token.split('.')[1]));
        } catch (e) {
            return null;
        }
    };

    const decodedToken = parseJwt(token);
    const username = decodedToken?.sub;

    useEffect(() => {
        fetchRibbonTrending();
        fetchGridShows(1, 'trending', '', 'all');
    }, []);

    // ყოველ ჯერზე, როცა სერიალების სია განახლდება, სათითაოდ მოგვაქვს მათი სტატუსები ბაზიდან
    useEffect(() => {
        if (!username) return;
        const uniqueShowIds = [...new Set([...allShows, ...trendingShowsList].map(s => s.id))];
        uniqueShowIds.forEach(id => {
            fetchShowStatusFromBackend(id);
        });
    }, [allShows, trendingShowsList, token, username]);

    // ბაზიდან კონკრეტული სერიალის სტატუსის და ფავორიტის წამოღება
    const fetchShowStatusFromBackend = (showId) => {
        fetch(`${TRACKING_URL}/get-status?username=${username}&showId=${showId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(res => {
                if (res.ok) return res.json();
                return null;
            })
            .then(data => {
                if (data) {
                    setActiveStatus(prev => ({ ...prev, [showId]: data.status }));
                    setFavorites(prev => ({ ...prev, [showId]: data.favorite }));
                }
            })
            .catch(err => console.error(`Error fetching status for show ${showId}:`, err));
    };

    const fetchRibbonTrending = async () => {
        try {
            const response = await fetch(`${BASE_URL}/trending?page=1`);
            if (response.ok) {
                const data = await response.json();
                setTrendingShowsList(data.results || []);
            }
        } catch (error) {
            console.error("Error loading ribbon: ", error);
        }
    };

    const fetchGridShows = async (page = 1, currentMode = searchMode, query = searchQuery, genre = selectedGenre) => {
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

            const response = await fetch(url);
            if (!response.ok) throw new Error('Failed to fetch data');
            const data = await response.json();

            setAllShows(data.results || []);
            setCurrentPage(data.page || page);
            setTotalPages(data.total_pages > 500 ? 500 : (data.total_pages || 1));
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    // სტატუსის განახლების ლოგიკა (POST მოთხოვნა ტოკენით)
    const handleStatusUpdate = (showId, statusName) => {
        const currentStatus = activeStatus[showId];
        const newStatus = currentStatus === statusName ? null : statusName;

        setActiveStatus(prev => ({ ...prev, [showId]: newStatus }));

        fetch(`${TRACKING_URL}/show-status?username=${username}&showId=${showId}&status=${statusName}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .catch(err => console.error("Request failed:", err));
    };

    // ფავორიტის გადართვის ლოგიკა (POST მოთხოვნა ტოკენით)
    const handleFavoriteToggle = (showId) => {
        const currentFav = !!favorites[showId];
        setFavorites(prev => ({ ...prev, [showId]: !currentFav }));

        fetch(`${TRACKING_URL}/toggle-favorite?username=${username}&showId=${showId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .catch(err => console.error("Favorite toggle failed:", err));
    };

    const handleSearch = (e) => {
        e.preventDefault();
        setSelectedGenre('all');
        if (!searchQuery.trim()) {
            setSearchMode('trending');
            fetchGridShows(1, 'trending', '', 'all');
            return;
        }
        setSearchMode('text');
        fetchGridShows(1, 'text', searchQuery, 'all');
    };

    const handleGenreChange = (e) => {
        const genreId = e.target.value;
        setSelectedGenre(genreId);
        setSearchQuery('');
        if (genreId === 'all') {
            setSearchMode('trending');
            fetchGridShows(1, 'trending', '', 'all');
        } else {
            setSearchMode('genre');
            fetchGridShows(1, 'genre', '', genreId);
        }
    };

    const renderShowCard = (show) => {
        const id = show.id;
        const isFavoriteShow = !!favorites[id];
        const currentShowStatus = activeStatus[id];

        // თვალის აიქონის კლასების განსაზღვრა (Details გვერდის ანალოგიურად)
        let eyeClass = "action-icon eye-icon";
        if (currentShowStatus === 'WATCHING') eyeClass += " active-half";
        if (currentShowStatus === 'COMPLETED') eyeClass += " active-full";

        return (
            <div
                key={show.id}
                className="show-card"
                onClick={() => navigate(`/shows/${id}`)}
                style={{ cursor: 'pointer' }}
            >
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
                            {/* WATCHING / COMPLETED (თვალი) */}
                            <button
                                className={eyeClass}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    if (!currentShowStatus) handleStatusUpdate(id, 'WATCHING');
                                    else if (currentShowStatus === 'WATCHING') handleStatusUpdate(id, 'COMPLETED');
                                    else handleStatusUpdate(id, null);
                                }}
                                title="Mark as Watched / Watching"
                            >
                                👁
                            </button>

                            {/* FAVORITE (გული) */}
                            <button
                                className={`action-icon heart-icon ${isFavoriteShow ? 'active' : ''}`}
                                onClick={(e) => { e.stopPropagation(); handleFavoriteToggle(id); }}
                                title="Favorite"
                            >
                                {isFavoriteShow ? '❤️' : '♡'}
                            </button>

                            {/* PLAN TO WATCH (ვარსკვლავი) */}
                            <button
                                className={`action-icon star-icon ${currentShowStatus === 'PLAN_TO_WATCH' ? 'active' : ''}`}
                                onClick={(e) => { e.stopPropagation(); handleStatusUpdate(id, 'PLAN_TO_WATCH'); }}
                                title="Plan to Watch"
                            >
                                ★
                            </button>

                            {/* DROPPED (X აიქონი) */}
                            <button
                                className={`action-icon drop-icon ${currentShowStatus === 'DROPPED' ? 'active' : ''}`}
                                onClick={(e) => { e.stopPropagation(); handleStatusUpdate(id, 'DROPPED'); }}
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
            <header className="shows-header">
                <div className="filter-wrapper">
                    <select value={selectedGenre} onChange={handleGenreChange} className="glass-select">
                        {GENRES.map(genre => (
                            <option key={genre.id} value={genre.id} className="dark-option">{genre.name}</option>
                        ))}
                    </select>
                </div>
                <form onSubmit={handleSearch} className="search-form">
                    <input
                        type="text"
                        placeholder="Search for TV shows..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        className="glass-input"
                    />
                    <button type="submit" className="neon-button">Search</button>
                </form>
            </header>

            {searchMode === 'trending' && trendingShowsList.length > 0 && (
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
                                <button onClick={() => fetchGridShows(currentPage - 1)} disabled={currentPage === 1} className="pagination-button">⟨ Prev</button>
                                <span className="pagination-info">Page <strong className="neon-text">{currentPage}</strong> of {totalPages}</span>
                                <button onClick={() => fetchGridShows(currentPage + 1)} disabled={currentPage === totalPages} className="pagination-button">Next ⟩</button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}