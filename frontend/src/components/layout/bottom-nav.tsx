"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Home,
  Calendar,
  User,
  Search,
  LayoutDashboard,
  Clock,
  FileText,
  CalendarCheck,
  MessageCircle,
  type LucideIcon,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

const clienteNav: NavItem[] = [
  { href: "/cliente/home", label: "Início", icon: Home },
  { href: "/cliente/home?search=true", label: "Buscar", icon: Search },
  { href: "/cliente/agendamentos", label: "Agenda", icon: Calendar },
  { href: "/cliente/conversas", label: "Conversas", icon: MessageCircle },
  { href: "/cliente/perfil", label: "Perfil", icon: User },
];

const prestadorNav: NavItem[] = [
  { href: "/prestador/dashboard", label: "Painel", icon: LayoutDashboard },
  { href: "/prestador/solicitacoes", label: "Pedidos", icon: CalendarCheck },
  { href: "/prestador/conversas", label: "Conversas", icon: MessageCircle },
  { href: "/prestador/disponibilidade", label: "Horários", icon: Clock },
  { href: "/prestador/documentos", label: "Docs", icon: FileText },
  { href: "/prestador/perfil", label: "Perfil", icon: User },
];

interface BottomNavProps {
  role: "cliente" | "prestador";
}

export function BottomNav({ role }: BottomNavProps) {
  const pathname = usePathname();
  const items = role === "cliente" ? clienteNav : prestadorNav;
  const basePath = role === "cliente" ? "" : "";

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 border-t border-secondary-100 bg-white/95 backdrop-blur-md safe-bottom dark:border-secondary-800 dark:bg-surface-dark/95">
      <div className="mx-auto flex max-w-lg items-center justify-around px-2 py-1">
        {items.map((item) => {
          const href = `${basePath}${item.href}`;
          const isActive =
            pathname === href || pathname.startsWith(href.split("?")[0] + "/");

          return (
            <Link
              key={item.href}
              href={href}
              className={cn(
                "flex flex-col items-center gap-0.5 rounded-lg px-3 py-2 text-xs font-medium transition-colors",
                isActive
                  ? "text-primary dark:text-primary-400"
                  : "text-text-muted dark:text-text-dark-muted"
              )}
            >
              <item.icon
                className={cn(
                  "h-5 w-5",
                  isActive && "scale-110 transition-transform"
                )}
              />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
