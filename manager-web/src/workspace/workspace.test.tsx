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
        canRead: true,
        canEdit: true,
        canInvoke: false,
        canManage: true,
        version: 1,
        allowAllInstances: false,
        enabled: true,
    },
    parameterSchema: '', definitions: [], parameterValues: {}, instances: [],
    target: {selectionMode: 'RANDOM', instanceId: '', timeoutSeconds: 180},
    draftChanged: false, executing: false, userId: 'tester', allowAllInstances: false,
    parameterTab: 'values', parametersOpen: false,
    onValueChange: noop, onSchemaChange: noop, onTargetChange: noop, onTabChange: noop,
    onClose: noop, onPreview: noop, onExecute: noop, onExample: noop, onEditScript: noop,
    onValuesChange: noop,
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
        expect(html).toContain('生产环境 · 请核对目标和操作风险，确认不是审批');
    });

    it('只有运行权限时明确运行保存版本，不伪装成草稿调试', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters}
                                                                 script={{
                                                                     ...parameters.script,
                                                                     canEdit: false,
                                                                     canInvoke: true
                                                                 }}
                                                                 instances={[{
                                                                     id: 'instance-1',
                                                                     serviceId: 'sample',
                                                                     host: '127.0.0.1',
                                                                     port: 8080,
                                                                     secure: false,
                                                                     uri: 'http://127.0.0.1:8080',
                                                                     metadata: {}
                                                                 }]}
                                                                 environment={{
                                                                     value: 'test',
                                                                     name: '测试',
                                                                     icon: '',
                                                                     production: false
                                                                 }}/>);
        expect(html).toContain('当前无编辑权限');
        expect(html).toContain('运行已保存版本');
        expect(html).not.toContain('调试当前内容');
        expect(html).toContain('type="submit" form="execution-form"');
        expect(html).not.toContain('type="submit" form="execution-form" disabled=""');
    });

    it('缺少运行权限时直接说明原因', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters}
                                                                 script={{
                                                                     ...parameters.script,
                                                                     canEdit: true,
                                                                     canInvoke: false
                                                                 }}/>);
        expect(html).toContain('当前无运行权限');
    });

    it('配置页不提交隐藏表单，而是引导进入运行填值', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters} parameterTab="schema"/>);
        expect(html).toContain('id="execution-form" hidden=""');
        expect(html).toContain('完成配置，填写运行参数');
        expect(html).not.toContain('form="execution-form"');
    });

    it('脚本操作按职责分组，常用入口保持直接可见', () => {
        const html = renderToStaticMarkup(<WorkspaceToolbar script={parameters.script} draftChanged={false}
                                                            canCreateTools
                                                            saving={false} scriptIsFavorite={false} aiEnabled
                                                            parameterCount={6} parametersOpen={false}
                                                            onNameChange={noop} onParametersToggle={noop} onSave={noop}
                                                            onHistory={noop} onRevisions={noop}
                                                            onFavorite={noop} onPermissions={noop} onExample={noop}
                                                            onAiAssistant={noop} onDetails={noop} onCopy={noop}/>);
        for (const label of ['主要操作', '回溯', '工具设置', '开发辅助', '收藏', '版本', '授权', '示例', 'AI']) {
            expect(html).toContain(label);
        }
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
        const execution = {error: '保留的执行结果', running: false, elapsed: 0};
        const collapsed = renderToStaticMarkup(<ExecutionResultsPanel execution={execution}
                                                                      resultView="collapsed" onViewChange={noop}/>);
        expect(collapsed).toContain('id="execution-results" hidden=""');
        expect(collapsed).toContain('保留的执行结果');
        const maximized = renderToStaticMarkup(<ExecutionResultsPanel execution={execution}
                                                                      resultView="maximized" onViewChange={noop}/>);
        expect(maximized).toContain('还原结果区');
        expect(maximized).not.toContain('hidden=""');
    });
});
