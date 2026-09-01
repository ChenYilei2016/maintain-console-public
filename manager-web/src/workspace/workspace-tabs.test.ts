import {describe, expect, it} from 'vitest';
import {MAX_OPEN_SCRIPTS, restoredTabIds} from './WorkspaceTabs';

describe('编辑会话恢复', () => {
    it('只恢复有界且格式合法的脚本 ID', () => {
        const ids = restoredTabIds(JSON.stringify(['new', 'a', '../bad', 'b', 'c', 'd', 'e', 'f']));
        expect(ids).toEqual(['a', 'b', 'c', 'd', 'e']);
        expect(ids).toHaveLength(MAX_OPEN_SCRIPTS);
        expect(restoredTabIds('{broken')).toEqual([]);
    });
});
