import {useCallback, useEffect, useRef, useState} from 'react';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {Button, Card, Input, Space, Typography} from 'antd';
import {ArrowLeftOutlined, ClearOutlined, PauseCircleOutlined, PlayCircleOutlined,} from '@ant-design/icons';
import {useWebSocket} from '../../hooks/useWebSocket';

const {Title} = Typography;

export default function ServiceLogs() {
    const {id} = useParams<{ id: string }>();
    const serviceId = Number(id);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const podName = searchParams.get('pod') || '';

    const [logs, setLogs] = useState<string[]>([]);
    const [paused, setPaused] = useState(false);
    const [filterKeyword, setFilterKeyword] = useState('');
    const logContainerRef = useRef<HTMLDivElement>(null);

    // 构建 WebSocket URL
    const wsUrl = podName
        ? `/api/v1/services/${serviceId}/logs?pod_name=${encodeURIComponent(podName)}`
        : null;

    const handleMessage = useCallback(
        (event: MessageEvent) => {
            if (!paused) {
                const line = event.data as string;
                setLogs((prev) => [...prev.slice(-2000), line]); // 最多保留 2000 行
            }
        },
        [paused]
    );

    const {status} = useWebSocket(wsUrl, {onMessage: handleMessage});

    // 自动滚动到底部
    useEffect(() => {
        if (logContainerRef.current) {
            logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
        }
    }, [logs]);

    const clearLogs = () => setLogs([]);

    const filteredLogs = logs.filter((line) =>
        !filterKeyword ? true : line.includes(filterKeyword)
    );

    return (
        <Card
            title={
                <Space>
                    <Button
                        icon={<ArrowLeftOutlined/>}
                        type="text"
                        onClick={() => navigate(-1)}
                    />
                    <Title level={4} style={{margin: 0}}>
                        实时日志 - {podName || '未指定 Pod'}
                    </Title>
                </Space>
            }
            extra={
                <Space>
                    <Button
                        icon={paused ? <PlayCircleOutlined/> : <PauseCircleOutlined/>}
                        onClick={() => setPaused(!paused)}
                    >
                        {paused ? '继续' : '暂停'}
                    </Button>
                    <Button icon={<ClearOutlined/>} onClick={clearLogs}>
                        清屏
                    </Button>
                    <span>
            状态：
                        {status === 'open'
                            ? '🟢 已连接'
                            : status === 'connecting'
                                ? '🟡 连接中'
                                : '🔴 断开'}
          </span>
                </Space>
            }
        >
            <Input
                placeholder="过滤关键词"
                value={filterKeyword}
                onChange={(e) => setFilterKeyword(e.target.value)}
                style={{marginBottom: 12, width: 300}}
                allowClear
            />
            <div
                ref={logContainerRef}
                style={{
                    height: 500,
                    overflowY: 'auto',
                    background: '#1e1e1e',
                    color: '#d4d4d4',
                    padding: 12,
                    fontFamily: 'monospace',
                    borderRadius: 4,
                    whiteSpace: 'pre-wrap',
                }}
            >
                {filteredLogs.length === 0 ? (
                    <div style={{color: '#888'}}>暂无日志...</div>
                ) : (
                    filteredLogs.map((line, idx) => <div key={idx}>{line}</div>)
                )}
            </div>
        </Card>
    );
}