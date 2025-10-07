import React, { useState, useRef, useEffect } from 'react';
import { Form, InputGroup, Button } from 'react-bootstrap';

function ChatPanel({ messages, onSendMessage, currentUser }) {
    const [currentMessage, setCurrentMessage] = useState('');
    const chatBoxRef = useRef(null);

    useEffect(() => {
        if (chatBoxRef.current) {
            chatBoxRef.current.scrollTop = chatBoxRef.current.scrollHeight;
        }
    }, [messages]);

    const handleSubmit = (e) => {
        e.preventDefault();
        onSendMessage(currentMessage);
        setCurrentMessage('');
    };

    return (
        <div className="d-flex flex-column h-100">
            <div className="panel-content chat-messages" ref={chatBoxRef}>
                {messages.map((msg, index) => {
                    const isCurrentUser = msg.sender === currentUser;
                    const showSenderName = !isCurrentUser && msg.type !== 'JOIN';

                    if (msg.type === 'JOIN') {
                        return (
                            <div key={index} className="chat-notification">
                                <span><strong>{msg.sender}</strong> {msg.content}</span>
                            </div>
                        );
                    }

                    return (
                        <div key={index} className={`message-wrapper ${isCurrentUser ? 'sent' : 'received'}`}>
                            <div className="message-bubble">
                                {showSenderName && <div className="sender-name">{msg.sender}</div>}
                                <div className="message-content">{msg.content}</div>
                            </div>
                        </div>
                    );
                })}
            </div>
            <Form className="p-2 border-top border-secondary" onSubmit={handleSubmit}>
                <InputGroup>
                    <Form.Control
                        className="bg-dark text-white border-secondary"
                        type="text"
                        placeholder="Type a message..."
                        value={currentMessage}
                        onChange={(e) => setCurrentMessage(e.target.value)}
                        autoComplete="off"
                    />
                    <Button type="submit" variant="primary">Send</Button>
                </InputGroup>
            </Form>
        </div>
    );
}

export default ChatPanel;