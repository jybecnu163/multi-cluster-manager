import {Layout, Menu, theme} from 'antd'
import {Outlet, useNavigate, useLocation} from 'react-router-dom';
import {
    DashboardOutlined, ApartmentOutlined, TeamOutlined, CloudServerOutlined,
    AppstoreOutlined, RocketOutlined, UnorderedListOutlined, AuditOutlined,
    FileTextOutlined, LoginOutlined
} from '@ant-design/icons';
import {useAuthStore} from '../store/useAuthStore';
import {Button} from 'antd';

const {Header, Sider, Content} = Layout
const DashboardLayout = () => {
    const navigate = useNavigate()
    const location = useLocation();
    const {token} = theme.useToken()
    const {user, logout} = useAuthStore();

// 基础菜单项，每个项包含所需角色
    const allMenuItems = [
        {key: '/dashboard', icon: <DashboardOutlined/>, label: '仪表盘', roles: ['*']},
        {key: '/companies', icon: <ApartmentOutlined/>, label: '公司管理', roles: ['系统管理员']},
        {key: '/departments', icon: <TeamOutlined/>, label: '部门管理', roles: ['系统管理员', '部门主管']},
        {key: '/users', icon: <TeamOutlined/>, label: '成员管理', roles: ['系统管理员', '部门主管']},
        {key: '/clusters', icon: <CloudServerOutlined/>, label: '集群管理', roles: ['系统管理员']},
        {key: '/services', icon: <AppstoreOutlined/>, label: '服务管理', roles: ['*']},
        {
            key: '/canary',
            icon: <RocketOutlined/>,
            label: '灰度发布',
            roles: ['系统管理员', '运维工程师', '开发工程师', '部门主管']
        },
        {
            key: '/batch',
            icon: <UnorderedListOutlined/>,
            label: '批次发布',
            roles: ['系统管理员', '运维工程师', '开发工程师', '部门主管']
        },
        {
            key: '/autoscaling',
            icon: <RocketOutlined/>,
            label: '动态扩缩',
            roles: ['系统管理员', '运维工程师', '开发工程师', '部门主管']
        },
        {
            key: '/pipelines',
            icon: <RocketOutlined/>,
            label: '流水线',
            roles: ['系统管理员', '运维工程师', '开发工程师']
        },
        {key: '/approvals', icon: <AuditOutlined/>, label: '待审批', roles: ['系统管理员', '部门主管']},
        {key: '/audit', icon: <FileTextOutlined/>, label: '审计日志', roles: ['系统管理员', '审计员']},
    ];


    // 根据用户角色过滤可见菜单
    const visibleMenuItems = allMenuItems.filter(item =>
        item.roles.includes('*') || (user && item.roles.some(role => user.roles.includes(role)))
    );

    const handleLogout = async () => {
        try {
            // 调用注销接口，告知后端使 token 失效
            const {logout: apiLogout} = await import('../api/auth');
            await apiLogout();
        } finally {
            logout();
            navigate('/login');
        }
    };

    return (
        <Layout style={{minHeight: '100vh'}}>
            <Sider collapsible theme="light">
                <div style={{
                    height: 32,
                    margin: 16,
                    background: token.colorPrimary,
                    borderRadius: 6,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white'
                }}>
                    K8s Platform
                </div>
                <Menu
                    theme="light"
                    mode="inline"
                    selectedKeys={[location.pathname]}
                    items={visibleMenuItems}
                    onClick={({key}) => navigate(key)}
                />
            </Sider>
            <Layout>
                <Header style={{
                    padding: '0 24px',
                    background: token.colorBgContainer,
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignItems: 'center'
                }}>
                    <span style={{marginRight: 16}}>欢迎，{user?.name || '用户'}</span>
                    <Button icon={<LoginOutlined/>} onClick={handleLogout}>注销</Button>
                </Header>
                <Content style={{margin: '24px 16px', padding: 24, background: token.colorBgContainer}}>
                    <Outlet/>
                </Content>
            </Layout>
        </Layout>
    );
};

export default DashboardLayout;