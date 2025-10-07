import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { Navbar, Nav, Button, Container } from 'react-bootstrap';

function AuthNavbar() {
    const [currentUser, setCurrentUser] = useState(null);
    const navigate = useNavigate();
    const location = useLocation(); // To force re-render on navigation

    useEffect(() => {
        const token = localStorage.getItem('jwt_token');
        if (token) {
            try {
                const decoded = jwtDecode(token);
                if (decoded.exp * 1000 > Date.now()) {
                    setCurrentUser(decoded.sub);
                } else {
                    localStorage.removeItem('jwt_token');
                    setCurrentUser(null);
                }
            } catch (error) {
                localStorage.removeItem('jwt_token');
                setCurrentUser(null);
            }
        } else {
            setCurrentUser(null);
        }
    }, [location]);

    const handleLogout = () => {
        localStorage.removeItem('jwt_token');
        setCurrentUser(null);
        navigate('/');
    };

    return (
        <Navbar bg="dark" variant="dark" expand="lg" sticky="top" className="border-bottom border-secondary">
            <Container>
                <Navbar.Brand as={Link} to="/">CodeCollab 🚀</Navbar.Brand>
                <Nav className="ms-auto">
                    {currentUser ? (
                        <>
                            <Navbar.Text className="me-3">Signed in as: <span className="fw-bold">{currentUser}</span></Navbar.Text>
                            <Button variant="outline-light" size="sm" onClick={handleLogout}>Logout</Button>
                        </>
                    ) : (
                        <>
                            <Nav.Link as={Link} to="/login">Login</Nav.Link>
                            <Nav.Link as={Link} to="/register">Register</Nav.Link>
                        </>
                    )}
                </Nav>
            </Container>
        </Navbar>
    );
}
export default AuthNavbar;