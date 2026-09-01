import {useEffect, useState} from 'react';

const NAVIGATION_EVENT = 'maintain:navigation';
const adminPath = (path: string) => path === '/admin' || path.startsWith('/admin/');

export function sameFrontendSurface(currentPath: string, targetPath: string) {
    return adminPath(currentPath) === adminPath(targetPath);
}

export function navigate(href: string, replace = false) {
    if (replace) window.history.replaceState({}, '', href);
    else window.history.pushState({}, '', href);
    window.dispatchEvent(new Event(NAVIGATION_EVENT));
}

export function subscribeNavigation(listener: () => void) {
    window.addEventListener('popstate', listener);
    window.addEventListener(NAVIGATION_EVENT, listener);
    return () => {
        window.removeEventListener('popstate', listener);
        window.removeEventListener(NAVIGATION_EVENT, listener);
    };
}

export function useNavigationPath() {
    const [path, setPath] = useState(() => window.location.pathname);
    useEffect(() => subscribeNavigation(() => setPath(window.location.pathname)), []);
    useEffect(() => {
        const followInternalLink = (event: MouseEvent) => {
            if (event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            const anchor = event.target instanceof Element ? event.target.closest('a') : null;
            if (!(anchor instanceof HTMLAnchorElement) || anchor.origin !== window.location.origin || anchor.target || anchor.download) return;
            if (!sameFrontendSurface(window.location.pathname, anchor.pathname)) return;
            event.preventDefault();
            navigate(anchor.pathname + anchor.search + anchor.hash);
        };
        document.addEventListener('click', followInternalLink);
        return () => document.removeEventListener('click', followInternalLink);
    }, []);
    return path;
}
