import {renderToStaticMarkup} from 'react-dom/server';
import {describe, expect, it} from 'vitest';
import ExecutionResultsPanel from './ExecutionResultsPanel';
import ScriptParametersPanel from './ScriptParametersPanel';
import WorkspaceToolbar from './WorkspaceToolbar';
import ScriptResourceExplorer from './ScriptResourceExplorer';
import DirectoryTree from '../DirectoryTree';
import {scrollEdges} from './ParameterScrollArea';
import type {ComponentProps} from 'react';
import {canOpenScriptEditor, ScriptPermissionFallback} from './ScriptWorkspace';
import ExecutionConfirmation from './ExecutionConfirmation';
import ScriptImportDialog from './ScriptImportDialog';
import {TOOL_TEMPLATES} from './templates';

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
    it('EDIT 单独授权也进入编辑器，INVOKE 单独授权进入仅运行视图', () => {
        expect(canOpenScriptEditor({canRead: false, canEdit: true})).toBe(true);
        expect(canOpenScriptEditor({canRead: false, canEdit: false})).toBe(false);
    });

    it('运行操作绑定到表单，保留权限与生产环境提示', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters}
                                                                 environment={{
                                                                     value: 'prod',
                                                                     name: '生产',
                                                                     icon: '',
                                                                     production: true
                                                                 }}/>);
        const formId = html.match(/<form id="([^"]+)"/)?.[1];
        expect(formId).toBeTruthy();
        expect(html).toContain(`type="submit" form="${formId}"`);
        expect(html).not.toContain(`type="submit" form="${formId}" disabled=""`);
        expect(html).toContain('生产环境 · 请核对目标和操作风险，确认不是审批');
    });

    it('多个控制台的运行按钮只提交各自表单', () => {
        const html = renderToStaticMarkup(<>
            <ScriptParametersPanel {...parameters}/>
            <ScriptParametersPanel {...parameters} script={{...parameters.script, id: 'script-2'}}/>
        </>);
        const formIds = [...html.matchAll(/<form id="([^"]+)"/g)].map(match => match[1]);
        const buttonTargets = [...html.matchAll(/type="submit" form="([^"]+)"/g)].map(match => match[1]);

        expect(new Set(formIds).size).toBe(2);
        expect(buttonTargets).toEqual(formIds);
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
        const formId = html.match(/<form id="([^"]+)"/)?.[1];
        expect(formId).toBeTruthy();
        expect(html).toContain(`type="submit" form="${formId}"`);
        expect(html).not.toContain(`type="submit" form="${formId}" disabled=""`);
    });

    it('缺少运行权限时直接说明原因', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters}
                                                                 script={{
                                                                     ...parameters.script,
                                                                     canEdit: true,
                                                                     canInvoke: false
                                                                 }}/>);
        expect(html).toContain('当前无运行权限');
        const formId = html.match(/<form id="([^"]+)"/)?.[1];
        expect(html).toContain(`type="submit" form="${formId}"`);
        expect(html).not.toContain(`type="submit" form="${formId}" disabled=""`);
    });

    it('配置页不提交隐藏表单，而是引导进入运行填值', () => {
        const html = renderToStaticMarkup(<ScriptParametersPanel {...parameters} parameterTab="schema"/>);
        const formId = html.match(/<form id="([^"]+)"/)?.[1];
        expect(formId).toBeTruthy();
        expect(html).toContain(`id="${formId}" hidden=""`);
        expect(html).toContain('完成配置，填写运行参数');
        expect(html).not.toContain(`form="${formId}"`);
    });

    it('脚本操作按职责分组，常用入口保持直接可见', () => {
        const html = renderToStaticMarkup(<WorkspaceToolbar script={parameters.script} draftChanged={false}
                                                            saving={false} scriptIsFavorite={false} aiEnabled
                                                            parameterCount={6} parametersOpen={false}
                                                            onNameChange={noop} onParametersToggle={noop} onSave={noop}
                                                            onHistory={noop} onRevisions={noop}
                                                            onFavorite={noop} onPermissions={noop} onExample={noop}
                                                            onAiAssistant={noop} onDetails={noop} onCopy={noop}
                                                            onImport={noop}/>);
        for (const label of ['主要操作', '回溯', '脚本设置', '开发辅助', '收藏', '版本', '授权', '示例', 'AI']) {
            expect(html).toContain(label);
        }
        expect(html).toContain('导入 JSON');
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
                                                                  onImport={noop} onDelete={noop}/>);
        expect(html).toContain('tree-row selected');
        expect(html).toContain('搜索目录树');
        expect(html).toContain('导入');
    });

    it('目录树同时展示文件夹和脚本名称，不用 public/private 标记冒充 ACL', () => {
        const html = renderToStaticMarkup(<DirectoryTree searching={false} onSelect={noop} nodes={[{
            id: 'folder-1', name: '订单运维', type: 'folder', serviceName: 'sample', level: 0, children: [{
                ...parameters.script, parentId: 'folder-1', permissionType: 'private'
            }]
        }]}/>);
        expect(html).toContain('订单运维');
        expect(html).toContain('示例');
        expect(html).not.toContain('私有脚本');
    });

    it('目录在点击前展示脚本能力，无任何能力的脚本不可盲点', () => {
        const html = renderToStaticMarkup(<DirectoryTree searching={false} onSelect={noop} nodes={[{
            ...parameters.script,
            id: 'locked',
            name: '锁定脚本',
            canRead: false,
            canEdit: false,
            canInvoke: false,
            canManage: false
        }, {
            ...parameters.script,
            id: 'debuggable',
            name: '可调试脚本',
            canRead: true,
            canEdit: true,
            canInvoke: true,
            canManage: true
        }]}/>);
        expect(html).toContain('仅目录');
        expect(html).toContain('可调试');
        expect(html).toContain('disabled=""');
    });

    it('只有授权管理能力时保留可操作入口，不把可授权脚本变成死路', () => {
        const html = renderToStaticMarkup(<ScriptPermissionFallback name="待授权脚本" canManage onManage={noop}/>);

        expect(html).toContain('只有授权管理能力');
        expect(html).toContain('管理授权');
        expect(html).not.toContain('没有查看、运行或授权管理能力');
    });

    it('末级目录仍能新建脚本', () => {
        const html = renderToStaticMarkup(<DirectoryTree searching={false} onSelect={noop} onCreate={noop} nodes={[{
            id: 'leaf-folder', name: '末级目录', type: 'folder', serviceName: 'sample', level: 2
        }]}/>);
        expect(html).toContain('在 末级目录 下新建');
    });

    it('只有可管理的目录节点提供原生拖拽入口', () => {
        const html = renderToStaticMarkup(<DirectoryTree searching={false} selectedFolderId="destination"
                                                         onSelect={noop} onMove={noop} nodes={[{
            id: 'movable', name: '可移动脚本', type: 'script', serviceName: 'sample', canRename: true
        }, {
            id: 'locked', name: '不可移动脚本', type: 'script', serviceName: 'sample', canRename: false
        }]}/>);

        expect(html).toContain('draggable="true"');
        expect(html).toContain('可拖拽移动');
        expect(html).toContain('draggable="false"');
        expect(html).toContain('将 可移动脚本 移动到当前目录');
    });

    it('结果始终可见，放大后仍展示同一份结果', () => {
        const execution = {error: '保留的执行结果', running: false, elapsed: 0};
        const open = renderToStaticMarkup(<ExecutionResultsPanel execution={execution}
                                                                 resultView="open" onViewChange={noop}/>);
        expect(open).not.toContain('id="execution-results" hidden=""');
        expect(open).toContain('保留的执行结果');
        expect(open).not.toContain('收起结果');
        const maximized = renderToStaticMarkup(<ExecutionResultsPanel execution={execution}
                                                                      resultView="maximized" onViewChange={noop}/>);
        expect(maximized).toContain('还原结果区');
        expect(maximized).not.toContain('hidden=""');
    });

    it('风险确认明确说明尚未执行，并给出不含糊的确认动作', () => {
        const html = renderToStaticMarkup(<ExecutionConfirmation scriptName="生产排障" environment="生产环境"
                                                                 target="随机单实例" version={3}
                                                                 riskNote="可能修改订单状态"
                                                                 onCancel={noop} onConfirm={noop}/>);
        expect(html).toContain('尚未发起执行');
        expect(html).toContain('取消执行');
        expect(html).toContain('确认并调试');
        expect(html).toContain('可能修改订单状态');
    });

    it('导入面板在一个界面完成输入、目标选择与安全确认', () => {
        const html = renderToStaticMarkup(<ScriptImportDialog defaultServiceName="sample"
                                                               defaultEnvironment="test"
                                                               initialServices={['sample']}
                                                               environments={[{
                                                                   value: 'test',
                                                                   name: '测试',
                                                                   icon: '',
                                                                   production: false
                                                               }]}
                                                               onClose={noop}/>);

        expect(html).toContain('粘贴工具导入 JSON');
        expect(html).toContain('type="file"');
        expect(html).toContain('创建为私有工具并打开');
        expect(html).toContain('不会执行工具');
        expect(html).toContain('不会导入任何授权');
    });

    it('单结果示例直接返回结果块，多结果示例只包装一次', () => {
        expect(TOOL_TEMPLATES.table.content).toContain("return resultTable('");
        expect(TOOL_TEMPLATES.table.content).not.toContain('return result(resultTable(');
        expect(TOOL_TEMPLATES.empty.content).toContain("return resultText('");
        expect(TOOL_TEMPLATES.dashboard.content.match(/return result\(/g)).toHaveLength(1);
    });
});
