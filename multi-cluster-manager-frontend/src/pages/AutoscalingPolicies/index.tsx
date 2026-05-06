import {useEffect, useState} from 'react';
import {
    Button, Form, Input, InputNumber, message, Modal, Popconfirm,
    Select, Space, Switch, Table, Typography
} from 'antd';
import {EditOutlined, PlusOutlined, ReloadOutlined} from '@ant-design/icons';
import {AutoScalingPolicy, createOrUpdatePolicy, getPolicies} from '../../api/autoscaling';
import {getServices} from '../../api/services';
import {ServiceInstance} from '../../api/types';

const {Title} = Typography;
const {Option} = Select;

export default function AutoscalingPolicies() {
    const [policies, setPolicies] = useState<AutoScalingPolicy[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [editingPolicy, setEditingPolicy] = useState<AutoScalingPolicy | null>(null);
    const [services, setServices] = useState<ServiceInstance[]>([]);
    const [servicesLoading, setServicesLoading] = useState(false);

    useEffect(() => {
        fetchPolicies();
        // 获取服务列表供选择
        setServicesLoading(true);
        getServices({page_size: 200})
            .then((res) => setServices(res.records))
            .catch(() => message.error('获取服务列表失败'))
            .finally(() => setServicesLoading(false));
    }, []);

    const fetchPolicies = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getPolicies();
            setPolicies(data);
        } catch {
            setError('获取策略列表失败');
            message.error('获取策略列表失败');
        } finally {
            setLoading(false);
        }
    };

    const handleToggleEnabled = async (policy: AutoScalingPolicy) => {
        try {
            await createOrUpdatePolicy({...policy, enabled: !policy.enabled});
            message.success(`策略已${policy.enabled ? '禁用' : '启用'}`);
            fetchPolicies();
        } catch (err: any) {
            message.error('操作失败：' + (err?.response?.data?.message || ''));
        }
    };

    const handleEdit = (policy: AutoScalingPolicy) => {
        setEditingPolicy(policy);
        setModalOpen(true);
    };

    const handleCreate = () => {
        setEditingPolicy(null);
        setModalOpen(true);
    };

    const handleSubmitPolicy = async (values: any) => {
        try {
            await createOrUpdatePolicy(values as AutoScalingPolicy);
            message.success(editingPolicy ? '策略已更新' : '策略已创建');
            setModalOpen(false);
            fetchPolicies();
        } catch (err: any) {
            message.error('保存失败：' + (err?.response?.data?.message || '未知错误'));
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '服务ID', dataIndex: 'service_instance_id', key: 'service_instance_id', width: 100},
        {title: '指标类型', dataIndex: 'metric_type', key: 'metric_type'},
        {title: '阈值', dataIndex: 'target_threshold', key: 'target_threshold'},
        {
            title: '副本范围',
            key: 'replicas',
            render: (_: any, record: AutoScalingPolicy) => `${record.min_replicas} / ${record.max_replicas}`,
        },
        {
            title: '启用',
            dataIndex: 'enabled',
            key: 'enabled',
            render: (enabled: boolean, record: AutoScalingPolicy) => (
                <Popconfirm
                    title={`确认${enabled ? '禁用' : '启用'}此策略？`}
                    onConfirm={() => handleToggleEnabled(record)}
                >
                    <Switch checked={enabled}/>
                </Popconfirm>
            ),
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: AutoScalingPolicy) => (
                <Button icon={<EditOutlined/>} size="small" onClick={() => handleEdit(record)}>
                    编辑
                </Button>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>动态扩缩容策略</Title>
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={fetchPolicies} loading={loading}>
                        刷新
                    </Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>
                        新建策略
                    </Button>
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={policies}
                rowKey="id"
                loading={loading}
                locale={{emptyText: error ? '加载失败' : '暂无策略'}}
            />

            {/* 创建/编辑策略模态框 */}
            <Modal
                title={editingPolicy ? '编辑策略' : '新建策略'}
                open={modalOpen}
                onCancel={() => setModalOpen(false)}
                footer={null}
                destroyOnClose
            >
                <PolicyForm
                    initialValues={editingPolicy}
                    services={services}
                    servicesLoading={servicesLoading}
                    onSubmit={handleSubmitPolicy}
                />
            </Modal>
        </>
    );
}

