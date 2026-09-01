const NAVIGATION_EVENT = 'maintain:navigation';

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
