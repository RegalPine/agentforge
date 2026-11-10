import * as React from "react"
import { cva } from "class-variance-authority"
import { cn } from "../../lib/utils"

const timelineVariants = cva("flex", {
  variants: {
    orientation: {
      vertical: "flex-col",
      horizontal: "flex-row",
    },
  },
  defaultVariants: {
    orientation: "vertical",
  },
})

export interface TimelineProps
  extends React.HTMLAttributes<HTMLDivElement> {
  orientation?: "vertical" | "horizontal"
}

interface TimelineContextValue {
  orientation: "vertical" | "horizontal"
}

const TimelineContext = React.createContext<TimelineContextValue>({
  orientation: "vertical",
})

const useTimeline = () => React.useContext(TimelineContext)

const Timeline = React.forwardRef<HTMLDivElement, TimelineProps>(
  ({ className, orientation = "vertical", ...props }, ref) => {
    return (
      <TimelineContext.Provider value={{ orientation }}>
        <div
          ref={ref}
          className={cn(timelineVariants({ orientation, className }))}
          {...props}
        />
      </TimelineContext.Provider>
    )
  }
)

Timeline.displayName = "Timeline"

const TimelineItem = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => {
  const { orientation } = useTimeline()

  return (
    <div
      ref={ref}
      className={cn(
        "relative flex",
        orientation === "vertical" ? "flex-row gap-4" : "flex-col gap-2",
        className
      )}
      {...props}
    />
  )
})

TimelineItem.displayName = "TimelineItem"

interface TimelinePointProps extends React.HTMLAttributes<HTMLDivElement> {
  icon?: React.ReactNode
  status?: "default" | "success" | "warning" | "error"
}

const TimelinePoint = React.forwardRef<HTMLDivElement, TimelinePointProps>(
  ({ className, icon, status = "default", ...props }, ref) => {
    const statusColors = {
      default: "bg-primary border-primary",
      success: "bg-green-500 border-green-500",
      warning: "bg-yellow-500 border-yellow-500",
      error: "bg-red-500 border-red-500",
    }

    return (
      <div
        ref={ref}
        className={cn(
          "relative z-10 flex h-3 w-3 shrink-0 items-center justify-center rounded-full border-2 border-background shadow-sm",
          statusColors[status],
          icon && "h-8 w-8",
          className
        )}
        {...props}
      >
        {icon}
      </div>
    )
  }
)

TimelinePoint.displayName = "TimelinePoint"

const TimelineConnector = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => {
  const { orientation } = useTimeline()

  return (
    <div
      ref={ref}
      className={cn(
        "absolute bg-border",
        orientation === "vertical"
          ? "left-[5px] top-8 h-full w-px"
          : "top-[5px] left-8 w-full h-px",
        className
      )}
      {...props}
    />
  )
})

TimelineConnector.displayName = "TimelineConnector"

const TimelineContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => {
  return (
    <div
      ref={ref}
      className={cn("flex-1 space-y-1 pb-8", className)}
      {...props}
    />
  )
})

TimelineContent.displayName = "TimelineContent"

const TimelineTime = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => {
  return (
    <p
      ref={ref}
      className={cn("text-sm text-muted-foreground", className)}
      {...props}
    />
  )
})

TimelineTime.displayName = "TimelineTime"

const TimelineTitle = React.forwardRef<
  HTMLHeadingElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => {
  return (
    <h3
      ref={ref}
      className={cn("font-semibold leading-none", className)}
      {...props}
    />
  )
})

TimelineTitle.displayName = "TimelineTitle"

const TimelineDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => {
  return (
    <p
      ref={ref}
      className={cn("text-sm text-muted-foreground", className)}
      {...props}
    />
  )
})

TimelineDescription.displayName = "TimelineDescription"

export {
  Timeline,
  TimelineItem,
  TimelinePoint,
  TimelineConnector,
  TimelineContent,
  TimelineTime,
  TimelineTitle,
  TimelineDescription,
  timelineVariants,
}
