import {useState} from 'react';
import type {LoginInfo} from '../types';
import WorkspaceResources from './WorkspaceResources';
import './workspace.css';

/** `/workspace` 仍是脚本工作台，只是不强造一个与控制台割裂的首页。 */
export default function WorkspaceEntry({login}: { login: LoginInfo }) {
    const [resourcesOpen, setResourcesOpen] = useState(true);

    return <main className={'workbench workspace-entry ' + (resourcesOpen ? '' : 'resources-collapsed')}>
        <header className="workbench-header">
            <button className="icon-button" aria-label={resourcesOpen ? '收起资源栏' : '展开资源栏'}
                    onClick={() => setResourcesOpen(!resourcesOpen)}>☰
            </button>
            <span className="app-name">脚本工作台</span>
            <div className="app-header-actions"><small>{login.employeeName}</small></div>
        </header>
        {resourcesOpen && <button className="resource-backdrop" aria-label="关闭资源栏"
                                  onClick={() => setResourcesOpen(false)}/>}
        <aside className="workbench-sidebar"><WorkspaceResources serviceName=""
                                                                 environment={login.availableEnvironments[0]?.value || ''}
                                                                 revision={0}/></aside>
        <section className="workbench-main">
            <div className="welcome-card workspace-empty-console">
                <span className="empty-console-icon" aria-hidden="true">⌘</span>
                <h1>从左侧选择脚本</h1>
                <p>目录已标明当前账号的查看、编辑、运行能力；也可以先选中目录，再点击“新建”。</p>
            </div>
        </section>
    </main>;
}
