// @vitest-environment happy-dom
import {act} from 'react';
import {createRoot, type Root} from 'react-dom/client';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import DirectoryTree from './DirectoryTree';

Object.assign(globalThis, {IS_REACT_ACT_ENVIRONMENT: true});

let container: HTMLDivElement;
let root: Root;

beforeEach(() => {
    container = document.createElement('div');
    document.body.append(container);
});

afterEach(async () => {
    if (root) await act(async () => root.unmount());
    container.remove();
});

describe('目录树拖拽', () => {
    it('用真实指针拖动把脚本移到放下的文件夹', async () => {
        const move = vi.fn();
        root = createRoot(container);
        await act(async () => root.render(<DirectoryTree searching={false} onSelect={() => undefined} onMove={move}
                                                         nodes={[{
                                                             id: 'target', name: '目标目录', type: 'folder',
                                                             serviceName: 'sample', canRename: true,
                                                         }, {
                                                             id: 'script', name: '待移动脚本', type: 'script',
                                                             serviceName: 'sample', canRename: true,
                                                         }]}/>));

        const handle = container.querySelector<HTMLElement>('[aria-label^="拖动 待移动脚本"]')!;
        const target = [...container.querySelectorAll<HTMLElement>('.tree-row')]
            .find(row => row.textContent?.includes('目标目录'))!;
        vi.spyOn(document, 'elementFromPoint').mockReturnValue(target);
        for (const [type, clientX, clientY] of [
            ['pointerdown', 247, 74],
            ['pointermove', 180, 45],
            ['pointerup', 140, 35],
        ] as const) {
            await act(async () => handle.dispatchEvent(new PointerEvent(type, {
                bubbles: true, cancelable: true, button: 0, pointerId: 1, clientX, clientY,
            })));
        }

        expect(move).toHaveBeenCalledWith('script', 'target');

        move.mockClear();
        await act(async () => handle.dispatchEvent(new PointerEvent('pointerdown', {
            bubbles: true, button: 0, pointerId: 2, clientX: 247, clientY: 74,
        })));
        await act(async () => handle.dispatchEvent(new PointerEvent('pointermove', {
            bubbles: true, pointerId: 2, clientX: 210, clientY: 95,
        })));
        vi.mocked(document.elementFromPoint).mockReturnValue(container.querySelector('[data-tree-root-drop]'));
        await act(async () => handle.dispatchEvent(new PointerEvent('pointerup', {
            bubbles: true, pointerId: 2, clientX: 180, clientY: 120,
        })));

        expect(move).toHaveBeenCalledWith('script');
    });
});
