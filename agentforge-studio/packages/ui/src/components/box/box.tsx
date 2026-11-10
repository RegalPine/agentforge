import * as React from "react"
import { Slot } from "@radix-ui/react-slot"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "../../lib/utils"

const boxVariants = cva("", {
  variants: {
    p: {
      none: "p-0",
      xs: "p-1",
      sm: "p-2",
      md: "p-4",
      lg: "p-6",
      xl: "p-8",
    },
    m: {
      none: "m-0",
      xs: "m-1",
      sm: "m-2",
      md: "m-4",
      lg: "m-6",
      xl: "m-8",
    },
    bg: {
      transparent: "bg-transparent",
      background: "bg-background",
      muted: "bg-muted",
      primary: "bg-primary",
    },
    rounded: {
      none: "rounded-none",
      sm: "rounded-sm",
      md: "rounded-md",
      lg: "rounded-lg",
      xl: "rounded-xl",
      full: "rounded-full",
    },
  },
})

export interface BoxProps
  extends React.HTMLAttributes<HTMLElement>,
    VariantProps<typeof boxVariants> {
  as?: React.ElementType
  asChild?: boolean
}

const Box = React.forwardRef<HTMLElement, BoxProps>(
  ({ className, as: Component = "div", asChild, p, m, bg, rounded, ...props }, ref) => {
    const Comp = asChild ? Slot : Component

    return (
      <Comp
        ref={ref}
        className={cn(boxVariants({ p, m, bg, rounded, className }))}
        {...props}
      />
    )
  }
)

Box.displayName = "Box"

export { Box, boxVariants }
