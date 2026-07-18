import { cn } from '../utils/cn';

interface SidebarProps {
  title: string;
  children: React.ReactNode;
  className?: string;
}

export function Sidebar({ title, children, className }: SidebarProps) {
  return (
    <aside className={cn('flex h-full w-64 flex-col border-r border-gray-200 bg-white', className)}>
      <div className="border-b border-gray-100 px-4 py-4">
        <h1 className="text-lg font-bold text-gray-900">{title}</h1>
      </div>
      <nav className="flex-1 space-y-1 px-3 py-4">{children}</nav>
    </aside>
  );
}

interface SidebarItemProps {
  label: string;
  icon?: React.ReactNode;
  active?: boolean;
  onClick?: () => void;
  href?: string;
}

export function SidebarItem({ label, icon, active, onClick, href }: SidebarItemProps) {
  const classes = cn(
    'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
    active ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
  );

  if (href) {
    return (
      <a href={href} className={classes}>
        {icon}
        {label}
      </a>
    );
  }

  return (
    <button onClick={onClick} className={cn(classes, 'w-full text-left')}>
      {icon}
      {label}
    </button>
  );
}
