import {useCallback, useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import {Button, Card, Descriptions, Input, message, Modal, Popconfirm, Space, Steps, Tag, Typography} from 'antd';
import {CopyOutlined, PlayCircleOutlined, ReloadOutlined, RollbackOutlined} from '@ant-design/icons';
import {
    CanaryTaskDetail as CanaryTaskType,
    getCanaryTask,
    getInternalEndpoint,
    promoteStage,
    resumeCanary,
    rollbackCanary
} from '../../api/canary';

const {Title, Text} = Typography;

// 状态与阶段映射
const statusMap: Record<string, string> = {
    internal_test: '内部测试',
    waiting_approval: '等待审批',
    traffic_5: '5% 流量',
    traffic_25: '25% 流量',
    full: '全量',
    success: '已完成',
    failed: '已失败',
    paused: '已暂停',
    rolled_back: '已回滚',
};

const stepLabels = ['任务创建', '内部测试', '请求流量', '5%流量', '25%流量', '全量', '完成'];

export default function CanaryTaskDetail() {
    const {taskId} = useParams<{ taskId: string }>();
    const [task, setTask] = useState<CanaryTaskType | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [internalUrl, setInternalUrl] = useState<string | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [actionLoading, setActionLoading] = useState<Record<string, boolean>>({});

    const fetchTask = useCallback(async () => {
        if (!taskId) return;
        setLoading(true);
        setError(null);
        try {
            const data = await getCanaryTask(Number(taskId));
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
            fetchTask(); // 刷新任务状态
        } catch (err: any) {
            message.error(err?.response?.data?.message || '操作失败');
        } finally {
            setActionLoading((prev) => ({...prev, [actionName]: false}));
        }
    };

    const showInternalEndpoint = async () => {
        if (!task) return;
        try {
            const res = await getInternalEndpoint(task.id);
            setInternalUrl(res.internal_url);
            setModalOpen(true);
        } catch {
            message.error('无法获取内测链接');
        }
    };

    const getActions = () => {
        if (!task) return null;
        const controls: React.ReactNode[] = [];

        if (task.status === 'internal_test') {
            controls.push(
                <Button
                    key="internal_tested"
                    onClick={() => handleAction(() => promoteStage(task.id, 'internal_tested'), '内部测试完成')}
                    loading={actionLoading['internal_tested']}
                >
                    内部测试完成
                </Button>
            );
            controls.push(
                <Button
                    key="request_traffic"
                    onClick={() => handleAction(() => promoteStage(task.id, 'request_traffic'), '请求流量')}
                    loading={actionLoading['request_traffic']}
                >
                    请求接入流量
                </Button>
            );
        }

        if (task.status === 'traffic_5') {
            controls.push(
                <Button
                    key="promote_25"
                    onClick={() => handleAction(() => promoteStage(task.id, 'promote_25'), '扩大至25%')}
                    loading={actionLoading['promote_25']}
                >
                    扩大至25%
                </Button>
            );
        }

        if (task.status === 'traffic_25') {
            controls.push(
                <Button
                    key="promote_100"
                    onClick={() => handleAction(() => promoteStage(task.id, 'promote_100'), '全量')}
                    loading={actionLoading['promote_100']}
                >
                    全量发布
                </Button>
            );
        }

        if (task.status === 'paused') {
            controls.push(
                <Button
                    key="resume"
                    icon={<PlayCircleOutlined/>}
                    onClick={() => handleAction(() => resumeCanary(task.id), '恢复')}
                    loading={actionLoading['resume']}
                >
                    恢复任务
                </Button>
            );
        }

        // 回滚按钮：非终态且未回滚可执行
        if (!['success', 'failed', 'rolled_back'].includes(task.status)) {
            controls.push(
                <Popconfirm
                    key="rollback"
                    title="确定回滚该灰度任务？"
                    onConfirm={() => handleAction(() => rollbackCanary(task.id), '回滚')}
                >
                    <Button danger icon={<RollbackOutlined/>} loading={actionLoading['rollback']}>
                        回滚
                    </Button>
                </Popconfirm>
            );
        }

        return controls.length > 0 ? <Space style={{marginTop: 16}}>{controls}</Space> : null;
    };

    if (loading) return <Card loading={true}/>;
    if (error) return <Card>{error}</Card>;
    if (!task) return <Card>任务不存在</Card>;

    return (
        <Card
            title={<Title level={2} style={{margin: 0}}>灰度任务 #{task.id}</Title>}
            extra={
                <Button icon={<ReloadOutlined/>} onClick={fetchTask}>
                    刷新
                </Button>
            }
        >
            <Descriptions column={2} bordered size="small" style={{marginBottom: 24}}>
                <Descriptions.Item label="状态">
                    <Tag
                        color={task.status === 'success' ? 'green' : task.status === 'failed' || task.status === 'rolled_back' ? 'red' : 'blue'}>
                        {statusMap[task.status] || task.status}
                    </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="目标镜像">{task.target_image}</Descriptions.Item>
                <Descriptions.Item label="当前流量权重">{task.canary_weight}%</Descriptions.Item>
                <Descriptions.Item label="审批超时">{task.approval_timeout_hours}小时</Descriptions.Item>
                <Descriptions.Item label="创建时间">{task.created_at}</Descriptions.Item>
            </Descriptions>

            <Steps
                current={task.current_stage}
                status={task.status === 'failed' ? 'error' : task.status === 'rolled_back' ? 'error' : undefined}
                size="small"
                style={{marginBottom: 24}}
            >
                {stepLabels.map((label) => (
                    <Steps.Step key={label} title={label}/>
                ))}
            </Steps>

            {task.status === 'internal_test' && (
                <Button
                    style={{marginBottom: 16}}
                    onClick={showInternalEndpoint}
                    icon={<CopyOutlined/>}
                >
                    获取内测链接
                </Button>
            )}

            {task.status === 'waiting_approval' && (
                <Card style={{marginBottom: 16, background: '#fffbe6'}}>
                    <Text strong>等待审批中</Text>
                    <p>审批单ID: {task.approval_id}，请通知部门主管审批。</p>
                </Card>
            )}

            {getActions()}

            <Modal
                title="内部测试访问链接"
                open={modalOpen}
                onCancel={() => setModalOpen(false)}
                footer={null}
            >
                <Input value={internalUrl || ''} readOnly/>
                <Button
                    style={{marginTop: 8}}
                    icon={<CopyOutlined/>}
                    onClick={() => {
                        navigator.clipboard.writeText(internalUrl || '');
                        message.success('已复制');
                    }}
                >
                    复制链接
                </Button>
            </Modal>
        </Card>
    );
}