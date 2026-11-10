import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import { ExternalLink } from "lucide-react";
import { cn } from "../../lib/utils";

const linkVariants = cva(
  [
    "inline-flex items-center gap-1",
    "transition-colors duration-fast",
    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
    "rounded-sm",
  ],
  {
    variants: {
      variant: {
        default: "text-primary hover:text-primary/80",
        muted: "text-muted-foreground hover:text-foreground",
        destructive: "text-destructive hover:text-destructive/80",
      },
      underline: {
        hover: "hover:underline",
        always: "underline",
        none: "no-underline",
      },
      disabled: {
        true: "opacity-50 pointer-events-none cursor-not-allowed",
        false: "",
      },
    },
    defaultVariants: {
      variant: "default",
      underline: "hover",
      disabled: false,
    },
  }
);

export interface LinkProps
  extends React.AnchorHTMLAttributes<HTMLAnchorElement>,
    VariantProps<typeof linkVariants> {
  /**
   * 是否渲染为子元素
   * 当为 true 时，Link 将其样式应用到子元素上
   */
  asChild?: boolean;
  /**
   * 是否为外部链接
   * 当为 true 时，自动添加 target="_blank" 和 rel="noopener noreferrer"
   */
  external?: boolean;
  /**
   * 是否显示外部链接图标
   * 仅在 external=true 时有效
   */
  showExternalIcon?: boolean;
  /**
   * 是否禁用
   * 禁用时不响应任何交互
   */
  disabled?: boolean;
}

const Link = React.forwardRef<HTMLAnchorElement, LinkProps>(
  (
    {
      className,
      variant,
      underline,
      asChild = false,
      external = false,
      showExternalIcon = true,
      disabled = false,
      children,
      href,
      target,
      rel,
      onClick,
      ...props
    },
    ref
  ) => {
    const Comp = asChild ? Slot : "a";

    // 处理外部链接属性
    const externalProps = external
      ? {
          target: target || "_blank",
          rel: rel || "noopener noreferrer",
        }
      : {
          target,
          rel,
        };

    // 处理点击事件
    const handleClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
      if (disabled) {
        event.preventDefault();
        return;
      }
      if (onClick) {
        onClick(event);
      }
    };

    // 如果使用 asChild 且需要显示外部链接图标,发出警告
    React.useEffect(() => {
      if (
        typeof process !== "undefined" &&
        process.env?.NODE_ENV === "development" &&
        asChild &&
        external &&
        showExternalIcon
      ) {
        console.warn(
          "Link: asChild 模式下不支持自动添加外部链接图标。请在子元素中手动添加图标。"
        );
      }
    }, [asChild, external, showExternalIcon]);

    const content = (
      <>
        {children}
        {external && showExternalIcon && !asChild && (
          <ExternalLink size={14} className="inline-block" aria-hidden="true" />
        )}
      </>
    );

    return (
      <Comp
        ref={ref}
        className={cn(
          linkVariants({
            variant,
            underline,
            disabled,
          }),
          className
        )}
        href={disabled ? undefined : href}
        onClick={handleClick}
        aria-disabled={disabled ? "true" : undefined}
        tabIndex={disabled ? -1 : undefined}
        {...externalProps}
        {...props}
      >
        {asChild ? children : content}
      </Comp>
    );
  }
);

Link.displayName = "Link";

export { Link, linkVariants };
