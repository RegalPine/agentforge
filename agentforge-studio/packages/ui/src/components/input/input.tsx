import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";

const inputVariants = cva(
  [
    "flex h-7 w-full rounded border bg-surface px-2 py-1",
    "text-sm text-on-surface font-mono",
    "placeholder:text-outline",
    "transition-colors duration-fast ease-mac",
    "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent-primary focus-visible:border-accent-primary",
    "disabled:cursor-not-allowed disabled:opacity-50",
    "file:border-0 file:bg-transparent file:text-sm file:font-medium",
  ],
  {
    variants: {
      error: {
        true: "border-error focus-visible:ring-error/20 focus-visible:border-error",
        false: "border-outline-variant",
      },
    },
    defaultVariants: {
      error: false,
    },
  }
);

export interface InputProps
  extends React.InputHTMLAttributes<HTMLInputElement>,
    VariantProps<typeof inputVariants> {}

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, type, error, ...props }, ref) => {
    return (
      <input
        type={type}
        className={cn(inputVariants({ error, className }))}
        ref={ref}
        {...props}
      />
    );
  }
);

Input.displayName = "Input";

export { Input, inputVariants };
