import { Layout, Menu, theme } from 'antd'
import { Outlet, useNavigate } from 'react-router-dom'
import { DashboardOutlined, ApartmentOutlined, TeamOutlined, CloudServerOutlined, AppstoreOutlined, RocketOutlined, UnorderedListOutlined, AuditOutlined, FileTextOutlined } from '@ant-design/icons'
const { Header, Sider, Content } = Layout
const DashboardLayout = () => {
  const navigate = useNavigate()
  const { token } = theme.useToken()
  const menuItems = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
    { key: '/companies', icon: <ApartmentOutlined />, label: '公司管理' },
    { key: '/departments', icon: <TeamOutlined />, label: '部门管理' },
    { key: '/users', icon: <TeamOutlined />, label: '成员管理' },
    { key: '/clusters', icon: <CloudServerOutlined />, label: '集群管理' },
    { key: '/services', icon: <AppstoreOutlined />, label: '服务管理' },
    { key: '/canary', icon: <RocketOutlined />, label: '灰度发布' },
    { key: '/batch', icon: <UnorderedListOutlined />, label: '批次发布' },
    { key: '/autoscaling', icon: <RocketOutlined />, label: '动态扩缩' },
    { key: '/pipelines', icon: <RocketOutlined />, label: '流水线' },
    { key: '/approvals', icon: <AuditOutlined />, label: '待审批' },
    { key: '/audit', icon: <FileTextOutlined />, label: '审计日志' }
  ]
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible theme="light"><div style={{ height: 32, margin: 16, background: token.colorPrimary, borderRadius: 6 }} /><Menu theme="light" mode="inline" items={menuItems} onClick={({ key }) => navigate(key)} /></Sider>
      <Layout><Header style={{ padding: 0, background: token.colorBgContainer }} /><Content style={{ margin: '24px 16px', padding: 24, background: token.colorBgContainer }}><Outlet /></Content></Layout>
    </Layout>
  )
}
export default DashboardLayout