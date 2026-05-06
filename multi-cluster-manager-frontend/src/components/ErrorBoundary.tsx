import React from 'react';
import {Button, Result} from 'antd'

export class ErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean }> {
    constructor(props: any) {
        super(props);
        this.state = {hasError: false}
    }

    static getDerivedStateFromError() {
        return {hasError: true}
    }

    render() {
        if (this.state.hasError) return <Result status="error" title="页面出错了" extra={<Button
            onClick={() => window.location.reload()}>刷新</Button>}/>;
        return this.props.children
    }
}