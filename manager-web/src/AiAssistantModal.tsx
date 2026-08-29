import {useState} from 'react';
import {api} from './api';
import Modal from './Modal';
import type {AiAssistAction, AiAssistResponse, NoticeType, ScriptDetail} from './types';

const ACTION_LABELS: Record<AiAssistAction, string> = {
    GENERATE_SCRIPT: '生成脚本',
    EXPLAIN_SCRIPT: '解释脚本',
    GENERATE_PARAMETER_SCHEMA: '生成参数 Schema',
    REVIEW_RISK: '执行前风险审查',
};

interface AiAssistantModalProps {
    script: ScriptDetail;
    serviceName: string;
    parameterSchema: string;
    onApplyScript: (content: string) => void;
    onApplyParameterSchema: (schema: string) => void;
    onNotice: (message: string, type?: NoticeType) => void;
    onClose: () => void;
}

function messageOf(error: unknown): string {
    return error instanceof Error ? error.message : '未知错误';
}

function stripCodeFence(value: string): string {
    return value.trim().replace(/^```(?:groovy|json)?\s*/i, '').replace(/\s*```$/, '');
}

export default function AiAssistantModal({
                                             script,
                                             serviceName,
                                             parameterSchema,
                                             onApplyScript,
                                             onApplyParameterSchema,
                                             onNotice,
                                             onClose,
                                         }: AiAssistantModalProps) {
    const [action, setAction] = useState<AiAssistAction>('REVIEW_RISK');
    const [instruction, setInstruction] = useState('');
    const [response, setResponse] = useState<AiAssistResponse>();
    const [loading, setLoading] = useState(false);

    const requestAssistance = async () => {
        if (action === 'GENERATE_SCRIPT' && !instruction.trim()) {
            onNotice('请描述需要生成的脚本目标', 'warning');
            return;
        }
        setLoading(true);
        setResponse(undefined);
        try {
            setResponse(await api.assistScript({
                action,
                scriptId: script.id,
                serviceName,
                script: script.content,
                parameterSchema,
                instruction,
            }));
        } catch (error) {
            onNotice(`AI 助手调用失败：${messageOf(error)}`);
        } finally {
            setLoading(false);
        }
    };

    const canApply = response && (action === 'GENERATE_SCRIPT' || action === 'GENERATE_PARAMETER_SCHEMA');
    const applySuggestion = () => {
        if (!response || !canApply || !script.canEdit) return;
        const content = stripCodeFence(response.content);
        if (action === 'GENERATE_SCRIPT') onApplyScript(content);
        if (action === 'GENERATE_PARAMETER_SCHEMA') onApplyParameterSchema(content);
        onClose();
        onNotice('AI 建议已应用到未保存草稿，请人工复核', 'warning');
    };

    return (
        <Modal title="AI 脚本助手" wide onClose={onClose} footer={<>
            <span className="ai-review-notice">AI 不会执行、保存或审批脚本</span>
            <button type="button" onClick={onClose}>关闭</button>
            {canApply && <button className="primary" type="button" disabled={!script.canEdit}
                                 onClick={applySuggestion}>应用到未保存草稿</button>}
        </>}>
            <div className="ai-assistant">
                <div className="ai-actions" role="group" aria-label="AI 辅助类型">
                    {(Object.keys(ACTION_LABELS) as AiAssistAction[]).map((item) => <button type="button"
                                                                                            key={item}
                                                                                            className={action === item ? 'active' : ''}
                                                                                            onClick={() => {
                                                                                                setAction(item);
                                                                                                setInstruction('');
                                                                                                setResponse(undefined);
                                                                                            }}>{ACTION_LABELS[item]}</button>)}
                </div>
                <label><span>补充说明</span><textarea rows={4} value={instruction}
                                                      onChange={(event) => setInstruction(event.target.value)}
                                                      placeholder={action === 'GENERATE_SCRIPT'
                                                          ? '描述目标、只读/写入边界、期望结果格式和异常处理'
                                                          : '可补充关注范围；不会发送运行参数或执行结果'}/></label>
                <div className="safety-note"><strong>数据边界</strong>
                    仅发送当前脚本、参数 Schema、服务名和上方说明；常见密钥字面量会先脱敏。模型输出始终需要人工确认。
                </div>
                <button className="primary ai-submit" type="button" disabled={loading}
                        onClick={() => void requestAssistance()}>{loading ? '分析中…' : ACTION_LABELS[action]}</button>
                {response && <section className="ai-result">
                    <header><strong>{ACTION_LABELS[response.action]}</strong><small>{response.model}</small></header>
                    <pre>{response.content}</pre>
                    <p>{response.notice}</p>
                </section>}
            </div>
        </Modal>
    );
}
