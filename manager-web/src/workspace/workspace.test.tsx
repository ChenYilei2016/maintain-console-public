import {renderToStaticMarkup} from 'react-dom/server';
import {describe, expect, it} from 'vitest';
import ExecutionResultsPanel from './ExecutionResultsPanel';
import ScriptParametersPanel from './ScriptParametersPanel';
import WorkspaceToolbar from './WorkspaceToolbar';
import ScriptResourceExplorer from './ScriptResourceExplorer';
import {scrollEdges} from './ParameterScrollArea';
import type {ComponentProps} from 'react';

const noop = () => undefined;
const parameters: ComponentProps<typeof ScriptParametersPanel> = {
    script: {
        id: 'script-1', name: '示例', type: 'script', serviceName: 'sample', content: 'return 1',
        canRead: true, canEdit: true, canInvoke: false
    },
    parameterSchema: '', definitions: [], parameterValues: {}, instances: [],
    target: {selectionMode: 'RANDOM', instanceId: '', timeoutSeconds: 180},
    draftChanged: false, executing: false, hasApproval: false,
    parameterTab: 'values', parametersOpen: false,
    onValueChange: noop, onSchemaChange: noop, onTargetChange: noop, onTabChange: noop,
    onClose: noop, onPreview: noop, onExecute: noop, onExample: noop, onEditScript: noop,
};

describe('工作区模块契约', () => {
    it('运行操作绑定到表单，保留权限与生产环境提示', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters}
                                                                 environment={{
                                                                     value: 'prod',
                                                                     name: '生产',
                                                                     icon: '',
                                                                     production: true
                                                                 }}/>);
        expect(html).toContain('type="submit" form="execution-form" disabled=""');
        expect(html).toContain('生产环境 · 执行需要审批与二次确认');
    });

    it('配置页不提交隐藏表单，而是引导进入运行填值', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters} parameterTab="schema"/>);
        expect(html).toContain('id="execution-form" hidden=""');
        expect(html).toContain('完成配置，填写运行参数');
        expect(html).not.toContain('form="execution-form"');
    });

    it('所有脚本操作直接展示，不再折叠到更多菜单', () => {
        const html = renderToStaticMarkup(<WorkspaceToolbar script={parameters.script} draftChanged={false}
                                                            saving={false} scriptIsFavorite={false} aiEnabled
                                                            parameterCount={6} parametersOpen={false}
                                                            onNameChange={noop} onParametersToggle={noop} onSave={noop}
                                                            onHistory={noop} onRevisions={noop}
                                                            onFavorite={noop} onPermissions={noop} onExample={noop}
                                                            onAiAssistant={noop}/>);
        for (const label of ['收藏脚本', '版本历史', '权限设置', '入门示例', 'AI 助手']) expect(html).toContain(label);
        expect(html).not.toContain('<details');
    });

    it('执行目标摘要位于滚动参数区外，不会被长列表推走', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters}/>);
        expect(html.indexOf('aria-label="执行目标"')).toBeLessThan(html.indexOf('class="parameter-scroll-area"'));
        expect(html).toContain('目标设置');
    });

    it('根据真实高度和滚动位置提示上下剩余内容', () => {
        expect(scrollEdges({scrollTop: 0, scrollHeight: 900, clientHeight: 200})).toEqual({above: false, below: true});
        expect(scrollEdges({scrollTop: 300, scrollHeight: 900, clientHeight: 200})).toEqual({above: true, below: true});
        expect(scrollEdges({scrollTop: 700, scrollHeight: 900, clientHeight: 200})).toEqual({
            above: true,
            below: false
        });
        expect(scrollEdges({scrollTop: 0, scrollHeight: 200, clientHeight: 200})).toEqual({above: false, below: false});
        expect(scrollEdges({scrollTop: 0, scrollHeight: 900, clientHeight: 0})).toEqual({above: false, below: false});
    });

    it('资源浏览模块保留当前脚本选中状态', () => {
        const html = renderToStaticMarkup(<ScriptResourceExplorer serviceName="sample" tree={[parameters.script]}
                                                                  overview={{favorites: [], recent: []}} loading={false}
                                                                  selectedId={parameters.script.id}
                                                                  onSelect={noop} onCreate={noop} onRename={noop}
                                                                  onDelete={noop}/>);
        expect(html).toContain('tree-row selected');
        expect(html).toContain('搜索目录树');
    });

    it('结果收起只隐藏内容，放大后仍展示同一份结果', () => {
        const collapsed = renderToStaticMarkup(<ExecutionResultsPanel result="保留的执行结果" executing={false}
                                                                      resultView="collapsed" onViewChange={noop}
                                                                      onCancel={noop}/>);
        expect(collapsed).toContain('id="execution-results" hidden=""');
        expect(collapsed).toContain('保留的执行结果');
        const maximized = renderToStaticMarkup(<ExecutionResultsPanel result="保留的执行结果" executing={false}
                                                                      resultView="maximized" onViewChange={noop}
                                                                      onCancel={noop}/>);
        expect(maximized).toContain('还原结果区');
        expect(maximized).not.toContain('hidden=""');
    });
});