// 策略表单组件
const PolicyForm = ({
                        initialValues,
                        services,
                        servicesLoading,
                        onSubmit,
                    }: any) => {
    const [form] = Form.useForm();
    const [metricType, setMetricType] = useState(initialValues?.metric_type || 'cpu');

    useEffect(() => {
        if (initialValues) {
            form.setFieldsValue(initialValues);
            setMetricType(initialValues.metric_type);
        } else {
            form.resetFields();
            setMetricType('cpu');
        }
    }, [initialValues, form]);

    const handleFinish = (values: any) => {
        onSubmit(values);
    };

    return (
        <Form
            form={form}
            layout="vertical"
            onFinish={handleFinish}
            initialValues={
                initialValues || {
                    enabled: true,
                    cooldown_seconds: 300,
                    scale_down_delay_minutes: 10,
                    fallback_source: 'prometheus',
                }
            }
        >
            {/* 隐藏的 id 字段，用于更新 */}
            {initialValues?.id && (
                <Form.Item name="id" hidden>
                    <Input/>
                </Form.Item>
            )}

            <Form.Item
                name="service_instance_id"
                label="服务实例"
                rules={[{required: true, message: '请选择服务'}]}
            >
                <Select
                    placeholder="选择服务实例"
                    loading={servicesLoading}
                    showSearch
                    filterOption={(input, option) =>
                        (option?.children as unknown as string)?.toLowerCase().includes(input.toLowerCase())
                    }
                >
                    {services.map((s: ServiceInstance) => (
                        <Option key={s.id} value={s.id}>
                            {s.name} (ID:{s.id})
                        </Option>
                    ))}
                </Select>
            </Form.Item>

            <Form.Item name="metric_type" label="指标类型" rules={[{required: true}]}>
                <Select onChange={(val) => setMetricType(val)}>
                    <Option value="cpu">CPU</Option>
                    <Option value="memory">内存</Option>
                    <Option value="qps">QPS</Option>
                    <Option value="custom">自定义 PromQL</Option>
                </Select>
            </Form.Item>

            {metricType === 'custom' && (
                <Form.Item
                    name="metric_query"
                    label="PromQL 查询"
                    rules={[{required: true, message: '请输入 PromQL'}]}
                >
                    <Input.TextArea rows={2}/>
                </Form.Item>
            )}

            <Form.Item
                name="target_threshold"
                label="目标阈值"
                rules={[{required: true, type: 'number', min: 1}]}
            >
                <InputNumber style={{width: '100%'}}/>
            </Form.Item>

            <Form.Item
                name="min_replicas"
                label="最小副本数"
                rules={[{required: true, type: 'number', min: 0}]}
            >
                <InputNumber style={{width: '100%'}}/>
            </Form.Item>

            <Form.Item
                name="max_replicas"
                label="最大副本数"
                rules={[{required: true, type: 'number', min: 1}]}
            >
                <InputNumber style={{width: '100%'}}/>
            </Form.Item>

            <Form.Item name="cooldown_seconds" label="冷却时间（秒）">
                <InputNumber style={{width: '100%'}} min={0}/>
            </Form.Item>

            <Form.Item name="scale_down_delay_minutes" label="缩容延迟（分钟）">
                <InputNumber style={{width: '100%'}} min={0}/>
            </Form.Item>

            <Form.Item name="fallback_source" label="降级源">
                <Select allowClear>
                    <Option value="prometheus">Prometheus</Option>
                    <Option value="metrics_server">Metrics Server</Option>
                </Select>
            </Form.Item>

            <Form.Item name="enabled" label="启用" valuePropName="checked">
                <Switch/>
            </Form.Item>

            <Form.Item>
                <Button type="primary" htmlType="submit">保存</Button>
            </Form.Item>
        </Form>
    );
};