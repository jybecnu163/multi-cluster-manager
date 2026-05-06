import { useState } from 'react';
import { Table, Button, Space, Typography, Tag } from 'antd';
import { PlusOutlined, ReloadOutlined, EyeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;

interface StoredBatchTask {
  id: number;
  serviceName?: string;
  targetImage?: string;
  status: string;
  createdAt: string;
}

const STORAGE_KEY = 'batch_tasks_list';

const loadTasks = (): StoredBatchTask[] => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
};

const saveTasks = (tasks: StoredBatchTask[]) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks));
};

export const addBatchTask = (task: StoredBatchTask) => {
  const tasks = loadTasks();
  tasks.unshift(task);
  if (tasks.length > 50) tasks.pop();
  saveTasks(tasks);
};

export default function BatchTasks() {
  const navigate = useNavigate();
  const [tasks, setTasks] = useState<StoredBatchTask[]>(loadTasks());

  const refresh = () => setTasks(loadTasks());

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
    { title: '服务', dataIndex: 'serviceName', key: 'serviceName', ellipsis: true },
    { title: '目标镜像', dataIndex: 'targetImage', key: 'targetImage', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', render: (s: string) => <Tag>{s}</Tag> },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (t: string) => new Date(t).toLocaleString(),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: StoredBatchTask) => (
        <Button
          icon={<EyeOutlined />}
          size="small"
          onClick={() => navigate(`/batch/${record.id}`)}
        >
          详情
        </Button>
      ),
    },
  ];

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={2} style={{ margin: 0 }}>批次发布任务</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={refresh}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/batch/create')}>
            创建批次任务
          </Button>
        </Space>
      </div>
      <Table
        columns={columns}
        dataSource={tasks}
        rowKey="id"
        locale={{ emptyText: '暂无批次任务，请点击“创建批次任务”开始' }}
      />
    </>
  );
}