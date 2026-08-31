import {type ReactNode, useEffect, useRef, useState} from 'react';
import './ParameterScrollArea.css';

export function scrollEdges({
                                scrollTop,
                                scrollHeight,
                                clientHeight
                            }: Pick<HTMLElement, 'scrollTop' | 'scrollHeight' | 'clientHeight'>) {
    const overflowing = clientHeight > 0 && scrollHeight > clientHeight + 1;
    return {
        above: overflowing && scrollTop > 1,
        below: overflowing && scrollHeight - clientHeight - Math.max(0, scrollTop) > 1,
    };
}

interface Props {
    children: ReactNode;
    label: string;
    itemCount: number;
    view: string;
}

/** 根据真实溢出提示后续内容，覆盖窗口缩放、表单增删和多行输入框调整。 */
export default function ParameterScrollArea({children, label, itemCount, view}: Props) {
    const viewport = useRef<HTMLDivElement>(null);
    const content = useRef<HTMLDivElement>(null);
    const [edges, setEdges] = useState({above: false, below: false});

    const measure = () => {
        if (!viewport.current) return;
        const next = scrollEdges(viewport.current);
        setEdges((previous) => previous.above === next.above && previous.below === next.below ? previous : next);
    };

    useEffect(() => {
        const element = viewport.current;
        const inner = content.current;
        if (!element || !inner) return;
        element.scrollTop = 0;
        measure();
        const observer = new ResizeObserver(measure);
        observer.observe(element);
        observer.observe(inner);
        return () => observer.disconnect();
    }, [view]);

    return <div className="parameter-scroll-area">
        <div ref={viewport} className="parameter-scroll-viewport" role="region" aria-label={label}
             tabIndex={0} onScroll={measure}>
            <div ref={content}>{children}</div>
        </div>
        <div className="parameter-scroll-status">
            <span>共 {itemCount} 个参数{edges.above && ' · ↑ 上方还有内容'}</span>
            {edges.below ? <button type="button" onClick={() => {
                const element = viewport.current;
                if (element) element.scrollBy({top: Math.max(80, element.clientHeight * 0.8)});
            }}>下方还有内容 ↓</button> : <small>{edges.above ? '已到末尾' : '已全部显示'}</small>}
        </div>
    </div>;
}
