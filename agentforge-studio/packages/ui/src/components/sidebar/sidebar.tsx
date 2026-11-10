import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "../../lib/utils"
import { ChevronRight } from "lucide-react"

const sidebarVariants = cva(
  "flex h-full flex-col border-r border-outline-variant bg-surface-container-low shadow-none transition-all duration-normal ease-mac",
  {
    variants: {
      collapsed: {
        true: "w-16",
        false: "w-[280px]",
      },
    },
    defaultVariants: {
      collapsed: false,
    },
  }
)

export interface SidebarProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof sidebarVariants> {
  collapsible?: boolean
  defaultCollapsed?: boolean
  onCollapsedChange?: (collapsed: boolean) => void
}

interface SidebarContextValue {
  collapsed: boolean
  setCollapsed: (collapsed: boolean) => void
}

const SidebarContext = React.createContext<SidebarContextValue | undefined>(undefined)

const useSidebar = () => {
  const context = React.useContext(SidebarContext)
  if (!context) {
    throw new Error("Sidebar components must be used within Sidebar")
  }
  return context
}

const Sidebar = React.forwardRef<HTMLDivElement, SidebarProps>(
  (
    {
      className,
      collapsible = false,
      defaultCollapsed = false,
      onCollapsedChange,
      children,
      ...props
    },
    ref
  ) => {
    const [collapsed, setCollapsed] = React.useState(defaultCollapsed)

    const handleCollapsedChange = (value: boolean) => {
      setCollapsed(value)
      onCollapsedChange?.(value)
    }

    return (
      <SidebarContext.Provider value={{ collapsed, setCollapsed: handleCollapsedChange }}>
        <aside
          ref={ref}
          className={cn(sidebarVariants({ collapsed, className }))}
          {...props}
        >
          {children}
        </aside>
      </SidebarContext.Provider>
    )
  }
)

Sidebar.displayName = "Sidebar"

const SidebarHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => {
  return (
    <div
      ref={ref}
      className={cn("flex h-11 items-center border-b border-outline-variant px-4", className)}
      {...props}
    />
  )
})

SidebarHeader.displayName = "SidebarHeader"

const SidebarContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => {
  return (
    <div
      ref={ref}
      className={cn("flex-1 overflow-y-auto py-4", className)}
      {...props}
    />
  )
})

SidebarContent.displayName = "SidebarContent"

const SidebarFooter = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => {
  return (
    <div
      ref={ref}
      className={cn("border-t p-4", className)}
      {...props}
    />
  )
})

SidebarFooter.displayName = "SidebarFooter"

const SidebarSection = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & { title?: string }
>(({ className, title, children, ...props }, ref) => {
  const { collapsed } = useSidebar()

  return (
    <div ref={ref} className={cn("px-3 py-2", className)} {...props}>
      {title && !collapsed && (
        <h4 className="mb-2 px-3 text-label-sm text-outline uppercase tracking-wider">
          {title}
        </h4>
      )}
      <div className="space-y-1">{children}</div>
    </div>
  )
})

SidebarSection.displayName = "SidebarSection"

interface SidebarItemProps extends React.AnchorHTMLAttributes<HTMLAnchorElement> {
  icon?: React.ReactNode
  active?: boolean
  badge?: React.ReactNode
}

const SidebarItem = React.forwardRef<HTMLAnchorElement, SidebarItemProps>(
  ({ className, icon, active, badge, children, ...props }, ref) => {
    const { collapsed } = useSidebar()

    return (
      <a
        ref={ref}
        className={cn(
          "flex items-center gap-3 rounded px-3 py-1.5 text-sm font-medium",
          "hover:bg-surface-container transition-colors duration-fast",
          active && "bg-surface-container-high text-primary",
          !active && "text-outline hover:text-on-surface",
          collapsed && "justify-center",
          className
        )}
        {...props}
      >
        {icon && <span className="shrink-0">{icon}</span>}
        {!collapsed && (
          <>
            <span className="flex-1">{children}</span>
            {badge && <span className="shrink-0">{badge}</span>}
          </>
        )}
      </a>
    )
  }
)

SidebarItem.displayName = "SidebarItem"

interface SidebarCollapseProps extends React.HTMLAttributes<HTMLDivElement> {
  icon?: React.ReactNode
  label: string
  defaultOpen?: boolean
}

const SidebarCollapse = React.forwardRef<HTMLDivElement, SidebarCollapseProps>(
  ({ className, icon, label, defaultOpen = false, children, ...props }, ref) => {
    const { collapsed: sidebarCollapsed } = useSidebar()
    const [open, setOpen] = React.useState(defaultOpen)

    if (sidebarCollapsed) {
      return (
        <SidebarItem icon={icon}>
          {label}
        </SidebarItem>
      )
    }

    return (
      <div ref={ref} {...props}>
        <button
          type="button"
          onClick={() => setOpen(!open)}
          className={cn(
            "flex w-full items-center gap-3 rounded px-3 py-1.5 text-sm font-medium",
            "text-outline hover:text-on-surface hover:bg-surface-container",
            "transition-colors duration-fast",
            className
          )}
        >
          {icon && <span className="shrink-0">{icon}</span>}
          <span className="flex-1 text-left">{label}</span>
          <ChevronRight
            className={cn(
              "h-4 w-4 shrink-0 transition-transform duration-fast",
              open && "rotate-90"
            )}
          />
        </button>
        {open && (
          <div className="pl-6 space-y-1 mt-1">
            {children}
          </div>
        )}
      </div>
    )
  }
)

SidebarCollapse.displayName = "SidebarCollapse"

export {
  Sidebar,
  SidebarHeader,
  SidebarContent,
  SidebarFooter,
  SidebarSection,
  SidebarItem,
  SidebarCollapse,
  sidebarVariants,
}
