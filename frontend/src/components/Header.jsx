import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { PlusCircle } from 'lucide-react';
import '../style/Header.css';

export default function Header() {
    const navigate = useNavigate();
    const location = useLocation();
    const [searchQuery, setSearchQuery] = useState('');


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

    const handleSearchSubmit = (e) => {
        e.preventDefault();
        const params = new URLSearchParams(location.search); // ვინარჩუნებთ მიმდინარე ჟანრსაც თუ არის

        if (searchQuery.trim()) {
            params.set('query', searchQuery);
        } else {
            params.delete('query');
        }

        navigate(`/shows?${params.toString()}`);
    };

    return (
        <header className="main-header">
            <div className="header-container">

                <div className="header-left">
                    <Link to="/" className="header-logo">LOGO</Link>
                    <nav className="header-nav">
                        <Link to="/shows" className="nav-item">TV Series</Link>
                        <Link to="/lists" className="nav-item">Lists</Link>
                        <Link to="/friends" className="nav-item">Members</Link>
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
                            {/* ავტორიზებული იუზერი */}
                            <button className="header-log-btn" onClick={() => navigate('/log')}>
                                <PlusCircle size={16} />
                                <span>LOG</span>
                            </button>

                            <div className="header-profile-wrapper" onClick={() => navigate('/profile')}>
                                <span className="header-username">{username}</span>
                                <div className="header-avatar">
                                    {username.charAt(0).toUpperCase()}
                                </div>
                            </div>
                        </>
                    ) : (
                        <>
                            {/* არაავტორიზებული იუზერი (სტუმარი) */}
                            <div className="header-profile-wrapper" onClick={() => navigate('/login')}>
                                <span className="header-username" style={{ color: '#5f758a' }}>Guest</span>
                                <div className="header-avatar" style={{ backgroundColor: 'rgba(255,255,255,0.1)', color: '#5f758a', boxShadow: 'none' }}>
                                    ?
                                </div>
                            </div>
                        </>
                    )}
                </div>

            </div>
        </header>
    );
}