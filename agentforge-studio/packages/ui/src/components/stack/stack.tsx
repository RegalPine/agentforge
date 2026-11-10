import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "../../lib/utils"

const stackVariants = cva("flex", {
  variants: {
    direction: {
      vertical: "flex-col",
      horizontal: "flex-row",
    },
    spacing: {
      none: "gap-0",
      xs: "gap-1",
      sm: "gap-2",
      md: "gap-4",
      lg: "gap-6",
      xl: "gap-8",
    },
    align: {
      start: "items-start",
      center: "items-center",
      end: "items-end",
      stretch: "items-stretch",
      baseline: "items-baseline",
    },
    justify: {
      start: "justify-start",
      center: "justify-center",
      end: "justify-end",
      between: "justify-between",
      around: "justify-around",
      evenly: "justify-evenly",
    },
  },
  defaultVariants: {
    direction: "vertical",
    spacing: "md",
    align: "stretch",
    justify: "start",
  },
})

export interface StackProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof stackVariants> {
  divider?: React.ReactElement
}

const Stack = React.forwardRef<HTMLDivElement, StackProps>(
  ({ className, direction, spacing, align, justify, divider, children, ...props }, ref) => {
    const childrenArray = React.Children.toArray(children)

    return (
      <div
        ref={ref}
        className={cn(stackVariants({ direction, spacing, align, justify, className }))}
        {...props}
      >
        {divider
          ? childrenArray.map((child, index) => (
              <React.Fragment key={index}>
                {child}
                {index < childrenArray.length - 1 && divider}
              </React.Fragment>
            ))
          : children}
      </div>
    )
  }
)

Stack.displayName = "Stack"

const VStack = React.forwardRef<HTMLDivElement, StackProps>((props, ref) => (
  <Stack ref={ref} direction="vertical" {...props} />
))
VStack.displayName = "VStack"

const HStack = React.forwardRef<HTMLDivElement, StackProps>((props, ref) => (
  <Stack ref={ref} direction="horizontal" {...props} />
))
HStack.displayName = "HStack"

export { Stack, VStack, HStack, stackVariants }
