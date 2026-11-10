import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";

const textareaVariants = cva(
  [
    "flex min-h-[80px] w-full rounded-md border bg-background px-3 py-2",
    "text-sm text-foreground",
    "placeholder:text-muted-foreground",
    "transition-colors duration-fast ease-mac",
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/20 focus-visible:border-primary",
    "disabled:cursor-not-allowed disabled:opacity-50",
    "resize-y",
  ],
  {
    variants: {
      error: {
        true: "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
        false: "border-border",
      },
    },
    defaultVariants: {
      error: false,
    },
  }
);

export interface TextareaProps
  extends React.TextareaHTMLAttributes<HTMLTextAreaElement>,
    VariantProps<typeof textareaVariants> {}

const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, error, ...props }, ref) => {
    return (
      <textarea
        className={cn(textareaVariants({ error, className }))}
        ref={ref}
        {...props}
      />
    );
  }
);

Textarea.displayName = "Textarea";

export { Textarea, textareaVariants };
