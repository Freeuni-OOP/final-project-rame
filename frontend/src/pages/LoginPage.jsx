import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import CustomInput from '../components/CustomInput';

const LoginPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await axios.post("https://localhost:8443/api/auth/login", { username, password });
            localStorage.setItem('token', response.data); // ვინახავთ JWT ტოკენს
            alert("Login successful!");
            navigate('/dashboard'); // გადავიყვანოთ მთავარ გვერდზე
        } catch (err) {
            setError("Invalid username or password");
        }
    };

    return (
        <div style={{
            maxWidth: '400px', margin: '80px auto', padding: '30px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)', borderRadius: '8px', backgroundColor: '#fff'
        }}>
            <h2 style={{ marginBottom: '20px', color: '#333' }}>Welcome Back</h2>
            {error && <p style={{ color: 'red', marginBottom: '15px' }}>{error}</p>}

            <form onSubmit={handleLogin}>
                <CustomInput type="text" placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} required />
                <CustomInput type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />

                <button type="submit" style={{
                    width: '100%', padding: '12px', backgroundColor: '#28a745', color: '#fff',
                    border: 'none', borderRadius: '6px', fontSize: '16px', cursor: 'pointer'
                }}>
                    Login
                </button>
            </form>
            <p style={{ marginTop: '20px' }}>
                Don't have an account? <Link to="/register" style={{ color: '#007bff', textDecoration: 'none' }}>Register here</Link>
            </p>
        </div>
    );
};

export default LoginPage;