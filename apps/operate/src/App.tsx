import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { ProcessDefinitionsPage } from './pages/ProcessDefinitionsPage';
import { ProcessInstancesPage } from './pages/ProcessInstancesPage';
import { IncidentsPage } from './pages/IncidentsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5000,
      retry: 1,
    },
  },
});

function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const navItems = [
    { path: '/', label: 'Process Definitions' },
    { path: '/instances', label: 'Process Instances' },
    { path: '/incidents', label: 'Incidents' },
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className="flex w-64 flex-col border-r border-gray-200 bg-white">
        <div className="border-b border-gray-100 px-4 py-4">
          <h1 className="text-lg font-bold text-gray-900">OOP Operate</h1>
          <p className="text-xs text-gray-500">Process Monitoring</p>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`w-full rounded-md px-3 py-2 text-left text-sm font-medium transition-colors ${
                location.pathname === item.path
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>
        <div className="border-t border-gray-100 px-4 py-3">
          <p className="text-xs text-gray-400">v0.1.0-alpha</p>
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-6">
        <Routes>
          <Route path="/" element={<ProcessDefinitionsPage />} />
          <Route path="/instances" element={<ProcessInstancesPage />} />
          <Route path="/incidents" element={<IncidentsPage />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppLayout />
      </BrowserRouter>
    </QueryClientProvider>
  );
}
