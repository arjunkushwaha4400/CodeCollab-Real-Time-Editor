import React from 'react';
import { ListGroup, Button, Badge, Form } from 'react-bootstrap';
import { Role } from '../pages/EditorPage';

function ParticipantsPanel({
                               sessionDetails,
                               currentUsername,
                               onBlockUser,
                               onApprove,
                               onDeny,
                               onChangeRole,
                               isLoading
                           }) {
    if (!sessionDetails) return <div>Loading session details...</div>;

    const isOwner = sessionDetails.ownerUsername === currentUsername;

    console.log("DEBUG - ParticipantsPanel:", {
        pendingRequests: sessionDetails.pendingRequests,
        isOwner: isOwner
    });

    return (
        <div className="panel-content">
            {/* PENDING REQUESTS - This should now work */}
            {isOwner && sessionDetails.pendingRequests && sessionDetails.pendingRequests.length > 0 && (
                <>
                    <h5>Pending Requests ({sessionDetails.pendingRequests.length})</h5>
                    <ListGroup variant="flush" className="mb-4">
                        {sessionDetails.pendingRequests.map(username => (
                            <ListGroup.Item
                                key={username}
                                className="bg-dark text-white d-flex justify-content-between align-items-center"
                            >
                                <span>{username}</span>
                                <div>
                                    <Button
                                        variant="success"
                                        size="sm"
                                        className="me-2"
                                        onClick={() => onApprove(username)}
                                        disabled={isLoading}
                                    >
                                        Approve
                                    </Button>
                                    <Button
                                        variant="danger"
                                        size="sm"
                                        onClick={() => onDeny(username)}
                                        disabled={isLoading}
                                    >
                                        Deny
                                    </Button>
                                </div>
                            </ListGroup.Item>
                        ))}
                    </ListGroup>
                </>
            )}

            {/* PARTICIPANTS LIST */}
            <h5>Participants</h5>
            <ListGroup variant="flush">
                {Object.entries(sessionDetails.participants || {}).map(([username, role]) => (
                    <ListGroup.Item
                        key={username}
                        className="bg-dark text-white d-flex justify-content-between align-items-center"
                    >
                        <div>
                            {username}
                            {username === sessionDetails.ownerUsername ? (
                                <Badge bg="primary" className="ms-2">Owner</Badge>
                            ) : (
                                <Badge bg="secondary" className="ms-2">{role}</Badge>
                            )}
                            {sessionDetails.blockedUsers?.includes(username) && (
                                <Badge bg="danger" className="ms-2">Blocked</Badge>
                            )}
                        </div>
                        {isOwner && username !== currentUsername && username !== sessionDetails.ownerUsername && (
                            <div className="d-flex align-items-center">
                                <Form.Select
                                    size="sm"
                                    value={role}
                                    className="me-2 bg-dark text-white"
                                    style={{ width: '120px' }}
                                    onChange={(e) => onChangeRole(username, e.target.value)}
                                    disabled={isLoading}
                                >
                                    <option value={Role.EDITOR}>Editor</option>
                                    <option value={Role.VIEWER}>Viewer</option>
                                </Form.Select>
                                {!sessionDetails.blockedUsers?.includes(username) && (
                                    <Button
                                        variant="outline-danger"
                                        size="sm"
                                        onClick={() => onBlockUser(username)}
                                        disabled={isLoading}
                                    >
                                        Block
                                    </Button>
                                )}
                            </div>
                        )}
                    </ListGroup.Item>
                ))}
            </ListGroup>
        </div>
    );
}

export default ParticipantsPanel;