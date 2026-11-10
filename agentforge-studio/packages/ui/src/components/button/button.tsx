import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";
import { Spinner } from "../spinner";

const buttonVariants = cva(
  [
    "inline-flex items-center justify-center gap-2",
    "font-medium text-sm",
    "rounded",
    "transition-all duration-fast ease-mac",
    "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-accent-primary focus-visible:ring-offset-2 focus-visible:ring-offset-surface",
    "disabled:pointer-events-none disabled:opacity-50",
    "active:scale-[0.98]",
  ],
  {
    variants: {
      variant: {
        default: [
          "bg-accent-primary text-on-primary",
          "hover:brightness-110",
          "active:brightness-95",
        ],
        secondary: [
          "bg-surface-container text-on-surface",
          "border border-outline-variant",
          "hover:bg-surface-container-high",
        ],
        destructive: [
          "bg-error text-white",
          "hover:bg-error/90",
        ],
        ghost: [
          "hover:bg-surface-container",
          "hover:text-on-surface",
        ],
        link: [
          "text-primary underline-offset-4",
          "hover:underline",
        ],
      },
      size: {
        sm: "h-8 px-3 text-xs",
        md: "h-9 px-4 text-sm",
        lg: "h-10 px-6 text-base",
        icon: "h-9 w-9",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "md",
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
  loading?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, loading, children, disabled, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";
    
    // 当使用 asChild 时，不能添加额外的子元素（如 Spinner）
    // 因为 Slot 只接受单个 React 元素
    if (asChild && loading) {
      console.warn("Button: asChild 模式下不支持 loading 状态");
    }
    
    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        disabled={disabled || loading}
        aria-busy={loading || undefined}
        aria-disabled={disabled || loading || undefined}
        {...props}
      >
        {asChild ? (
          children
        ) : (
          <>
            {loading && <Spinner size="sm" />}
            {children}
          </>
        )}
      </Comp>
    );
  }
);

Button.displayName = "Button";

export { Button, buttonVariants };
