import {useState, useEffect} from 'react';
import {Tabs, Table, Button, Space, Tag, Typography, message, Modal, Input} from 'antd';
import {CheckOutlined, CloseOutlined, ReloadOutlined} from '@ant-design/icons';
import {
    getPendingApprovals,
    getApprovalHistory,
    handleApproval,
    ApprovalDetail,
} from '../../api/approvals';
import dayjs from 'dayjs';

const {Title} = Typography;
const {TabPane} = Tabs;

export default function ApprovalList() {
    const [activeTab, setActiveTab] = useState<'pending' | 'history'>('pending');
    const [pendingList, setPendingList] = useState<ApprovalDetail[]>([]);
    const [historyList, setHistoryList] = useState<ApprovalDetail[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [approveModal, setApproveModal] = useState<{
        approval: ApprovalDetail;
        action: 'approve' | 'reject';
    } | null>(null);
    const [comment, setComment] = useState('');

    const fetchPending = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getPendingApprovals();
            setPendingList(data);
        } catch (err) {
            setError('获取待审批列表失败');
            message.error('获取待审批列表失败');
        } finally {
            setLoading(false);
        }
    };

    const fetchHistory = async (page = 1) => {
        setLoading(true);
        try {
            const data = await getApprovalHistory(page);
            setHistoryList(data);
        } catch {
            message.error('获取历史记录失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (activeTab === 'pending') fetchPending();
        else fetchHistory();
    }, [activeTab]);

    const executeApproval = async () => {
        if (!approveModal) return;
        try {
            await handleApproval(approveModal.approval.id, {
                action: approveModal.action,
                comment: comment || undefined,
            });
            message.success(approveModal.action === 'approve' ? '审批通过' : '已拒绝');
            setApproveModal(null);
            setComment('');
            fetchPending();
        } catch (err: any) {
            message.error(err?.response?.data?.message || '操作失败');
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '任务ID', dataIndex: 'task_id', key: 'task_id', width: 100},
        {title: '任务类型', dataIndex: 'task_type', key: 'task_type'},
        {title: '请求人', dataIndex: 'requester_name', key: 'requester_name'},
        {
            title: '创建时间',
            dataIndex: 'created_at',
            key: 'created_at',
            render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm:ss'),
        },
        {
            title: '截止时间',
            dataIndex: 'expires_at',
            key: 'expires_at',
            render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm:ss'),
        },
        ...(activeTab === 'pending'
            ? [
                {
                    title: '操作',
                    key: 'action',
                    render: (_: any, record: ApprovalDetail) => (
                        <Space>
                            <Button
                                icon={<CheckOutlined/>}
                                size="small"
                                type="primary"
                                onClick={() => setApproveModal({approval: record, action: 'approve'})}
                            >
                                通过
                            </Button>
                            <Button
                                icon={<CloseOutlined/>}
                                size="small"
                                danger
                                onClick={() => setApproveModal({approval: record, action: 'reject'})}
                            >
                                拒绝
                            </Button>
                        </Space>
                    ),
                },
            ]
            : [
                {
                    title: '审批动作',
                    dataIndex: 'action',
                    key: 'action',
                    render: (a: string) => (
                        <Tag color={a === 'approve' ? 'green' : 'red'}>{a}</Tag>
                    ),
                },
                {
                    title: '审批意见',
                    dataIndex: 'comment',
                    key: 'comment',
                    ellipsis: true,
                },
            ]),
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>审批中心</Title>
                <Button
                    icon={<ReloadOutlined/>}
                    onClick={() => (activeTab === 'pending' ? fetchPending() : fetchHistory())}
                    loading={loading}
                >
                    刷新
                </Button>
            </div>

            <Tabs activeKey={activeTab} onChange={(key) => setActiveTab(key as 'pending' | 'history')}>
                <TabPane tab="待审批" key="pending">
                    <Table
                        columns={columns}
                        dataSource={pendingList}
                        rowKey="id"
                        loading={loading}
                        locale={{emptyText: error ? '加载失败' : '暂无待审批事项'}}
                    />
                </TabPane>
                <TabPane tab="已审批历史" key="history">
                    <Table
                        columns={columns}
                        dataSource={historyList}
                        rowKey="id"
                        loading={loading}
                        locale={{emptyText: '暂无历史记录'}}
                    />
                </TabPane>
            </Tabs>

            <Modal
                title={approveModal?.action === 'approve' ? '审批通过' : '拒绝审批'}
                open={!!approveModal}
                onCancel={() => setApproveModal(null)}
                onOk={executeApproval}
                okText="确认"
                cancelText="取消"
            >
                <p>
                    确定{approveModal?.action === 'approve' ? '通过' : '拒绝'}该审批吗？
                </p>
                <Input.TextArea
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder="审批意见（可选）"
                    rows={2}
                />
            </Modal>
        </>
    );
}