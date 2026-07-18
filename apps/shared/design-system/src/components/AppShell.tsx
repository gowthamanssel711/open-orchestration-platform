import { cn } from '../utils/cn';

interface AppShellProps {
  sidebar: React.ReactNode;
  header?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

export function AppShell({ sidebar, header, children, className }: AppShellProps) {
  return (
    <div className={cn('flex h-screen bg-gray-50', className)}>
      {sidebar}
      <div className="flex flex-1 flex-col overflow-hidden">
        {header && (
          <header className="border-b border-gray-200 bg-white px-6 py-3">
            {header}
          </header>
        )}
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  );
}
