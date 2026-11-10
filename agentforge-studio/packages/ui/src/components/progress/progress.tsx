import * as React from "react";
import * as ProgressPrimitive from "@radix-ui/react-progress";
import { cn } from "../../lib/utils";

export interface ProgressProps
  extends React.ComponentPropsWithoutRef<typeof ProgressPrimitive.Root> {
  indeterminate?: boolean;
}

const Progress = React.forwardRef<
  React.ElementRef<typeof ProgressPrimitive.Root>,
  ProgressProps
>(({ className, value, indeterminate, ...props }, ref) => (
  <ProgressPrimitive.Root
    ref={ref}
    className={cn(
      "relative h-2 w-full overflow-hidden rounded-full bg-muted",
      className
    )}
    value={indeterminate ? null : value}
    aria-label={props["aria-label"] ?? "Progress"}
    aria-valuenow={indeterminate || value == null ? undefined : value}
    {...props}
  >
    <ProgressPrimitive.Indicator
      className={cn(
        "h-full bg-primary transition-all duration-normal ease-mac",
        indeterminate && "animate-progress-indeterminate w-1/3"
      )}
      style={
        indeterminate ? undefined : { transform: `translateX(-${100 - (value || 0)}%)` }
      }
    />
  </ProgressPrimitive.Root>
));
Progress.displayName = ProgressPrimitive.Root.displayName;

export { Progress };
