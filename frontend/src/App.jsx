import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage.jsx';
import Register from './pages/RegisterPage.jsx'; // შემოვიტანოთ ახალი კომპონენტი
import ShowsPage from './pages/ShowsPage.jsx';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<Register />} />
                <Route path="/shows" element={<ShowsPage />} />
            </Routes>
        </Router>
    );
}

export default App;