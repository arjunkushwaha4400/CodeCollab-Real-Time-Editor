import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { Container, Row, Col, Card, Button, Form, Spinner, InputGroup } from 'react-bootstrap';

function HomePage() {
    const [isLoading, setIsLoading] = useState(false);
    const [joinSessionId, setJoinSessionId] = useState('');
    const [isPrivate, setIsPrivate] = useState(true);
    const [currentUser, setCurrentUser] = useState(null);
    const navigate = useNavigate();
    const [initialLanguage, setInitialLanguage] = useState('java');

    useEffect(() => {
        const token = localStorage.getItem('jwt_token');
        if (token) {
            try {
                const decoded = jwtDecode(token);
                if (decoded.exp * 1000 > Date.now()) setCurrentUser(decoded.sub);
            } catch (e) { setCurrentUser(null); }
        }
    }, []);

    const handleCreateSession = () => {
        const token = localStorage.getItem('jwt_token');
        if (!token) { navigate('/login'); return; }
        setIsLoading(true);
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub; // or whatever field contains the username in your JWT
        } catch (error) {
            console.error('Error decoding token:', error);
            setIsLoading(false);
            return;
        }
        fetch('http://localhost:8080/session-service/api/sessions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}`,'X-Authenticated-Username': username },
            body: JSON.stringify({ isPrivate: isPrivate , language: initialLanguage})
        })
            .then(res => res.ok ? res.json() : Promise.reject(new Error('Failed to create session')))
            .then(data => {
                // --- THIS IS THE FIX ---
                // We pass the data we just received directly to the next page.
                navigate(`/session/${data.uniqueId}`, { state: { sessionData: data } });
            })
            .catch(err => {
                alert(err.message);
                setIsLoading(false);
            });
    };

    const handleJoinSession = (e) => {
        e.preventDefault();
        if (joinSessionId.trim()) navigate(`/session/${joinSessionId.trim()}`);
        else alert("Please enter a Session ID.");
    };

    return (
        <Container fluid className="homepage-bg px-0">
            {currentUser ? (
                <Container className="homepage-content">
                    <h1 className="display-4 text-white mb-5">Start Collaborating</h1>
                    <Row className="w-100" style={{ maxWidth: '900px' }}>
                        <Col md={6} className="mb-4">
                            <Card bg="dark" text="white" className="h-100 border-secondary">
                                <Card.Body className="d-flex flex-column justify-content-between p-4">
                                    <div>
                                        <Card.Title as="h2">Create a New Session</Card.Title>
                                        <Card.Text className="text-white-50">Start a new public or private session.</Card.Text>
                                    </div>
                                    <Form.Group className="mt-3">
                                        <Form.Label>Select Language</Form.Label>
                                        <Form.Select value={initialLanguage} onChange={e => setInitialLanguage(e.target.value)} className="bg-dark text-white border-secondary">
                                            <option value="java">Java</option>
                                            <option value="python">Python</option>
                                            <option value="javascript">JavaScript</option>
                                        </Form.Select>
                                    </Form.Group>
                                    <Form.Check type="switch" id="private-session-switch" label="Make this session private" checked={isPrivate} onChange={(e) => setIsPrivate(e.target.checked)} className="mt-3" />
                                    <Button variant="primary" size="lg" onClick={handleCreateSession} disabled={isLoading} className="mt-4">
                                        {isLoading ? <><Spinner as="span" animation="border" size="sm" /> Creating...</> : 'Create & Join Session'}
                                    </Button>
                                </Card.Body>
                            </Card>
                        </Col>
                        <Col md={6} className="mb-4">
                            <Card bg="dark" text="white" className="h-100 border-secondary">
                                <Card.Body className="d-flex flex-column justify-content-between p-4">
                                    <div>
                                        <Card.Title as="h2">Join an Existing Session</Card.Title>
                                        <Card.Text className="text-white-50">Have a session ID? Paste it below to join.</Card.Text>
                                    </div>
                                    <Form onSubmit={handleJoinSession} className="mt-4">
                                        <InputGroup>
                                            <Form.Control className="bg-dark text-white border-secondary" type="text" placeholder="Paste Session ID" value={joinSessionId} onChange={(e) => setJoinSessionId(e.target.value)} />
                                            <Button variant="success" type="submit">Join Session</Button>
                                        </InputGroup>
                                    </Form>
                                </Card.Body>
                            </Card>
                        </Col>
                    </Row>
                </Container>
            ) : (
                <Container className="homepage-content">
                    <h1 className="display-3 text-white mb-3">Welcome to CodeCollab 🚀</h1>
                    <p className="lead text-white-50 mb-5">Please login or register to continue.</p>
                </Container>
            )}
        </Container>
    );
}

export default HomePage;