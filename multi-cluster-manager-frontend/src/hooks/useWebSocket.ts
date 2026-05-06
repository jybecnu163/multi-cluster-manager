import {useCallback, useEffect, useRef, useState} from 'react';

type WebSocketStatus = 'connecting' | 'open' | 'closed' | 'error';

interface UseWebSocketOptions {
    /** 收到消息时的回调 */
    onMessage: (event: MessageEvent) => void;
    /** 错误时的回调（可选） */
    onError?: (event: Event) => void;
    /** 连接关闭时的回调（可选） */
    onClose?: (event: CloseEvent) => void;
    /** 是否在组件卸载时自动关闭连接，默认为 true */
    autoClose?: boolean;
}

/**
 * React Hook：管理一个 WebSocket 连接。
 * 自动从 localStorage 获取 access_token 并作为查询参数附加到 URL 中。
 *
 * @param url          WebSocket 服务端地址，如果为 null 则不建立连接
 * @param options      回调及配置
 * @returns { status, send, close }  连接状态、发送消息、关闭连接
 */
export function useWebSocket(
    url: string | null,
    options: UseWebSocketOptions
) {
    const {onMessage, onError, onClose, autoClose = true} = options;
    const [status, setStatus] = useState<WebSocketStatus>('closed');
    const wsRef = useRef<WebSocket | null>(null);
    const onMessageRef = useRef(onMessage);
    const onErrorRef = useRef(onError);
    const onCloseRef = useRef(onClose);

    // 保持回调引用最新，避免 useEffect 重新执行
    onMessageRef.current = onMessage;
    onErrorRef.current = onError;
    onCloseRef.current = onClose;

    useEffect(() => {
        if (!url) {
            setStatus('closed');
            return;
        }

        // 构建带 token 的 WebSocket URL
        const token = localStorage.getItem('access_token');
        const wsUrl = new URL(url, window.location.origin);
        if (token) {
            wsUrl.searchParams.set('token', token);
        }

        let ws: WebSocket | null = null;
        let shouldReconnect = true;

        const connect = () => {
            if (!shouldReconnect) return;

            ws = new WebSocket(wsUrl.toString());
            wsRef.current = ws;
            setStatus('connecting');

            ws.onopen = () => {
                setStatus('open');
            };

            ws.onmessage = (event: MessageEvent) => {
                onMessageRef.current(event);
            };

            ws.onerror = (event: Event) => {
                setStatus('error');
                onErrorRef.current?.(event);
            };

            ws.onclose = (event: CloseEvent) => {
                setStatus('closed');
                onCloseRef.current?.(event);
                wsRef.current = null;
                // 不自动重连，由调用方决定（错误时可手动重连）
            };
        };

        connect();

        return () => {
            shouldReconnect = false;
            if (autoClose && ws && ws.readyState !== WebSocket.CLOSED) {
                ws.close();
            }
            wsRef.current = null;
        };
    }, [url, autoClose]);

    // 发送消息
    const send = useCallback((data: string | ArrayBufferLike | Blob | ArrayBufferView) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
            wsRef.current.send(data);
        } else {
            console.warn('WebSocket is not open, cannot send message');
        }
    }, []);

    // 手动关闭
    const close = useCallback(() => {
        if (wsRef.current) {
            wsRef.current.close();
        }
    }, []);

    return {status, send, close};
}