// @vitest-environment happy-dom
import {act} from 'react';
import {createRoot, type Root} from 'react-dom/client';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {api} from '../api';
import validImportDocument from '../../../skills/maintain-console-script-author/references/example-import-v1.json';
import ScriptImportDialog from './ScriptImportDialog';
import {MAX_SCRIPT_IMPORT_SIZE} from './scriptImport';

Object.assign(globalThis, {IS_REACT_ACT_ENVIRONMENT: true});

const validImport = JSON.stringify(validImportDocument);
const environments = [{value: 'test', name: '测试环境', icon: '', production: false}];
let container: HTMLDivElement;
let root: Root;

function button(name: string) {
    const match = [...container.querySelectorAll('button')].find(item => item.textContent?.trim() === name);
    if (!match) throw new Error(`找不到按钮：${name}`);
    return match;
}

async function renderDialog(props: Partial<Parameters<typeof ScriptImportDialog>[0]> = {}) {
    root = createRoot(container);
    await act(async () => {
        root.render(<ScriptImportDialog defaultServiceName="demo-service" defaultEnvironment="test"
                                        initialServices={['demo-service']} environments={environments}
                                        onClose={() => undefined} {...props}/>);
    });
}

async function fillJson(json: string) {
    const textarea = container.querySelector('textarea')!;
    await act(async () => {
        Object.getOwnPropertyDescriptor(HTMLTextAreaElement.prototype, 'value')?.set?.call(textarea, json);
        textarea.dispatchEvent(new Event('input', {bubbles: true}));
    });
}

async function chooseFile(file: File) {
    const input = container.querySelector<HTMLInputElement>('input[type="file"]')!;
    Object.defineProperty(input, 'files', {configurable: true, value: [file]});
    await act(async () => {
        input.dispatchEvent(new Event('change', {bubbles: true}));
        await Promise.resolve();
    });
}

beforeEach(() => {
    container = document.createElement('div');
    document.body.append(container);
    vi.spyOn(api, 'getDirectoryTree').mockResolvedValue([]);
});

afterEach(async () => {
    if (root) await act(async () => root.unmount());
    vi.restoreAllMocks();
    container.remove();
});

describe('工具导入面板流程', () => {
    it('编辑区导入只覆盖当前草稿，不创建新工具', async () => {
        const apply = vi.fn();
        const close = vi.fn();
        const save = vi.spyOn(api, 'saveTreeNode');
        await renderDialog({onApply: apply, onClose: close});

        await fillJson(validImport);
        expect(container.textContent).toContain('会替换当前脚本名称、说明、代码、参数和风险配置');
        expect(container.textContent).not.toContain('所属应用');
        expect(container.textContent).not.toContain('允许环境');
        await act(async () => button('覆盖当前草稿').dispatchEvent(new MouseEvent('click', {bubbles: true})));

        expect(apply).toHaveBeenCalledWith(expect.objectContaining({
            script: expect.objectContaining({name: '生成问候明细'}),
        }));
        expect(save).not.toHaveBeenCalled();
        expect(api.getDirectoryTree).not.toHaveBeenCalled();
        expect(close).toHaveBeenCalledOnce();
    });

    it('从粘贴预览走既有创建接口并打开新工具', async () => {
        const save = vi.spyOn(api, 'saveTreeNode').mockResolvedValue('created-tool');
        const open = vi.fn();
        await renderDialog({onCreated: open});

        await fillJson(validImport);
        expect(container.textContent).toContain('1 个参数 · 1 个必填 · 0 个敏感');
        await act(async () => button('创建为私有工具并打开').dispatchEvent(new MouseEvent('click', {bubbles: true})));

        expect(save).toHaveBeenCalledWith(expect.objectContaining({
            nodeType: 'script',
            nodeName: '生成问候明细',
            serviceName: 'demo-service',
            allowedEnvironments: ['test'],
        }));
        expect(save.mock.calls[0][0]).not.toHaveProperty('permissions');
        expect(open).toHaveBeenCalledWith('created-tool');
    });

    it('本地文件与粘贴文本产生相同预览', async () => {
        await renderDialog();

        await chooseFile(new File([validImport], 'tool.json', {type: 'application/json'}));

        expect(container.querySelector('textarea')?.value).toBe(validImport);
        expect(container.textContent).toContain('生成问候明细');
        expect(container.textContent).toContain('1 个参数 · 1 个必填 · 0 个敏感');
    });

    it('文件失败会清空旧文档并禁止创建', async () => {
        await renderDialog();
        await fillJson(validImport);
        expect((button('创建为私有工具并打开') as HTMLButtonElement).disabled).toBe(false);

        await chooseFile(new File(['x'.repeat(MAX_SCRIPT_IMPORT_SIZE + 1)], 'too-large.json'));

        expect(container.querySelector('textarea')?.value).toBe('');
        expect(container.textContent).toContain('工具导入文件不能超过');
        expect((button('创建为私有工具并打开') as HTMLButtonElement).disabled).toBe(true);
    });

    it('创建失败保留文档、名称和目标选择', async () => {
        vi.spyOn(api, 'saveTreeNode').mockRejectedValue(new Error('目标目录存在同名资源'));
        await renderDialog();
        await fillJson(validImport);

        await act(async () => button('创建为私有工具并打开').dispatchEvent(new MouseEvent('click', {bubbles: true})));

        expect(container.textContent).toContain('目标目录存在同名资源');
        expect(container.querySelector('textarea')?.value).toBe(validImport);
        expect(container.querySelector<HTMLInputElement>('input[maxlength]')?.value).toBe('生成问候明细');
        expect(container.querySelector<HTMLSelectElement>('.script-import-targets select')?.value).toBe('demo-service');
    });

    it('编辑区默认位置可见，取消只关闭导入面板', async () => {
        vi.mocked(api.getDirectoryTree).mockResolvedValue([{
            id: 'folder-1', name: '当前目录', type: 'folder', serviceName: 'demo-service'
        }]);
        const close = vi.fn();
        await renderDialog({defaultParentId: 'folder-1', onClose: close});
        await fillJson(validImport);
        await act(async () => Promise.resolve());

        const selects = container.querySelectorAll<HTMLSelectElement>('.script-import-targets select');
        expect(selects[0].value).toBe('demo-service');
        expect(selects[1].value).toBe('folder-1');
        await act(async () => button('取消').dispatchEvent(new MouseEvent('click', {bubbles: true})));
        expect(close).toHaveBeenCalledOnce();
    });
});
