import * as React from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { cn } from "../../lib/utils";

type DrawerSide = "left" | "right" | "top" | "bottom";

interface DrawerProps {
  /** 是否打开 */
  open?: boolean;
  /** 打开状态变化回调 */
  onOpenChange?: (open: boolean) => void;
  /** 是否显示遮罩（默认 true） */
  modal?: boolean;
  /** 子元素 */
  children: React.ReactNode;
}

interface DrawerContentProps
  extends React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content> {
  /** 抽屉方向 */
  side?: DrawerSide;
}

const Drawer: React.FC<DrawerProps> = ({ open, onOpenChange, modal = true, children }) => {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange} modal={modal}>
      {children}
    </DialogPrimitive.Root>
  );
};
Drawer.displayName = "Drawer";

const DrawerTrigger = DialogPrimitive.Trigger;
const DrawerClose = DialogPrimitive.Close;
const DrawerPortal = DialogPrimitive.Portal;

const DrawerOverlay = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Overlay
    ref={ref}
    className={cn(
      "fixed inset-0 z-50",
      "bg-black/40 backdrop-blur-sm",
      "data-[state=open]:animate-in data-[state=closed]:animate-out",
      "data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0",
      className
    )}
    {...props}
  />
));
DrawerOverlay.displayName = "DrawerOverlay";

const DrawerContent = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Content>,
  DrawerContentProps
>(({ side = "right", className, children, ...props }, ref) => {
  // 根据方向确定定位和动画类
  const sideClasses = {
    left: {
      position: "left-0 top-0 h-full",
      size: "w-[var(--drawer-width,320px)]",
      animation: [
        "data-[state=closed]:slide-out-to-left",
        "data-[state=open]:slide-in-from-left",
      ].join(" "),
    },
    right: {
      position: "right-0 top-0 h-full",
      size: "w-[var(--drawer-width,320px)]",
      animation: [
        "data-[state=closed]:slide-out-to-right",
        "data-[state=open]:slide-in-from-right",
      ].join(" "),
    },
    top: {
      position: "top-0 left-0 w-full",
      size: "h-[var(--drawer-height,320px)]",
      animation: [
        "data-[state=closed]:slide-out-to-top",
        "data-[state=open]:slide-in-from-top",
      ].join(" "),
    },
    bottom: {
      position: "bottom-0 left-0 w-full",
      size: "h-[var(--drawer-height,320px)]",
      animation: [
        "data-[state=closed]:slide-out-to-bottom",
        "data-[state=open]:slide-in-from-bottom",
      ].join(" "),
    },
  };

  const config = sideClasses[side];

  return (
    <DrawerPortal>
      <DrawerOverlay />
      <DialogPrimitive.Content
        ref={ref}
        className={cn(
          // 基础样式
          "fixed z-50",
          "bg-background",
          "shadow-mac-window",
          "border border-border/50",
          // 定位和尺寸
          config.position,
          config.size,
          // 动画
          "duration-normal ease-mac",
          "data-[state=open]:animate-in data-[state=closed]:animate-out",
          config.animation,
          className
        )}
        {...props}
      >
        {children}
        <DialogPrimitive.Close
          className={cn(
            "absolute right-4 top-4",
            "rounded-full p-1",
            "text-muted-foreground hover:text-foreground",
            "hover:bg-muted",
            "transition-colors duration-fast",
            "focus:outline-none focus:ring-2 focus:ring-primary"
          )}
        >
          <X className="h-4 w-4" />
          <span className="sr-only">Close</span>
        </DialogPrimitive.Close>
      </DialogPrimitive.Content>
    </DrawerPortal>
  );
});
DrawerContent.displayName = "DrawerContent";

const DrawerHeader = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn(
      "flex flex-col space-y-1.5 px-6 pt-6 pb-4 border-b border-border/50",
      className
    )}
    {...props}
  />
);
DrawerHeader.displayName = "DrawerHeader";

const DrawerFooter = ({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn(
      "flex justify-end gap-2 px-6 py-4 border-t border-border/50 bg-muted/30",
      className
    )}
    {...props}
  />
);
DrawerFooter.displayName = "DrawerFooter";

const DrawerTitle = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title
    ref={ref}
    className={cn("text-lg font-semibold text-foreground", className)}
    {...props}
  />
));
DrawerTitle.displayName = "DrawerTitle";

const DrawerDescription = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description
    ref={ref}
    className={cn("text-sm text-muted-foreground", className)}
    {...props}
  />
));
DrawerDescription.displayName = "DrawerDescription";

export {
  Drawer,
  DrawerTrigger,
  DrawerClose,
  DrawerContent,
  DrawerHeader,
  DrawerFooter,
  DrawerTitle,
  DrawerDescription,
  type DrawerProps,
  type DrawerContentProps,
  type DrawerSide,
};
