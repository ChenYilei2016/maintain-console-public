import {describe, expect, it} from 'vitest';
import type {DirectoryNode} from './types';
import {extractParameters, filterTree} from './tree';

const tree: DirectoryNode[] = [{
    id: '1',
    name: '业务运维',
    type: 'folder',
    serviceName: 'demo',
    children: [{id: '2', name: '查询订单', type: 'script', serviceName: 'demo'}],
}];

describe('控制台树与参数解析', () => {
    it('保留匹配脚本的父目录', () => {
        expect(filterTree(tree, '订单')).toEqual(tree);
        expect(filterTree(tree, '不存在')).toEqual([]);
    });

    it('按出现顺序提取并去重脚本参数', () => {
        expect(extractParameters('$${ id } / $${name} / $${id}')).toEqual(['id', 'name']);
    });
});
