import { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import '../style/ShowsDetailsPage.css';
import RecommendButton from '../components/RecommendButton';
import AddToListButton from '../components/AddToListButton';

function ShowsDetailsPage() {
    const { id } = useParams();
    const [showData, setShowData] = useState(null);
    const [activeStatus, setActiveStatus] = useState(null);
    const [isFavorite, setIsFavorite] = useState(false);
    const [watchedEpisodes, setWatchedEpisodes] = useState([]);
    const [selectedSeason, setSelectedSeason] = useState(1);
    const [seasonEpisodes, setSeasonEpisodes] = useState([]);
    const [reviews, setReviews] = useState([]);

    const carouselRef = useRef(null);

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

    // Fetch reviews - filtered by last watched episode (spoiler protection)
    useEffect(() => {
        const params = new URLSearchParams();
        if (username) params.append('username', username);

        let lastSeason = null;
        let lastEpisode = null;
        if (watchedEpisodes.length > 0) {
            watchedEpisodes.forEach(ep => {
                if (lastSeason === null ||
                    ep.seasonNumber > lastSeason ||
                    (ep.seasonNumber === lastSeason && ep.episodeNumber > lastEpisode)) {
                    lastSeason = ep.seasonNumber;
                    lastEpisode = ep.episodeNumber;
                }
            });
        }

        if (lastSeason !== null && lastEpisode !== null) {
            params.append('season', lastSeason);
            params.append('episode', lastEpisode);
        }

        fetch(`https://localhost:8443/api/reviews/${id}?${params.toString()}`)
            .then(res => res.ok ? res.json() : [])
            .then(data => setReviews(data || []))
            .catch(err => {
                console.error("Error fetching reviews:", err);
                setReviews([]);
            });
    }, [id, username, watchedEpisodes]);

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

    const friendReviews = reviews.filter(r => r.friend);
    const otherReviews = reviews.filter(r => !r.friend);

    const renderReview = (r, i) => {
        const initial = r.username ? r.username.charAt(0).toUpperCase() : '?';
        const badge = r.seasonNumber != null && r.episodeNumber != null
            ? `S${r.seasonNumber} E${r.episodeNumber}`
            : 'Whole show';
        return (
            <div key={i} className="review-item">
                <div className="review-avatar">{initial}</div>
                <div className="review-body">
                    <div className="review-header">
                        <span className="review-author">{r.username}</span>
                        {r.rating != null && r.rating > 0 && (
                            <span className="review-stars">{'\u2605'.repeat(r.rating)}</span>
                        )}
                        {r.liked && <span className="review-heart">{'\u2665'}</span>}
                        {badge && <span className="review-badge">{badge}</span>}
                        {r.rewatch && <span className="review-rewatch">{'\u21bb'} rewatch</span>}
                    </div>
                    {r.review && <p className="review-text">{r.review}</p>}
                </div>
            </div>
        );
    };

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
                                    {'\ud83d\udc41'}
                                </button>

                                <button
                                    onClick={handleFavoriteToggle}
                                    className={`action-icon heart-icon ${isFavorite ? 'active' : ''}`}
                                    title="Favorite"
                                >
                                    {isFavorite ? '\u2764\ufe0f' : '\u2661'}
                                </button>

                                <button
                                    onClick={() => handleStatusUpdate('PLAN_TO_WATCH')}
                                    className={`action-icon star-icon ${activeStatus === 'PLAN_TO_WATCH' ? 'active' : ''}`}
                                    title="Plan to Watch"
                                >
                                    {'\u2605'}
                                </button>

                                <button
                                    onClick={() => handleStatusUpdate('DROPPED')}
                                    className={`action-icon drop-icon ${activeStatus === 'DROPPED' ? 'active' : ''}`}
                                    title="Dropped"
                                >
                                    {'\u2715'}
                                </button>
                            </div>

                            <button className="design-btn log-btn">{'\u2795'} LOG</button>
                            <AddToListButton showId={showData.id} showName={showData.name} />

                            <RecommendButton showId={showData.id} showName={showData.name} />
                        </div>
                    )}
                </aside>

                <main className="right-panel">
                    <h1 className="show-title-main">{showData.name}</h1>
                    <div className="show-rating-main">{'\u2b50'} {showData.vote_average?.toFixed(1)} / 10</div>
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
                                        {'\u25be'} Seasons ({s.season_number})
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div className="carousel-wrapper-relative">
                            <button className="carousel-arrow-btn left-arrow" onClick={() => scrollCarousel('left')}>{'\u27e8'}</button>

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
                                                    <span className="ep-hover-eye-icon">{isWatched ? '\u2713' : '\ud83d\udc41'}</span>
                                                </div>

                                                {episode.vote_average > 0 && (
                                                    <div className="ep-card-rating">{'\u2b50'} {episode.vote_average.toFixed(1)}</div>
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

                            <button className="carousel-arrow-btn right-arrow" onClick={() => scrollCarousel('right')}>{'\u27e9'}</button>
                        </div>
                    </div>

                    <div className="reviews-section" style={{ marginTop: '50px', paddingBottom: '100px' }}>
                        <div className="reviews-block">
                            <div className="reviews-heading">Friends' reviews</div>
                            {friendReviews.length > 0
                                ? friendReviews.map(renderReview)
                                : <p className="reviews-empty">None of your friends have reviewed this yet</p>}
                        </div>

                        <div className="reviews-block" style={{ marginTop: '40px' }}>
                            <div className="reviews-heading">All reviews</div>
                            {otherReviews.length > 0
                                ? otherReviews.map(renderReview)
                                : <p className="reviews-empty">No reviews yet</p>}
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
}

export default ShowsDetailsPage;
