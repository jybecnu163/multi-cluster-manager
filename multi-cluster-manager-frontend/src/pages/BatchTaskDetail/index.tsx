import {useState, useEffect, useCallback} from 'react';
import {useParams} from 'react-router-dom';
import {Card, Button, Space, Descriptions, Tag, Typography, message, Progress, List, Popconfirm, Modal} from 'antd';
import {
    ReloadOutlined,
    ForwardOutlined,
    RollbackOutlined,
    PlayCircleOutlined,
    PauseCircleOutlined,
} from '@ant-design/icons';
import {
    getBatchTask,
    nextBatch,
    rollbackBatch,
    resumeBatch,
    pauseBatch,
    BatchTaskDetail as BatchTaskType
} from '../../api/batch';

const {Title} = Typography;

export default function BatchTaskDetail() {
    const {taskId} = useParams<{ taskId: string }>();
    const [task, setTask] = useState<BatchTaskType | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({});

    const fetchTask = useCallback(async () => {
        if (!taskId) return;
        setLoading(true);
        setError(null);
        try {
            const data = await getBatchTask(Number(taskId));
            setTask(data);
        } catch (err: any) {
            setError('加载任务详情失败');
            message.error('加载失败');
        } finally {
            setLoading(false);
        }
    }, [taskId]);

    useEffect(() => {
        fetchTask();
    }, [fetchTask]);

    const handleAction = async (actionFn: () => Promise<any>, actionName: string) => {
        setActionLoading((prev) => ({...prev, [actionName]: true}));
        try {
            await actionFn();
            message.success(`操作 "${actionName}" 成功`);
            fetchTask();
        } catch (err: any) {
            const msg = err?.response?.data?.message || '操作失败';
            message.error(msg);
        } finally {
            setActionLoading((prev) => ({...prev, [actionName]: false}));
        }
    };

    const handleNextBatch = () => {
        if (!task) return;
        // 如果后端返回了 batch_config，且 require_confirmation 为 true，则弹出确认框；否则默认总是弹出确认以提供手动确认机会
        const needConfirm = task.batch_config?.require_confirmation ?? true;
        if (needConfirm) {
            Modal.confirm({
                title: '确认执行下一批？',
                content: '请确认已检查当前批次状态，是否继续发布下一批？',
                onOk: () => handleAction(() => nextBatch(task.id), '下一批'),
            });
        } else {
            handleAction(() => nextBatch(task.id), '下一批');
        }
    };

    const handlePause = () => {
        if (!task) return;
        Modal.confirm({
            title: '暂停任务',
            content: '确定暂停该批次发布任务吗？',
            onOk: () => handleAction(() => pauseBatch(task.id), '暂停'),
        });
    };

    if (loading) return <Card loading={true}/>;
    if (error) return <Card>{error}</Card>;
    if (!task) return <Card>任务不存在</Card>;

    const progressPercent = task.total_batches > 0
        ? Math.round(((task.current_batch - 1) / task.total_batches) * 100)
        : 0;

    const isActive = !['finished', 'rolled_back', 'failed', 'success'].includes(task.status);
    const isPaused = task.status === 'paused';

    return (
        <Card
            title={<Title level={2} style={{margin: 0}}>批次发布任务 #{task.id}</Title>}
            extra={<Button icon={<ReloadOutlined/>} onClick={fetchTask}>刷新</Button>}
        >
            <Descriptions column={2} bordered size="small" style={{marginBottom: 24}}>
                <Descriptions.Item label="状态">
                    <Tag
                        color={task.status === 'success' ? 'green' : task.status === 'rolled_back' || task.status === 'failed' ? 'red' : 'blue'}>
                        {task.status}
                    </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="当前批次">
                    {task.current_batch} / {task.total_batches}
                </Descriptions.Item>
            </Descriptions>

            <Progress
                percent={progressPercent}
                status={task.status === 'failed' ? 'exception' : task.status === 'success' ? 'success' : 'active'}
                format={() => `${task.current_batch - 1} / ${task.total_batches}`}
            />

            <Title level={4} style={{marginTop: 24}}>批次状态</Title>
            <List
                dataSource={task.batch_statuses}
                renderItem={(item: any, index: number) => (
                    <List.Item>
                        <List.Item.Meta
                            title={`第 ${item.batch_index ?? index + 1} 批`}
                            description={<Tag>{item.status || 'pending'}</Tag>}
                        />
                    </List.Item>
                )}
                locale={{emptyText: '暂无批次状态信息'}}
            />

            {isActive && (
                <Space style={{marginTop: 16}}>
                    <Button
                        icon={<ForwardOutlined/>}
                        type="primary"
                        onClick={handleNextBatch}
                        loading={actionLoading['下一批']}
                    >
                        执行下一批
                    </Button>

                    {!isPaused && (
                        <Button
                            icon={<PauseCircleOutlined/>}
                            onClick={handlePause}
                            loading={actionLoading['暂停']}
                        >
                            暂停
                        </Button>
                    )}

                    {isPaused && (
                        <Button
                            icon={<PlayCircleOutlined/>}
                            onClick={() => handleAction(() => resumeBatch(task.id), '恢复')}
                            loading={actionLoading['恢复']}
                        >
                            恢复任务
                        </Button>
                    )}

                    <Popconfirm
                        title="确定回滚该批次任务？"
                        onConfirm={() => handleAction(() => rollbackBatch(task.id), '回滚')}
                    >
                        <Button danger icon={<RollbackOutlined/>} loading={actionLoading['回滚']}>
                            回滚
                        </Button>
                    </Popconfirm>
                </Space>
            )}
        </Card>
    );
}