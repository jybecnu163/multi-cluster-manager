import {useState, useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import {Card, Form, Input, Select, InputNumber, Switch, Checkbox, Button, Typography, message, Space} from 'antd';
import {getServices} from '../../api/services';
import {createCanaryTask, CanaryStrategy} from '../../api/canary';
import {ServiceInstance} from '../../api/types';
import {addCanaryTask} from '../CanaryTasks'; // 引入存储函数

const {Title} = Typography;
const {Option} = Select;

export default function CanaryTaskCreate() {
    const navigate = useNavigate();
    const [form] = Form.useForm();
    const [services, setServices] = useState<ServiceInstance[]>([]);
    const [loadingServices, setLoadingServices] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        setLoadingServices(true);
        getServices({page_size: 200})
            .then((res) => setServices(res.records))
            .catch(() => message.error('获取服务列表失败'))
            .finally(() => setLoadingServices(false));
    }, []);

    const onFinish = async (values: any) => {
        const strategy: CanaryStrategy = {
            canary_replicas: values.canary_replicas ?? 1,
            auto_approve_traffic: values.auto_approve_traffic ?? false,
            auto_promote_steps: values.auto_promote_steps ?? [],
            rollback_on_error: values.rollback_on_error ?? true,
        };

        const payload = {
            service_instance_id: values.service_instance_id,
            target_image: values.target_image,
            strategy,
        };

        setSubmitting(true);
        try {
            const task = await createCanaryTask(payload);
            // 写入本地任务列表
            const service = services.find(s => s.id === values.service_instance_id);
            addCanaryTask({
                id: task.id,
                serviceName: service?.name,
                targetImage: values.target_image,
                status: task.status,
                createdAt: task.created_at,
            });
            message.success('灰度任务创建成功');
            navigate(`/canary/${task.id}`);
        } catch (err: any) {
            message.error(err?.response?.data?.message || '创建失败');
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Card title={<Title level={2} style={{margin: 0}}>创建灰度发布任务</Title>}>
            <Form
                form={form}
                layout="vertical"
                onFinish={onFinish}
                initialValues={{
                    canary_replicas: 1,
                    auto_approve_traffic: false,
                    rollback_on_error: true,
                    auto_promote_steps: [],
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

                <Title level={4}>灰度策略</Title>
                <Form.Item label="金丝雀实例数" name="canary_replicas">
                    <InputNumber min={1}/>
                </Form.Item>

                <Form.Item label="自动审批流量接入" name="auto_approve_traffic" valuePropName="checked">
                    <Switch/>
                </Form.Item>

                <Form.Item label="自动扩大阶段" name="auto_promote_steps">
                    <Checkbox.Group
                        options={[
                            {label: '5%', value: 5},
                            {label: '25%', value: 25},
                        ]}
                    />
                </Form.Item>

                <Form.Item label="出错自动回滚" name="rollback_on_error" valuePropName="checked">
                    <Switch/>
                </Form.Item>

                <Form.Item>
                    <Space>
                        <Button type="primary" htmlType="submit" loading={submitting}>
                            创建任务
                        </Button>
                        <Button onClick={() => navigate('/canary')}>取消</Button>
                    </Space>
                </Form.Item>
            </Form>
        </Card>
    );
}