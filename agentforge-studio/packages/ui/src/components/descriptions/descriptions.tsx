import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "../../lib/utils"

const descriptionsVariants = cva("w-full", {
  variants: {
    bordered: {
      true: "border border-border rounded-lg overflow-hidden",
      false: "",
    },
    layout: {
      horizontal: "",
      vertical: "",
    },
  },
  defaultVariants: {
    bordered: false,
    layout: "horizontal",
  },
})

const descriptionsItemVariants = cva("", {
  variants: {
    bordered: {
      true: "border-b border-border last:border-b-0",
      false: "py-2",
    },
    layout: {
      horizontal: "grid grid-cols-[auto_1fr] gap-4",
      vertical: "flex flex-col gap-1",
    },
  },
  defaultVariants: {
    bordered: false,
    layout: "horizontal",
  },
})

const descriptionsLabelVariants = cva("font-medium", {
  variants: {
    bordered: {
      true: "bg-muted px-4 py-3",
      false: "text-muted-foreground",
    },
    layout: {
      horizontal: "",
      vertical: "text-sm",
    },
  },
  defaultVariants: {
    bordered: false,
    layout: "horizontal",
  },
})

const descriptionsContentVariants = cva("", {
  variants: {
    bordered: {
      true: "px-4 py-3",
      false: "",
    },
    layout: {
      horizontal: "",
      vertical: "",
    },
  },
  defaultVariants: {
    bordered: false,
    layout: "horizontal",
  },
})

interface DescriptionsContextValue {
  bordered?: boolean
  layout?: "horizontal" | "vertical"
  column?: number
}

const DescriptionsContext = React.createContext<DescriptionsContextValue>({})

export interface DescriptionsProps
  extends Omit<React.HTMLAttributes<HTMLDivElement>, "title">,
    VariantProps<typeof descriptionsVariants> {
  /**
   * 标题
   */
  title?: React.ReactNode
  /**
   * 额外内容
   */
  extra?: React.ReactNode
  /**
   * 列数
   */
  column?: number
}

export function Descriptions({
  title,
  extra,
  bordered,
  layout,
  column = 3,
  className,
  children,
  ...props
}: DescriptionsProps) {
  return (
    <DescriptionsContext.Provider value={{ bordered: bordered ?? undefined, layout: layout ?? undefined, column }}>
      <div className={cn("space-y-4", className)} {...props}>
        {/* 标题栏 */}
        {(title || extra) && (
          <div className="flex items-center justify-between">
            {title && (
              <h3 className="text-lg font-semibold">{title}</h3>
            )}
            {extra && <div>{extra}</div>}
          </div>
        )}

        {/* 描述列表 */}
        <div className={cn(descriptionsVariants({ bordered, layout }))}>
          {children}
        </div>
      </div>
    </DescriptionsContext.Provider>
  )
}

Descriptions.displayName = "Descriptions"

export interface DescriptionsItemProps
  extends React.HTMLAttributes<HTMLDivElement> {
  /**
   * 标签
   */
  label: React.ReactNode
  /**
   * 跨列数
   */
  span?: number
}

export function DescriptionsItem({
  label,
  span = 1,
  className,
  children,
  ...props
}: DescriptionsItemProps) {
  const { bordered, layout } = React.useContext(DescriptionsContext)

  return (
    <div
      className={cn(
        descriptionsItemVariants({ bordered, layout }),
        className
      )}
      style={{
        gridColumn: layout === "horizontal" && span > 1 ? `span ${span * 2}` : undefined,
      }}
      {...props}
    >
      <div className={cn(descriptionsLabelVariants({ bordered, layout }))}>
        {label}
      </div>
      <div className={cn(descriptionsContentVariants({ bordered, layout }))}>
        {children}
      </div>
    </div>
  )
}

DescriptionsItem.displayName = "DescriptionsItem"
