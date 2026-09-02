import type {ScriptDetail} from '../types';

interface Props {
    script?: ScriptDetail;
    draftChanged: boolean;
    saving: boolean;
    scriptIsFavorite: boolean;
    aiEnabled: boolean;
    parameterCount: number;
    parametersOpen: boolean;
    onNameChange: (name: string) => void;
    onParametersToggle: () => void;
    onSave: () => void;
    onHistory: () => void;
    onRevisions: () => void;
    onFavorite: () => void;
    onPermissions: () => void;
    onExample: () => void;
    onAiAssistant: () => void;
    onDetails: () => void;
    onCopy: () => void;
    onImport: () => void;
}

export default function WorkspaceToolbar({
                                             script,
                                             draftChanged,
                                             saving,
                                             scriptIsFavorite,
                                             aiEnabled,
                                             parameterCount,
                                             parametersOpen,
                                             onNameChange,
                                             onParametersToggle,
                                             onSave,
                                             onHistory,
                                             onRevisions,
                                             onFavorite,
                                             onPermissions,
                                             onExample,
                                             onAiAssistant,
                                             onDetails,
                                             onCopy,
                                             onImport,
                                         }: Props) {
    const accessSummary = !script ? '' : script.canEdit && script.canInvoke ? '可调试'
        : script.canInvoke ? '可运行' : script.canEdit ? '可编辑' : script.canRead ? '只读'
            : script.canManage ? '可授权' : '仅目录';
    const accessDetails = !script ? '' : [script.canRead && '查看', script.canEdit && '编辑',
        script.canInvoke && '运行', script.canManage && '授权'].filter(Boolean).join('、');
    return (
        <header className="workbench-toolbar">
            <div className="script-title">
                {script ? <input value={script.name} disabled={!script.canEdit}
                                 onChange={(event) => onNameChange(event.target.value)}
                                 aria-label="脚本名称"/> : <h1>脚本工作台</h1>}
                {script && <span className={'draft-state ' + (draftChanged ? 'unsaved' : '')}>
                    {!script.canEdit ? '只读 · 运行保存版本' : draftChanged ? '● 未保存' : '✓ 已保存'}</span>}
                {script && <span className="script-capabilities" aria-label="当前脚本能力"
                                 title={`当前能力：${accessDetails}`}>{accessSummary}{script.canManage && accessSummary !== '可授权'
                    ? ' · 可授权' : ''}</span>}
            </div>
            {script && <div className="panel-actions">
                <div className="toolbar-action-group primary-actions" aria-label="主要操作">
                    <button className="parameter-toggle" type="button" aria-expanded={parametersOpen}
                            aria-controls="parameter-sidebar" onClick={onParametersToggle}>参数与运行
                        · {parameterCount}</button>
                    <button className="primary" type="button" disabled={!script.canEdit || saving}
                            title="快捷键：Ctrl / ⌘ + S"
                            onClick={() => onSave()}>{saving ? '保存中…' : '保存脚本'}</button>
                </div>
                <div className="toolbar-action-group" aria-label="回溯"><span>回溯</span>
                    <button type="button" onClick={() => onHistory()}>执行记录</button>
                    <button type="button" onClick={() => onRevisions()}>版本</button>
                </div>
                <div className="toolbar-action-group" aria-label="脚本设置"><span>脚本设置</span>
                    <button type="button"
                            onClick={() => onFavorite()}>{scriptIsFavorite ? '★ 已收藏' : '☆ 收藏'}</button>
                    <button type="button" onClick={onDetails}>说明与风险</button>
                    {script.canManage && <button type="button" onClick={() => onPermissions()}>授权</button>}
                    <button type="button" title="复制当前草稿的导入 JSON" onClick={onCopy}>复制 JSON</button>
                </div>
                <div className="toolbar-action-group" aria-label="开发辅助"><span>开发辅助</span>
                    <button type="button" disabled={!script.canEdit}
                            title={script.canEdit ? '导入并覆盖当前草稿' : '当前无编辑权限'}
                            onClick={onImport}>导入 JSON
                    </button>
                    <button type="button" onClick={() => onExample()}>示例</button>
                    {aiEnabled && <button type="button" onClick={() => onAiAssistant()}>AI</button>}</div>
            </div>}
        </header>
    );
}
