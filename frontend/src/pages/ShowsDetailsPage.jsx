import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import '../style/ShowsDetailsPage.css';

function ShowsDetailsPage() {
    const { id } = useParams();
    const [showData, setShowData] = useState(null);
    const [activeStatus, setActiveStatus] = useState(null);
    const [isFavorite, setIsFavorite] = useState(false);
    const [watchedEpisodes, setWatchedEpisodes] = useState([]);
    const [selectedSeason, setSelectedSeason] = useState(1);
    const [seasonEpisodes, setSeasonEpisodes] = useState([]);

    // 🟢 ტოკენისა და იუზერნეიმის სტაბილური ამოღება
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
    const username = decodedToken?.sub || decodedToken?.username || null;

    useEffect(() => {
        fetch(`https://localhost:8443/api/shows/${id}`)
            .then(res => res.json())
            .then(data => setShowData(data))
            .catch(err => console.error("Error fetching show:", err));
    }, [id]);

    useEffect(() => {
        fetch(`https://localhost:8443/api/shows/${id}/season/${selectedSeason}`)
            .then(res => res.json())
            .then(data => {
                setSeasonEpisodes(data.episodes || []);
            })
            .catch(err => {
                console.error("Error fetching season episodes:", err);
                setSeasonEpisodes([]);
            });

        // ⚡ თუ იუზერნეიმი არსებობს, მხოლოდ მაშინ მოგვაქვს მონაცემები
        if (username) {
            fetch(`https://localhost:8443/api/tracking/watched-episodes?username=${username}&showId=${id}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            })
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP error! Status: ${res.status}`);
                    return res.text();
                })
                .then(text => {
                    const data = text ? JSON.parse(text) : [];
                    setWatchedEpisodes(data || []);
                })
                .catch(err => console.error("Error fetching watched episodes:", err));

            fetch(`https://localhost:8443/api/tracking/get-status?username=${username}&showId=${id}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            })
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP error! Status: ${res.status}`);
                    return res.text();
                })
                .then(text => {
                    const data = text ? JSON.parse(text) : null;
                    if (data) {
                        setActiveStatus(data.status);
                        setIsFavorite(data.favorite);
                    }
                })
                .catch(err => console.error("Error fetching status:", err));
        }
    }, [id, selectedSeason, token, username]);

    const handleStatusUpdate = (statusName) => {
        if (!username) {
            console.error("User is not logged in!");
            return;
        }

        const newStatus = activeStatus === statusName ? null : statusName;
        setActiveStatus(newStatus);

        fetch(`https://localhost:8443/api/tracking/show-status?username=${username}&showId=${id}&status=${newStatus !== null ? newStatus : ''}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
            .then((res) => {
                if (!res.ok) throw new Error("Failed to update status");

                if (newStatus === 'COMPLETED') {
                    fetch(`https://localhost:8443/api/tracking/watch-all-episodes?username=${username}&showId=${id}`, {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    })
                        .then(episodeRes => {
                            if (!episodeRes.ok) throw new Error("Backend failed to sync episodes");
                            return fetch(`https://localhost:8443/api/tracking/watched-episodes?username=${username}&showId=${id}`, {
                                headers: { 'Authorization': `Bearer ${token}` }
                            });
                        })
                        .then(res => res && res.text())
                        .then(text => {
                            const data = text ? JSON.parse(text) : [];
                            if (data) setWatchedEpisodes(data);
                        })
                        .catch(err => console.error("Error setting all watched:", err));
                }

                else if (newStatus === null) {
                    fetch(`https://localhost:8443/api/tracking/unwatch-all-episodes?username=${username}&showId=${id}`, {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    })
                        .then(episodeRes => {
                            if (!episodeRes.ok) throw new Error("Backend failed to clear episodes");
                            setWatchedEpisodes([]);
                        })
                        .catch(err => console.error("Error clearing watched episodes:", err));
                }
            })
            .catch(err => {
                console.error("Request failed:", err);
                setActiveStatus(activeStatus);
            });
    };

    const handleFavoriteToggle = () => {
        if (!username) return;
        setIsFavorite(!isFavorite);

        fetch(`https://localhost:8443/api/tracking/toggle-favorite?username=${username}&showId=${id}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .catch(err => console.error("Favorite toggle failed:", err));
    };

    const handleEpisodeToggle = (seasonNum, episodeNum) => {
        if (!username) return;

        if (activeStatus === 'COMPLETED') {
            setActiveStatus('WATCHING');
            fetch(`https://localhost:8443/api/tracking/show-status?username=${username}&showId=${id}&status=WATCHING`, {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${token}` }
            }).catch(err => console.error(err));
        }

        fetch(`https://localhost:8443/api/tracking/toggle-episode?username=${username}&showId=${id}&seasonNumber=${seasonNum}&episodeNumber=${episodeNum}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` }
        })
            .then(() => {
                const isAlreadyWatched = watchedEpisodes.some(ep => ep.seasonNumber === seasonNum && ep.episodeNumber === episodeNum);
                if (isAlreadyWatched) {
                    setWatchedEpisodes(watchedEpisodes.filter(ep => !(ep.seasonNumber === seasonNum && ep.episodeNumber === episodeNum)));
                } else {
                    setWatchedEpisodes([...watchedEpisodes, { seasonNumber: seasonNum, episodeNumber: episodeNum }]);
                }
            })
            .catch(err => console.error("Episode toggle failed:", err));
    };

    const isEpisodeWatched = (seasonNum, episodeNum) => {
        if (activeStatus === 'COMPLETED') {
            return true;
        }
        return watchedEpisodes.some(ep => ep.seasonNumber === seasonNum && ep.episodeNumber === episodeNum);
    };

    const carouselRef = useRef(null);

    const scrollCarousel = (direction) => {
        if (carouselRef.current) {
            const scrollAmount = direction === 'left' ? -600 : 600;
            carouselRef.current.scrollBy({
                left: scrollAmount,
                behavior: 'smooth'
            });
        }
    };

    if (!showData) return <div className="text-white text-center mt-10">Loading show details...</div>;

    const backdropUrl = showData.backdrop_path
        ? `https://image.tmdb.org/t/p/original${showData.backdrop_path}`
        : '';

    return (
        <div className="details-page-wrapper">
            {backdropUrl && (
                <div
                    className="backdrop-bg-layer"
                    style={{ backgroundImage: `url(${backdropUrl})` }}
                />
            )}

            <div className="details-container">
                <aside className="left-panel">
                    <div className="poster-wrapper">
                        {showData.poster_path ? (
                            <img
                                src={`https://image.tmdb.org/t/p/w300${showData.poster_path}`}
                                alt={showData.name}
                                className="details-poster"
                            />
                        ) : (
                            <div className="no-image-placeholder">No Image</div>
                        )}
                    </div>

                    {username && (
                        <div className="letterboxd-actions-wrapper">
                            <div className="letterboxd-actions">
                                <button
                                    onClick={() => {
                                        if (!activeStatus) handleStatusUpdate('WATCHING');
                                        else if (activeStatus === 'WATCHING') handleStatusUpdate('COMPLETED');
                                        else handleStatusUpdate(null);
                                    }}
                                    className={`action-icon eye-icon ${
                                        activeStatus === 'WATCHING' ? 'active-half' : activeStatus === 'COMPLETED' ? 'active-full' : ''
                                    }`}
                                    title="Mark as Watched / Watching"
                                >
                                    👁
                                </button>

                                <button
                                    onClick={handleFavoriteToggle}
                                    className={`action-icon heart-icon ${isFavorite ? 'active' : ''}`}
                                    title="Favorite"
                                >
                                    {isFavorite ? '❤️' : '♡'}
                                </button>

                                <button
                                    onClick={() => handleStatusUpdate('PLAN_TO_WATCH')}
                                    className={`action-icon star-icon ${activeStatus === 'PLAN_TO_WATCH' ? 'active' : ''}`}
                                    title="Plan to Watch"
                                >
                                    ★
                                </button>

                                <button
                                    onClick={() => handleStatusUpdate('DROPPED')}
                                    className={`action-icon drop-icon ${activeStatus === 'DROPPED' ? 'active' : ''}`}
                                    title="Dropped"
                                >
                                    ✕
                                </button>
                            </div>

                            <button className="design-btn log-btn">➕ LOG</button>
                            <button className="design-btn list-btn">Add to list</button>
                        </div>
                    )}
                </aside>

                <main className="right-panel">
                    <h1 className="show-title-main">{showData.name}</h1>
                    <div className="show-rating-main">⭐ {showData.vote_average?.toFixed(1)} / 10</div>
                    <p className="show-overview-main">{showData.overview}</p>

                    <div className="placeholder-tabs"></div>
                    <hr className="panel-divider" />

                    <div className="episodes-section-container">
                        <div className="season-selector-container">
                            <select
                                value={selectedSeason}
                                onChange={(e) => setSelectedSeason(Number(e.target.value))}
                                className="season-dropdown-btn"
                            >
                                {showData.seasons?.map(s => (
                                    <option key={s.id} value={s.season_number}>
                                        ▾ Seasons ({s.season_number})
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="carousel-wrapper-relative">
                            <button className="carousel-arrow-btn left-arrow" onClick={() => scrollCarousel('left')}>⟨</button>

                            <div className="episodes-carousel-track" ref={carouselRef}>
                                {seasonEpisodes?.map((episode) => {
                                    const isWatched = isEpisodeWatched(selectedSeason, episode.episode_number);

                                    const episodeImgUrl = episode.still_path
                                        ? `https://image.tmdb.org/t/p/w300${episode.still_path}`
                                        : 'https://via.placeholder.com/300x169/182027/99aabb?text=No+Image';

                                    return (
                                        <div key={episode.id} className="episode-carousel-card">
                                            <div className="ep-card-media">
                                                <img src={episodeImgUrl} alt={episode.name} className="ep-card-img" />

                                                <div
                                                    className={`ep-hover-overlay ${isWatched ? 'is-watched' : ''}`}
                                                    onClick={() => handleEpisodeToggle(selectedSeason, episode.episode_number)}
                                                >
                                                    <span className="ep-hover-eye-icon">{isWatched ? '✓' : '👁'}</span>
                                                </div>

                                                {episode.vote_average > 0 && (
                                                    <div className="ep-card-rating">⭐ {episode.vote_average.toFixed(1)}</div>
                                                )}
                                            </div>

                                            <div className="ep-card-info">
                                                <span className="ep-card-meta">Episode {episode.episode_number}</span>
                                                <h4 className="ep-card-title" title={episode.name}>{episode.name}</h4>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>

                            <button className="carousel-arrow-btn right-arrow" onClick={() => scrollCarousel('right')}>⟩</button>
                        </div>
                    </div>

                    <div className="reviews-section-future" style={{ marginTop: '50px', paddingBottom: '100px' }}></div>
                </main>
            </div>
        </div>
    );
}

export default ShowsDetailsPage;