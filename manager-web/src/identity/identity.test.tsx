import {describe, expect, it} from 'vitest';
import {renderToStaticMarkup} from 'react-dom/server';
import LoginPage from './LoginPage';

describe('Mock 登录页', () => {
    it('只展示服务端给出的固定身份和演示边界', () => {
        const html = renderToStaticMarkup(<LoginPage returnTo="/workspace/example" onLogin={() => undefined} state={{
            authenticated: false, provider: 'MOCK_SDK', csrfToken: 'csrf', accounts: [
                {id: 'admin', name: '演示管理员', description: '管理用户'},
                {id: 'developer', name: '演示开发者', description: '编辑工具'},
                {id: 'operator', name: '演示使用者', description: '运行授权工具'},
            ]
        }}/>);
        expect(html).toContain('演示管理员');
        expect(html).toContain('演示开发者');
        expect(html).toContain('演示使用者');
        expect(html).toContain('仅供 local/demo 环境');
        expect(html).not.toContain('password');
    });
});
