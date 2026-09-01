import Modal from '../Modal';

export default function ExecutionConfirmation({
                                                  scriptName, environment, target, version, riskNote,
                                                  confirmLabel = '确认并调试', onCancel, onConfirm
                                              }: {
    scriptName: string; environment: string; target: string; version: number; riskNote: string;
    confirmLabel?: string; onCancel: () => void; onConfirm: () => void;
}) {
    return <Modal title="执行前二次确认" onClose={onCancel} footer={<>
        <button type="button" onClick={onCancel}>取消执行</button>
        <button className="primary" type="button" onClick={onConfirm}>{confirmLabel}</button>
    </>}>
        <div className="execution-confirmation">
            <strong>尚未发起执行</strong>
            <p>请核对这一次执行的实际范围，确认后才会向目标服务发送请求。</p>
            <dl>
                <div>
                    <dt>脚本</dt>
                    <dd>{scriptName} · v{version}</dd>
                </div>
                <div>
                    <dt>环境</dt>
                    <dd>{environment}</dd>
                </div>
                <div>
                    <dt>目标</dt>
                    <dd>{target}</dd>
                </div>
            </dl>
            <p className="production-warning">{riskNote || '请确认业务影响范围；二次确认不是审批或安全隔离。'}</p>
        </div>
    </Modal>;
}
