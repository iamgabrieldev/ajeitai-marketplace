export function LoadingScreen() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-white dark:bg-surface-dark">
      <div className="flex flex-col items-center gap-4">
        <div className="flex items-center gap-2">
          <div className="h-10 w-10 rounded-xl bg-primary flex items-center justify-center">
            <span className="text-xl font-bold text-white">A</span>
          </div>
          <span className="text-2xl font-bold text-text dark:text-text-dark">
            Ajeita<span className="text-primary">i</span>
          </span>
        </div>
        <div className="flex gap-1">
          <div className="h-2 w-2 animate-bounce rounded-full bg-primary [animation-delay:-0.3s]" />
          <div className="h-2 w-2 animate-bounce rounded-full bg-primary [animation-delay:-0.15s]" />
          <div className="h-2 w-2 animate-bounce rounded-full bg-primary" />
        </div>
        <p className="text-sm text-text-muted dark:text-text-dark-muted">
          Carregando...
        </p>
      </div>
    </div>
  );
}
