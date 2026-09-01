import {useCallback, useEffect, useMemo, useState} from 'react';
import {navigate} from '../navigation';
import type {LoginInfo} from '../types';
import ScriptWorkspace from './ScriptWorkspace';

export const MAX_OPEN_SCRIPTS = 5;

export interface WorkspaceTabSummary {
    name: string;
    dirty: boolean;
    running: boolean;
}

export function restoredTabIds(raw: string | null): string[] {
    try {
        const value = JSON.parse(raw || '[]');
        return Array.isArray(value) ? value.filter(id => typeof id === 'string' && id !== 'new'
            && /^[A-Za-z0-9_-]{1,128}$/.test(id))
            .slice(0, MAX_OPEN_SCRIPTS) : [];
    } catch {
        return [];
    }
}

export default function WorkspaceTabs({scriptId, login}: { scriptId: string; login: LoginInfo }) {
    const storageKey = 'maintain-workspace-tabs:' + login.userId;
    const [tabs, setTabs] = useState(() => {
        const restored = restoredTabIds(sessionStorage.getItem(storageKey));
        return restored.includes(scriptId) ? restored : [...restored, scriptId].slice(-MAX_OPEN_SCRIPTS);
    });
    const [activeId, setActiveId] = useState(scriptId);
    const [summaries, setSummaries] = useState<Record<string, WorkspaceTabSummary>>({});
    const [notice, setNotice] = useState('');
    useEffect(() => {
        if (tabs.includes(scriptId)) {
            setActiveId(scriptId);
            return;
        }
        if (tabs.length < MAX_OPEN_SCRIPTS) {
            setTabs(current => [...current, scriptId]);
            setActiveId(scriptId);
            return;
        }
        const replaceable = tabs.find(id => !summaries[id]?.dirty && !summaries[id]?.running);
        if (replaceable) {
            setTabs(current => current.map(id => id === replaceable ? scriptId : id));
            setActiveId(scriptId);
        } else {
            setNotice(`最多同时打开 ${MAX_OPEN_SCRIPTS} 个脚本；请先保存或关闭一个会话。`);
            navigate(`/workspace/${activeId}`, true);
        }
    }, [scriptId]);
    useEffect(() => {
        sessionStorage.setItem(storageKey, JSON.stringify(tabs));
    }, [storageKey, tabs]);
    const updateSummary = useCallback((id: string, summary: WorkspaceTabSummary) => {
        setSummaries(current => {
            const previous = current[id];
            return previous?.name === summary.name && previous.dirty === summary.dirty && previous.running === summary.running
                ? current : {...current, [id]: summary};
        });
    }, []);
    const tabButtons = useMemo(() => tabs.map(id => {
        const summary = summaries[id];
        return <div className={id === activeId ? 'active' : ''} key={id}>
            <button className="workspace-tab-label" role="tab" aria-selected={id === activeId}
                    onClick={() => navigate(`/workspace/${id}`)}
                    title={summary?.name || id}>{summary?.dirty ? '● ' : ''}{summary?.name || '正在加载…'}
                {summary?.running && <small>运行中</small>}</button>
            <button className="workspace-tab-close" aria-label={`关闭 ${summary?.name || id}`} onClick={() => {
                if (summary?.dirty && !window.confirm('此脚本有未保存草稿，确认关闭这个编辑会话？')) return;
                if (summary?.running && !window.confirm('请求仍在等待结果。关闭页面不会终止远端操作，仍要关闭吗？')) return;
                const index = tabs.indexOf(id);
                const next = tabs.filter(item => item !== id);
                setTabs(next);
                setSummaries(current => {
                    const copy = {...current};
                    delete copy[id];
                    return copy;
                });
                if (id === activeId) navigate(next[Math.min(index, next.length - 1)] ? `/workspace/${next[Math.min(index, next.length - 1)]}` : '/workspace');
            }}>×
            </button>
        </div>;
    }), [tabs, summaries, activeId]);
    return <div className="workspace-tabs-shell">
        <nav className="workspace-tabs" role="tablist" aria-label="已打开脚本">{tabButtons}
            <button className="workspace-tabs-home" onClick={() => navigate('/workspace')}>＋ 打开脚本</button>
        </nav>
        {notice && <div className="workspace-tabs-notice" role="status">{notice}
            <button onClick={() => setNotice('')}>×</button>
        </div>}
        <div className="workspace-tab-pages">{tabs.map(id => <div className="workspace-tab-page" role="tabpanel"
                                                                  hidden={id !== activeId} key={id}>
            <ScriptWorkspace id={id} login={login} onSummaryChange={summary => updateSummary(id, summary)}/>
        </div>)}</div>
    </div>;
}
