import {useState, useEffect} from 'react';
import {
    Table, Button, Space, Select, DatePicker, Typography, message, Tag, Modal, Tooltip
} from 'antd';
import {
    ReloadOutlined, ExportOutlined, SecurityScanOutlined, CopyOutlined
} from '@ant-design/icons';
import {getAuditLogs, exportAuditLogs} from '../../api/audit';
import {AuditLog, PaginatedResponse} from '../../api/types';
import dayjs from 'dayjs';

const {Title} = Typography;
const {RangePicker} = DatePicker;
const {Option} = Select;

// 预定义常用操作类型（可扩展）
const OPERATION_TYPES = [
    'user.login', 'user.logout', 'company.create', 'deployment.start',
    'scale.manual', 'canary.create', 'batch.create', 'pipeline.trigger',
    'approval.approve', 'approval.reject',
];

export default function AuditLogs() {
    const [logs, setLogs] = useState<AuditLog[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [pagination, setPagination] = useState({current: 1, pageSize: 20, total: 0});

    // 筛选条件
    const [timeRange, setTimeRange] = useState<[string, string] | null>(null);
    const [operation, setOperation] = useState<string | undefined>(undefined);
    const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null);

    const fetchLogs = async (page: number = 1, pageSize: number = 20) => {
        setLoading(true);
        setError(null);
        try {
            const params: any = {
                page,
                page_size: pageSize,
                operation: operation || undefined,
            };
            if (timeRange && timeRange[0] && timeRange[1]) {
                params.start_time = dayjs(timeRange[0]).toISOString();
                params.end_time = dayjs(timeRange[1]).toISOString();
            }
            const res: PaginatedResponse<AuditLog> = await getAuditLogs(params);
            setLogs(res.records);
            setPagination({
                current: res.current,
                pageSize: res.size,
                total: res.total,
            });
        } catch (err: any) {
            const msg = '加载审计日志失败';
            setError(msg);
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLogs(1, pagination.pageSize);
    }, [operation, timeRange]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleTableChange = (pagination: any) => {
        fetchLogs(pagination.current, pagination.pageSize);
    };

    const handleExport = async () => {
        try {
            const params: any = {};
            if (timeRange && timeRange[0] && timeRange[1]) {
                params.start_time = dayjs(timeRange[0]).toISOString();
                params.end_time = dayjs(timeRange[1]).toISOString();
            }
            if (operation) params.operation = operation;
            const blob = await exportAuditLogs(params);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `audit-logs-${dayjs().format('YYYYMMDDHHmmss')}.csv`;
            a.click();
            window.URL.revokeObjectURL(url);
            message.success('导出成功');
        } catch {
            message.error('导出失败，可能是数据量过大或后端不支持大分页');
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {
            title: '操作时间',
            dataIndex: 'created_at',
            key: 'created_at',
            width: 180,
            render: (t: string) => dayjs(t).format('YYYY-MM-DD HH:mm:ss'),
        },
        {title: '操作者ID', dataIndex: 'user_id', key: 'user_id', width: 100},
        {title: '操作', dataIndex: 'operation', key: 'operation', ellipsis: true},
        {title: '目标类型', dataIndex: 'target_type', key: 'target_type'},
        {
            title: '哈希链',
            dataIndex: 'prev_hash',
            key: 'prev_hash',
            width: 120,
            render: (hash: string) => (
                hash ? <Tag color="blue">{hash.substring(0, 8)}...</Tag> : '-'
            ),
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: AuditLog) => (
                <Space>
                    <Tooltip title="查看详情与哈希链">
                        <Button
                            icon={<SecurityScanOutlined/>}
                            size="small"
                            onClick={() => setSelectedLog(record)}
                        >
                            详情/哈希
                        </Button>
                    </Tooltip>
                </Space>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>审计日志</Title>
                <Space wrap>
                    <RangePicker
                        showTime
                        onChange={(dates) => {
                            if (dates && dates[0] && dates[1]) {
                                setTimeRange([dates[0].toISOString(), dates[1].toISOString()]);
                            } else {
                                setTimeRange(null);
                            }
                        }}
                    />
                    <Select
                        placeholder="操作类型"
                        allowClear
                        style={{width: 200}}
                        onChange={setOperation}
                        value={operation}
                    >
                        {OPERATION_TYPES.map(op => (
                            <Option key={op} value={op}>{op}</Option>
                        ))}
                    </Select>
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={() => fetchLogs(pagination.current, pagination.pageSize)}
                        loading={loading}
                    >
                        刷新
                    </Button>
                    <Button icon={<ExportOutlined/>} onClick={handleExport}>
                        导出 CSV
                    </Button>
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={logs}
                rowKey="id"
                loading={loading}
                pagination={pagination}
                onChange={handleTableChange}
                locale={{emptyText: error ? '加载失败' : '暂无日志'}}
            />

            {/* 日志详情模态框（含哈希链） */}
            <Modal
                title="日志详情"
                open={!!selectedLog}
                onCancel={() => setSelectedLog(null)}
                footer={null}
                width={600}
            >
                {selectedLog && (
                    <div>
                        <p><strong>ID：</strong>{selectedLog.id}</p>
                        <p><strong>操作：</strong>{selectedLog.operation}</p>
                        <p><strong>目标：</strong>{selectedLog.target_type} #{selectedLog.target_id}</p>
                        <p><strong>请求IP：</strong>{selectedLog.request_ip}</p>
                        <p><strong>User-Agent：</strong>{selectedLog.user_agent}</p>
                        <p><strong>详细数据：</strong></p>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 8,
                            borderRadius: 4,
                            maxHeight: 200,
                            overflow: 'auto'
                        }}>
              {JSON.stringify(selectedLog.details, null, 2)}
            </pre>
                        <p style={{marginTop: 12}}>
                            <strong>前一条哈希（prev_hash）：</strong>
                            <code>{selectedLog.prev_hash}</code>
                            <Button
                                icon={<CopyOutlined/>}
                                size="small"
                                style={{marginLeft: 8}}
                                onClick={() => {
                                    navigator.clipboard.writeText(selectedLog.prev_hash);
                                    message.success('已复制');
                                }}
                            />
                        </p>
                        <p style={{color: 'gray', fontSize: 12}}>
                            哈希链防篡改说明：每条日志存储前一条的 SHA256 哈希，形成链式结构，可用于校验完整性。
                        </p>
                    </div>
                )}
            </Modal>
        </>
    );
}