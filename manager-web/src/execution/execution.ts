import {post, unwrap} from '../api';
import type {ScriptExecutionResult, TargetSelectionMode} from '../types';

export const OUTCOME_LABELS = {
    SUCCESS: '执行成功', FAILED: '执行失败', UNKNOWN: '结果未知',
    NOT_STARTED: '未开始', PARTIAL_SUCCESS: '部分成功',
} as const;
export type ExecutionOutcome = keyof typeof OUTCOME_LABELS;

export interface ExecutionReport {
    id: string;
    scriptId: string;
    scriptVersion: number;
    environment: string;
    draft: boolean;
    outcome: ExecutionOutcome;
    duration: number;
    startedAt: string;
    warning?: string;
    targets: Array<{
        instanceId: string; host: string; port: number;
        outcome: ExecutionOutcome; duration: number;
        result?: ScriptExecutionResult; message?: string;
    }>;
}

export interface RunToolRequest {
    scriptId: string;
    version: number;
    parameters: Record<string, unknown>;
    target: { environment: string; selectionMode: TargetSelectionMode; instanceId?: string; timeoutSeconds: number };
    riskConfirmed: boolean;
}

export interface DebugDraftRequest extends RunToolRequest {
    content: string;
    parameterSchema: string;
}

export async function runTool(request: RunToolRequest): Promise<ExecutionReport> {
    return unwrap(await post<ExecutionReport>('/manager/tools/run', request));
}

export async function debugDraft(request: DebugDraftRequest): Promise<ExecutionReport> {
    return unwrap(await post<ExecutionReport>('/manager/scripts/debug', request));
}
