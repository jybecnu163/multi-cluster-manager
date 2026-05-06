import {useState, useEffect} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {
    Card, Button, Space, Descriptions, Tag, Table, Typography, message, Row, Col,
    Slider, InputNumber, Form, Input, Switch, Alert, Tooltip
} from 'antd';
import {ArrowLeftOutlined, DownloadOutlined} from '@ant-design/icons';
import ReactEChartsCore from 'echarts-for-react/lib/core';
import * as echarts from 'echarts/core';
import {LineChart} from 'echarts/charts';
import {GridComponent, TooltipComponent, TitleComponent} from 'echarts/components';
import {CanvasRenderer} from 'echarts/renderers';
import {
    getServiceDetail, getServiceMetrics, exportServiceReport, manualScale
} from '../../api/services';
import {getDepartmentSettings} from '../../api/departments';
import {ServiceDetail as ServiceDetailType, PodInfo, MetricTimeSeries} from '../../api/types';
import {useAuthStore} from '../../store/useAuthStore';

echarts.use([LineChart, GridComponent, TooltipComponent, TitleComponent, CanvasRenderer]);

const {Title: TypTitle, Text} = Typography;

export default function ServiceDetailPage() {
    const {id} = useParams<{ id: string }>();
    const serviceId = Number(id);
    const navigate = useNavigate();
    const [detail, setDetail] = useState<ServiceDetailType | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [cpuMetrics, setCpuMetrics] = useState<MetricTimeSeries | null>(null);
    const [memMetrics, setMemMetrics] = useState<MetricTimeSeries | null>(null);
    const {user} = useAuthStore();

    // 扩缩容相关状态
    const [deptSettings, setDeptSettings] = useState<{ allow_ops_bypass_prod_scale: boolean } | null>(null);
    const [scaleResult, setScaleResult] = useState<{ task_id: number; requires_approval: boolean } | null>(null);
    const [scaleSubmitting, setScaleSubmitting] = useState(false);
    const [scaleForm] = Form.useForm();

    const isOps = user?.roles?.includes('运维工程师');
    const canIgnoreApproval = deptSettings?.allow_ops_bypass_prod_scale && isOps;

// 加载详情、指标和部门设置
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

    // 报表导出
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

// 手动扩缩容提交
    const handleManualScale = async (values: {
        target_replicas: number;
        reason: string;
        ignore_approval?: boolean
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

    const fetchDetail = async () => {
        setLoading(true);
        setError(null);
        try {
            const [detailData, cpuData, memData] = await Promise.all([
                getServiceDetail(serviceId),
                getServiceMetrics(serviceId, 'cpu', '1h'),
                getServiceMetrics(serviceId, 'memory', '1h'),
            ]);
            setDetail(detailData);
            setCpuMetrics(cpuData);
            setMemMetrics(memData);
        } catch (err: any) {
            setError('无法加载服务详情');
            message.error('加载失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDetail();
    }, [serviceId]); // eslint-disable-line react-hooks/exhaustive-deps


    const buildChartOption = (title: string, data: MetricTimeSeries | null) => {
        if (!data) return {};
        return {
            title: {text: title},
            tooltip: {trigger: 'axis'},
            xAxis: {type: 'category', data: data.timestamps.map((t) => new Date(t).toLocaleTimeString())},
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

            {/* Pod 实例列表 */}
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
                        <ReactEChartsCore option={buildChartOption('CPU 使用率', cpuMetrics)} style={{height: 300}}/>
                    </Card>
                </Col>
                <Col xs={24} lg={12}>
                    <Card>
                        <ReactEChartsCore option={buildChartOption('内存使用率', memMetrics)} style={{height: 300}}/>
                    </Card>
                </Col>
            </Row>

            {/* 手动扩缩容面板 */}
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
                        <Row gutter={16} align="middle">
                            <Col xs={24} sm={18}>
                                <Slider min={0} max={Math.max(detail.replicas * 2, 10)} step={1}/>
                            </Col>
                            <Col xs={24} sm={6}>
                                <InputNumber min={0} style={{width: '100%'}}/>
                            </Col>
                        </Row>
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
                            title={
                                !canIgnoreApproval
                                    ? '仅当部门设置允许运维免批且您具备运维工程师角色时可用'
                                    : '勾选后将跳过生产审批（需部门主管已开启免批）'
                            }
                        >
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