"use client";

import { type ReactNode } from "react";
import { BottomNav } from "@/components/layout/bottom-nav";

export default function PrestadorLayout({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-surface-alt dark:bg-surface-dark">
      <main className="mx-auto w-full max-w-lg flex-1 pb-20">{children}</main>
      <BottomNav role="prestador" />
    </div>
  );
}
