// src/pages/CompanyList/index.tsx
import {useState, useEffect} from 'react';
import {Table, Button, Space, message, Popconfirm, Typography} from 'antd';
import {PlusOutlined, DeleteOutlined, ReloadOutlined} from '@ant-design/icons';
import {getCompanies, createCompany, deleteCompany} from '../../api/companies';
import {Company} from '../../api/types';
// import CompanyFormModal from './CompanyFormModal'; // 一个简单的模态框用于新增
import dayjs from 'dayjs';

const {Title} = Typography;

export default function CompanyList() {
    const [companies, setCompanies] = useState<Company[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    const fetchCompanies = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getCompanies();
            setCompanies(data);
        } catch (err: any) {
            const msg = err?.response?.data?.message || '获取公司列表失败';
            setError(msg);
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCompanies();
    }, []);

    const handleDelete = async (id: number) => {
        try {
            await deleteCompany(id);
            message.success('公司已删除');
            fetchCompanies();
        } catch (err: any) {
            if (err?.response?.status === 409) {
                message.error('删除失败：该公司下存在依赖资源（部门、成员或服务），请先清理。');
            } else {
                message.error('删除失败：' + (err?.response?.data?.message || '未知错误'));
            }
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '公司名称', dataIndex: 'name', key: 'name'},
        {
            title: '创建时间',
            dataIndex: 'created_at',
            key: 'created_at',
            render: (text: string) => dayjs(text).format('YYYY-MM-DD HH:mm:ss')
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: Company) => (
                <Popconfirm
                    title="确定要删除这个公司吗？"
                    description="如果公司下还有部门或成员，删除将被阻止。"
                    onConfirm={() => handleDelete(record.id)}
                    okText="确定删除"
                    cancelText="取消"
                >
                    <Button type="primary" danger icon={<DeleteOutlined/>} size="small">
                        删除
                    </Button>
                </Popconfirm>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>公司管理</Title>
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={fetchCompanies} loading={loading}>刷新</Button>
                    <Button type="primary" icon={<PlusOutlined/>} onClick={() => setIsModalOpen(true)}>
                        新增公司
                    </Button>
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={companies}
                rowKey="id"
                loading={loading}
                locale={{emptyText: error ? '加载失败，请重试' : '暂无公司数据'}}
            />

            <CompanyFormModal
                open={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSuccess={() => {
                    setIsModalOpen(false);
                    fetchCompanies();
                }}
            />
        </>
    );
}

// 内联一个简单的公司创建模态框组件（符合实现复杂度）
import {Modal, Form, Input} from 'antd';

const CompanyFormModal = ({open, onClose, onSuccess}: {
    open: boolean;
    onClose: () => void;
    onSuccess: () => void
}) => {
    const [form] = Form.useForm();
    const [creating, setCreating] = useState(false);

    const handleSubmit = async (values: { name: string }) => {
        setCreating(true);
        try {
            await createCompany(values);
            message.success('公司创建成功');
            form.resetFields();
            onSuccess();
        } catch (err: any) {
            message.error('创建失败：' + (err?.response?.data?.message || '未知错误'));
        } finally {
            setCreating(false);
        }
    };

    return (
        <Modal title="新增公司" open={open} onCancel={onClose} footer={null} destroyOnClose>
            <Form form={form} layout="vertical" onFinish={handleSubmit}>
                <Form.Item name="name" label="公司名称" rules={[{required: true, message: '请输入公司名称'}]}>
                    <Input/>
                </Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit" loading={creating}>
                        提交
                    </Button>
                </Form.Item>
            </Form>
        </Modal>
    );
};