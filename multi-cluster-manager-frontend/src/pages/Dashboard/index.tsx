// src/pages/Dashboard/index.tsx
import {Card, Typography, Row, Col, Statistic, Space, Button} from 'antd';
import {useNavigate} from 'react-router-dom';
import {useAuthStore} from '../../store/useAuthStore';
import {
    TeamOutlined,
    CloudServerOutlined,
    AppstoreOutlined,
    AuditOutlined,
} from '@ant-design/icons';

const {Title, Text} = Typography;

export default function Dashboard() {
    const {user} = useAuthStore();
    const navigate = useNavigate();

    // 基于角色的快捷操作
    const quickActions = [
        {icon: <AppstoreOutlined/>, title: '服务总览', path: '/services', roles: ['*']},
        {icon: <TeamOutlined/>, title: '成员管理', path: '/users', roles: ['系统管理员', '部门主管']},
        {icon: <CloudServerOutlined/>, title: '集群状态', path: '/clusters', roles: ['系统管理员']},
        {icon: <AuditOutlined/>, title: '待我审批', path: '/approvals', roles: ['部门主管']},
    ];

    return (
        <>
            <Title level={2}>仪表盘</Title>
            <Card style={{marginBottom: 24}}>
                <Space direction="vertical">
                    <Text strong>当前登录用户：</Text>
                    <Text>{user?.name} ({user?.email})</Text>
                    <Text>角色：{user?.roles?.join(', ')}</Text>
                </Space>
            </Card>

            <Row gutter={16} style={{marginBottom: 24}}>
                <Col span={6}><Card><Statistic title="健康服务" value={0} suffix="个"/></Card></Col>
                <Col span={6}><Card><Statistic title="在线集群" value={0} suffix="个"/></Card></Col>
                <Col span={6}><Card><Statistic title="待审批任务" value={0}/></Card></Col>
                <Col span={6}><Card><Statistic title="今日告警" value={0}/></Card></Col>
            </Row>

            <Card title="快捷操作">
                <Space wrap>
                    {quickActions
                        .filter(action => action.roles.includes('*') || user?.roles?.some(r => action.roles.includes(r)))
                        .map(action => (
                            <Button
                                key={action.path}
                                type="primary"
                                ghost
                                icon={action.icon}
                                onClick={() => navigate(action.path)}
                            >
                                {action.title}
                            </Button>
                        ))}
                </Space>
            </Card>
        </>
    );
}