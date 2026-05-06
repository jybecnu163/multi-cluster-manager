// src/pages/DepartmentList/index.tsx
import {useEffect, useState} from 'react';
import {Button, Form, Input, message, Modal, Select, Space, Switch, Table, Tooltip, Typography} from 'antd';
import {PlusOutlined, ReloadOutlined, SettingOutlined} from '@ant-design/icons';
import {
    createDepartment,
    getCompanies,
    getDepartments,
    getDepartmentSettings,
    updateDepartmentSettings
} from '../../api/departments';
import {Company, Department} from '../../api/types';
import {useAuthStore} from '../../store/useAuthStore';

const {Title} = Typography;
const {Option} = Select;

export default function DepartmentList() {
    // 定义扩展类型，包含 Department 和 DepartmentSettings 的可选字段
    interface DepartmentWithSettings extends Department {
        allow_ops_bypass_prod_scale?: boolean;
    }

    // 状态定义改为扩展类型
    const [settingsModal, setSettingsModal] = useState<{
        open: boolean;
        dept: DepartmentWithSettings | null;
    }>({open: false, dept: null});

    // 打开设置弹窗时合并数据
    const openSettings = async (dept: Department) => {
        try {
            const settings = await getDepartmentSettings(dept.id);
            // 合并为 DepartmentWithSettings
            const merged = {...dept, ...settings} as DepartmentWithSettings;
            setSettingsModal({open: true, dept: merged});
        } catch {
            message.error('无法加载部门设置，请确认接口是否就绪。');
            // 即使获取失败也打开窗口（设置字段为 undefined）
            setSettingsModal({open: true, dept: {...dept}});
        }
    };

    const [companies, setCompanies] = useState<Company[]>([]);
    const [departments, setDepartments] = useState<Department[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [selectedCompany, setSelectedCompany] = useState<number | undefined>();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const {user} = useAuthStore();

    const isAdmin = user?.roles.includes('系统管理员');

    useEffect(() => {
        // @ts-ignore
        getCompanies().then(setCompanies).catch(e => console.error(e));
    }, []);

    const fetchDepartments = async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getDepartments(selectedCompany);
            // @ts-ignore
            setDepartments(data);
        } catch (err: any) {
            const msg = '获取部门列表失败';
            setError(msg);
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDepartments();
    }, [selectedCompany]);

    const handleCompanyChange = (value: number | undefined) => {
        setSelectedCompany(value);
    };


// 开关处理（移除 @ts-ignore）
    const handleSettingToggle = async (deptId: number, checked: boolean) => {
        try {
            await updateDepartmentSettings(deptId, {allow_ops_bypass_prod_scale: checked});
            message.success('部门设置已更新');
            // 同步更新本地状态，让开关立即反映
            setSettingsModal((prev) => ({
                ...prev,
                dept: prev.dept ? {...prev.dept, allow_ops_bypass_prod_scale: checked} : prev.dept,
            }));
        } catch (err: any) {
            message.error('更新设置失败');
        }
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {title: '部门名称', dataIndex: 'name', key: 'name'},
        {title: '所属公司', dataIndex: 'company_id', key: 'company_id'}, // 实际应展示公司名，此处简化
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: Department) => (
                <Space>
                    {isAdmin && (
                        <Tooltip title="部门设置">
                            <Button icon={<SettingOutlined/>} size="small" onClick={() => openSettings(record)}>
                                设置
                            </Button>
                        </Tooltip>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>部门管理</Title>
                <Space>
                    <Select
                        placeholder="按公司筛选"
                        style={{width: 200}}
                        allowClear
                        onChange={handleCompanyChange}
                    >
                        {companies.map(c => <Option key={c.id} value={c.id}>{c.name}</Option>)}
                    </Select>
                    <Button icon={<ReloadOutlined/>} onClick={fetchDepartments} loading={loading}>刷新</Button>
                    {isAdmin && (
                        <Button type="primary" icon={<PlusOutlined/>} onClick={() => setIsModalOpen(true)}>
                            新增部门
                        </Button>
                    )}
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={departments}
                rowKey="id"
                loading={loading}
                locale={{emptyText: error ? '加载失败' : '暂无部门数据'}}
            />

            <DepartmentFormModal
                open={isModalOpen}
                companies={companies}
                onClose={() => setIsModalOpen(false)}
                onSuccess={() => {
                    setIsModalOpen(false);
                    fetchDepartments();
                }}
            />

            <Modal
                title={`部门设置 - ${settingsModal.dept?.name}`}
                open={settingsModal.open}
                onCancel={() => setSettingsModal({open: false, dept: null})}
                footer={null}
            >
                {settingsModal.dept && (
                    <div>
                        <p>允许运维工程师在生产环境跳过审批手动扩缩容：</p>
                        // Switch 部分（使用 ?? false 避免 undefined）
                        <Switch
                            checked={settingsModal.dept?.allow_ops_bypass_prod_scale ?? false}
                            onChange={(checked) => handleSettingToggle(settingsModal.dept!.id, checked)}
                        />
                    </div>
                )}
            </Modal>
        </>
    );
}

// 内联创建部门模态框组件
const DepartmentFormModal = ({open, companies, onClose, onSuccess}: any) => {
    const [form] = Form.useForm();
    const [creating, setCreating] = useState(false);
    const {Item} = Form;

    const handleSubmit = async (values: any) => {
        setCreating(true);
        try {
            await createDepartment(values);
            message.success('部门创建成功');
            form.resetFields();
            onSuccess();
        } catch (err: any) {
            message.error('创建失败');
        } finally {
            setCreating(false);
        }
    };

    return (
        <Modal title="新增部门" open={open} onCancel={onClose} footer={null} destroyOnClose>
            <Form form={form} layout="vertical" onFinish={handleSubmit}>
                <Item name="company_id" label="所属公司" rules={[{required: true}]}>
                    <Select>{companies.map((c: Company) => <Option key={c.id} value={c.id}>{c.name}</Option>)}</Select>
                </Item>
                <Item name="name" label="部门名称" rules={[{required: true}]}>
                    <Input/>
                </Item>
                <Item name="director_user_id" label="主管用户ID（可选）">
                    <Input type="number"/>
                </Item>
                <Item><Button type="primary" htmlType="submit" loading={creating}>提交</Button></Item>
            </Form>
        </Modal>
    );
};