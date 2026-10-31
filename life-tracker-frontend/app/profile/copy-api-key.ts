export async function copyApiKey(token: string, input: HTMLInputElement): Promise<boolean> {
  if (navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(token);
      return true;
    } catch {
      // Use the selected field when clipboard access is denied.
    }
  }

  input.select();
  try {
    return document.execCommand("copy");
  } catch {
    return false;
  }
}
