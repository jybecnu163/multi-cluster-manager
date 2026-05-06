// src/pages/UserList/index.tsx
import {useEffect, useState} from 'react';
import {Button, Form, Input, message, Modal, Radio, Select, Space, Table, Tag, Typography} from 'antd';
import {PlusOutlined, ReloadOutlined, SafetyCertificateOutlined} from '@ant-design/icons';
import {assignRole, createUser, getUsers} from '../../api/users';
import {getDepartments} from '../../api/departments';
import {Department, Role, User, UserRoleAssignment} from '../../api/types';
import {useAuthStore} from '../../store/useAuthStore';

const {Title} = Typography;
const {Option} = Select;

export default function UserList() {
    const [users, setUsers] = useState<User[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isUserModalOpen, setIsUserModalOpen] = useState(false);
    const [, setIsRoleModalOpen] = useState(false);
    const [editingUser, setEditingUser] = useState<User | null>(null);
    const [allDepartments, setAllDepartments] = useState<Department[]>([]);
    // const [departments, setDepartments] = useState<Department[]>([]);
    const [allRoles] = useState<Role[]>([]); // 实际应从 getRoles() 获取

    const {user: currentUser} = useAuthStore();
    const isAdmin = currentUser?.roles.includes('系统管理员');
    currentUser?.roles.includes('部门主管');

    useEffect(() => {
        // @ts-ignore
        fetchUsers();
        getDepartments()
            .then((data) => setAllDepartments(data as unknown as Department[]))   // 显式断言为 Department[]
            .catch(console.error);
    }, []);
    const fetchUsers = async () => {
        setLoading(true);
        setError(null);
        try {
            // 如果是部门主管，只展示本部门的成员（通过后端参数过滤，或前端过滤）
            const data = await getUsers();
            // 暂时拉取全部，部门主管过滤逻辑可根据实际后端接口调整
            setUsers(data);
        } catch (err: any) {
            setError('获取用户列表失败');
            message.error('获取用户列表失败');
        } finally {
            setLoading(false);
        }
    };

    const handleRoleAssignment = async (userId: number, assignment: UserRoleAssignment) => {
        try {
            await assignRole(userId, assignment);
            message.success('角色分配成功');
            setIsRoleModalOpen(false);
        } catch (err: any) {
            message.error('角色分配失败：' + (err?.response?.data?.message || ''));
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '姓名', dataIndex: 'name', key: 'name'},
        {title: '邮箱', dataIndex: 'email', key: 'email'},
        {
            title: '所属部门',
            dataIndex: 'department_ids',
            key: 'department_ids',
            render: (ids: number[]) => <>{ids?.map(id => <Tag
                key={id}>{allDepartments.find(d => d.id === id)?.name || id}</Tag>)}</>
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: User) => (
                <Space>
                    {isAdmin && (
                        <Button
                            icon={<SafetyCertificateOutlined/>}
                            size="small"
                            onClick={() => setEditingUser(record)}
                            // 弹出角色分配模态框
                        >
                            分配角色
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>成员管理</Title>
                <Space>
                    <Button icon={<ReloadOutlined/>} onClick={fetchUsers} loading={loading}>刷新</Button>
                    {isAdmin && (
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => setIsUserModalOpen(true)}>
                            新增成员
                        </Button>
                    )}
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={users}
                rowKey="id"
                loading={loading}
                locale={{emptyText: error ? '加载失败' : '暂无成员数据'}}
            />

            {/* 新增/编辑用户模态框 */}
            <UserFormModal
                open={isUserModalOpen}
                departments={allDepartments}
                onClose={() => setIsUserModalOpen(false)}
                onSuccess={() => {
                    setIsUserModalOpen(false);
                    fetchUsers();
                }}
            />

            {/* 角色分配模态框 */}
            <RoleAssignmentModal
                open={!!editingUser}
                user={editingUser}
                roles={allRoles}
                onClose={() => setEditingUser(null)}
                onAssign={handleRoleAssignment}
            />
        </>
    );
}

// 用户表单模态框（简化版，实际应包含完整字段）
const UserFormModal = ({open, departments, onClose, onSuccess}: any) => {
    const [form] = Form.useForm();
    const [submitting, setSubmitting] = useState(false);

    const handleSubmit = async (values: any) => {
        setSubmitting(true);
        try {
            await createUser(values);
            message.success('用户创建成功');
            form.resetFields();
            onSuccess();
        } catch (err: any) {
            message.error('创建失败：' + (err?.response?.data?.message || '未知错误'));
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <Modal title="新增成员" open={open} onCancel={onClose} footer={null} destroyOnClose>
            <Form form={form} layout="vertical" onFinish={handleSubmit}>
                <Form.Item name="name" label="姓名" rules={[{required: true}]}><Input/></Form.Item>
                <Form.Item name="email" label="邮箱" rules={[{required: true, type: 'email'}]}><Input/></Form.Item>
                <Form.Item name="password" label="密码" rules={[{required: true, min: 8}]}><Input.Password/></Form.Item>
                <Form.Item name="department_ids" label="所属部门">
                    <Select mode="multiple" placeholder="选择部门">
                        {departments.map((d: Department) => <Option key={d.id} value={d.id}>{d.name}</Option>)}
                    </Select>
                </Form.Item>
                <Form.Item name="primary_department_id" label="主部门">
                    <Select placeholder="选择主部门" allowClear>
                        {departments.map((d: Department) => <Option key={d.id} value={d.id}>{d.name}</Option>)}
                    </Select>
                </Form.Item>
                <Form.Item><Button type="primary" htmlType="submit" loading={submitting}>提交</Button></Form.Item>
            </Form>
        </Modal>
    );
};

// 角色分配模态框
const RoleAssignmentModal = ({open, user, roles, onClose, onAssign}: any) => {
    const [form] = Form.useForm();

    const handleSubmit = (values: any) => {
        if (user) {
            onAssign(user.id, values);
        }
    };

    return (
        <Modal title={`为 ${user?.name} 分配角色`} open={open} onCancel={onClose} footer={null} destroyOnClose>
            <Form form={form} layout="vertical" onFinish={handleSubmit}>
                <Form.Item name="role_id" label="角色" rules={[{required: true}]}>
                    <Select>
                        {roles.map((r: Role) => <Option key={r.id} value={r.id}>{r.name}</Option>)}
                    </Select>
                </Form.Item>
                <Form.Item name="env_type" label="环境" rules={[{required: true}]}>
                    <Radio.Group>
                        <Radio.Button value="all">所有环境</Radio.Button>
                        <Radio.Button value="dev">开发</Radio.Button>
                        <Radio.Button value="test">测试</Radio.Button>
                        <Radio.Button value="prod">生产</Radio.Button>
                    </Radio.Group>
                </Form.Item>
                <Form.Item name="department_id" label="部门（角色作用域）" rules={[{required: true}]}>
                    <Input placeholder="请输入部门ID (int64)" type="number"/>
                </Form.Item>
                <Form.Item>
                    <Button type="primary" htmlType="submit">分配</Button>
                </Form.Item>
            </Form>
        </Modal>
    );
};