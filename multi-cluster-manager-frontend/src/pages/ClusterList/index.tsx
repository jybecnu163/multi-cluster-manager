import {useState, useEffect} from 'react';
import {
    Table, Button, Space, Tag, Typography, message, Modal, Form, Input, Select, Upload, Tooltip, Popconfirm
} from 'antd';
import {
    PlusOutlined, ReloadOutlined, HeartOutlined, EditOutlined, DeleteOutlined, UploadOutlined,
    ExclamationCircleOutlined
} from '@ant-design/icons';
import {
    getClusters, registerCluster, getClusterHealth, updateCluster, deleteCluster,
    Cluster, ClusterRegisterRequest, ClusterUpdateRequest
} from '../../api/clusters';
import {useAuthStore} from '../../store/useAuthStore';
import dayjs from 'dayjs';

const {Title, Text} = Typography;
const {Option} = Select;

export default function ClusterList() {
    const {user} = useAuthStore();
    const isAdmin = user?.roles?.includes('系统管理员');

    const [clusters, setClusters] = useState<Cluster[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [registerModalOpen, setRegisterModalOpen] = useState(false);
    const [editModalOpen, setEditModalOpen] = useState(false);
    const [editingCluster, setEditingCluster] = useState<Cluster | null>(null);
    const [healthLoading, setHealthLoading] = useState<number | null>(null);

    const [registerForm] = Form.useForm();
    const [editForm] = Form.useForm();

    // 获取集群列表
    const fetchClusters = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getClusters();
            setClusters(data);
        } catch (err: any) {
            const msg = '获取集群列表失败';
            setError(msg);
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchClusters();
    }, []);

    // 健康检查
    const handleHealthCheck = async (clusterId: number) => {
        setHealthLoading(clusterId);
        try {
            const res = await getClusterHealth(clusterId);
            // 更新本地状态
            setClusters(prev =>
                prev.map(c =>
                    c.id === clusterId
                        ? {...c, status: res.status, last_heartbeat: new Date().toISOString()}
                        : c
                )
            );
            message.success(`集群健康状态：${res.status}`);
        } catch (err: any) {
            message.error('健康检查失败，集群可能不可达');
        } finally {
            setHealthLoading(null);
        }
    };

    // 打开注册弹窗
    const openRegisterModal = () => {
        registerForm.resetFields();
        setRegisterModalOpen(true);
    };

    // 提交注册
    const handleRegister = async () => {
        try {
            const values = await registerForm.validateFields();
            let kubeconfigBase64 = '';
            if (values.kubeconfig_file && values.kubeconfig_file.fileList?.[0]?.originFileObj) {
                const file = values.kubeconfig_file.fileList[0].originFileObj;
                const text = await file.text();
                kubeconfigBase64 = btoa(text);
            } else if (values.kubeconfig_text) {
                kubeconfigBase64 = btoa(values.kubeconfig_text);
            } else {
                message.error('请提供 kubeconfig（上传文件或粘贴内容）');
                return;
            }

            const payload: ClusterRegisterRequest = {
                name: values.name,
                env_type: values.env_type,
                api_endpoint: values.api_endpoint,
                kubeconfig: kubeconfigBase64,
            };

            await registerCluster(payload);
            message.success('集群注册成功');
            setRegisterModalOpen(false);
            fetchClusters();
        } catch (err: any) {
            if (err?.errorFields) return; // 表单验证错误
            message.error('注册失败：' + (err?.response?.data?.message || '未知错误'));
        }
    };

    // 打开编辑弹窗
    const openEditModal = (cluster: Cluster) => {
        setEditingCluster(cluster);
        editForm.setFieldsValue({
            name: cluster.name,
            env_type: cluster.env_type,
            api_endpoint: cluster.api_endpoint,
            kubeconfig_text: '', // 出于安全，不回显原配置
        });
        setEditModalOpen(true);
    };

    // 提交编辑
    const handleEdit = async () => {
        if (!editingCluster) return;
        try {
            const values = await editForm.validateFields();
            let kubeconfigBase64 = undefined;
            if (values.kubeconfig_file && values.kubeconfig_file.fileList?.[0]?.originFileObj) {
                const file = values.kubeconfig_file.fileList[0].originFileObj;
                const text = await file.text();
                kubeconfigBase64 = btoa(text);
            } else if (values.kubeconfig_text_edit) {
                kubeconfigBase64 = btoa(values.kubeconfig_text_edit);
            }

            const payload: ClusterUpdateRequest = {
                name: values.name,
                env_type: values.env_type,
                api_endpoint: values.api_endpoint,
            };
            if (kubeconfigBase64) {
                payload.kubeconfig = kubeconfigBase64;
            }

            await updateCluster(editingCluster.id, payload);
            message.success('集群信息已更新');
            setEditModalOpen(false);
            fetchClusters();
        } catch (err: any) {
            if (err?.errorFields) return;
            message.error('更新失败：' + (err?.response?.data?.message || '未知错误'));
        }
    };

    // 删除集群
    const handleDelete = async (clusterId: number) => {
        try {
            await deleteCluster(clusterId);
            message.success('集群已删除');
            fetchClusters();
        } catch (err: any) {
            message.error('删除失败：' + (err?.response?.data?.message || '未知错误'));
        }
    };

    // 判断集群是否失联
    const isClusterOffline = (status: string) => status === 'offline' || status === 'error';

    const columns = [
        {
            title: 'ID',
            dataIndex: 'id',
            key: 'id',
            width: 80,
        },
        {
            title: '名称',
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: '环境',
            dataIndex: 'env_type',
            key: 'env_type',
            render: (env: string) => {
                const color = env === 'prod' ? 'red' : env === 'test' ? 'orange' : 'blue';
                return <Tag color={color}>{env}</Tag>;
            },
        },
        {
            title: 'API Endpoint',
            dataIndex: 'api_endpoint',
            key: 'api_endpoint',
            ellipsis: true,
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                const color = status === 'online' ? 'green' : status === 'offline' ? 'red' : 'orange';
                return <Tag color={color}>{status}</Tag>;
            },
        },
        {
            title: '最后心跳',
            dataIndex: 'last_heartbeat',
            key: 'last_heartbeat',
            render: (text: string) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-',
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: Cluster) => {
                const offline = isClusterOffline(record.status);
                return (
                    <Space>
                        <Tooltip title="健康检查">
                            <Button
                                icon={<HeartOutlined/>}
                                size="small"
                                loading={healthLoading === record.id}
                                onClick={() => handleHealthCheck(record.id)}
                            />
                        </Tooltip>

                        {isAdmin && (
                            <>
                                <Tooltip title={offline ? '集群离线，无法编辑' : '编辑'}>
                                    <Button
                                        icon={<EditOutlined/>}
                                        size="small"
                                        disabled={offline}
                                        onClick={() => openEditModal(record)}
                                    />
                                </Tooltip>

                                <Tooltip title={offline ? '集群离线，可强制删除' : '删除'}>
                                    <Popconfirm
                                        title="确定要删除该集群吗？"
                                        onConfirm={() => handleDelete(record.id)}
                                        okText="确认"
                                        cancelText="取消"
                                        icon={<ExclamationCircleOutlined style={{color: 'red'}}/>}
                                    >
                                        <Button
                                            icon={<DeleteOutlined/>}
                                            size="small"
                                            danger
                                            disabled={offline ? false : false} // 允许离线时删除，但不置灰，仅提示
                                        />
                                    </Popconfirm>
                                </Tooltip>
                            </>
                        )}

                        {!isAdmin && (
                            <Text type="secondary" style={{fontSize: 12}}>无操作权限</Text>
                        )}
                    </Space>
                );
            },
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>集群管理</Title>
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={fetchClusters} loading={loading}>
                        刷新
                    </Button>
                    {isAdmin && (
                        <Button type="primary" icon={<PlusOutlined/>} onClick={openRegisterModal}>
                            注册集群
                        </Button>
                    )}
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={clusters}
                rowKey="id"
                loading={loading}
                locale={{emptyText: error ? '加载失败' : '暂无集群数据'}}
            />

            {/* 注册集群模态框 */}
            <Modal
                title="注册新集群"
                open={registerModalOpen}
                onCancel={() => setRegisterModalOpen(false)}
                onOk={handleRegister}
                confirmLoading={false}
                destroyOnClose
            >
                <Form form={registerForm} layout="vertical" preserve={false}>
                    <Form.Item
                        name="name"
                        label="集群名称"
                        rules={[{required: true, message: '请输入集群名称'}]}
                    >
                        <Input/>
                    </Form.Item>
                    <Form.Item
                        name="env_type"
                        label="环境类型"
                        rules={[{required: true, message: '请选择环境类型'}]}
                    >
                        <Select>
                            <Option value="dev">开发</Option>
                            <Option value="test">测试</Option>
                            <Option value="prod">生产</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="api_endpoint"
                        label="API Server 地址"
                        rules={[{required: true, type: 'url', message: '请输入有效的 URL'}]}
                    >
                        <Input placeholder="https://cluster-api.example.com:6443"/>
                    </Form.Item>
                    <Form.Item label="kubeconfig 文件" name="kubeconfig_file" valuePropName="file">
                        <Upload beforeUpload={() => false} maxCount={1} accept=".yaml,.yml,.conf,text/plain">
                            <Button icon={<UploadOutlined/>}>上传文件</Button>
                        </Upload>
                    </Form.Item>
                    <Form.Item label="或直接粘贴内容" name="kubeconfig_text">
                        <Input.TextArea rows={6} placeholder="粘贴 kubeconfig 内容..."/>
                    </Form.Item>
                </Form>
            </Modal>

            {/* 编辑集群模态框 */}
            <Modal
                title="编辑集群"
                open={editModalOpen}
                onCancel={() => setEditModalOpen(false)}
                onOk={handleEdit}
                destroyOnClose
            >
                <Form form={editForm} layout="vertical" preserve={false}>
                    <Form.Item
                        name="name"
                        label="集群名称"
                        rules={[{required: true, message: '请输入集群名称'}]}
                    >
                        <Input/>
                    </Form.Item>
                    <Form.Item
                        name="env_type"
                        label="环境类型"
                        rules={[{required: true, message: '请选择环境类型'}]}
                    >
                        <Select>
                            <Option value="dev">开发</Option>
                            <Option value="test">测试</Option>
                            <Option value="prod">生产</Option>
                        </Select>
                    </Form.Item>
                    <Form.Item
                        name="api_endpoint"
                        label="API Server 地址"
                        rules={[{required: true, type: 'url', message: '请输入有效的 URL'}]}
                    >
                        <Input placeholder="https://cluster-api.example.com:6443"/>
                    </Form.Item>
                    <Form.Item label="kubeconfig 文件（可选，不选则不修改）" name="kubeconfig_file" valuePropName="file">
                        <Upload beforeUpload={() => false} maxCount={1} accept=".yaml,.yml,.conf,text/plain">
                            <Button icon={<UploadOutlined/>}>上传文件</Button>
                        </Upload>
                    </Form.Item>
                    <Form.Item label="或粘贴新内容（可选）" name="kubeconfig_text_edit">
                        <Input.TextArea rows={6} placeholder="留空则不修改 kubeconfig"/>
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
}