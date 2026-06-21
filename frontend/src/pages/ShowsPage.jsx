import React, { useState, useEffect } from 'react';
import '../style/ShowsPage.css'; // სტილებისთვის

export default function ShowsPage() {
    const [shows, setShows] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [loading, setLoading] = useState(false);
    const [title, setTitle] = useState('Trending This Week');

    // ბაზისური URL ჩვენი ბექენდისთვის
    const BASE_URL = 'https://localhost:8443/api/shows';

    // ტრენდული სერიალების წამოღება
    const fetchTrendingShows = async () => {
        setLoading(true);
        try {
            const response = await fetch(`${BASE_URL}/trending`);
            if (!response.ok) throw new Error('Failed to fetch trending');
            const data = await response.json();
            setShows(data.results || []);
            setTitle('Trending This Week');
        } catch (error) {
            console.error("Error loading shows:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchTrendingShows();
    }, []);

    // ძებნის ფუნქციონალი
    const handleSearch = async (e) => {
        e.preventDefault();
        if (!searchQuery.trim()) {
            fetchTrendingShows();
            return;
        }

        setLoading(true);
        try {
            const response = await fetch(`${BASE_URL}/search?query=${encodeURIComponent(searchQuery)}`);
            if (!response.ok) throw new Error('Search failed');
            const data = await response.json();
            setShows(data.results || []);
            setTitle(`Search Results for "${searchQuery}"`);
        } catch (error) {
            console.error("Search error:", error);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="shows-container">
            {/* Search Bar კომპონენტი */}
            <form onSubmit={handleSearch} className="search-form">
                <input
                    type="text"
                    placeholder="Search for TV shows..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="search-input"
                />
                <button type="submit" className="search-button">Search</button>
            </form>

            <h2 className="section-title">{title}</h2>

            {loading ? (
                <div className="loading">Loading shows...</div>
            ) : (
                <div className="shows-grid">
                    {shows.map((show) => (
                        <div key={show.id} className="show-card">
                            <div className="card-image-wrapper">
                                {show.poster_path ? (
                                    <img
                                        src={`https://image.tmdb.org/t/p/w500${show.poster_path}`}
                                        alt={show.name}
                                        className="show-poster"
                                    />
                                ) : (
                                    <div className="no-poster">No Image Available</div>
                                )}
                                <div className="show-rating">
                                    ★ {show.vote_average ? show.vote_average.toFixed(1) : 'N/A'}
                                </div>
                            </div>
                            <div className="show-info">
                                <h3 className="show-title">{show.name}</h3>
                                <p className="show-date">First Air: {show.first_air_date || 'Unknown'}</p>
                                <p className="show-overview">
                                    {show.overview ? show.overview.slice(0, 100) + '...' : 'No description available.'}
                                </p>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}