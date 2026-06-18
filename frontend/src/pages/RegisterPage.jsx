import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import CustomInput from '../components/CustomInput';
import {isValidEmail} from "../utils/validators.js";

const RegisterPage = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
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

        if (!isValidEmail(email)) {
            setError('please enter valid Email!');
            return;
        }

        try {
            const response = await fetch("https://localhost:8443/api/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({ username, email, password })
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Registration failed.");
            }

            alert("Registration successful!");
            navigate('/login');
        } catch (err) {
            setError(err.message || "Registration failed.");
        }
    };

    return (
        <div className="register-bg">
            <div className="register-card">
                <h2 style={{ marginBottom: '25px', color: '#fff', textAlign: 'center', fontSize: '28px', fontWeight: 'bold' }}>Sign Up</h2>
                {error && <p style={{ color: '#ff6b6b', marginBottom: '15px', textAlign: 'center' }}>{error}</p>}

                <form onSubmit={handleRegister}>
                    <CustomInput type="text" placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} required />
                    <CustomInput type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                    <CustomInput type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                    <CustomInput type="password" placeholder="Repeat Password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required />

                    <button type="submit" className="signup-btn">
                        Sign Up
                    </button>
                </form>

                <p style={{ marginTop: '25px', textAlign: 'center', color: 'rgba(255, 255, 255, 0.6)', fontSize: '14px' }}>
                    Already have an account?{' '}
                    <Link to="/login" style={{ color: '#14b8a6', textDecoration: 'none', fontWeight: '600', marginLeft: '5px' }}>
                        Login
                    </Link>
                </p>
            </div>
        </div>
    );
};

export default RegisterPage;