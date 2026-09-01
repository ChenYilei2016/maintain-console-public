import {describe, expect, it} from 'vitest';
import {renderToStaticMarkup} from 'react-dom/server';
import LoginPage from './LoginPage';

describe('独立账号登录页', () => {
    it('使用用户名密码，不枚举任何内置身份', () => {
        const html = renderToStaticMarkup(<LoginPage returnTo="/workspace/example" onLogin={() => undefined}/>);
        expect(html).toContain('用户名');
        expect(html).toContain('type="password"');
        expect(html).toContain('登录脚本工作台');
        expect(html).not.toContain('演示管理员');
    });

    it('管理端直达使用独立入口文案', () => {
        const html = renderToStaticMarkup(<LoginPage returnTo="/admin" onLogin={() => undefined}/>);
        expect(html).toContain('登录管理端');
        expect(html).toContain('管理角色不会授予任何脚本能力');
        expect(html).not.toContain('登录脚本工作台');
    });
});
