import * as React from 'react';
import { cn } from '../../lib/utils';

export interface InlineCodeProps
  extends React.HTMLAttributes<HTMLElement> {
  /**
   * 代码内容
   */
  children: React.ReactNode;
  /**
   * 变体样式
   * @default 'default'
   */
  variant?: 'default' | 'subtle' | 'outline';
}

export const InlineCode = React.forwardRef<HTMLElement, InlineCodeProps>(
  ({ children, variant = 'default', className, ...props }, ref) => {
    return (
      <code
        ref={ref}
        className={cn(
          'relative rounded px-[0.5em] py-[0.25em] font-mono text-sm font-medium',
          'transition-colors',
          {
            // IDE style - surface container
            'bg-surface-container text-on-surface ring-1 ring-outline-variant/50':
              variant === 'default',
            // Subtle
            'bg-surface-container-low/50 text-outline': variant === 'subtle',
            // Outline
            'border border-outline-variant bg-transparent text-on-surface':
              variant === 'outline',
          },
          className
        )}
        {...props}
      >
        {children}
      </code>
    );
  }
);

InlineCode.displayName = 'InlineCode';
