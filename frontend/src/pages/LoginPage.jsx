import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import CustomInput from '../components/CustomInput';

const LoginPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');

        try {
            // 🔄 შეცვლილია Axios -> ჩვეულებრივ fetch-ზე
            const response = await fetch("https://localhost:8443/api/auth/login", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, password })
            });

            if (!response.ok) {
                throw new Error("Invalid username or password");
            }

            const token = await response.text();
            localStorage.setItem('token', token);

            alert("Login successful!");
          //  navigate('/dashboard');
        } catch (err) {
            setError(err.message || "Something went wrong. Please try again.");
        }
    };

    return (
        <div className="register-bg">
            <div className="register-card">
                <h2 style={{ marginBottom: '25px', color: '#fff', textAlign: 'center', fontSize: '28px', fontWeight: 'bold' }}>Sign In</h2>
                {error && <p style={{ color: '#ff6b6b', marginBottom: '15px', textAlign: 'center' }}>{error}</p>}

                <form onSubmit={handleLogin}>
                    <CustomInput type="text"  placeholder="Username or Email" value={username} onChange={(e) => setUsername(e.target.value)} required />
                    <CustomInput type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />

                    <button type="submit" className="signup-btn">
                        Sign In
                    </button>
                </form>

                <p style={{ marginTop: '25px', textAlign: 'center', color: 'rgba(255, 255, 255, 0.6)', fontSize: '14px' }}>
                    New to Serial Tracker?{' '}
                    <Link to="/register" style={{ color: '#14b8a6', textDecoration: 'none', fontWeight: '600', marginLeft: '5px' }}>
                        Sign up now
                    </Link>
                </p>
            </div>
        </div>
    );
};

export default LoginPage;