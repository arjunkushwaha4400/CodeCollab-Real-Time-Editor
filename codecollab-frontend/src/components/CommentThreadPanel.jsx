import React, { useState } from 'react';
import { Button, Card, Form, ListGroup, Badge, Modal } from 'react-bootstrap';

const CommentThreadPanel = ({
                                thread,
                                currentUser,
                                onReply,
                                onResolve,
                                onClose,
                                isOwner
                            }) => {
    const [replyContent, setReplyContent] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmitReply = async () => {
        if (!replyContent.trim()) return;

        setIsSubmitting(true);
        try {
            await onReply(thread.id, replyContent);
            setReplyContent('');
        } catch (error) {
            console.error('Error submitting reply:', error);
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleResolveThread = async () => {
        if (window.confirm('Are you sure you want to resolve this thread?')) {
            await onResolve(thread.id);
        }
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString();
    };

    return (
        <Modal show={true} onHide={onClose} size="lg" centered>
            <Modal.Header closeButton className="bg-dark text-white">
                <Modal.Title>
                    💬 Comments on Line {thread.lineNumber}
                    {thread.status === 'RESOLVED' && (
                        <Badge bg="success" className="ms-2">Resolved</Badge>
                    )}
                </Modal.Title>
            </Modal.Header>

            <Modal.Body className="bg-dark text-white p-0">
                {/* Comments List */}
                <div style={{ maxHeight: '400px', overflowY: 'auto' }}>
                    <ListGroup variant="flush">
                        {thread.comments.map((comment, index) => (
                            <ListGroup.Item
                                key={comment.id || index}
                                className="bg-dark text-white border-secondary"
                            >
                                <div className="d-flex justify-content-between align-items-start mb-2">
                                    <div>
                                        <strong className="text-primary">
                                            {comment.author || 'Unknown'}
                                        </strong>
                                        {comment.author === currentUser && (
                                            <Badge bg="outline-primary" className="ms-2" size="sm">You</Badge>
                                        )}
                                    </div>
                                    <small className="text-muted">
                                        {formatDate(comment.createdAt || comment.timestamp)}
                                    </small>
                                </div>
                                <p className="mb-0 text-light">{comment.content}</p>
                            </ListGroup.Item>
                        ))}
                    </ListGroup>
                </div>

                {/* Reply Form */}
                {thread.status === 'OPEN' && (
                    <div className="p-3 border-top border-secondary">
                        <Form.Group>
                            <Form.Label className="text-light">Add your reply:</Form.Label>
                            <Form.Control
                                as="textarea"
                                rows={3}
                                value={replyContent}
                                onChange={(e) => setReplyContent(e.target.value)}
                                placeholder="Type your reply here..."
                                className="bg-secondary text-white border-dark"
                            />
                        </Form.Group>
                        <div className="d-flex justify-content-between mt-3">
                            <div>
                                <Button
                                    variant="primary"
                                    onClick={handleSubmitReply}
                                    disabled={!replyContent.trim() || isSubmitting}
                                    size="sm"
                                >
                                    {isSubmitting ? 'Posting...' : '💬 Reply'}
                                </Button>
                            </div>
                            <div>
                                {(isOwner || currentUser === thread.comments[0]?.author) && (
                                    <Button
                                        variant="success"
                                        onClick={handleResolveThread}
                                        size="sm"
                                    >
                                        ✅ Resolve Thread
                                    </Button>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* Show message if thread is resolved */}
                {thread.status === 'RESOLVED' && (
                    <div className="p-3 text-center border-top border-success">
                        <Badge bg="success" className="p-2">
                            ✅ This thread has been resolved
                        </Badge>
                        <p className="text-muted mt-2 mb-0">
                            No further replies can be added to resolved threads.
                        </p>
                    </div>
                )}
            </Modal.Body>
        </Modal>
    );
};

export default CommentThreadPanel;