import React, { useState } from 'react';
import axios from 'axios';
import { useNavigate, Link } from 'react-router-dom';
import CustomInput from '../components/CustomInput';

const RegisterPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleRegister = async (e) => {
        e.preventDefault();
        if (password !== confirmPassword) {
            setError("Passwords do not match!");
            return;
        }

        try {
            await axios.post("https://localhost:8443/api/auth/register", { username, password });
            alert("Registration successful!");
            navigate('/login');
        } catch (err) {
            setError(err.response?.data || "Registration failed.");
        }
    };

    return (
        <div style={{
            maxWidth: '400px',
            margin: '80px auto',
            padding: '30px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
            borderRadius: '8px',
            backgroundColor: '#fff'
        }}>
            <h2 style={{ marginBottom: '20px', color: '#333' }}>Create Account</h2>
            {error && <p style={{ color: 'red', marginBottom: '15px' }}>{error}</p>}

            <form onSubmit={handleRegister}>
                <CustomInput type="text" placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} required />
                <CustomInput type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                <CustomInput type="password" placeholder="Confirm Password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />

                <button type="submit" style={{
                    width: '100%', padding: '12px', backgroundColor: '#007bff', color: '#fff',
                    border: 'none', borderRadius: '6px', fontSize: '16px', cursor: 'pointer'
                }}>
                    Sign Up
                </button>
            </form>
            <p style={{ marginTop: '20px' }}>
                Already have an account? <Link to="/login" style={{ color: '#007bff', textDecoration: 'none' }}>Login</Link>
            </p>
        </div>
    );
};

export default RegisterPage;