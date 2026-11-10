import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded px-2 py-0.5 text-xs font-medium transition-colors",
  {
    variants: {
      variant: {
        default: "bg-accent-primary text-on-primary",
        secondary: "bg-secondary/20 text-secondary",
        tertiary: "bg-tertiary/20 text-tertiary",
        tool: "bg-accent-tool/20 text-accent-tool",
        destructive: "bg-error text-white",
        outline: "border border-outline-variant text-on-surface",
        success: "bg-success/20 text-success",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
