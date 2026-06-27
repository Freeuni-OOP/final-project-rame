import React from 'react';
import '../style/ListsPage.css';

// No "Lists" feature exists on the backend yet (no List entity, repo,
// service, or controller anywhere in the codebase) — every list, creator,
// and stat on this page is fixed placeholder content purely to show the
// intended layout, mirroring the Letterboxd Lists page. The "Upgrade to
// Pro" ad banner from the reference screenshot was intentionally skipped
// since it's monetization-specific and not relevant to this app.

const FEATURED_LISTS = [
    { title: 'Top 500 Narrative Feature Films', creator: 'Official Lists', official: true },
    { title: 'Most Fans on Letterboxd', creator: 'Official Lists', official: true },
    { title: 'One Million Watched Club', creator: 'Alexander', official: false },
];

const POPULAR_LISTS = [
    { title: 'Movies To Fuel Your Misandry', creator: 'sapphixx', films: 23, likes: '6.4K', comments: 336 },
    { title: "Letterboxd's Top 500 Films", creator: 'Official Lists', official: true, films: 500, likes: '393K', comments: '33K' },
    { title: 'Movies everyone should watch at least once during their lifetime', creator: 'fcbarcelona', films: 800, likes: '404K', comments: '1.8K' },
];

const RECENTLY_LIKED = [
    { title: "⋆.✦ 2000's", creator: 'gabi ✦.*', films: 31, likes: 480, comments: 2, desc: "iconic 2000's girly films ⋆.°." },
    { title: 'favorites 🎵🍓', creator: '*.lisraa', films: 30, likes: 4 },
    { title: 'Giant Insects & Naked Ladies!', creator: 'Funktual', films: 26, likes: 1 },
    { title: "2000's", creator: 'paden19', films: 150, likes: '2.5K', comments: 7, desc: "literally every 2000's chick flick you can think of and ones you don't even know about <3" },
];

const CREW_PICKS = [
    { title: 'Summerween', creator: 'Luke', films: 52 },
    { title: 'lesbian movies for fucked up girls', creator: 'kind_cruelty', films: 150 },
    { title: 'you seem pretty sad for a girl' },
];

const RECENT_SHOWDOWNS = [
    { title: 'Bright Lights', subtitle: 'Best Las Vegas films', inProgress: true },
    { title: 'Love Lies Bleeding', subtitle: 'Best "be gay, do crime" films' },
    { title: "Short 'n' Sweet", subtitle: 'Best adaptation of short to feature' },
];

const CREW_LISTS = [
    { title: 'Sad girls in love starter pack' },
    { title: 'Every film available on Letterboxd Video Store' },
    { title: "Under The Hood: A Century's Worth of Robin Hood On Screen" },
];

const AVATAR_COLORS = ['#00b4a2', '#e85d75', '#f2b134', '#5b8def', '#9b59b6', '#2ecc71'];

function colorForName(name) {
    if (!name) return AVATAR_COLORS[0];
    const code = name.charCodeAt(0) || 0;
    return AVATAR_COLORS[code % AVATAR_COLORS.length];
}

function MiniAvatar({ name, size = 22 }) {
    const initial = name ? name.trim().charAt(0).toUpperCase() : '?';
    return (
        <div
            className="lp-avatar"
            style={{ width: size, height: size, backgroundColor: colorForName(name), fontSize: size * 0.5 }}
        >
            {initial}
        </div>
    );
}

function OfficialBadge() {
    return (
        <span className="lp-official-badge" aria-label="Official Lists">
            <span className="lp-official-dot lp-dot-orange" />
            <span className="lp-official-dot lp-dot-green" />
            <span className="lp-official-dot lp-dot-blue" />
        </span>
    );
}

function HeartIcon() {
    return (
        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 21l7.8-7.8 1-1a5.5 5.5 0 0 0 0-7.8z" />
        </svg>
    );
}

function CommentIcon() {
    return (
        <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
            <path d="M4 4h16a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H9l-5 4V5a1 1 0 0 1 1-1z" />
        </svg>
    );
}

function PosterStrip({ size = 'lg' }) {
    return (
        <div className={`lp-poster-strip lp-poster-strip-${size}`}>
            {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className={`lp-poster-strip-item lp-poster-strip-item-${size}`} />
            ))}
        </div>
    );
}

