export function trim(text: string): string {
    if (text && text.length > 0) {
        return text.trim();
    }
    return text;
}
