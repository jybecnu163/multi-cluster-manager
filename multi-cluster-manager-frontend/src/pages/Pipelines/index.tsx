import {useState, useEffect} from 'react';
import {
    Table, Button, Space, Typography, message, Modal, Form, Input, Tag
} from 'antd';
import {PlusOutlined, ReloadOutlined, PlayCircleOutlined} from '@ant-design/icons';
import {useNavigate} from 'react-router-dom';
import {getPipelines, createPipeline, triggerPipeline, Pipeline} from '../../api/pipelines';

const {Title} = Typography;

export default function PipelineList() {
    const navigate = useNavigate();
    const [pipelines, setPipelines] = useState<Pipeline[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [createModalOpen, setCreateModalOpen] = useState(false);
    const [triggerLoading, setTriggerLoading] = useState<number | null>(null);
    const [form] = Form.useForm();

    const fetchPipelines = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getPipelines();
            setPipelines(data);
        } catch (err) {
            setError('获取流水线列表失败');
            message.error('获取流水线列表失败');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchPipelines();
    }, []);

    const handleCreate = async (values: { name: string; steps_json: string }) => {
        try {
            let steps = [];
            try {
                steps = JSON.parse(values.steps_json || '[]');
            } catch {
                message.error('步骤 JSON 格式错误');
                return;
            }
            await createPipeline({name: values.name, steps});
            message.success('流水线创建成功');
            setCreateModalOpen(false);
            form.resetFields();
            fetchPipelines();
        } catch (err: any) {
            message.error(err?.response?.data?.message || '创建失败');
        }
    };

    const handleTrigger = async (pipelineId: number) => {
        setTriggerLoading(pipelineId);
        try {
            await triggerPipeline(pipelineId);
            message.success('流水线已触发');
            // 触发后可能产生审批、运行，详情需进入详情页查看
        } catch (err: any) {
            message.error(err?.response?.data?.message || '触发失败');
        } finally {
            setTriggerLoading(null);
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '名称', dataIndex: 'name', key: 'name'},
        {
            title: '触发方式',
            dataIndex: 'trigger_type',
            key: 'trigger_type',
            render: (t: string) => <Tag>{t || '手动'}</Tag>,
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: Pipeline) => (
                <Space>
                    <Button
                        icon={<PlayCircleOutlined/>}
                        size="small"
                        loading={triggerLoading === record.id}
                        onClick={() => handleTrigger(record.id)}
                    >
                        触发
                    </Button>
                    <Button
                        size="small"
                        onClick={() => navigate(`/pipelines/${record.id}`)}
                    >
                        详情
                    </Button>
                </Space>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>CI/CD 流水线</Title>
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={fetchPipelines} loading={loading}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>}
                            onClick={() => setCreateModalOpen(true)}>创建流水线</Button>
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={pipelines}
                rowKey="id"
                loading={loading}
                locale={{emptyText: error ? '加载失败' : '暂无流水线'}}
            />

            <Modal
                title="创建流水线"
                open={createModalOpen}
                onCancel={() => setCreateModalOpen(false)}
                footer={null}
                destroyOnClose
            >
                <Form form={form} layout="vertical" onFinish={handleCreate}>
                    <Form.Item name="name" label="流水线名称" rules={[{required: true}]}>
                        <Input/>
                    </Form.Item>
                    <Form.Item
                        name="steps_json"
                        label="步骤 (JSON)"
                        rules={[{required: true}]}
                        extra='输入步骤数组，如 [{"type":"build","command":"mvn clean package"}]'
                    >
                        <Input.TextArea rows={6}/>
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit">创建</Button>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
}