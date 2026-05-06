// src/pages/ServiceList/index.tsx
import {useEffect, useState} from 'react';
import {Button, Input, message, Select, Space, Table, Tag, Typography} from 'antd';
import {useNavigate} from 'react-router-dom';
import {ExportOutlined, EyeOutlined, ReloadOutlined, SearchOutlined} from '@ant-design/icons';
import {getServices} from '../../api/services';
import {getDepartments} from '../../api/departments';
import {Department, PaginatedResponse, ServiceInstance} from '../../api/types';
import {useAuthStore} from '../../store/useAuthStore';

const {Title} = Typography;
const {Option} = Select;

function ServiceList() {
    const navigate = useNavigate();
    useAuthStore();
    const [services, setServices] = useState<ServiceInstance[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [pagination, setPagination] = useState({current: 1, pageSize: 20, total: 0});

    // 筛选条件
    const [searchName, setSearchName] = useState('');
    const [envFilter, setEnvFilter] = useState<string | undefined>(undefined);
    const [deptFilter, setDeptFilter] = useState<number | undefined>(undefined);
    const [departments, setDepartments] = useState<Department[]>([]);

    // 获取部门列表用于筛选
    // useEffect(() => {
    //   getDepartments().then(setDepartments).catch(console.error);
    // }, []);
    useEffect(() => {
        // @ts-ignore
        getDepartments()
            .then((data) => setDepartments(data as unknown as Department[]))   // 显式断言为 Department[]
            .catch(console.error);
    }, []);

    const fetchServices = async (page = 1, pageSize = 20) => {
        setLoading(true);
        setError(null);
        try {
            const res: PaginatedResponse<ServiceInstance> = await getServices({
                department_id: deptFilter,
                env_type: envFilter,
                name: searchName || undefined,
                page,
                page_size: pageSize,
            });
            setServices(res.records);
            setPagination({
                current: res.current,
                pageSize: res.size,
                total: res.total,
            });
        } catch (err: any) {
            const msg = '获取服务列表失败';
            setError(msg);
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchServices(1, pagination.pageSize);
    }, [deptFilter, envFilter]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleTableChange = (pagination: any) => {
        fetchServices(pagination.current, pagination.pageSize);
    };

    const columns = [
        {title: 'ID', dataIndex: 'id', key: 'id', width: 80},
        {
            title: '服务名',
            dataIndex: 'name',
            key: 'name',
            render: (text: string, record: ServiceInstance) => (
                <a onClick={() => navigate(`/services/${record.id}`)}>{text}</a>
            ),
        },
        {
            title: '环境',
            dataIndex: 'cluster_id',
            key: 'env',
            render: (_: any, record: ServiceInstance) => {
                // 集群信息可从 departments/clusters 关联，此处简化显示
                return <Tag color="blue">ID:{record.cluster_id}</Tag>;
            },
        },
        {title: '副本数', dataIndex: 'replicas', key: 'replicas'},
        {
            title: 'Nacos 注册',
            dataIndex: 'nacos_health_status',
            key: 'nacos_health',
            render: (status: string) => {
                const color = status === 'registered' ? 'green' : status === 'unregistered' ? 'red' : 'orange';
                const text = status === 'registered' ? '已注册' : status === 'unregistered' ? '未注册' : '未知';
                return <Tag color={color}>{text}</Tag>;
            },
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: ServiceInstance) => (
                <Space>
                    <Button
                        type="primary"
                        icon={<EyeOutlined/>}
                        size="small"
                        onClick={() => navigate(`/services/${record.id}`)}
                    >
                        详情
                    </Button>
                    <Button
                        icon={<ExportOutlined/>}
                        size="small"
                        onClick={() => navigate(`/services/${record.id}/logs`)}
                    >
                        日志
                    </Button>
                </Space>
            ),
        },
    ];

    return (
        <>
            <div style={{display: 'flex', justifyContent: 'space-between', marginBottom: 16}}>
                <Title level={2} style={{margin: 0}}>服务列表</Title>
                <Space wrap>
                    <Input
                        placeholder="搜索服务名"
                        prefix={<SearchOutlined/>}
                        value={searchName}
                        onChange={(e) => setSearchName(e.target.value)}
                        onPressEnter={() => fetchServices(1, pagination.pageSize)}
                        style={{width: 200}}
                        allowClear
                    />
                    <Select
                        placeholder="按环境"
                        allowClear
                        style={{width: 120}}
                        onChange={setEnvFilter}
                    >
                        <Option value="dev">开发</Option>
                        <Option value="test">测试</Option>
                        <Option value="prod">生产</Option>
                    </Select>
                    <Select
                        placeholder="按部门"
                        allowClear
                        style={{width: 180}}
                        onChange={setDeptFilter}
                    >
                        {departments.map((d) => (
                            <Option key={d.id} value={d.id}>
                                {d.name}
                            </Option>
                        ))}
                    </Select>
                    <Button
                        icon={<ReloadOutlined/>}
                        onClick={() => fetchServices(pagination.current, pagination.pageSize)}
                        loading={loading}
                    >
                        刷新
                    </Button>
                </Space>
            </div>

            <Table
                columns={columns}
                dataSource={services}
                rowKey="id"
                loading={loading}
                pagination={pagination}
                onChange={handleTableChange}
                locale={{emptyText: error ? '加载失败' : '暂无服务数据'}}
            />
        </>
    );
}

export default ServiceList