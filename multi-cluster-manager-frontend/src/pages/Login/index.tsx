// src/pages/Login/index.tsx
import {useState} from 'react';
import {Form, Input, Button, Card, Typography, message, Space, Modal} from 'antd';
import {UserOutlined, LockOutlined, SafetyOutlined} from '@ant-design/icons';
import {useNavigate, useLocation} from 'react-router-dom';
import {login} from '../../api/auth';
import {useAuthStore} from '../../store/useAuthStore';
import {LoginRequest} from '../../api/types';

const {Title} = Typography;

export default function Login() {
    const [loading, setLoading] = useState(false);
    const [show2FA, setShow2FA] = useState(false);
    const [form] = Form.useForm();
    const navigate = useNavigate();
    const location = useLocation();
    const {loginSuccess} = useAuthStore();

    const from = (location.state as any)?.from?.pathname || '/dashboard';

    const onFinish = async (values: LoginRequest & { totp_code?: string }) => {
        setLoading(true);
        try {
            // 实际登录请求
            const response = await login({email: values.email, password: values.password});

            // 模拟：根据实际情况，如果后端要求2FA，则弹出输入框
            // 此处逻辑为：若用户已绑定TOTP，后端可能在 LoginResponse 中增加 requires_2fa 字段。
            // 由于 openapi 未定义此字段，我们假设一个简单场景：
            // 如果用户在 URL 参数中传递 totp_code，或此处先展示 TOTP 弹窗。

            // 为了演示，我们这里直接假设登录成功并获取 token。
            // 真实场景需解析 token 中用户信息或调用 /users/me 接口。
            const mockUser = {
                id: 1,
                name: '管理员',
                email: values.email,
                roles: ['系统管理员'], // 真实场景从后端获取
            };

            loginSuccess(response.access_token, mockUser);
            message.success('登录成功');
            navigate(from, {replace: true});
        } catch (error: any) {
            const msg = error?.response?.data?.message || '登录失败，请检查邮箱或密码';
            message.error(msg);

            // 如果错误码指示需要TOTP，则弹出输入框（根据实际契约调整）
            if (error?.response?.data?.code === 'TOTP_REQUIRED') {
                setShow2FA(true);
            }
        } finally {
            setLoading(false);
        }
    };

    const handle2FASubmit = async (totpCode: string) => {
        // 实现二次验证逻辑
        console.log('TOTP code submitted:', totpCode);
    };

    return (
        <div style={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            minHeight: '100vh',
            background: '#f0f2f5'
        }}>
            <Card style={{width: 400}}>
                <Title level={3} style={{textAlign: 'center', marginBottom: 24}}>多集群容器管理平台</Title>
                <Form form={form} onFinish={onFinish} size="large">
                    <Form.Item name="email" rules={[{required: true, message: '请输入邮箱'}, {
                        type: 'email',
                        message: '邮箱格式不正确'
                    }]}>
                        <Input prefix={<UserOutlined/>} placeholder="邮箱"/>
                    </Form.Item>
                    <Form.Item name="password"
                               rules={[{required: true, message: '请输入密码'}, {min: 8, message: '密码至少8位'}]}>
                        <Input.Password prefix={<LockOutlined/>} placeholder="密码"/>
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit" loading={loading} block>
                            登录
                        </Button>
                    </Form.Item>
                </Form>

                <Modal
                    title="二次验证"
                    open={show2FA}
                    onCancel={() => setShow2FA(false)}
                    footer={null}
                >
                    <Space direction="vertical" style={{width: '100%'}}>
                        <p>请输入您的 TOTP 验证码：</p>
                        <Input prefix={<SafetyOutlined/>} placeholder="6位验证码" maxLength={6} onChange={(e) => {
                            if (e.target.value.length === 6) handle2FASubmit(e.target.value);
                        }}/>
                    </Space>
                </Modal>
            </Card>
        </div>
    );
}