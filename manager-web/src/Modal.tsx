import type {ReactNode} from 'react';

interface ModalProps {
    title: string;
    children: ReactNode;
    footer?: ReactNode;
    wide?: boolean;
    onClose: () => void;
}

export default function Modal({title, children, footer, wide, onClose}: ModalProps) {
    return (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
            if (event.target === event.currentTarget) onClose();
        }}>
            <section className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
                <header className="modal-header">
                    <h2>{title}</h2>
                    <button className="icon-button" type="button" aria-label="关闭" onClick={onClose}>×</button>
                </header>
                <div className="modal-body">{children}</div>
                {footer && <footer className="modal-footer">{footer}</footer>}
            </section>
        </div>
    );
}
