"use client";

import { forwardRef, type TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ label, error, className, id, ...props }, ref) => {
    const textareaId = id || label?.toLowerCase().replace(/\s+/g, "-");

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={textareaId}
            className="mb-1.5 block text-sm font-medium text-text dark:text-text-dark"
          >
            {label}
          </label>
        )}
        <textarea
          ref={ref}
          id={textareaId}
          className={cn(
            "w-full rounded-xl border bg-white px-4 py-3 text-base text-text",
            "transition-colors duration-200",
            "placeholder:text-text-muted/60",
            "focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20",
            "dark:border-secondary-700 dark:bg-surface-dark dark:text-text-dark dark:placeholder:text-text-dark-muted/60",
            "dark:focus:border-primary-500 dark:focus:ring-primary-500/20",
            error
              ? "border-danger focus:border-danger focus:ring-danger/20"
              : "border-secondary-200",
            className
          )}
          {...props}
        />
        {error && <p className="mt-1 text-sm text-danger">{error}</p>}
      </div>
    );
  }
);

Textarea.displayName = "Textarea";
