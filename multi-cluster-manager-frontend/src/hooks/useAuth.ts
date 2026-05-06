import {useEffect, useRef, useState, useCallback} from 'react';

type WebSocketStatus = 'connecting' | 'open' | 'closed' | 'error';

interface UseWebSocketOptions {
    onMessage: (event: MessageEvent) => void;
    onError?: (event: Event) => void;
    onClose?: (event: CloseEvent) => void;
}

export function useWebSocket(url: string | null, options: UseWebSocketOptions) {
    const [status, setStatus] = useState<WebSocketStatus>('closed');
    const wsRef = useRef<WebSocket | null>(null);
    const {onMessage, onError, onClose} = options;

    useEffect(() => {
        if (!url) {
            setStatus('closed');
            return;
        }

        // 附加 JWT token 作为查询参数
        const token = localStorage.getItem('access_token');
        const wsUrl = new URL(url, window.location.origin);
        if (token) {
            wsUrl.searchParams.set('token', token);
        }

        const ws = new WebSocket(wsUrl.toString());
        wsRef.current = ws;
        setStatus('connecting');

        ws.onopen = () => setStatus('open');
        ws.onmessage = onMessage;
        ws.onerror = (event) => {
            setStatus('error');
            onError?.(event);
        };
        ws.onclose = (event) => {
            setStatus('closed');
            onClose?.(event);
        };

        return () => {
            ws.close();
            wsRef.current = null;
        };
    }, [url]); // 仅在 url 变化时重连

    const send = useCallback((data: string) => {
        wsRef.current?.send(data);
    }, []);

    const close = useCallback(() => {
        wsRef.current?.close();
    }, []);

    return {status, send, close};
}