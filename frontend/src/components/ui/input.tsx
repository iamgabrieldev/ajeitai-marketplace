"use client";

import { forwardRef, type InputHTMLAttributes, type ReactNode } from "react";
import { cn } from "@/lib/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  icon?: ReactNode;
  endIcon?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, icon, endIcon, className, id, ...props }, ref) => {
    const inputId = id || label?.toLowerCase().replace(/\s+/g, "-");

    return (
      <div className="w-full">
        {label && (
          <label
            htmlFor={inputId}
            className="mb-1.5 block text-sm font-medium text-text dark:text-text-dark"
          >
            {label}
          </label>
        )}
        <div className="relative">
          {icon && (
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-text-muted dark:text-text-dark-muted">
              {icon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            className={cn(
              "h-11 w-full rounded-xl border bg-white px-4 text-base text-text",
              "transition-colors duration-200",
              "placeholder:text-text-muted/60",
              "focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20",
              "dark:border-secondary-700 dark:bg-surface-dark dark:text-text-dark dark:placeholder:text-text-dark-muted/60",
              "dark:focus:border-primary-500 dark:focus:ring-primary-500/20",
              icon && "pl-10",
              endIcon && "pr-10",
              error
                ? "border-danger focus:border-danger focus:ring-danger/20"
                : "border-secondary-200",
              className
            )}
            {...props}
          />
          {endIcon && (
            <span className="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted dark:text-text-dark-muted">
              {endIcon}
            </span>
          )}
        </div>
        {error && (
          <p className="mt-1 text-sm text-danger">{error}</p>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";