function ListCard({ item }) {
    return (
        <div className="lp-card">
            <PosterStrip size="lg" />
            <div className="lp-card-title">{item.title}</div>
            {item.creator && (
                <div className="lp-card-meta">
                    {item.official ? <OfficialBadge /> : <MiniAvatar name={item.creator} />}
                    <span>
                        Created by <span className="lp-creator-name">{item.creator}</span>
                    </span>
                </div>
            )}
            {(item.films !== undefined) && (
                <div className="lp-card-stats">
                    {item.films !== undefined && <span className="lp-stat">{item.films} films</span>}
                    {item.likes !== undefined && <span className="lp-stat"><HeartIcon /> {item.likes}</span>}
                    {item.comments !== undefined && <span className="lp-stat"><CommentIcon /> {item.comments}</span>}
                </div>
            )}
        </div>
    );
}

export default function ListsPage() {
    return (
        <div className="lp-page">
            <main className="lp-main">
                <p className="lp-tagline">Collect, curate, and share. Lists are the perfect way to group films.</p>
                <div className="lp-cta-wrap">
                    <button className="lp-start-btn">Start your own list</button>
                </div>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">Featured Lists</span>
                        <span className="lp-section-link">All &bull; Official</span>
                    </div>
                    <div className="lp-grid-3">
                        {FEATURED_LISTS.map((item, i) => <ListCard key={i} item={item} />)}
                    </div>
                </section>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">Popular This Week</span>
                        <span className="lp-section-link">More</span>
                    </div>
                    <div className="lp-grid-3">
                        {POPULAR_LISTS.map((item, i) => <ListCard key={i} item={item} />)}
                    </div>
                </section>

                <div className="lp-two-col">
                    <section className="lp-section lp-recently-liked">
                        <div className="lp-section-header">
                            <span className="lp-kicker">Recently Liked</span>
                        </div>
                        <div className="lp-liked-list">
                            {RECENTLY_LIKED.map((item, i) => (
                                <div key={i} className="lp-liked-row">
                                    <PosterStrip size="sm" />
                                    <div className="lp-liked-content">
                                        <div className="lp-liked-title">{item.title}</div>
                                        <div className="lp-liked-meta">
                                            <MiniAvatar name={item.creator} size={18} />
                                            <span className="lp-creator-name">{item.creator}</span>
                                            <span className="lp-stat">{item.films} films</span>
                                            {item.likes !== undefined && <span className="lp-stat"><HeartIcon /> {item.likes}</span>}
                                            {item.comments !== undefined && <span className="lp-stat"><CommentIcon /> {item.comments}</span>}
                                        </div>
                                        {item.desc && <p className="lp-liked-desc">{item.desc}</p>}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </section>

                    <section className="lp-section lp-crew-picks">
                        <div className="lp-section-header">
                            <span className="lp-kicker">Crew Picks</span>
                        </div>
                        <div className="lp-crew-picks-list">
                            {CREW_PICKS.map((item, i) => (
                                <div key={i} className="lp-crew-pick-item">
                                    <PosterStrip size="sm" />
                                    <div className="lp-crew-pick-title">{item.title}</div>
                                    {item.creator && (
                                        <div className="lp-crew-pick-meta">
                                            <MiniAvatar name={item.creator} size={18} />
                                            <span className="lp-creator-name">{item.creator}</span>
                                            {item.films !== undefined && <span className="lp-stat">{item.films} films</span>}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </section>
                </div>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">Recent Showdowns</span>
                        <span className="lp-section-link">More</span>
                    </div>
                    <div className="lp-grid-3">
                        {RECENT_SHOWDOWNS.map((item, i) => (
                            <div key={i} className="lp-showdown-card">
                                <div className="lp-showdown-image">
                                    {item.inProgress && <span className="lp-showdown-badge">In Progress</span>}
                                </div>
                                <div className="lp-showdown-title">{item.title}</div>
                                <div className="lp-showdown-subtitle">{item.subtitle}</div>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="lp-section">
                    <div className="lp-section-header">
                        <span className="lp-kicker">Crew Lists</span>
                        <span className="lp-section-link">More</span>
                    </div>
                    <div className="lp-grid-3">
                        {CREW_LISTS.map((item, i) => <ListCard key={i} item={item} />)}
                    </div>
                </section>
            </main>
        </div>
    );
}
