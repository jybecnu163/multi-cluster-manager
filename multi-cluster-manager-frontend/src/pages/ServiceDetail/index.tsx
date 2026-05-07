import {useEffect, useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {
    Alert,
    Button,
    Card,
    Col,
    Descriptions,
    Form,
    Input,
    InputNumber,
    message,
    Row,
    Slider,
    Space,
    Switch,
    Table,
    Tag,
    Tooltip,
    Typography,
} from 'antd';
import {ArrowLeftOutlined, DownloadOutlined} from '@ant-design/icons';
import ReactECharts from 'echarts-for-react';
import {exportServiceReport, getServiceDetail, getServiceMetrics, manualScale,} from '../../api/services';
import {getDepartmentSettings} from '../../api/departments';
import {MetricTimeSeries, PodInfo, ServiceDetail as ServiceDetailType} from '../../api/types';
import {useAuthStore} from '../../store/useAuthStore';

const {Title: TypTitle, Text} = Typography;

// ---------- 自定义双控件：滑块 + 数字输入，受控于 Form.Item ----------
const ReplicasInput: React.FC<{
    value?: number;
    onChange?: (value: number) => void;
    max: number;
}> = ({value = 0, onChange, max}) => (
    <Row gutter={16} align="middle">
        <Col xs={24} sm={18}>
            <Slider min={0} max={max} step={1} value={value} onChange={onChange}/>
        </Col>
        <Col xs={24} sm={6}>
            <InputNumber
                min={0}
                max={max}
                style={{width: '100%'}}
                value={value}
                onChange={onChange}
            />
        </Col>
    </Row>
);

export default function ServiceDetailPage() {
    const {id} = useParams<{ id: string }>();
    const serviceId = Number(id);
    const navigate = useNavigate();
    const {user} = useAuthStore();

    const [detail, setDetail] = useState<ServiceDetailType | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [cpuMetrics, setCpuMetrics] = useState<MetricTimeSeries | null>(null);
    const [memMetrics, setMemMetrics] = useState<MetricTimeSeries | null>(null);

    // 扩缩容相关
    const [deptSettings, setDeptSettings] = useState<{ allow_ops_bypass_prod_scale: boolean } | null>(null);
    const [scaleResult, setScaleResult] = useState<{ task_id: number; requires_approval: boolean } | null>(null);
    const [scaleSubmitting, setScaleSubmitting] = useState(false);
    const [scaleForm] = Form.useForm();

    const isOps = user?.roles?.includes('运维工程师');
    const canIgnoreApproval = deptSettings?.allow_ops_bypass_prod_scale && isOps;

    useEffect(() => {
        if (!serviceId) return;
        setLoading(true);
        setError(null);
        Promise.all([
            getServiceDetail(serviceId),
            getServiceMetrics(serviceId, 'cpu', '1h'),
            getServiceMetrics(serviceId, 'memory', '1h'),
        ])
            .then(([detailData, cpuData, memData]) => {
                setDetail(detailData);
                setCpuMetrics(cpuData);
                setMemMetrics(memData);
                return getDepartmentSettings(detailData.department_id);
            })
            .then((settings) => setDeptSettings(settings))
            .catch(() => {
                setError('加载服务详情失败');
                message.error('加载失败');
            })
            .finally(() => setLoading(false));
    }, [serviceId]);

    const handleExport = async () => {
        try {
            const blob = await exportServiceReport(serviceId, 'day');
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `service-${serviceId}-report.csv`;
            a.click();
            window.URL.revokeObjectURL(url);
            message.success('报表已导出');
        } catch {
            message.error('导出失败');
        }
    };

    const handleManualScale = async (values: {
        target_replicas: number;
        reason: string;
        ignore_approval?: boolean;
    }) => {
        setScaleSubmitting(true);
        try {
            const res = await manualScale(serviceId, {
                target_replicas: values.target_replicas,
                reason: values.reason,
                ignore_approval: values.ignore_approval,
            });
            setScaleResult(res);
            message.success(`扩缩容任务已提交，任务ID: ${res.task_id}`);
        } catch (err: any) {
            message.error(err?.response?.data?.message || '操作失败');
        } finally {
            setScaleSubmitting(false);
        }
    };

    const buildChartOption = (title: string, data: MetricTimeSeries | null) => {
        if (!data) return {};
        return {
            title: {text: title, left: 'center'},
            tooltip: {trigger: 'axis'},
            xAxis: {
                type: 'category',
                data: data.timestamps.map((t) => new Date(t).toLocaleTimeString()),
            },
            yAxis: {type: 'value'},
            series: [{data: data.values, type: 'line', smooth: true}],
        };
    };

    if (loading) return <Card loading={true}/>;
    if (error) return <Card>{error}</Card>;
    if (!detail) return <Card>服务不存在</Card>;

    return (
        <>
            <Space style={{marginBottom: 16}}>
                <Button icon={<ArrowLeftOutlined/>} onClick={() => navigate('/services')}>
                    返回列表
                </Button>
                <Button icon={<DownloadOutlined/>} onClick={handleExport}>
                    导出报表 CSV
                </Button>
            </Space>

            {/* 基本信息 */}
            <Card title={<TypTitle level={3}>{detail.name}</TypTitle>}>
                <Descriptions column={2} bordered size="small">
                    <Descriptions.Item label="工作负载类型">{detail.workload_type}</Descriptions.Item>
                    <Descriptions.Item label="命名空间">{detail.namespace}</Descriptions.Item>
                    <Descriptions.Item label="副本数">{detail.replicas}</Descriptions.Item>
                    <Descriptions.Item label="CPU 请求">{detail.cpu_request}</Descriptions.Item>
                    <Descriptions.Item label="内存请求">{detail.memory_request}</Descriptions.Item>
                    <Descriptions.Item label="启动命令" span={2}>{detail.startup_command}</Descriptions.Item>
                </Descriptions>
                <div style={{marginTop: 16}}>
                    <Text strong>环境变量：</Text>
                    <pre style={{background: '#f5f5f5', padding: 8, borderRadius: 4}}>
            {JSON.stringify(detail.env_variables, null, 2)}
          </pre>
                </div>
            </Card>

            {/* Pod 列表 */}
            <Card title="Pod 实例" style={{marginTop: 16}}>
                <Table
                    dataSource={detail.pods}
                    rowKey="name"
                    columns={[
                        {title: 'Pod 名称', dataIndex: 'name', key: 'name'},
                        {
                            title: '状态',
                            dataIndex: 'status',
                            key: 'status',
                            render: (s: string) => <Tag color={s === 'Running' ? 'green' : 'red'}>{s}</Tag>,
                        },
                        {title: '重启次数', dataIndex: 'restart_count', key: 'restart_count'},
                        {title: 'IP', dataIndex: 'ip', key: 'ip'},
                        {
                            title: '操作',
                            key: 'action',
                            render: (_: any, record: PodInfo) => (
                                <Button
                                    size="small"
                                    onClick={() => navigate(`/services/${serviceId}/logs?pod=${record.name}`)}
                                >
                                    查看日志
                                </Button>
                            ),
                        },
                    ]}
                    size="small"
                />
            </Card>

            {/* 资源使用趋势图 */}
            <Row gutter={16} style={{marginTop: 16}}>
                <Col xs={24} lg={12}>
                    <Card>
                        <ReactECharts option={buildChartOption('CPU 使用率', cpuMetrics)} style={{height: 300}}/>
                    </Card>
                </Col>
                <Col xs={24} lg={12}>
                    <Card>
                        <ReactECharts option={buildChartOption('内存使用率', memMetrics)} style={{height: 300}}/>
                    </Card>
                </Col>
            </Row>

            {/* 手动扩缩容 */}
            <Card title="手动扩缩容" style={{marginTop: 16}}>
                <Form
                    form={scaleForm}
                    layout="vertical"
                    onFinish={handleManualScale}
                    initialValues={{target_replicas: detail.replicas, reason: ''}}
                >
                    <Form.Item
                        label="目标副本数"
                        name="target_replicas"
                        rules={[{required: true, type: 'number', min: 0, message: '请输入副本数'}]}
                    >
                        <ReplicasInput max={Math.max(detail.replicas * 2, 10)}/>
                    </Form.Item>

                    <Form.Item
                        label="操作原因"
                        name="reason"
                        rules={[{required: true, message: '请填写操作原因'}]}
                        extra="生产环境必填详细原因"
                    >
                        <Input.TextArea rows={2} maxLength={500}/>
                    </Form.Item>

                    <Form.Item label="跳过审批" name="ignore_approval" valuePropName="checked">
                        <Tooltip
                            title={!canIgnoreApproval ? '仅当部门设置允许运维免批且您具备运维工程师角色时可用' : '勾选后将跳过生产审批'}>
                            <Switch disabled={!canIgnoreApproval}/>
                        </Tooltip>
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={scaleSubmitting}>
                            提交扩缩容
                        </Button>
                    </Form.Item>
                </Form>

                {scaleResult && (
                    <Alert
                        type={scaleResult.requires_approval ? 'warning' : 'success'}
                        message={
                            scaleResult.requires_approval
                                ? `任务已创建（ID: ${scaleResult.task_id}），需要部门主管审批`
                                : `任务已执行（任务ID: ${scaleResult.task_id}），无需审批`
                        }
                    />
                )}
            </Card>
        </>
    );
}