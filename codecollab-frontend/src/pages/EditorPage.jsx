import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { jwtDecode } from 'jwt-decode';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import Editor from '@monaco-editor/react';
import { Button, Tab, Tabs, Spinner, Toast, ToastContainer, ListGroup,Form } from 'react-bootstrap';
import ParticipantsPanel from '../components/ParticipantsPanel';
import ChatPanel from '../components/ChatPanel';

export const Role = {
    OWNER: 'OWNER',
    EDITOR: 'EDITOR',
    VIEWER: 'VIEWER'
};
const userColors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FED766', '#F0B37E', '#8A2BE2'];
const getUserColor = (username) => {
    let hash = 0;
    for (let i = 0; i < username.length; i++) {
        hash = username.charCodeAt(i) + ((hash << 5) - hash);
    }
    return userColors[Math.abs(hash) % userColors.length];
};

function EditorPage() {
    const [language, setLanguage] = useState('java');
    const [remoteSelections, setRemoteSelections] = useState({});
    const oldDecorationsRef = useRef([]);
    const { sessionId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [currentUser, setCurrentUser] = useState(null);
    const [userRole, setUserRole] = useState(null);
    const [joinStatus, setJoinStatus] = useState('CHECKING');
    const [isConnected, setIsConnected] = useState(false);
    const [sessionDetails, setSessionDetails] = useState(null);
    const [joinRequests, setJoinRequests] = useState([]);
    const [codeContent, setCodeContent] = useState('// Loading code...');
    const [executionOutput, setExecutionOutput] = useState('Click "Run Code" to see the output here.');
    const [aiExplanation, setAiExplanation] = useState('Select code and click "Explain" to get an AI-powered explanation.');
    const [activeTab, setActiveTab] = useState('chat');
    const [isExecuting, setIsExecuting] = useState(false);
    const [isExplaining, setIsExplaining] = useState(false);
    const [isPermissionLoading, setIsPermissionLoading] = useState(false);
    const [isHistoryLoading, setIsHistoryLoading] = useState(false);
    const [chatMessages, setChatMessages] = useState([]);
    const stompClientRef = useRef(null);
    const editorRef = useRef(null);
    const monacoRef = useRef(null);

    const [stdin, setStdin] = useState('');


    const setupSnippets = (monaco) => {
        monaco.languages.registerCompletionItemProvider('java', {
            provideCompletionItems: () => {
                const suggestions = [
                    {
                        label: 'sysout',
                        kind: monaco.languages.CompletionItemKind.Snippet,
                        insertText: 'System.out.println(${1});$0',
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        documentation: 'Prints a string to the console.',
                    },
                    {
                        label: 'main',
                        kind: monaco.languages.CompletionItemKind.Snippet,
                        insertText: [
                            'public static void main(String[] args) {',
                            '\t${1}',
                            '}$0'
                        ].join('\n'),
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        documentation: 'Creates the main method.',
                    },
                    {
                        label: 'for',
                        kind: monaco.languages.CompletionItemKind.Snippet,
                        insertText: [
                            'for (int ${1:i} = 0; ${1:i} < ${2:length}; ${1:i}++) {',
                            '\t${3}',
                            '}$0'
                        ].join('\n'),
                        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
                        documentation: 'Creates a for-loop.',
                    }
                ];
                return { suggestions: suggestions };
            },
        });
    };

    const handleEditorDidMount = (editor, monaco) => {
        editorRef.current = editor;
        monacoRef.current = monaco;
        setupSnippets(monaco);

        let throttleTimer;

        const publishCursorUpdate = (data, type) => {
            if (stompClientRef.current?.connected && currentUser) {
                clearTimeout(throttleTimer);
                throttleTimer = setTimeout(() => {
                    const cursorData = {
                        username: currentUser, // Include username in the payload
                        startLineNumber: data.startLineNumber,
                        startColumn: data.startColumn,
                        endLineNumber: data.endLineNumber,
                        endColumn: data.endColumn,
                        selectionStartLineNumber: data.selectionStartLineNumber || data.startLineNumber,
                        selectionStartColumn: data.selectionStartColumn || data.startColumn,
                        positionLineNumber: data.positionLineNumber || data.startLineNumber,
                        positionColumn: data.positionColumn || data.startColumn
                    };

                    console.log(`📤 Publishing ${type} for user ${currentUser}:`, cursorData);

                    stompClientRef.current.publish({
                        destination: `/app/cursor/${sessionId}`,
                        body: JSON.stringify(cursorData)
                    });
                }, 100);
            }
        };

        editor.onDidChangeCursorPosition(e => {
            publishCursorUpdate({
                startLineNumber: e.position.lineNumber,
                startColumn: e.position.column,
                endLineNumber: e.position.lineNumber,
                endColumn: e.position.column,
                positionLineNumber: e.position.lineNumber,
                positionColumn: e.position.column
            });
        });

        editor.onDidChangeCursorSelection(e => {
            const selection = e.selection;
            publishCursorUpdate({
                startLineNumber: selection.startLineNumber,
                startColumn: selection.startColumn,
                endLineNumber: selection.endLineNumber,
                endColumn: selection.endColumn,
                selectionStartLineNumber: selection.selectionStartLineNumber,
                selectionStartColumn: selection.selectionStartColumn,
                positionLineNumber: selection.positionLineNumber,
                positionColumn: selection.positionColumn
            });
        });
    };

    const fetchSessionDetails = useCallback(async (token) => {
        try {
            console.log("🔄 DEBUG - Fetching session details for:", sessionId);
            const response = await fetch(`http://localhost:8080/session-service/api/sessions/${sessionId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            console.log("🔍 DEBUG - Response status:", response.status);

            if (!response.ok) throw new Error('Could not fetch session details');

            const data = await response.json();
            console.log("✅ DEBUG - Session details fetched:", data);
            console.log("✅ DEBUG - Pending requests in response:", data.pendingRequests);

            setSessionDetails(data);
            return data;
        } catch (error) {
            console.error("❌ DEBUG - Error fetching session:", error);
            // alert("Session not found or an error occurred.");
            navigate('/');
        }
    }, [sessionId, navigate]);

    useEffect(() => {
        // Auto-refresh session details for owner to see real-time join requests
        if (userRole === Role.OWNER && isConnected) {
            const interval = setInterval(() => {
                const token = localStorage.getItem('jwt_token');
                fetchSessionDetails(token);
                console.log("🔄 Auto-refreshing session details for owner");
            }, 3000); // Refresh every 3 seconds

            return () => clearInterval(interval);
        }
    }, [userRole, isConnected, fetchSessionDetails]);

    // Add this useEffect for automatic status checking

    const connectToWebSocket = useCallback((user, token, initialDetails) => {
        if (stompClientRef.current?.active) return;
        if (!token) {
            console.error("No token provided for WebSocket connection");
            return;
        }
        const socket = new SockJS(`http://localhost:8084/ws?token=${encodeURIComponent(token)}`);
        console.log("Connecting WebSocket with token:", token); // Debug log

        const stompClient = new Client({
            webSocketFactory: () => socket,

            connectHeaders: { Authorization: `Bearer ${token}` },
            onConnect: () => {
                setIsConnected(true);
                setCodeContent(initialDetails.codeContent);
                stompClient.subscribe(`/queue/notifications`, async msg => {
                    const notification = JSON.parse(msg.body);
                    console.log('🎯 Received notification:', notification);

                    if (notification.type === 'JOIN_REQUEST') {
                        setJoinRequests(prev => {
                            const exists = prev.some(req => req.fromUser === notification.fromUser);
                            if (!exists) {
                                return [...prev, notification];
                            }
                            return prev;
                        });

                        // Refresh session details when new request arrives
                        const token = localStorage.getItem('jwt_token');
                        fetchSessionDetails(token);

                    } else if (notification.type === 'JOIN_APPROVED') {
                        // Real-time approval - update status immediately
                        console.log("🎉 Join request approved via WebSocket!");
                        alert(notification.message);

                        // Refresh session details to get updated participant list
                        const token = localStorage.getItem('jwt_token');
                        const updatedDetails = await fetchSessionDetails(token);

                        // If we're now in participants, update status and connect
                        if (updatedDetails.participants[currentUser]) {
                            setJoinStatus('APPROVED');
                            setUserRole(updatedDetails.participants[currentUser]);
                            if (!isConnected) {
                                connectToWebSocket(currentUser, token, updatedDetails);
                            }
                        }

                    } else if (notification.type === 'JOIN_DENIED') {
                        // Real-time denial - update status immediately
                        console.log("❌ Join request denied via WebSocket");
                        alert(notification.message);
                        setJoinStatus('REQUIRES_REQUEST');

                        // Also refresh session details to see current state
                        const token = localStorage.getItem('jwt_token');
                        fetchSessionDetails(token);
                    }else if (notification.type === 'USER_LEFT') {
                        // Handle user leaving notification
                        console.log(`👋 User left: ${notification.fromUser}`);

                        // Refresh session details to update participants list
                        const token = localStorage.getItem('jwt_token');
                        fetchSessionDetails(token);

                        // Show toast notification
                        alert(`${notification.fromUser} ${notification.message}`);
                    }else if (notification.type === 'SESSION_DELETED') {
                        // Handle session deletion notification
                        console.log('💥 Session deleted by owner');
                        alert(notification.message || "The session has been deleted by the owner.");

                        // Disconnect WebSocket and redirect to home
                        if (stompClientRef.current) {
                            stompClientRef.current.deactivate();
                        }
                        navigate('/');
                    }
                });
                stompClient.subscribe(`/topic/session/${sessionId}`, msg => {
                    const notification = JSON.parse(msg.body);
                    if (notification.type === 'USER_LEFT') {
                        console.log(`👋 Broadcast: User left: ${notification.fromUser}`);

                        // Refresh session details to update participants list
                        const token = localStorage.getItem('jwt_token');
                        fetchSessionDetails(token);
                    }else if (notification.type === 'SESSION_DELETED') {
                        console.log('💥 Broadcast: Session deleted');
                        alert(notification.message || "The session has been deleted by the owner.");
                        if (stompClientRef.current) {
                            stompClientRef.current.deactivate();
                        }
                        navigate('/');
                    }
                });
                stompClient.subscribe(`/topic/code/${sessionId}`, msg => setCodeContent(JSON.parse(msg.body).content));
                stompClient.subscribe(`/topic/output/${sessionId}`, msg => {
                    const result = JSON.parse(msg.body);
                    setExecutionOutput(result.output || result.error || 'Execution finished.');
                    setIsExecuting(false);
                    setActiveTab('output');
                });
                stompClient.subscribe(`/topic/chat/${sessionId}`, msg => setChatMessages(prev => [...prev, JSON.parse(msg.body)]));
                stompClient.publish({ destination: `/app/chat/${sessionId}`, body: JSON.stringify({ sender: user, content: 'has joined!', type: 'JOIN' }) });
                stompClient.subscribe(`/topic/cursor/${sessionId}`, msg => {
                    const cursorData = JSON.parse(msg.body);
                    console.log('🎯 Received cursor data:', cursorData);
                    // Ignore our own cursor updates
                    if (cursorData.username !== user) {
                        setRemoteSelections(prev => ({
                            ...prev,
                            [cursorData.username]: {
                                startLineNumber: cursorData.startLineNumber,
                                startColumn: cursorData.startColumn,
                                endLineNumber: cursorData.endLineNumber || cursorData.startLineNumber,
                                endColumn: cursorData.endColumn || cursorData.startColumn,
                                selectionStartLineNumber: cursorData.selectionStartLineNumber || cursorData.startLineNumber,
                                selectionStartColumn: cursorData.selectionStartColumn || cursorData.startColumn,
                                positionLineNumber: cursorData.positionLineNumber || cursorData.startLineNumber,
                                positionColumn: cursorData.positionColumn || cursorData.startColumn,
                                username: cursorData.username,
                                color: getUserColor(cursorData.username)
                            }
                        }));
                    }
                });
            },
        });
        stompClient.activate();
        stompClientRef.current = stompClient;
    }, [sessionId]);

    useEffect(() => {
        if (!editorRef.current || !monacoRef.current || !sessionDetails) return;

        console.log('🎨 Applying decorations for remote selections:', remoteSelections);

        const decorations = Object.entries(remoteSelections)
            .filter(([username, selection]) =>
                sessionDetails.participants[username] &&
                username !== currentUser
            )
            .map(([username, selection]) => {
                const sanitizedUsername = username.replace(/[^a-zA-Z0-9]/g, '');
                const userColor = getUserColor(username);

                // Check if it's a cursor (single position) or selection
                const isCursor = selection.startLineNumber === selection.endLineNumber &&
                    selection.startColumn === selection.endColumn;

                if (isCursor) {
                    // Create cursor decoration with username label
                    return {
                        range: new monacoRef.current.Range(
                            selection.startLineNumber,
                            selection.startColumn,
                            selection.endLineNumber,
                            selection.endColumn
                        ),
                        options: {
                            className: `remote-cursor-${sanitizedUsername}`,
                            hoverMessage: { value: `**${username}**` }, // Tooltip on hover
                            stickiness: 1, // Never shrink when typing at edges
                            // This creates the inline widget with username
                            after: {
                                content: username,
                                inlineClassName: `remote-cursor-label-${sanitizedUsername}`,
                                margin: '0 0 0 2px' // Add some spacing
                            }
                        }
                    };
                } else {
                    // Create selection decoration
                    return {
                        range: new monacoRef.current.Range(
                            selection.startLineNumber,
                            selection.startColumn,
                            selection.endLineNumber,
                            selection.endColumn
                        ),
                        options: {
                            className: `remote-selection-${sanitizedUsername}`,
                            hoverMessage: { value: `**${username}**` }, // Tooltip on hover
                            stickiness: 1
                        }
                    };
                }
            });

        console.log('🖌️ Creating decorations:', decorations);

        // Apply decorations
        oldDecorationsRef.current = editorRef.current.deltaDecorations(
            oldDecorationsRef.current,
            decorations
        );

        // Create dynamic styles
        const styleId = 'dynamic-cursor-styles';
        let styleElement = document.getElementById(styleId);
        if (!styleElement) {
            styleElement = document.createElement('style');
            styleElement.id = styleId;
            document.head.appendChild(styleElement);
        }

        // Generate CSS for all remote users
        const styles = Object.entries(remoteSelections)
            .filter(([username]) => sessionDetails.participants[username] && username !== currentUser)
            .map(([username]) => {
                const sanitizedUsername = username.replace(/[^a-zA-Z0-9]/g, '');
                const userColor = getUserColor(username);

                return `
                /* Cursor line */
                .remote-cursor-${sanitizedUsername} {
                    border-left: 2px solid ${userColor} !important;
                    border-right: none !important;
                    background-color: transparent !important;
                    width: 0px !important;
                    margin-left: -1px !important;
                }
                
                /* Username label */
                .remote-cursor-label-${sanitizedUsername} {
                    color: white !important;
                    background-color: ${userColor} !important;
                    padding: 1px 6px !important;
                    border-radius: 10px !important;
                    margin-left: 4px !important;
                    font-size: 10px !important;
                    font-weight: bold !important;
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif !important;
                    position: relative !important;
                    z-index: 1000 !important;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.3) !important;
                    border: 1px solid ${userColor}80 !important;
                    white-space: nowrap !important;
                }
                
                /* Selection background */
                .remote-selection-${sanitizedUsername} {
                    background-color: ${userColor}30 !important;
                    border: 1px solid ${userColor} !important;
                }
                
                /* Optional: Add a small caret above the username */
                .remote-cursor-label-${sanitizedUsername}::before {
                    content: '' !important;
                    position: absolute !important;
                    top: -4px !important;
                    left: 8px !important;
                    width: 0 !important;
                    height: 0 !important;
                    border-left: 4px solid transparent !important;
                    border-right: 4px solid transparent !important;
                    border-bottom: 4px solid ${userColor} !important;
                }
            `;
            })
            .join('\n');

        styleElement.innerHTML = styles;

    }, [remoteSelections, sessionDetails, currentUser]);

    useEffect(() => {
        console.log('📝 Editor ref:', editorRef.current);
        console.log('🔧 Monaco ref:', monacoRef.current);
        console.log('👥 Session details:', sessionDetails);
    }, [editorRef.current, monacoRef.current, sessionDetails]);

    useEffect(() => {
        console.log('🔍 Remote selections state updated:', remoteSelections);
    }, [remoteSelections]);



    const handleRequestToJoin = useCallback(async (token, user, currentDetails) => {
        setJoinStatus('PENDING');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub;
        } catch (error) {
            console.error('Error decoding token:', error);
            setIsHistoryLoading(false);
            return;
        }
        console.log("🔄 DEBUG - Sending join request as:", username);

        const response = await fetch(`http://localhost:8080/session-service/api/sessions/${sessionId}/request-join`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'X-Authenticated-Username': username
            }
        });
        console.log("🔍 DEBUG - Join request response status:", response.status);

        if (response.ok) {
            console.log("✅ DEBUG - Join request successful, refreshing details...");

            // REFRESH THE SESSION DETAILS AFTER MAKING REQUEST
            const updatedDetails = await fetchSessionDetails(token);
            console.log("✅ DEBUG - Updated details after request:", updatedDetails);
            setSessionDetails(updatedDetails);

            if (!currentDetails?.isPrivate) {
                connectToWebSocket(user, token, updatedDetails);
            }
            alert("Join request sent successfully!");
        } else {
            console.log("❌ DEBUG - Join request failed");
            alert('Failed to send join request');
            setJoinStatus('REQUIRES_REQUEST');
        }
    }, [sessionId, fetchSessionDetails, connectToWebSocket]);

    useEffect(() => {
        let interval;

        // Auto-check status every 5 seconds if user is waiting for approval
        if (joinStatus === 'PENDING' && currentUser) {
            interval = setInterval(async () => {
                console.log("🔄 Auto-checking join status...");
                const token = localStorage.getItem('jwt_token');
                try {
                    const details = await fetchSessionDetails(token);

                    // Check if approved
                    if (details.participants[currentUser]) {
                        console.log("✅ Auto-check: User approved!");
                        setJoinStatus('APPROVED');
                        setUserRole(details.participants[currentUser]);
                        connectToWebSocket(currentUser, token, details);
                        clearInterval(interval);
                    }
                } catch (error) {
                    console.error("Auto-check error:", error);
                }
            }, 5000); // Check every 5 seconds
        }

        return () => {
            if (interval) clearInterval(interval);
        };
    }, [joinStatus, currentUser, fetchSessionDetails, connectToWebSocket]);

    useEffect(() => {
        const token = localStorage.getItem('jwt_token');
        if (!token) { navigate('/login'); return; }

        let user;
        try {
            const decoded = jwtDecode(token);
            if (decoded.exp * 1000 < Date.now()) { navigate('/login'); return; }
            user = decoded.sub;
            setCurrentUser(user);
        } catch (error) { navigate('/login'); return; }

        // Remove location.state dependency and always check permissions
        const checkPermissionsAndConnect = async () => {
            const passedData = location.state?.sessionData;
            const details = await fetchSessionDetails(token);
            if (!details) return;
            setLanguage(details.language);

            if (passedData) setSessionDetails(details);

            const role = details.participants[user];
            setUserRole(role);

            if (role) {
                setJoinStatus('APPROVED');
                connectToWebSocket(user, token, details);
            } else if (details.isPrivate) {
                setJoinStatus('REQUIRES_REQUEST');
            } else {
                handleRequestToJoin(token, user, details);
            }
        };

        checkPermissionsAndConnect();

        return () => {
            if (stompClientRef.current) stompClientRef.current.deactivate();
        };
    }, [sessionId, navigate, connectToWebSocket, fetchSessionDetails, handleRequestToJoin]);

    const handleApproveOrDeny = async (userToActOn, action) => {
        const token = localStorage.getItem('jwt_token');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub;
        } catch (error) {
            console.error('Error decoding token:', error);
            setIsHistoryLoading(false);
            return;
        }

        setIsPermissionLoading(true);

        try {
            await fetch(`http://localhost:8080/session-service/api/sessions/${sessionId}/${action}/${userToActOn}`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'X-Authenticated-Username': username
                }
            });

            // Remove from local state
            setJoinRequests(reqs => reqs.filter(r => r.fromUser !== userToActOn));

            // Refresh session details to see updated participant list
            await fetchSessionDetails(token);

            console.log(`✅ Successfully ${action}d user: ${userToActOn}`);

        } catch (error) {
            console.error(`❌ Error ${action}ing user:`, error);
            alert(`Failed to ${action} user`);
        } finally {
            setIsPermissionLoading(false);
        }
    };

    const handleChangeRole = async (usernameToChange, newRole) => {
        const token = localStorage.getItem('jwt_token');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub;
        } catch (error) {
            console.error('Error decoding token:', error);
            setIsHistoryLoading(false);
            return;
        }
        setIsPermissionLoading(true);
        try {
            const response = await fetch(`http://localhost:8080/session-service/api/sessions/${sessionId}/permissions/${usernameToChange}`, {
                method: 'PUT', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}`,'X-Authenticated-Username': username },
                body: JSON.stringify({ role: newRole })
            });
            if (!response.ok) throw new Error('Failed to change role');
            fetchSessionDetails(token);
        } catch (error) { alert(error.message); }
        finally { setIsPermissionLoading(false); }
    };

    const handleEditorChange = (value) => { if (stompClientRef.current?.connected) stompClientRef.current.publish({ destination: `/app/code/${sessionId}`, body: JSON.stringify({ content: value }) }); };

    const handleRunCode = () => {
        setIsExecuting(true);
        setExecutionOutput("Executing...");
        setActiveTab('output');
        fetch('http://localhost:8080/execution-service/api/execute',
            { method: 'POST',
                headers: {
                'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` },
                body: JSON.stringify({
                    sessionId,
                    language: language,
                    code: codeContent,
                    stdin: stdin
                }),
            }).catch(error => {
                setExecutionOutput(`Error: ${error.message}`);
                setIsExecuting(false); });
    };

    const handleSendMessage = (message) => { if (message.trim() && stompClientRef.current?.connected) stompClientRef.current.publish({ destination: `/app/chat/${sessionId}`, body: JSON.stringify({ sender: currentUser, content: message, type: 'CHAT' }) }); };

    const handleBlockUser = async (usernameToBlock) => { const token = localStorage.getItem('jwt_token'); setIsPermissionLoading(true); try { const response = await fetch(`http://localhost:8080/session-service/api/sessions/${sessionId}/block/${usernameToBlock}`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } }); if (!response.ok) throw new Error('Failed to block user'); fetchSessionDetails(token); } catch (error) { alert(error.message); } finally { setIsPermissionLoading(false); } };

    const handleExplainCode = () => { if (!editorRef.current) return; const selectedText = editorRef.current.getModel().getValueInRange(editorRef.current.getSelection()); if (!selectedText.trim()) { alert("Please select code to explain."); return; } setIsExplaining(true); setAiExplanation("🤖 AI is thinking..."); setActiveTab('ai-assistant'); fetch('http://localhost:8080/ai-service/api/ai/explain', { method: 'POST', headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('jwt_token')}` }, body: JSON.stringify({ text: selectedText }), }).then(res => res.text()).then(exp => setAiExplanation(exp)).catch(err => setAiExplanation(`Error: ${err.message}`)).finally(() => setIsExplaining(false)); };

    const handleSaveSnapshot = async () => {
        setIsHistoryLoading(true);
        const token = localStorage.getItem('jwt_token');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub; // or whatever field contains the username in your JWT
        } catch (error) {
            console.error('Error decoding token:', error);
            setIsHistoryLoading(false);
            return;
        }

        try {
            const response = await fetch(
                `http://localhost:8080/session-service/api/sessions/${sessionId}/snapshots`,
                {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}`,'X-Authenticated-Username': username }
                }
            );

            if (!response.ok) throw new Error('Failed to save snapshot');

            const updatedSession = await response.json();
            setSessionDetails(updatedSession);
        } catch (error) {
            alert(error.message);
        } finally {
            setIsHistoryLoading(false);
        }
    };

    // Add this function in your EditorPage component
    // Update your handleLeaveSession function:
    const handleLeaveSession = async () => {
        if (!window.confirm("Are you sure you want to leave this session? You can rejoin later if it's public or request to join again if it's private.")) {
            return;
        }

        const token = localStorage.getItem('jwt_token');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub;
        } catch (error) {
            console.error('Error decoding token:', error);
            return;
        }

        try {
            const response = await fetch(
                `http://localhost:8080/session-service/api/sessions/${sessionId}/leave`,
                {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'X-Authenticated-Username': username
                    }
                }
            );

            if (response.ok) {
                // Send leave message to chat before disconnecting
                if (stompClientRef.current?.connected) {
                    stompClientRef.current.publish({
                        destination: `/app/chat/${sessionId}`,
                        body: JSON.stringify({
                            sender: currentUser,
                            content: 'has left the session',
                            type: 'LEAVE'
                        })
                    });
                }

                // alert("You have left the session successfully.");
                navigate('/'); // Redirect to home page

            } else if (response.status === 403) {
                const errorData = await response.json();
                alert(errorData.message || "Session owner cannot leave. Please delete the session instead.");
            } else {
                throw new Error('Failed to leave session');
            }
        } catch (error) {
            console.error('Error leaving session:', error);
            alert('Error leaving session: ' + error.message);
        }
    };
    // Add this function in your EditorPage component
    const handleDeleteSession = async () => {
        if (!window.confirm("Are you sure you want to DELETE this session? This action cannot be undone and all participants will be disconnected.")) {
            return;
        }

        const token = localStorage.getItem('jwt_token');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub;
        } catch (error) {
            console.error('Error decoding token:', error);
            return;
        }

        try {
            const response = await fetch(
                `http://localhost:8080/session-service/api/sessions/${sessionId}`,
                {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'X-Authenticated-Username': username
                    }
                }
            );

            if (response.ok) {
                // Send delete message to chat before disconnecting
                if (stompClientRef.current?.connected) {
                    stompClientRef.current.publish({
                        destination: `/app/chat/${sessionId}`,
                        body: JSON.stringify({
                            sender: currentUser,
                            content: 'has deleted the session',
                            type: 'SESSION_DELETED'
                        })
                    });
                }

                // alert("Session deleted successfully.");
                navigate('/'); // Redirect to home page
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to delete session');
            }
        } catch (error) {
            console.error('Error deleting session:', error);
            alert('Error deleting session: ' + error.message);
        }
    };
    const handleRevertToSnapshot = async (snapshotId) => {
        if (!window.confirm("Are you sure? This will replace the code for everyone.")) return;

        setIsHistoryLoading(true);
        const token = localStorage.getItem('jwt_token');
        let username = '';
        try {
            const decoded = jwtDecode(token);
            username = decoded.sub; // or whatever field contains the username in your JWT
        } catch (error) {
            console.error('Error decoding token:', error);
            setIsHistoryLoading(false);
            return;
        }

        try {
            const response = await fetch(
                `http://localhost:8080/session-service/api/sessions/${sessionId}/revert/${snapshotId}`,
                {
                    method: 'POST',
                    headers: { 'Authorization': `Bearer ${token}`,'X-Authenticated-Username': username }
                }
            );

            if (!response.ok) throw new Error('Failed to revert');

            const updatedSession = await response.json();

            if (stompClientRef.current?.connected) {
                stompClientRef.current.publish({
                    destination: `/app/code/${sessionId}`,
                    body: JSON.stringify({ content: updatedSession.codeContent })
                });
            }

            setCodeContent(updatedSession.codeContent);
            setSessionDetails(updatedSession);
        } catch (error) {
            alert(error.message);
        } finally {
            setIsHistoryLoading(false);
        }
    };

    if (joinStatus === 'CHECKING' || !currentUser) return <div className="loading-screen"><Spinner animation="border" variant="primary" /><h4>Checking permissions...</h4></div>;
    if (joinStatus === 'REQUIRES_REQUEST') return <div className="loading-screen"><h2>Private Session</h2><p>You need permission to join.</p><Button onClick={() => handleRequestToJoin(localStorage.getItem('jwt_token'), currentUser, sessionDetails)}>Request to Join</Button><Button variant="outline-secondary" className="mt-3" onClick={() => navigate('/')}>Back to Home</Button></div>;
    // Update the PENDING return statement:
    // Replace your PENDING return statement with this:
    if (joinStatus === 'PENDING') {
        const handleCheckStatus = async () => {
            console.log("🔄 Manual status check triggered");
            const token = localStorage.getItem('jwt_token');
            try {
                const details = await fetchSessionDetails(token);
                console.log("📊 Status check result:", details);

                // Check if user is now in participants (approved)
                if (details.participants[currentUser]) {
                    console.log("✅ User is now approved!");
                    setJoinStatus('APPROVED');
                    setUserRole(details.participants[currentUser]);
                    connectToWebSocket(currentUser, token, details);
                }
                // Check if user is still in pending requests
                else if (details.pendingRequests?.includes(currentUser)) {
                    console.log("⏳ Still pending...");
                    // Stay in PENDING state
                }
                // If not in pending and not approved, might have been denied
                else if (!details.pendingRequests?.includes(currentUser) && !details.participants[currentUser]) {
                    console.log("❌ Request might have been denied or removed");
                    setJoinStatus('REQUIRES_REQUEST');
                    alert("Your join request is no longer pending. Please request again if needed.");
                }
            } catch (error) {
                console.error("Error checking status:", error);
            }
        };

        return (
            <div className="loading-screen">
                <Spinner animation="border" variant="primary" />
                <h4>Request sent. Waiting for owner approval...</h4>
                <p className="text-muted mt-2">
                    Session: <strong>{sessionId}</strong><br />
                    Owner: <strong>{sessionDetails?.ownerUsername}</strong>
                </p>
                <Button
                    variant="outline-primary"
                    className="mt-3"
                    onClick={handleCheckStatus}
                    disabled={isPermissionLoading}
                >
                    {isPermissionLoading ? <Spinner size="sm" /> : '🔄 Check Status'}
                </Button>
                <Button
                    variant="outline-secondary"
                    className="mt-2 ms-2"
                    onClick={() => navigate('/')}
                >
                    Back to Home
                </Button>
            </div>
        );
    }
    if (joinStatus === 'APPROVED' && isConnected) {
        const isOwner = sessionDetails && sessionDetails.ownerUsername === currentUser;
        console.log("🎯 DEBUG - EditorPage RENDER:", {
            joinStatus,
            isConnected,
            sessionDetails,
            currentUser,
            userRole,
            pendingRequests: sessionDetails?.pendingRequests,
            isOwner: sessionDetails?.ownerUsername === currentUser
        });

        return (
            <>
                <ToastContainer position="top-end" className="p-3" style={{ zIndex: 1050 }}>
                    {joinRequests.map(req => (
                        <Toast key={req.fromUser} onClose={() => handleApproveOrDeny(req.fromUser, 'deny')}>
                            <Toast.Header><strong className="me-auto">Join Request</strong></Toast.Header>
                            <Toast.Body><p><strong>{req.fromUser}</strong> wants to join.</p><Button size="sm" variant="success" className="me-2" onClick={() => handleApproveOrDeny(req.fromUser, 'approve')}>Approve</Button><Button size="sm" variant="danger" onClick={() => handleApproveOrDeny(req.fromUser, 'deny')}>Deny</Button></Toast.Body>
                        </Toast>
                    ))}
                </ToastContainer>
                <div className="editor-page-container">
                    <div className="editor-container">
                        <div className="controls p-2 border-bottom border-secondary bg-dark d-flex justify-content-between align-items-center">
                            <div>
                                <span>Session: <strong>{sessionId.substring(0)}</strong> | Language: <strong>{language.charAt(0).toUpperCase() + language.slice(1)}</strong></span>
                            </div>
                            <div>
                                {isOwner && <Button variant="outline-warning" size="sm" onClick={handleSaveSnapshot} disabled={isHistoryLoading} className="me-2">{isHistoryLoading ? <Spinner as="span" size="sm" animation="border" /> : '💾 Save'}</Button>}
                                <Button variant="outline-info" size="sm" onClick={handleExplainCode} disabled={isExplaining || isExecuting} className="me-2">{isExplaining ? <Spinner as="span" size="sm" animation="border" /> : '✨ Explain'}</Button>
                                <Button variant="success" size="sm" onClick={handleRunCode} disabled={isExecuting || isExplaining}>{isExecuting ? <Spinner as="span" size="sm" animation="border" /> : '▶️ Run'}</Button>
                                {isOwner ? (
                                    // Owner sees Delete Session button
                                    <Button
                                        variant="outline-danger"
                                        size="sm"
                                        className="ms-3"
                                        onClick={handleDeleteSession}
                                        title="Permanently delete this session"
                                    >
                                        🗑️ Delete Session
                                    </Button>
                                ) : (
                                    // Participants see Leave Session button
                                    <Button
                                        variant="outline-warning"
                                        size="sm"
                                        className="ms-3"
                                        onClick={handleLeaveSession}
                                        title="Leave this session"
                                    >
                                        🚪 Leave Session
                                    </Button>
                                )}
                            </div>
                        </div>
                        <Editor
                            height="calc(100% - 49px)"
                            theme="vs-dark"
                            language={language}
                            value={codeContent}
                            onChange={handleEditorChange}
                            onMount={handleEditorDidMount}
                            options={{ automaticLayout: true, wordWrap: 'on', readOnly: userRole === Role.VIEWER }} />
                    </div>
                    <div className="side-panel">
                        <Tabs activeKey={activeTab} onSelect={(k) => setActiveTab(k)} id="side-panel-tabs" className="mb-0 flex-shrink-0" fill>
                            <Tab eventKey="chat" title="Chat 💬"><ChatPanel messages={chatMessages} onSendMessage={handleSendMessage} currentUser={currentUser} /></Tab>
                            <Tab eventKey="participants" title="Users 👥">{sessionDetails && <ParticipantsPanel sessionDetails={sessionDetails} currentUsername={currentUser} onBlockUser={handleBlockUser} onApprove={(username) => handleApproveOrDeny(username, 'approve')} onDeny={(username) => handleApproveOrDeny(username, 'deny')}    onChangeRole={handleChangeRole} isLoading={isPermissionLoading || isHistoryLoading} onLeaveSession={handleLeaveSession} />}</Tab>
                            <Tab eventKey="history" title="History 💾"><div className="panel-content">{isHistoryLoading && <div className="text-center"><Spinner animation="border" size="sm" /></div>}<ListGroup variant="flush">{sessionDetails?.history?.length > 0 ? (sessionDetails.history.map(snap => (<ListGroup.Item key={snap.id} className="bg-dark text-white d-flex justify-content-between align-items-center"><div className="snapshot-timestamp small">{new Date(snap.timestamp).toLocaleString()}</div>{isOwner && (<Button variant="outline-warning" size="sm" onClick={() => handleRevertToSnapshot(snap.id)} disabled={isHistoryLoading}>Revert</Button>)}</ListGroup.Item>))) : (<p className="text-muted text-center mt-3">No snapshots saved.</p>)}</ListGroup></div></Tab>
                            <Tab eventKey="input" title="Input (stdin)">
                                <div className="panel-content" style={{ padding: 0 }}>
                                    <Form.Control
                                        as="textarea"
                                        placeholder="Enter standard input for your program here..."
                                        value={stdin}
                                        onChange={(e) => setStdin(e.target.value)}
                                        className="bg-dark text-white border-0 h-100"
                                        style={{ resize: 'none' }}
                                    />
                                </div>
                            </Tab>
                            <Tab eventKey="output" title="Output >_"><pre className="panel-content output-panel">{executionOutput}</pre></Tab>
                            <Tab eventKey="ai-assistant" title="AI ✨"><pre className="panel-content output-panel">{aiExplanation}</pre></Tab>
                        </Tabs>
                    </div>
                </div>
            </>
        );
    }

    return <div className="loading-screen"><Spinner animation="border" variant="primary" /><h4>Loading Editor...</h4></div>;
}

export default EditorPage;