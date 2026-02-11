"use client";

import { type ReactNode } from "react";
import { ArrowLeft } from "lucide-react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/utils";

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  back?: boolean;
  actions?: ReactNode;
  className?: string;
}

export function PageHeader({
  title,
  subtitle,
  back = false,
  actions,
  className,
}: PageHeaderProps) {
  const router = useRouter();

  return (
    <header
      className={cn(
        "sticky top-0 z-40 flex items-center gap-3 bg-white/95 px-4 py-3 backdrop-blur-md dark:bg-surface-dark/95",
        className
      )}
    >
      {back && (
        <button
          onClick={() => router.back()}
          className="flex h-9 w-9 items-center justify-center rounded-full text-text hover:bg-secondary-100 dark:text-text-dark dark:hover:bg-secondary-800"
          aria-label="Voltar"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
      )}
      <div className="flex-1 min-w-0">
        <h1 className="truncate text-lg font-bold text-text dark:text-text-dark">
          {title}
        </h1>
        {subtitle && (
          <p className="truncate text-sm text-text-muted dark:text-text-dark-muted">
            {subtitle}
          </p>
        )}
      </div>
      {actions && <div className="flex items-center gap-2">{actions}</div>}
    </header>
  );
}
