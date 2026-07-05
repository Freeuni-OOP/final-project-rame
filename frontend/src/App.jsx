import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import LoginPage from './pages/LoginPage.jsx';
import Register from './pages/RegisterPage.jsx';
import ShowsPage from './pages/ShowsPage.jsx';
import ShowsDetailsPage from "./pages/ShowsDetailsPage.jsx";
import ListsPage from "./pages/ListsPage.jsx";
import ListDetailPage from "./pages/ListDetailPage.jsx";
import ListEditPage from "./pages/ListEditPage.jsx";
import AllListsPage from "./pages/AllListsPage.jsx";
import CrewPickDetailPage from "./pages/CrewPickDetailPage.jsx";
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
                <Route path="/lists/all/:type" element={<AllListsPage />} />
                <Route path="/crew-picks/:index" element={<CrewPickDetailPage />} />
                <Route path="/lists/:id/edit" element={<ListEditPage />} />
                <Route path="/lists/:id" element={<ListDetailPage />} />
                <Route path="/profile" element={<ProfilePage />} />
                <Route path="/profile/:username" element={<ProfilePage />} />
            </Routes>
        </Router>
    );
}

export default App;
