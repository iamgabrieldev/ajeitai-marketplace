"use client";

import { type ReactNode } from "react";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

interface BottomSheetProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  className?: string;
}

export function BottomSheet({
  open,
  onClose,
  title,
  children,
  className,
}: BottomSheetProps) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[60]">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50 transition-opacity"
        onClick={onClose}
      />
      {/* Sheet */}
      <div
        className={cn(
          "absolute bottom-0 left-0 right-0 max-h-[85vh] overflow-y-auto rounded-t-3xl bg-white p-6 pb-8 shadow-2xl dark:bg-surface-dark-alt",
          "animate-[slideUp_0.3s_ease-out]",
          className
        )}
      >
        {/* Handle */}
        <div className="mx-auto mb-4 h-1 w-10 rounded-full bg-secondary-200 dark:bg-secondary-700" />

        {title && (
          <div className="mb-4 flex items-center justify-between">
            <h3 className="text-lg font-bold text-text dark:text-text-dark">
              {title}
            </h3>
            <button
              onClick={onClose}
              className="rounded-full p-1.5 hover:bg-secondary-100 dark:hover:bg-secondary-800"
            >
              <X className="h-5 w-5 text-text-muted dark:text-text-dark-muted" />
            </button>
          </div>
        )}

        {children}
      </div>
    </div>
  );
}
