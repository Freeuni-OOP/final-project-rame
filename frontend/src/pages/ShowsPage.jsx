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
    const [trendingShows, setTrendingShows] = useState([]);
    const [allShows, setAllShows] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedGenre, setSelectedGenre] = useState('all');
    const [loading, setLoading] = useState(false);
    const [gridTitle, setGridTitle] = useState('Explore TV Shows');

    const [currentPage, setCurrentPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [searchMode, setSearchMode] = useState('trending');

    const navigate = useNavigate();

    // 🚀 დროებითი ვიზუალური სთეითები Letterboxd-ის ეფექტებისთვის
    const [watchedStatus, setWatchedStatus] = useState({}); // 'watched', 'watching', ან null
    const [favorites, setFavorites] = useState({});         // true/false
    const [planToWatch, setPlanToWatch] = useState({});     // true/false
    const [dropped, setDropped] = useState({});             // true/false

    const BASE_URL = 'https://localhost:8443/api/shows';
    const ribbonRef = useRef(null);

    useEffect(() => {
        fetchRibbonTrending();
        fetchGridShows(1, 'trending', '', 'all');
    }, []);

    const fetchRibbonTrending = async () => {
        try {
            const response = await fetch(`${BASE_URL}/trending?page=1`);
            if (response.ok) {
                const data = await response.json();
                setTrendingShows(data.results || []);
            }
        } catch (error) {
            System.out.println("Error loading ribbon: " + error);
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


    const toggleFavorite = (id) => setFavorites(prev => ({ ...prev, [id]: !prev[id] }));
    const toggleWatched = (id) => {

        setDropped(prev => ({ ...prev, [id]: false }));
        setPlanToWatch(prev => ({ ...prev, [id]: false }));

        setWatchedStatus(prev => {
            const current = prev[id];
            if (!current) return { ...prev, [id]: 'watched' };   // 1 კლიკი: Watched
            if (current === 'watched') return { ...prev, [id]: 'watching' }; // 2 კლიკი: Watching
            return { ...prev, [id]: null };                       // 3 კლიკი: Off
        });
    };

    const togglePlan = (id) => {
        // თუ გეგმაში ვამატებთ, ავტომატურად იშლება Dropped და ნანახის სტატუსები
        setDropped(prev => ({ ...prev, [id]: false }));
        setWatchedStatus(prev => ({ ...prev, [id]: null }));

        setPlanToWatch(prev => ({ ...prev, [id]: !prev[id] }));
    };

    const toggleDropped = (id) => {
        // თუ სერიალს ვაგდებთ (Dropped), ავტომატურად უქმდება ნანახი/საყურებელი და გეგმები
        if (!dropped[id]) {
            setWatchedStatus(prev => ({ ...prev, [id]: null }));
            setPlanToWatch(prev => ({ ...prev, [id]: false }));
        }

        setDropped(prev => ({ ...prev, [id]: !prev[id] }));
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
        const isFav = favorites[id];
        const isPlan = planToWatch[id];
        const isDrop = dropped[id];
        const watchMode = watchedStatus[id]; // 'watched', 'watching', ან undefined

        // თვალის აიქონის დინამიკური კლასი
        let eyeClass = "action-icon eye-icon";
        if (watchMode === 'watched') eyeClass += " active-full";
        if (watchMode === 'watching') eyeClass += " active-half";

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


                    {/* დავამატეთ onClick={() => e.stopPropagation()} თითოეულ ღილაკზე,
                       რომ ღილაკზე კლიკმა არ გადაგვიყვანოს დეტალების გვერდზე */}
                    <div className="letterboxd-actions" onClick={(e) => e.stopPropagation()}>
                        {/* eye */}
                        <button
                            className={eyeClass}
                            onClick={(e) => { e.stopPropagation(); toggleWatched(id); }}
                            title={watchMode === 'watched' ? 'Watched' : watchMode === 'watching' ? 'Watching' : 'Mark as Watched/Watching'}
                        >
                            👁
                        </button>

                        {/* heart */}
                        <button
                            className={`action-icon heart-icon ${isFav ? 'active' : ''}`}
                            onClick={(e) => { e.stopPropagation(); toggleFavorite(id); }}
                            title="Favorite"
                        >
                            {isFav ? '❤️' : '♡'}
                        </button>

                        {/* star */}
                        <button
                            className={`action-icon star-icon ${isPlan ? 'active' : ''}`}
                            onClick={(e) => { e.stopPropagation(); togglePlan(id); }}
                            title="Plan to Watch"
                        >
                            ★
                        </button>

                        {/* X */}
                        <button
                            className={`action-icon drop-icon ${isDrop ? 'active' : ''}`}
                            onClick={(e) => { e.stopPropagation(); toggleDropped(id); }}
                            title="Dropped"
                        >
                            ✕
                        </button>
                    </div>
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

            {searchMode === 'trending' && trendingShows.length > 0 && (
                <div className="ribbon-section">
                    <div className="ribbon-header">
                        <h2 className="section-title">Trending This Week</h2>
                        <div className="ribbon-arrows">
                            <button onClick={() => ribbonRef.current.scrollLeft -= 500} className="arrow-btn">⟨</button>
                            <button onClick={() => ribbonRef.current.scrollLeft += 500} className="arrow-btn">⟩</button>
                        </div>
                    </div>
                    <div className="ribbon-track" ref={ribbonRef}>
                        {trendingShows.map(show => renderShowCard(show))}
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