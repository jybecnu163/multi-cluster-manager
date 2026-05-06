import {useState, useEffect} from 'react';
import {useParams, useNavigate} from 'react-router-dom';
import {Card, Button, Descriptions, Tag, Typography, message, Alert, Space} from 'antd';
import {ArrowLeftOutlined, PlayCircleOutlined} from '@ant-design/icons';
import {getPipelines, triggerPipeline, Pipeline} from '../../api/pipelines';

const {Title} = Typography;

export default function PipelineDetail() {
    const {id} = useParams<{ id: string }>();
    const pipelineId = Number(id);
    const navigate = useNavigate();
    const [pipeline, setPipeline] = useState<Pipeline | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (!pipelineId) return;
        setLoading(true);
        // 因无单条详情端点，从列表过滤
        getPipelines()
            .then((data) => {
                const found = data.find((p) => p.id === pipelineId);
                if (found) {
                    setPipeline(found);
                } else {
                    setError('流水线不存在');
                }
            })
            .catch(() => {
                setError('加载失败');
                message.error('加载流水线失败');
            })
            .finally(() => setLoading(false));
    }, [pipelineId]);

    const handleTrigger = async () => {
        try {
            await triggerPipeline(pipelineId);
            message.success('流水线已触发');
        } catch (err: any) {
            message.error(err?.response?.data?.message || '触发失败');
        }
    };

    if (loading) return <Card loading={true}/>;
    if (error) return <Card>{error}</Card>;
    if (!pipeline) return <Card>流水线不存在</Card>;

    return (
        <Card
            title={<Title level={2} style={{margin: 0}}>流水线 #{pipeline.id}</Title>}
            extra={
                <Button icon={<ArrowLeftOutlined/>} onClick={() => navigate('/pipelines')}>
                    返回列表
                </Button>
            }
        >
            <Descriptions column={2} bordered size="small" style={{marginBottom: 24}}>
                <Descriptions.Item label="名称">{pipeline.name}</Descriptions.Item>
                <Descriptions.Item label="触发方式">
                    {pipeline.trigger_type || '手动'}
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">
                    {pipeline.created_at || '-'}
                </Descriptions.Item>
                <Descriptions.Item label="状态">
                    {pipeline.status ? <Tag>{pipeline.status}</Tag> : '-'}
                </Descriptions.Item>
            </Descriptions>

            <Alert
                type="info"
                message="运行详情、步骤状态及日志功能依赖后端提供 GET /pipelines/{id}/runs 等端点，当前版本暂未开放。"
                style={{marginBottom: 16}}
            />

            <Space>
                <Button type="primary" icon={<PlayCircleOutlined/>} onClick={handleTrigger}>
                    触发流水线
                </Button>
            </Space>
        </Card>
    );
}