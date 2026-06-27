import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage.jsx';
import Register from './pages/RegisterPage.jsx';
import ShowsPage from './pages/ShowsPage.jsx';
import ShowsDetailsPage from "./pages/ShowsDetailsPage.jsx";
import ListsPage from "./pages/ListsPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";
import Header from "./components/Header.jsx";

function App() {
    return (
        <Router>
            <Header />
            <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<Register />} />
                <Route path="/shows" element={<ShowsPage />} />
                <Route path="/" element={<ShowsPage />} />
                <Route path="/shows/:id" element={<ShowsDetailsPage />} />
                <Route path="/lists" element={<ListsPage />} />
                <Route path="/profile" element={<ProfilePage />} />
            </Routes>
        </Router>
    );
}

export default App;