import {useEffect, useState} from 'react';
import {api} from '../api';
import Modal from '../Modal';
import type {ScriptDetail, ScriptRevision} from '../types';

export default function ScriptVersionsModal({tool, content, schema, onRestored, onClose}: {
    tool: ScriptDetail; content: string; schema: string; onRestored: () => void; onClose: () => void;
}) {
    const [revisions, setRevisions] = useState<ScriptRevision[]>([]);
    const [selected, setSelected] = useState<ScriptRevision>();
    const [view, setView] = useState<'content' | 'parameterSchema'>('content');
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    useEffect(() => {
        api.getScriptRevisions(tool.id).then(setRevisions).catch(failure => setError(failure.message));
    }, [tool.id]);
    return <Modal title="版本与差异" wide onClose={onClose} footer={<>
        <button onClick={onClose}>关闭</button>
        <button className="primary" disabled={!selected || !tool.canEdit || saving} onClick={async () => {
            if (!selected || !window.confirm(`恢复 v${selected.version} 将替换当前草稿并保存为新版本；不恢复旧授权，也不撤销已发生的业务操作。继续？`)) return;
            setSaving(true);
            try {
                await api.restoreScriptRevision(tool.id, selected.version, tool.version);
                onRestored();
                onClose();
            } catch (failure) {
                setError(failure instanceof Error ? failure.message : '恢复失败');
            } finally {
                setSaving(false);
            }
        }}>恢复所选内容为新版本
        </button>
    </>}>
        {error && <p role="alert" className="safety-note">{error}</p>}
        <div className="version-controls"><select aria-label="选择历史版本" value={selected?.version || ''}
                                                  onChange={event => setSelected(revisions.find(item => item.version === Number(event.target.value)))}>
            <option value="">选择一个版本进行比较</option>
            {revisions.map(item => <option key={item.id}
                                           value={item.version}>v{item.version} · {item.creatorName} · {item.createTime?.replace('T', ' ')}</option>)}
        </select>
            <button aria-pressed={view === 'content'} onClick={() => setView('content')}>代码差异</button>
            <button aria-pressed={view === 'parameterSchema'} onClick={() => setView('parameterSchema')}>参数定义差异
            </button>
        </div>
        {selected && <div className="source-comparison">
            <section><h3>历史 v{selected.version}</h3>
                <pre>{selected[view] || '无参数定义'}</pre>
            </section>
            <section><h3>当前草稿（基于 v{tool.version}）</h3>
                <pre>{view === 'content' ? content : schema || '无参数定义'}</pre>
            </section>
        </div>}
    </Modal>;
}
