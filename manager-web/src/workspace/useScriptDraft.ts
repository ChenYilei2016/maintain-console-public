import {useEffect, useState} from 'react';
import {api} from '../api';
import {parseParameterSchema} from '../parameters';
import type {ScriptDetail, ToolMetadata} from '../types';

export interface ScriptDraft {
    name: string;
    content: string;
    schema: string;
    description: string;
    metadata: ToolMetadata
}

interface Recovery {
    version: number;
    draft: ScriptDraft
}

function draftOf(tool: ScriptDetail): ScriptDraft {
    return {
        name: tool.name, content: tool.content, schema: tool.parameterSchema || '', description: tool.description || '',
        metadata: tool.toolMetadata || {operationType: 'UNSPECIFIED'}
    };
}

/** 页面只需修改草稿或保存；版本锁、离开保护及当前标签页恢复由编辑模块持有。 */
export function useScriptDraft(scriptId: string, userId: string) {
    const [tool, setTool] = useState<ScriptDetail>();
    const [draft, setDraft] = useState<ScriptDraft>();
    const [recovery, setRecovery] = useState<Recovery>();
    const [conflict, setConflict] = useState<ScriptDetail>();
    const [error, setError] = useState('');
    const [saving, setSaving] = useState(false);
    const key = 'maintain-draft:' + JSON.stringify([userId, scriptId]);
    const dirty = Boolean(tool && draft && JSON.stringify(draft) !== JSON.stringify(draftOf(tool)));

    const reload = async () => {
        const detail = await api.getScriptDetail(scriptId);
        setTool(detail);
        setDraft(draftOf(detail));
        setError('');
    };
    useEffect(() => {
        let active = true;
        api.getScriptDetail(scriptId).then(detail => {
            if (!active) return;
            setTool(detail);
            setDraft(draftOf(detail));
            try {
                const cached = JSON.parse(sessionStorage.getItem(key) || 'null') as Recovery | null;
                if (detail.canEdit && cached?.draft && typeof cached.version === 'number'
                    && typeof cached.draft.content === 'string' && typeof cached.draft.schema === 'string'
                    && typeof cached.draft.name === 'string' && cached.draft.metadata) setRecovery(cached);
            } catch { /* 损坏的缓存不影响读取服务器版本。 */
            }
        }).catch(failure => {
            if (active) setError(failure.message);
        });
        return () => {
            active = false;
        };
    }, [scriptId, key]);
    useEffect(() => {
        if (!dirty) return;
        const protect = (event: BeforeUnloadEvent) => {
            event.preventDefault();
            event.returnValue = '';
        };
        window.addEventListener('beforeunload', protect);
        return () => window.removeEventListener('beforeunload', protect);
    }, [dirty]);
    useEffect(() => {
        if (!tool || !draft || recovery) return;
        if (!dirty) {
            try {
                sessionStorage.removeItem(key);
            } catch { /* 撤销回保存版本时不保留过期草稿。 */
            }
            return;
        }
        const timer = window.setTimeout(() => {
            try {
                const schema = parseParameterSchema(draft.schema);
                schema?.parameters.filter(parameter => parameter.sensitive).forEach(parameter => {
                    delete parameter.defaultValue;
                    delete parameter.example;
                });
                sessionStorage.setItem(key, JSON.stringify({
                    version: tool.version,
                    draft: {...draft, schema: schema ? JSON.stringify(schema, null, 2) : ''}
                }));
            } catch { /* 无效 Schema 不覆盖上一份可恢复草稿，也不阻止继续编辑。 */
            }
        }, 500);
        return () => window.clearTimeout(timer);
    }, [dirty, tool, draft, key, recovery]);

    const save = async () => {
        if (!tool || !draft || saving) return false;
        setSaving(true);
        setError('');
        try {
            await api.saveTreeNode({
                nodeId: scriptId, nodeType: 'script', nodeName: draft.name,
                serviceName: tool.serviceName, expectedVersion: tool.version, content: draft.content,
                parameterSchema: draft.schema, description: draft.description, toolMetadata: draft.metadata
            });
            await reload();
            try {
                sessionStorage.removeItem(key);
            } catch { /* 存储不可用不改变已保存的事实。 */
            }
            setRecovery(undefined);
            return true;
        } catch (failure) {
            setError(failure instanceof Error ? failure.message : '保存失败');
            try {
                const latest = await api.getScriptDetail(scriptId);
                if (latest.version !== tool.version) setConflict(latest);
            } catch { /* 保留本地草稿，权限撤销或断网时不清空编辑内容。 */
            }
            return false;
        } finally {
            setSaving(false);
        }
    };
    return {
        tool, draft, dirty, error, saving, recovery, conflict, reload, save,
        update: (patch: Partial<ScriptDraft>) => setDraft(current => current ? {...current, ...patch} : current),
        recover: () => {
            if (recovery) setDraft(recovery.draft);
            setRecovery(undefined);
        },
        discardRecovery: () => {
            try {
                sessionStorage.removeItem(key);
            } catch { /* 仅清理当前标签页缓存。 */
            }
            setRecovery(undefined);
        },
        acceptLatestVersion: () => {
            if (conflict) setTool(conflict);
            setConflict(undefined);
        },
    };
}
