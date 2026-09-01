import {describe, expect, it} from 'vitest';
import {sameFrontendSurface} from './navigation';

describe('双前端导航边界', () => {
    it('只在同一前端内使用 History 导航', () => {
        expect(sameFrontendSurface('/workspace/1', '/workspace/2')).toBe(true);
        expect(sameFrontendSurface('/admin', '/admin/users')).toBe(true);
        expect(sameFrontendSurface('/workspace/1', '/admin')).toBe(false);
        expect(sameFrontendSurface('/admin', '/workspace')).toBe(false);
        expect(sameFrontendSurface('/admin', '/')).toBe(false);
    });
});
