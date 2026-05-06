import {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {
    Card, Form, Input, Select, InputNumber, Switch, Button, Typography, message, Space, Tooltip, Radio
} from 'antd';
import {QuestionCircleOutlined} from '@ant-design/icons';
import {getServices} from '../../api/services';
import {createBatchTask, BatchConfig, FailureCondition} from '../../api/batch';
import {ServiceInstance} from '../../api/types';
import {addBatchTask} from '../BatchTasks'; // 引入存储函数

const {Title} = Typography;
const {Option} = Select;

export default function BatchTaskCreate() {
    const navigate = useNavigate();
    const [form] = Form.useForm();
    const [services, setServices] = useState<ServiceInstance[]>([]);
    const [loadingServices, setLoadingServices] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [batchType, setBatchType] = useState<'count' | 'percentage'>('count');

    useEffect(() => {
        setLoadingServices(true);
        getServices({page_size: 200})
            .then((res) => setServices(res.records))
            .catch(() => message.error('获取服务列表失败'))
            .finally(() => setLoadingServices(false));
    }, []);

    const onFinish = async (values: any) => {
        const failureCondition: FailureCondition | undefined = values.enable_failure_condition
            ? {
                min_ready_percent: values.min_ready_percent,
                max_error_log_spike: values.max_error_log_spike,
                min_absolute_error_increment: values.min_absolute_error_increment,
            }
            : undefined;

        const batchConfig: BatchConfig = {
            batch_size_type: values.batch_size_type,
            batch_value: values.batch_value,
            interval_seconds: values.interval_seconds || 60,
            require_confirmation: values.require_confirmation,
            statefulset_partition_mode: values.statefulset_partition_mode,
            failure_condition: failureCondition,
        };

        const payload = {
            service_instance_id: values.service_instance_id,
            target_image: values.target_image,
            batch_config: batchConfig,
        };

        setSubmitting(true);
        try {
            const task = await createBatchTask(payload);
            const service = services.find(s => s.id === values.service_instance_id);
            addBatchTask({
                id: task.id,
                serviceName: service?.name,
                targetImage: values.target_image,
                status: task.status,
                createdAt: new Date().toISOString(), // 假设后端不返回创建时间，用当前时间
            });
            message.success('批次发布任务已创建');
            navigate(`/batch/${task.id}`);
        } catch (err: any) {
            message.error(err?.response?.data?.message || '创建失败');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Card title={<Title level={2} style={{margin: 0}}>创建批次发布任务</Title>}>
            <Form
                form={form}
                layout="vertical"
                onFinish={onFinish}
                initialValues={{
                    batch_size_type: 'count',
                    batch_value: 1,
                    interval_seconds: 60,
                    require_confirmation: false,
                    statefulset_partition_mode: false,
                    enable_failure_condition: false,
                    min_ready_percent: 80,
                    max_error_log_spike: 2.0,
                    min_absolute_error_increment: 10,
                }}
            >
                <Form.Item
                    name="service_instance_id"
                    label="目标服务"
                    rules={[{required: true, message: '请选择服务'}]}
                >
                    <Select
                        placeholder="选择服务实例"
                        loading={loadingServices}
                        showSearch
                        filterOption={(input, option) =>
                            (option?.children as unknown as string)?.toLowerCase().includes(input.toLowerCase())
                        }
                    >
                        {services.map((s) => (
                            <Option key={s.id} value={s.id}>
                                {s.name} (ID:{s.id})
                            </Option>
                        ))}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="target_image"
                    label="新镜像地址"
                    rules={[{required: true, message: '请输入镜像地址'}]}
                >
                    <Input placeholder="e.g. registry.example.com/app:v2.0"/>
                </Form.Item>

                <Title level={4}>分批策略</Title>
                <Form.Item name="batch_size_type" label="分批方式">
                    <Radio.Group onChange={(e) => setBatchType(e.target.value)}>
                        <Radio.Button value="count">固定数量</Radio.Button>
                        <Radio.Button value="percentage">百分比</Radio.Button>
                    </Radio.Group>
                </Form.Item>

                <Form.Item
                    name="batch_value"
                    label={batchType === 'count' ? '每批实例数' : '每批百分比'}
                    rules={[{required: true, message: '请输入数值'}]}
                >
                    <InputNumber min={1} max={batchType === 'percentage' ? 100 : undefined} style={{width: '100%'}}/>
                </Form.Item>

                <Form.Item name="interval_seconds" label="批次间隔（秒）" rules={[{required: true}]}>
                    <InputNumber min={0} style={{width: '100%'}}/>
                </Form.Item>

                <Form.Item name="require_confirmation" label="每批人工确认" valuePropName="checked">
                    <Switch/>
                </Form.Item>

                <Form.Item
                    name="statefulset_partition_mode"
                    label={
                        <span>
              StatefulSet 分区模式{' '}
                            <Tooltip title="StatefulSet 将从最大序号开始分批更新，请确认 partition 参数可用。">
                <QuestionCircleOutlined/>
              </Tooltip>
            </span>
                    }
                    valuePropName="checked"
                >
                    <Switch/>
                </Form.Item>

                <Title level={4}>失败回滚条件（可选）</Title>
                <Form.Item name="enable_failure_condition" label="启用失败条件" valuePropName="checked">
                    <Switch/>
                </Form.Item>

                <Form.Item noStyle
                           shouldUpdate={(prev, cur) => prev.enable_failure_condition !== cur.enable_failure_condition}>
                    {({getFieldValue}) =>
                        getFieldValue('enable_failure_condition') ? (
                            <>
                                <Form.Item name="min_ready_percent" label="最小就绪百分比" rules={[{required: true}]}>
                                    <InputNumber min={0} max={100}/>
                                </Form.Item>
                                <Form.Item name="max_error_log_spike" label="错误日志增幅倍数">
                                    <InputNumber min={1} step={0.1}/>
                                </Form.Item>
                                <Form.Item name="min_absolute_error_increment" label="错误日志绝对增量（条/分钟）">
                                    <InputNumber min={0}/>
                                </Form.Item>
                            </>
                        ) : null
                    }
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={submitting}>
                            创建任务
                        </Button>
                        <Button onClick={() => navigate('/batch')}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    );
}