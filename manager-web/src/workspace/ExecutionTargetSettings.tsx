import {useId, useState} from 'react';
import Modal from '../Modal';
import type {ServiceInstance, TargetSelectionMode} from '../types';
import './ExecutionTargetSettings.css';

export interface ExecutionTarget {
    selectionMode: TargetSelectionMode;
    instanceId: string;
    timeoutSeconds: number;
}

const SELECTION_LABELS: Record<TargetSelectionMode, string> = {
    RANDOM: '随机单实例', SPECIFIC: '指定单实例', ALL: '全部实例',
};

interface Props {
    target: ExecutionTarget;
    instances: ServiceInstance[];
    onChange: (target: ExecutionTarget) => void;
    allowAllInstances?: boolean;
}

/** 目标摘要不随参数滚动；编辑通过校验后一次性应用，取消不改变运行目标。 */
export default function ExecutionTargetSettings({target, instances, onChange, allowAllInstances = false}: Props) {
    const [draft, setDraft] = useState<ExecutionTarget>();
    const formId = useId();
    const selected = instances.find((instance) => instance.id === target.instanceId);
    const instanceLabel = target.selectionMode === 'SPECIFIC'
        ? selected ? `${selected.host}:${selected.port}` : target.instanceId ? '所选实例当前不可用' : '尚未选择实例'
        : `当前可用 ${instances.length} 个实例`;

    return <>
        <section className="execution-target-summary" aria-label="执行目标">
            <div>
                <strong>执行目标 <span>{SELECTION_LABELS[target.selectionMode]} · {target.timeoutSeconds} 秒</span></strong>
                <small title={target.instanceId || undefined}>{instanceLabel}</small>
            </div>
            <button type="button" onClick={() => setDraft({...target})}>目标设置</button>
        </section>
        {draft && <Modal title="执行目标设置" onClose={() => setDraft(undefined)} footer={<>
            <button type="button" onClick={() => setDraft(undefined)}>取消</button>
            <button className="primary" type="submit" form={formId}>应用设置</button>
        </>}>
            <form id={formId} className="form-stack" onSubmit={(event) => {
                event.preventDefault();
                onChange(draft);
                setDraft(undefined);
            }}>
                <label><span>执行模式</span><select value={draft.selectionMode}
                                                    onChange={(event) => setDraft({
                                                        ...draft,
                                                        selectionMode: event.target.value as TargetSelectionMode
                                                    })}>
                    {Object.entries(SELECTION_LABELS).filter(([value]) => value !== 'ALL' || allowAllInstances).map(([value, label]) =>
                        <option key={value}
                                                                                      value={value}>{label}</option>)}
                </select></label>
                {draft.selectionMode === 'SPECIFIC' && <label><span>目标实例</span>
                    <select required
                            value={instances.some((instance) => instance.id === draft.instanceId) ? draft.instanceId : ''}
                            onChange={(event) => setDraft({...draft, instanceId: event.target.value})}>
                        <option value="">请选择实例</option>
                        {instances.map((instance) => <option key={instance.id} value={instance.id}>
                            {instance.id} · {instance.host}:{instance.port}
                        </option>)}
                    </select>
                </label>}
                <label><span>超时时间（秒）</span><input type="number" required min={1} max={900}
                                                       value={draft.timeoutSeconds} onChange={(event) => setDraft({
                    ...draft,
                    timeoutSeconds: Number(event.target.value)
                })}/></label>
                <p>当前已发现 {instances.length} 个实例；实际执行前服务端仍会校验目标可用性。</p>
            </form>
        </Modal>}
    </>;
}
