import type { PropsWithChildren, ReactNode } from 'react';

interface WorkbenchShellProps extends PropsWithChildren {
  sidebar: ReactNode;
}

export const WorkbenchShell = ({ sidebar, children }: WorkbenchShellProps) => (
  <section className="workbench-shell">
    <aside className="workbench-shell__sidebar">{sidebar}</aside>
    <main className="workbench-shell__main">{children}</main>
  </section>
);
