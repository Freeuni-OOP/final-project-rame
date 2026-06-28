import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { PlusCircle, Bell } from 'lucide-react';
import '../style/Header.css';

export default function Header() {
    const navigate = useNavigate();
    const location = useLocation();
    const [searchQuery, setSearchQuery] = useState('');
    const [unreadCount, setUnreadCount] = useState(0);
    const [recommendations, setRecommendations] = useState([]);
    const [isBellOpen, setIsBellOpen] = useState(false);
    const bellRef = useRef(null);

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        setSearchQuery(params.get('query') || '');
    }, [location.search]);

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (token) => {
        if (!token) return null;
        try { return JSON.parse(atob(token.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const username = decodedToken?.sub;

    const fetchUnreadCount = () => {
        if (!username || !token) return;
        fetch(`https://localhost:8443/api/tracking/recommendations/unread-count?username=${username}`, {
            headers: { Authorization: `Bearer ${token}` }
        })
            .then(res => res.ok ? res.json() : 0)
            .then(count => setUnreadCount(count))
            .catch(err => console.error("Error fetching unread count:", err));
    };

    const fetchRecommendations = () => {
        if (!username || !token) return;
        fetch(`https://localhost:8443/api/tracking/recommendations?username=${username}`, {
            headers: { Authorization: `Bearer ${token}` }
        })
            .then(res => res.ok ? res.json() : [])
            .then(data => setRecommendations(data))
            .catch(err => console.error("Error fetching recommendations:", err));
    };

    useEffect(() => {
        fetchUnreadCount();
        const interval = setInterval(fetchUnreadCount, 15000);
        return () => clearInterval(interval);
    }, [username, token]);

    useEffect(() => {
        function handleClickOutside(event) {
            if (bellRef.current && !bellRef.current.contains(event.target)) {
                setIsBellOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    const handleBellClick = () => {
        if (!isBellOpen) {
            fetchRecommendations();
            setIsBellOpen(true);
            if (unreadCount > 0) {
                fetch(`https://localhost:8443/api/tracking/recommendations/mark-read?username=${username}`, {
                    method: 'POST',
                    headers: { Authorization: `Bearer ${token}` }
                })
                    .then(res => { if (res.ok) setUnreadCount(0); })
                    .catch(err => console.error("Error marking read:", err));
            }
        } else {
            setIsBellOpen(false);
        }
    };

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        const params = new URLSearchParams(location.search);
        if (searchQuery.trim()) { params.set('query', searchQuery); } else { params.delete('query'); }
        navigate(`/shows?${params.toString()}`);
    };

    // დამხმარე ფუნქცია ავატარის ფერისთვის (რომ პირველ სურათზე მწვანე "S" როა, ისე დარჩეს)
    const getAvatarColor = (name) => {
        if (!name) return '#00b4a2';
        const colors = ['#00b4a2', '#e85d75', '#f2b134', '#5b8def'];
        return colors[name.charCodeAt(0) % colors.length];
    };

    return (
        <header className="main-header">
            <div className="header-container">
                <div className="header-left">
                    <Link to="/" className="header-logo">LOGO</Link>
                    <nav className="header-nav">
                        <Link to="/shows" className="nav-item">TV Series</Link>
                        <Link to="/lists" className="nav-item">Lists</Link>
                    </nav>
                </div>

                <div className="header-middle">
                    <form onSubmit={handleSearchSubmit} className="header-search-form">
                        <input
                            type="text"
                            placeholder="Search for TV shows..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="glass-input-header"
                        />
                        <button type="submit" className="neon-button-header">Search</button>
                    </form>
                </div>

                <div className="header-right">
                    {username ? (
                        <>
                            {/* ზარი და ჩამოსაშლელი Dropdown */}
                            <div className="header-bell-container" ref={bellRef} style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                                <div className="header-bell-wrapper" onClick={handleBellClick} style={{ cursor: 'pointer', position: 'relative', display: 'flex', alignItems: 'center' }}>
                                    <Bell size={20} className="header-bell-icon" style={{ color: '#9ab3c8', transition: 'color 0.2s' }} />
                                    {unreadCount > 0 && (
                                        <span className="header-bell-badge" style={{ position: 'absolute', top: '-6px', right: '-6px', background: '#00ffd5', color: '#0d1117', fontSize: '10px', fontWeight: 'bold', borderRadius: '50%', width: '15px', height: '15px', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 0 8px #00ffd5' }}>
                                            {unreadCount}
                                        </span>
                                    )}
                                </div>

                                {isBellOpen && (
                                    <div className="bell-dropdown" style={{ position: 'absolute', top: '35px', right: '0', width: '300px', background: '#1c2530', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '8px', boxShadow: '0 10px 25px rgba(0,0,0,0.5)', zIndex: 9999, overflow: 'hidden' }}>
                                        <div style={{ padding: '10px 14px', borderBottom: '1px solid rgba(255,255,255,0.05)', fontSize: '13px', fontWeight: 'bold', color: '#fff' }}>
                                            Recommendations
                                        </div>
                                        <div style={{ maxHeight: '240px', overflowY: 'auto' }}>
                                            {recommendations.length === 0 ? (
                                                <div style={{ padding: '15px', textAlign: 'center', color: '#8b949e', fontSize: '12px' }}>No new recommendations</div>
                                            ) : (
                                                recommendations.slice(0, 5).map((rec) => (
                                                    <div key={rec.id} onClick={() => { setIsBellOpen(false); navigate(`/shows/${rec.showId}`); }} style={{ padding: '10px 14px', borderBottom: '1px solid rgba(255,255,255,0.02)', cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: '2px', transition: 'background 0.2s' }} onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.03)'} onMouseLeave={(e) => e.currentTarget.style.background = 'transparent'}>
                                                        <span style={{ fontSize: '12px', color: '#c9d1d9' }}><strong>{rec.senderUsername}</strong> recommended:</span>
                                                        <span style={{ fontSize: '12px', color: '#00ffd5', fontWeight: '500' }}>{rec.showName}</span>
                                                        {rec.comment && <span style={{ fontSize: '11px', color: '#8b949e', fontStyle: 'italic' }}>"{rec.comment}"</span>}
                                                    </div>
                                                ))
                                            )}
                                        </div>
                                    </div>
                                )}
                            </div>

                            <button className="header-log-btn" onClick={() => navigate('/log')}>
                                <PlusCircle size={16} />
                                <span>LOG</span>
                            </button>

                            <div className="header-profile-wrapper" onClick={() => navigate('/profile')} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '10px' }}>
                                <span className="header-username" style={{ color: '#fff', fontWeight: 'bold' }}>{username}</span>
                                <div className="header-avatar" style={{ backgroundColor: getAvatarColor(username), width: '32px', height: '32px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontWeight: 'bold', fontSize: '14px' }}>
                                    {username.charAt(0).toUpperCase()}
                                </div>
                            </div>
                        </>
                    ) : (
                        <div className="header-profile-wrapper" onClick={() => navigate('/login')}>
                            <span className="header-username" style={{ color: '#5f758a' }}>Guest</span>
                            <div className="header-avatar" style={{ backgroundColor: 'rgba(255,255,255,0.1)', color: '#5f758a' }}>?</div>
                        </div>
                    )}
                </div>
            </div>
        </header>
    );
}