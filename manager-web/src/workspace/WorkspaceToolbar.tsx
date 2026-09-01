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
                                         }: Props) {
    return (
        <header className="workbench-toolbar">
            <div className="script-title">
                {script ? <input value={script.name} disabled={!script.canEdit}
                                 onChange={(event) => onNameChange(event.target.value)}
                                 aria-label="脚本名称"/> : <h1>脚本工作台</h1>}
                {script && <span className={'draft-state ' + (draftChanged ? 'unsaved' : '')}>
                    {!script.canEdit ? '只读 · 运行保存版本' : draftChanged ? '● 未保存' : '✓ 已保存'}</span>}
            </div>
            {script && <div className="panel-actions">
                <div className="toolbar-action-group primary-actions" aria-label="主要操作">
                    <button className="parameter-toggle" type="button" aria-expanded={parametersOpen}
                            aria-controls="parameter-sidebar" onClick={onParametersToggle}>参数与运行
                        · {parameterCount}</button>
                    <button className="primary" type="button" disabled={!script.canEdit || saving}
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
                    <button type="button" onClick={onCopy}>复制</button>
                </div>
                <div className="toolbar-action-group" aria-label="开发辅助"><span>开发辅助</span>
                    <button type="button" onClick={() => onExample()}>示例</button>
                    {aiEnabled && <button type="button" onClick={() => onAiAssistant()}>AI</button>}</div>
            </div>}
        </header>
    );
}
