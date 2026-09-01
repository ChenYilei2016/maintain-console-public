import {useEffect, useRef, useState} from 'react';
import {ApiError} from '../api';
import type {ExecutionReport} from './execution';

/** 只管理本次请求的等待与结果；不重试、不查询后台任务。 */
export function useExecution() {
    const [report, setReport] = useState<ExecutionReport>();
    const [error, setError] = useState('');
    const [running, setRunning] = useState(false);
    const [elapsed, setElapsed] = useState(0);
    const inFlight = useRef(false);
    const mounted = useRef(true);
    useEffect(() => {
        mounted.current = true;
        return () => {
            mounted.current = false;
        };
    }, []);
    useEffect(() => {
        if (!running) return;
        const started = Date.now();
        const timer = window.setInterval(() => setElapsed(Date.now() - started), 250);
        const protect = (event: BeforeUnloadEvent) => {
            event.preventDefault();
            event.returnValue = '';
        };
        window.addEventListener('beforeunload', protect);
        return () => {
            window.clearInterval(timer);
            window.removeEventListener('beforeunload', protect);
        };
    }, [running]);

    const execute = async (request: () => Promise<ExecutionReport>) => {
        if (inFlight.current) return;
        inFlight.current = true;
        setRunning(true);
        setReport(undefined);
        setError('');
        setElapsed(0);
        try {
            const response = await request();
            if (mounted.current) setReport(response);
        } catch (failure) {
            if (mounted.current) setError(failure instanceof ApiError && failure.rejected
                ? `未发起执行：${failure.message}`
                : '未能收到最终结果，远端操作可能已经发生或仍在继续。请核查业务状态，不要直接重复执行。');
        } finally {
            inFlight.current = false;
            if (mounted.current) setRunning(false);
        }
    };
    const reject = (message: string) => {
        if (inFlight.current) return;
        setReport(undefined);
        setError(`未发起执行：${message}`);
        setElapsed(0);
    };
    return {report, error, running, elapsed, execute, reject};
}
