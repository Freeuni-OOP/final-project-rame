import React, { useState, useEffect } from 'react';
import '../style/RecommendButton.css';

export default function RecommendButton({ showId, showName }) {
    const [friends, setFriends] = useState([]);
    const [searchQuery, setSearchQuery] = useState('');
    const [isOpen, setIsOpen] = useState(false);
    const [sentStatus, setSentStatus] = useState({}); // ინახავს რომელი იუზერისთვისაა უკვე გაგზავნილი (მაგ: { nika99: true })

    const tokenObj = localStorage.getItem('token');
    const token = tokenObj ? JSON.parse(tokenObj).token : null;

    const parseJwt = (t) => {
        if (!t) return null;
        try { return JSON.parse(atob(t.split('.')[1])); } catch (e) { return null; }
    };

    const decodedToken = parseJwt(token);
    const currentUsername = decodedToken?.sub;

    // მეგობრების წამოღება მოდალის გახსნისას
    useEffect(() => {
        if (isOpen && currentUsername && token) {
            fetch(`https://localhost:8443/api/friends?actingUsername=${currentUsername}`, {
                headers: { Authorization: `Bearer ${token}` }
            })
                .then(res => res.ok ? res.json() : [])
                .then(data => setFriends(data))
                .catch(err => console.error("Error fetching friends:", err));
        }
    }, [isOpen, currentUsername, token]);

    // რეკომენდაციის გაგზავნა კონკრეტულ მეგობართან
    const handleSendRecommend = (targetFriend) => {
        if (sentStatus[targetFriend]) return; // თუ უკვე გაგზავნილია, არაფერი ქნას

        const commentPlaceholder = "Check out this awesome show!"; // მოდალს რადგან ტექსტარეა არ აქვს, სტანდარტული ტექსტი გავაყოლოთ
        const url = `https://localhost:8443/api/tracking/recommend?senderUsername=${currentUsername}&targetUsername=${targetFriend}&showId=${showId}&showName=${encodeURIComponent(showName)}&comment=${encodeURIComponent(commentPlaceholder)}`;

        fetch(url, {
            method: 'POST',
            headers: { Authorization: `Bearer ${token}` }
        })
            .then(res => {
                if (res.ok) {
                    // მოვნიშნოთ ეს იუზერი როგორც "Sent"
                    setSentStatus(prev => ({ ...prev, [targetFriend]: true }));
                } else {
                    alert("Failed to send recommendation.");
                }
            })
            .catch(() => alert("Error connecting to server."));
    };

    if (!currentUsername) return null;

    // გავაფილტროთ მეგობრები ძებნის ველის მიხედვით
    const filteredFriends = friends.filter(friend =>
        friend.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="recommend-wrapper">
            {/* ✉️ პრემიუმ ნეონის ღილაკი სერიალის გვერდისთვის */}
            <button className="recommend-main-btn" onClick={() => setIsOpen(true)}>
                <span>✉️</span> Recommend to Friend
            </button>

            {/* ოვერლეი + მოდალური ბლოკი */}
            {isOpen && (
                <div className="rec-overlay" onClick={() => setIsOpen(false)}>
                    <div className="rec-modal" onClick={(e) => e.stopPropagation()}>

                        <div className="rec-header">
                            <h3>Recommend <span className="rec-neon-title">"{showName}"</span></h3>
                            <button className="rec-close" onClick={() => setIsOpen(false)}>✕</button>
                        </div>

                        {/* ძებნის ინპუტი */}
                        <input
                            type="text"
                            className="rec-search"
                            placeholder="Search friends..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                        />

                        {/* მეგობრების სია */}
                        <div className="rec-list">
                            {filteredFriends.length === 0 ? (
                                <div className="rec-empty">No friends found</div>
                            ) : (
                                filteredFriends.map(friendName => (
                                    <div key={friendName} className="rec-row">
                                        <div className="rec-user-info">
                                            <span className="rec-avatar">👤</span>
                                            <span className="rec-username">{friendName}</span>
                                        </div>

                                        <button
                                            className={`rec-action-btn ${sentStatus[friendName] ? 'sent' : ''}`}
                                            onClick={() => handleSendRecommend(friendName)}
                                            disabled={sentStatus[friendName]}
                                        >
                                            {sentStatus[friendName] ? '✓ Sent' : 'Send'}
                                        </button>
                                    </div>
                                ))
                            )}
                        </div>

                    </div>
                </div>
            )}
        </div>
    );
}
