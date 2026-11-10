import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "../../lib/utils";

type TypographyVariant =
  | "h1"
  | "h2"
  | "h3"
  | "h4"
  | "h5"
  | "h6"
  | "body1"
  | "body2"
  | "caption"
  | "overline";

const typographyVariants = cva("", {
  variants: {
    variant: {
      h1: "text-4xl font-bold tracking-tight",
      h2: "text-3xl font-semibold tracking-tight",
      h3: "text-2xl font-semibold",
      h4: "text-xl font-semibold",
      h5: "text-lg font-medium",
      h6: "text-base font-medium",
      body1: "text-base",
      body2: "text-sm",
      caption: "text-xs text-outline",
      overline: "text-xs uppercase tracking-wider text-outline",
    },
    color: {
      default: "text-on-surface",
      muted: "text-outline",
      primary: "text-primary",
      destructive: "text-error",
    },
    align: {
      left: "text-left",
      center: "text-center",
      right: "text-right",
      justify: "text-justify",
    },
  },
  defaultVariants: {
    variant: "body1",
    color: "default",
    align: "left",
  },
});

// 变体到默认元素的映射
const variantElementMap: Record<TypographyVariant, React.ElementType> = {
  h1: "h1",
  h2: "h2",
  h3: "h3",
  h4: "h4",
  h5: "h5",
  h6: "h6",
  body1: "p",
  body2: "p",
  caption: "span",
  overline: "span",
};

interface TypographyProps<T extends React.ElementType = "p">
  extends VariantProps<typeof typographyVariants> {
  /** 渲染的 HTML 元素 */
  component?: T;
  /** 是否不换行并显示省略号 */
  noWrap?: boolean;
  /** 是否添加底部间距 */
  gutterBottom?: boolean;
  /** 子元素 */
  children: React.ReactNode;
  /** 自定义类名 */
  className?: string;
}

const Typography = <T extends React.ElementType = "p">({
  variant = "body1",
  color = "default",
  align = "left",
  component,
  noWrap = false,
  gutterBottom = false,
  children,
  className,
  ...props
}: TypographyProps<T> & Omit<React.ComponentPropsWithoutRef<T>, keyof TypographyProps<T>>) => {
  // 确定要渲染的元素
  const Component = component || variantElementMap[variant as TypographyVariant] || "p";

  return (
    <Component
      className={cn(
        typographyVariants({ variant, color, align }),
        noWrap && "truncate overflow-hidden whitespace-nowrap",
        gutterBottom && "mb-4",
        className
      )}
      {...props}
    >
      {children}
    </Component>
  );
};
Typography.displayName = "Typography";

export { Typography, typographyVariants, type TypographyProps, type TypographyVariant };
