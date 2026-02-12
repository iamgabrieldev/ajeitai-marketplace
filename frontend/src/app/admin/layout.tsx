"use client";

import { type ReactNode, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/providers/auth-provider";
import { LayoutDashboard, Users, Calendar, CreditCard } from "lucide-react";

export default function AdminLayout({ children }: { children: ReactNode }) {
  const { initialized, authenticated, hasRole } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!initialized) return;
    if (!authenticated || !hasRole("admin")) {
      router.replace("/");
      return;
    }
  }, [initialized, authenticated, hasRole, router]);

  if (!initialized || !authenticated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-text-muted dark:text-text-dark-muted">Carregando...</p>
      </div>
    );
  }

  if (!hasRole("admin")) {
    return null;
  }

  const nav = [
    { href: "/admin", label: "Visão geral", icon: LayoutDashboard },
    { href: "/admin/prestadores", label: "Prestadores", icon: Users },
    { href: "/admin/agendamentos", label: "Agendamentos", icon: Calendar },
    { href: "/admin/pagamentos", label: "Pagamentos e saques", icon: CreditCard },
  ];

  return (
    <div className="flex min-h-screen flex-col bg-surface-alt dark:bg-surface-dark">
      <header className="sticky top-0 z-10 border-b border-secondary-200 bg-white dark:border-secondary-700 dark:bg-surface-dark">
        <div className="mx-auto flex h-14 max-w-4xl items-center justify-between px-4">
          <Link href="/admin" className="font-semibold text-text dark:text-text-dark">
            Painel Admin
          </Link>
          <nav className="flex gap-2">
            {nav.map(({ href, label, icon: Icon }) => (
              <Link
                key={href}
                href={href}
                className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm ${
                  pathname === href
                    ? "bg-primary text-white"
                    : "text-text-muted hover:bg-secondary-100 dark:text-text-dark-muted dark:hover:bg-secondary-800"
                }`}
              >
                <Icon className="h-4 w-4" />
                {label}
              </Link>
            ))}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-6">{children}</main>
    </div>
  );
}
