import {useState} from 'react';
import {Table, Button, Space, Typography, Tag} from 'antd';
import {PlusOutlined, ReloadOutlined, EyeOutlined} from '@ant-design/icons';
import {useNavigate} from 'react-router-dom';

const {Title} = Typography;

interface StoredCanaryTask {
    id: number;
    serviceName?: string;
    targetImage?: string;
    status: string;
    createdAt: string;
}

const STORAGE_KEY = 'canary_tasks_list';

// 从 localStorage 读取已保存的任务列表
const loadTasks = (): StoredCanaryTask[] => {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : [];
    } catch {
        return [];
    }
};

const saveTasks = (tasks: StoredCanaryTask[]) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks));
};

// 供外部调用的存储函数（创建页会使用）
export const addCanaryTask = (task: StoredCanaryTask) => {
    const tasks = loadTasks();
    tasks.unshift(task); // 最新在前
    // 最多保留 50 条
    if (tasks.length > 50) tasks.pop();
    saveTasks(tasks);
};

export default function CanaryTasks() {
    const navigate = useNavigate();
    const [tasks, setTasks] = useState<StoredCanaryTask[]>(loadTasks());

    const refresh = () => setTasks(loadTasks());

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '服务', dataIndex: 'serviceName', key: 'serviceName', ellipsis: true},
        {title: '目标镜像', dataIndex: 'targetImage', key: 'targetImage', ellipsis: true},
        {title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag>{s}</Tag>},
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
            render: (t: string) => new Date(t).toLocaleString(),
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: StoredCanaryTask) => (
                <Button
                    icon={<EyeOutlined/>}
                    size="small"
                    onClick={() => navigate(`/canary/${record.id}`)}
                >
                    详情
                </Button>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>灰度发布任务</Title>
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={refresh}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => navigate('/canary/create')}>
                        创建灰度任务
                    </Button>
                </Space>
            </div>
            <Table
                columns={columns}
                dataSource={tasks}
                rowKey="id"
                locale={{emptyText: '暂无灰度任务，请点击“创建灰度任务”开始'}}
            />
        </>
    );
}