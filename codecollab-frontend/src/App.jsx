import { Routes, Route } from 'react-router-dom';
import HomePage from './pages/HomePage';
import EditorPage from './pages/EditorPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import AuthNavbar from './components/AuthNavbar';
// In your index.js or App.js
import './app.css';

function App() {
    return (
        <>
            <AuthNavbar />
            <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/session/:sessionId" element={<EditorPage />} />
            </Routes>
        </>
    );
}
export default App;