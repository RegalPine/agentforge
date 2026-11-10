import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "../../lib/utils"
import { InboxIcon } from "lucide-react"

const emptyVariants = cva(
  "flex flex-col items-center justify-center gap-4 py-12 px-6 text-center",
  {
    variants: {
      size: {
        sm: "py-8 gap-2",
        md: "py-12 gap-4",
        lg: "py-16 gap-6",
      },
    },
    defaultVariants: {
      size: "md",
    },
  }
)

const emptyImageVariants = cva("text-muted-foreground/40", {
  variants: {
    size: {
      sm: "w-12 h-12",
      md: "w-16 h-16",
      lg: "w-24 h-24",
    },
  },
  defaultVariants: {
    size: "md",
  },
})

const emptyDescriptionVariants = cva("text-muted-foreground", {
  variants: {
    size: {
      sm: "text-xs",
      md: "text-sm",
      lg: "text-base",
    },
  },
  defaultVariants: {
    size: "md",
  },
})

export interface EmptyProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof emptyVariants> {
  /**
   * 空状态图标或图片
   */
  image?: React.ReactNode
  /**
   * 描述文本
   */
  description?: React.ReactNode
  /**
   * 操作按钮或其他内容
   */
  action?: React.ReactNode
  /**
   * 是否显示默认图标
   * @default true
   */
  showDefaultImage?: boolean
}

export function Empty({
  image,
  description = "暂无数据",
  action,
  showDefaultImage = true,
  size,
  className,
  children,
  ...props
}: EmptyProps) {
  return (
    <div
      role="status"
      aria-label="空状态"
      className={cn(emptyVariants({ size }), className)}
      {...props}
    >
      {/* 图标/图片 */}
      {(image || showDefaultImage) && (
        <div className={cn(emptyImageVariants({ size }))}>
          {image || <InboxIcon className="w-full h-full" />}
        </div>
      )}

      {/* 描述文本 */}
      {description && (
        <p className={cn(emptyDescriptionVariants({ size }))}>
          {description}
        </p>
      )}

      {/* 自定义内容 */}
      {children}

      {/* 操作按钮 */}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}

Empty.displayName = "Empty"
