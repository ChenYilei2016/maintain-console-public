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
    canCreateTools: boolean;
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
                                             canCreateTools,
                                         }: Props) {
    return (
        <header className="workbench-toolbar">
            <div className="script-title">
                {script ? <input value={script.name} disabled={!script.canEdit}
                                 onChange={(event) => onNameChange(event.target.value)}
                                 aria-label="脚本名称"/> : <h1>脚本工作台</h1>}
                {script && <span className={'draft-state ' + (draftChanged ? 'unsaved' : '')}>
                    {draftChanged ? '● 未保存' : '✓ 已保存'}</span>}
            </div>
            {script && <div className="panel-actions">
                <button className="parameter-toggle" type="button" aria-expanded={parametersOpen}
                        aria-controls="parameter-sidebar" onClick={onParametersToggle}>参数与运行
                    · {parameterCount}</button>
                <button className="primary" type="button" disabled={!script.canEdit || saving}
                        onClick={() => onSave()}>{saving ? '保存中…' : '保存脚本'}</button>
                <button type="button" onClick={() => onHistory()}>执行历史</button>
                <button type="button"
                        onClick={() => onFavorite()}>{scriptIsFavorite ? '★ 取消收藏' : '☆ 收藏脚本'}</button>
                <button type="button" onClick={() => onRevisions()}>版本历史</button>
                {script.canManage && <button type="button" onClick={() => onPermissions()}>授权与分享</button>}
                <button type="button" onClick={onDetails}>用途与风险</button>
                {canCreateTools && <button type="button" onClick={onCopy}>复制为新工具</button>}
                {script.canInvoke && <a className="button" href={`/tools/${script.id}`}>运行页 ↗</a>}
                <button type="button" onClick={() => onExample()}>入门示例</button>
                {aiEnabled && <button type="button" onClick={() => onAiAssistant()}>AI 助手</button>}
            </div>}
        </header>
    );
}
